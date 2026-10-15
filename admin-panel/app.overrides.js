/* Safety overrides after app.bundle.js — ensure buttons and category save always work */
(function () {
  function $(s) { return document.querySelector(s); }
  function $$(s) { return document.querySelectorAll(s); }

  function toastMsg(msg) {
    const t = $("#toast");
    if (t) {
      t.textContent = msg;
      t.classList.add("show");
      setTimeout(() => t.classList.remove("show"), 2400);
    } else {
      console.log(msg);
    }
  }

  function openCategoryEditorFixed(id) {
    window.__editingCategoryId = id || null;
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
      const card = document.querySelector(`[data-cat-id="${CSS.escape(id)}"]`);
      if (card) {
        const h3 = card.querySelector("h3");
        const p = card.querySelector("p");
        if (h3 && $("#cat-title")) $("#cat-title").value = h3.textContent.trim();
        if (p && $("#cat-subtitle")) $("#cat-subtitle").value = p.textContent.trim();
      }
    }
    const modal = $("#category-modal");
    if (modal) {
      modal.classList.add("open");
      modal.setAttribute("aria-hidden", "false");
    }
  }

  async function saveCategoryFixed() {
    if (!window.firebase || !firebase.apps.length) throw new Error("Firebase غير جاهز");
    const db = firebase.firestore();
    const idRaw = ($("#cat-id")?.value || "").trim().toLowerCase();
    const title = ($("#cat-title")?.value || "").trim();
    if (!idRaw || !title) throw new Error("أدخل المعرّف والعنوان");
    if (!/^[a-z0-9_-]{2,30}$/.test(idRaw) || idRaw.startsWith("wird_")) {
      throw new Error("المعرّف: 2–30 حرفاً إنجليزياً صغيراً أو رقماً أو _ أو -");
    }
    const reserved = ["sabah","masaa","sleep","travel","worry","rizq","illness","waking","favorites"];
    if (reserved.includes(idRaw)) throw new Error("هذا المعرّف محجوز لتصنيف مدمج");
    const allowedIcons = ["sun","moon","bed","car","heart","book","star","home","food","rain","shield","mosque"];
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
    toastMsg("تم حفظ التصنيف");
  }

  function rewire() {
    const addZekr = $("#add-zekr");
    if (addZekr && !addZekr.dataset.wired) {
      addZekr.dataset.wired = "1";
      addZekr.addEventListener("click", () => {
        const modal = $("#editor-modal");
        if ($("#modal-title")) $("#modal-title").textContent = "إضافة ذكر";
        const legacy = $("#btn-add");
        if (legacy) { legacy.click(); return; }
        if (modal) {
          modal.classList.add("open");
          modal.setAttribute("aria-hidden", "false");
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

    let saveCat = $("#save-category");
    if (saveCat && !saveCat.dataset.wired) {
      const fresh = saveCat.cloneNode(true);
      saveCat.parentNode.replaceChild(fresh, saveCat);
      fresh.dataset.wired = "1";
      fresh.addEventListener("click", () => {
        saveCategoryFixed().catch((e) => toastMsg(e.message || "تعذر الحفظ"));
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
          window.__editingCategoryId = null;
        });
      }
    });

    $$("button[data-view]").forEach((btn) => {
      if (btn.dataset.wiredView) return;
      btn.dataset.wiredView = "1";
      btn.addEventListener("click", () => {
        const view = btn.dataset.view;
        $$(".view").forEach((v) => v.classList.remove("active-view"));
        $$(".nav-item").forEach((n) => n.classList.remove("active"));
        const target = document.getElementById(view);
        if (target) target.classList.add("active-view");
        btn.classList.add("active");
      });
    });
  }

  document.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-edit-cat]");
    if (!btn) return;
    e.preventDefault();
    e.stopPropagation();
    openCategoryEditorFixed(btn.getAttribute("data-edit-cat"));
  }, true);

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => setTimeout(rewire, 400));
  } else {
    setTimeout(rewire, 400);
  }
  setInterval(rewire, 3000);
})();
