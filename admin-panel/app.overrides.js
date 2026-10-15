/* Overrides: fix modal IDs + validCategory payload after app.bundle.js loads */
(function () {
  function $(s) { return document.querySelector(s); }

  function rewire() {
    const addZekr = $("#add-zekr");
    if (addZekr && !addZekr.dataset.wired) {
      addZekr.dataset.wired = "1";
      addZekr.addEventListener("click", () => {
        const legacy = $("#btn-add");
        if (legacy) legacy.click();
        else {
          const modal = $("#editor-modal");
          if ($("#modal-title")) $("#modal-title").textContent = "إضافة ذكر";
          if ($("#field-text")) $("#field-text").value = "";
          if ($("#field-repeat")) $("#field-repeat").value = "1";
          if ($("#field-fadl")) $("#field-fadl").value = "";
          if ($("#field-source")) $("#field-source").value = "";
          if ($("#field-published")) $("#field-published").checked = true;
          modal?.classList.add("open");
          modal?.setAttribute("aria-hidden", "false");
        }
      });
    }

    const saveZekr = $("#save-zekr");
    if (saveZekr && !saveZekr.dataset.wired) {
      saveZekr.dataset.wired = "1";
      saveZekr.addEventListener("click", () => {
        const legacy = $("#save-editor");
        if (legacy) legacy.click();
      });
    }

    const addCat = $("#add-category");
    if (addCat && !addCat.dataset.wired) {
      addCat.dataset.wired = "1";
      addCat.addEventListener("click", () => openCategoryEditorFixed(null));
    }

    const saveCat = $("#save-category");
    if (saveCat && !saveCat.dataset.wired) {
      saveCat.dataset.wired = "1";
      const fresh = saveCat.cloneNode(true);
      saveCat.parentNode.replaceChild(fresh, saveCat);
      fresh.dataset.wired = "1";
      fresh.addEventListener("click", () => {
        saveCategoryFixed().catch((e) => {
          const t = $("#toast");
          if (t) {
            t.textContent = e.message || "تعذر الحفظ";
            t.classList.add("show");
            setTimeout(() => t.classList.remove("show"), 2400);
          }
        });
      });
    }

    ["#close-modal", "#cancel-modal"].forEach((sel) => {
      const el = $(sel);
      if (el && !el.dataset.wired) {
        el.dataset.wired = "1";
        el.addEventListener("click", () => {
          const m = $("#editor-modal");
          m?.classList.remove("open");
          m?.setAttribute("aria-hidden", "true");
        });
      }
    });
    ["#close-cat-modal", "#cancel-cat-modal"].forEach((sel) => {
      const el = $(sel);
      if (el && !el.dataset.wired) {
        el.dataset.wired = "1";
        el.addEventListener("click", () => {
          const m = $("#category-modal");
          m?.classList.remove("open");
          m?.setAttribute("aria-hidden", "true");
        });
      }
    });
  }

  function openCategoryEditorFixed(id) {
    const titleEl = $("#cat-modal-title");
    if (titleEl) titleEl.textContent = id ? "تعديل التصنيف" : "إضافة تصنيف";
    const idEl = $("#cat-id");
    if (idEl) {
      idEl.value = id || "";
      idEl.disabled = !!id;
    }
    if ($("#cat-title")) $("#cat-title").value = "";
    if ($("#cat-subtitle")) $("#cat-subtitle").value = "";
    if ($("#cat-order")) $("#cat-order").value = "100";
    if ($("#cat-icon")) $("#cat-icon").value = "star";
    if ($("#cat-published")) $("#cat-published").checked = true;
    if (id) {
      const card = document.querySelector('[data-cat-id="' + id.replace(/"/g, '') + '"]');
      if (card) {
        const h3 = card.querySelector("h3");
        const p = card.querySelector("p");
        if (h3 && $("#cat-title")) $("#cat-title").value = h3.textContent || "";
        if (p && $("#cat-subtitle")) $("#cat-subtitle").value = p.textContent || "";
      }
    }
    const modal = $("#category-modal");
    modal?.classList.add("open");
    modal?.setAttribute("aria-hidden", "false");
    window.__editingCategoryId = id || null;
  }

  document.addEventListener(
    "click",
    (e) => {
      const btn = e.target.closest("[data-edit-cat]");
      if (!btn) return;
      e.preventDefault();
      e.stopPropagation();
      openCategoryEditorFixed(btn.getAttribute("data-edit-cat"));
    },
    true
  );

  async function saveCategoryFixed() {
    if (!window.firebase || !firebase.apps.length) throw new Error("Firebase غير جاهز");
    const db = firebase.firestore();
    const idRaw = ($("#cat-id")?.value || "").trim().toLowerCase();
    const title = ($("#cat-title")?.value || "").trim();
    if (!idRaw || !title) throw new Error("أدخل المعرّف والعنوان");
    if (!/^[a-z0-9_-]{2,30}$/.test(idRaw) || idRaw.startsWith("wird_")) {
      throw new Error("المعرّف: 2–30 حرفاً إنجليزياً صغيراً أو رقماً أو _ أو -");
    }
    const reserved = ["sabah", "masaa", "sleep", "travel", "worry", "rizq", "illness", "waking", "favorites"];
    if (reserved.includes(idRaw)) throw new Error("هذا المعرّف محجوز لتصنيف مدمج");
    const allowedIcons = ["sun", "moon", "bed", "car", "heart", "book", "star", "home", "food", "rain", "shield", "mosque"];
    const iconRaw = ($("#cat-icon")?.value || "star").trim();
    const icon = allowedIcons.includes(iconRaw) ? iconRaw : "star";
    const subtitle = (($("#cat-subtitle")?.value || "").trim()).slice(0, 80);
    const orderNum = Number($("#cat-order")?.value);
    const payload = {
      title: title.slice(0, 40),
      subtitle,
      icon,
      order: Number.isFinite(orderNum) ? orderNum : 100,
      published: !!$("#cat-published")?.checked,
      updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
    };
    const col = db.collection("content").doc("categories").collection("items");
    const editingId = window.__editingCategoryId;
    if (editingId) {
      await col.doc(editingId).set(payload);
    } else {
      payload.createdAt = firebase.firestore.FieldValue.serverTimestamp();
      await col.doc(idRaw).set(payload);
    }
    const modal = $("#category-modal");
    modal?.classList.remove("open");
    modal?.setAttribute("aria-hidden", "true");
    window.__editingCategoryId = null;
    const t = $("#toast");
    if (t) {
      t.textContent = "تم حفظ التصنيف";
      t.classList.add("show");
      setTimeout(() => t.classList.remove("show"), 2400);
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => setTimeout(rewire, 500));
  } else {
    setTimeout(rewire, 500);
  }
  setInterval(rewire, 2000);
})();
