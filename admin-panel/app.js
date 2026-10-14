/* الباقيات — لوحة المالك. ملف مدمج من app.part01..24. لا تحمّل الأجزاء من المتصفح. */
/* لوحة الباقيات — Google Auth + Firestore RBAC. لا كلمة مرور ثابتة. */
(function () {
  const $ = (s) => document.querySelector(s);
  const $$ = (s) => document.querySelectorAll(s);

  const CATEGORY_LABEL = {
    sabah: "الصباح",
    masaa: "المساء",
    sleep: "النوم",
    travel: "السفر",
    worry: "الهم والقلق",
    rizq: "الرزق",
    illness: "المرض والشفاء",
    waking: "الاستيقاظ",
  };

  const BUILTIN_CATEGORIES = [
    { id: "sabah", icon: "☀", name: "أذكار الصباح", desc: "ورد الصباح", builtin: true },
    { id: "masaa", icon: "☾", name: "أذكار المساء", desc: "ورد المساء", builtin: true },
    { id: "sleep", icon: "☾", name: "أذكار النوم", desc: "طمأنينة قبل النوم", builtin: true },
    { id: "travel", icon: "✈", name: "أذكار السفر", desc: "حفظ وأمان", builtin: true },
  ];

  let db = null;
  let auth = null;
  let currentUser = null;
  let adminRole = "";
  let azkar = [];
  let categories = [...BUILTIN_CATEGORIES];
  let feedbackItems = [];
  let selectedFeedbackId = null;
  let editingId = null;
  let editingCategoryId = null;
  let lastNotifyAt = 0;
  let unsubAzkar = null;
  let unsubFeedback = null;
  let unsubCategories = null;

  function escapeHtml(value) {
    return String(value ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function toast(msg) {
    const el = $("#toast");
    if (!el) return;
    el.textContent = msg;
    el.classList.add("show");
    setTimeout(() => el.classList.remove("show"), 2400);
  }

  function go(view) {
    $$(".view").forEach((x) => x.classList.remove("active-view"));
    $("#" + view)?.classList.add("active-view");
    $$(".nav-item").forEach((x) => x.classList.toggle("active", x.dataset.view === view));
    const titles = {
      overview: "نظرة عامة",
      azkar: "مكتبة الأذكار",
      feedback: "صندوق الرسائل",
      notify: "إشعار عام",
      categories: "التصنيفات",
      settings: "إعدادات وأمان",
    };
    if ($("#page-title")) $("#page-title").textContent = titles[view] || "لوحة التحكم";
  }

  function itemsCol() {
    return db.collection("content").doc("azkar").collection("items");
  }

  function categoriesCol() {
    return db.collection("content").doc("categories").collection("items");
  }

  function setSessionUi(loggedIn) {
    const gate = $("#auth-gate");
    const shell = $("#shell");
    if (loggedIn) {
      gate?.classList.add("hidden");
      if (gate) gate.style.display = "none";
      shell?.classList.add("ready");
    } else {
      gate?.classList.remove("hidden");
      if (gate) gate.style.display = "grid";
      shell?.classList.remove("ready");
    }
  }

  async function assertAdmin(user) {
    const token = await user.getIdTokenResult(true);

    // Keep the displayed role aligned with Firestore security rules:
    // the generic admin claim is not, by itself, a super-admin claim.
    if (token.claims.super_admin === true) {
      return { ok: true, role: "super_admin" };
    }
    if (token.claims.admin === true) {
      return { ok: true, role: "admin" };
    }

    const snap = await db.collection("admins").doc(user.uid).get();
    if (!snap.exists) return { ok: false, role: "" };
    const role = snap.data()?.role || "admin";
    return { ok: true, role };
  }

  function renderStats() {
    const published = azkar.filter((z) => z.published).length;
    if ($("#stat-total")) $("#stat-total").textContent = String(azkar.length);
    if ($("#stat-published")) $("#stat-published").textContent = String(published);
    if ($("#stat-draft")) $("#stat-draft").textContent = String(azkar.length - published);
    const unread = feedbackItems.filter((f) => f.status === "new" || !f.adminReply).length;
    if ($("#stat-feedback")) $("#stat-feedback").textContent = String(unread);
    const badge = $("#feedback-badge");
    if (badge) {
      badge.textContent = String(unread);
      badge.classList.toggle("hidden", unread === 0);
    }
  }

  const ALLOWED_ICONS = ["sun", "moon", "bed", "car", "heart", "book", "star", "home", "food", "rain", "shield", "mosque"];
  const ICON_EMOJI = {
    sun: "☀", moon: "☾", bed: "🛏", car: "🚗", heart: "♥", book: "📖",
    star: "★", home: "⌂", food: "🍽", rain: "🌧", shield: "🛡", mosque: "🕌",
  };
  const RESERVED_CATEGORY_IDS = ["sabah", "masaa", "sleep", "travel", "worry", "rizq", "illness", "waking", "favorites"];

  function categoryTitle(c) {
    return (c && (c.title || c.name)) || "";
  }
  function categorySubtitle(c) {
    return (c && (c.subtitle || c.desc)) || "";
  }
  function categoryIconKey(c) {
    const raw = (c && c.icon) || "star";
    return ALLOWED_ICONS.includes(raw) ? raw : "star";
  }

  function mergeCategories(remote) {
    const map = new Map();
    BUILTIN_CATEGORIES.forEach((c) => map.set(c.id, { ...c }));
    (remote || []).forEach((c) => {
      if (!c || !c.id) return;
      const title = categoryTitle(c) || c.id;
      map.set(c.id, {
        id: c.id,
        icon: c.icon || "star",
        name: title,
        title,
        desc: categorySubtitle(c),
        subtitle: categorySubtitle(c),
        order: c.order ?? 100,
        published: c.published !== false,
        builtin: !!c.builtin || BUILTIN_CATEGORIES.some((b) => b.id === c.id),
        createdAt: c.createdAt || null,
      });
    });
    categories = Array.from(map.values()).sort((a, b) => (a.order ?? 0) - (b.order ?? 0));
    categories.forEach((c) => {
      CATEGORY_LABEL[c.id] = categoryTitle(c) || c.id;
    });
  }

  function renderCategories() {
    const grid = $("#category-grid");
    if (!grid) return;
    grid.innerHTML = categories
      .map((c) => {
        const count = azkar.filter((z) => z.category === c.id).length;
        const badge = c.builtin ? '<span class="pill">مدمج</span>' : '<span class="pill status-published">ديناميكي</span>';
        const title = categoryTitle(c) || c.id;
        const subtitle = categorySubtitle(c);
        const iconKey = categoryIconKey(c);
        return `<article class="category-card" data-cat-id="${escapeHtml(c.id)}">
        <div class="symbol">${escapeHtml(ICON_EMOJI[iconKey] || "★")}</div>
        <h3>${escapeHtml(title)}</h3>
        <p>${escapeHtml(subtitle)}</p>
        <p style="margin-top:12px;color:var(--green)">${count} أذكار · ${badge}</p>
        <div class="modal-actions" style="margin-top:12px">
          <button class="icon-btn" data-edit-cat="${escapeHtml(c.id)}">تعديل</button>
          ${c.builtin ? "" : `<button class="icon-btn" data-del-cat="${escapeHtml(c.id)}">حذف</button>`}
        </div>
      </article>`;
      })
      .join("");

    $$("[data-edit-cat]").forEach((btn) =>
      btn.addEventListener("click", () => openCategoryEditor(btn.dataset.editCat))
    );
    $$("[data-del-cat]").forEach((btn) =>
      btn.addEventListener("click", () => deleteCategory(btn.dataset.delCat))
    );

    // update select options in azkar editor
    const sel = $("#field-category");
    if (sel) {
      const current = sel.value;
      sel.innerHTML = categories
        .map((c) => `<option value="${escapeHtml(c.id)}">${escapeHtml(categoryTitle(c) || c.id)}</option>`)
        .join("");
      if (categories.some((c) => c.id === current)) sel.value = current;
    }
    const filter = $("#filter");
    if (filter) {
      const cur = filter.value;
      filter.innerHTML =
        '<option value="all">الكل</option>' +
        categories.map((c) => `<option value="${escapeHtml(c.id)}">${escapeHtml(categoryTitle(c) || c.id)}</option>`).join("");
      if (cur === "all" || categories.some((c) => c.id === cur)) filter.value = cur;
    }
  }

  function setField(selector, value) {
    const el = $(selector);
    if (!el) return null;
    if ("value" in el) el.value = value ?? "";
    return el;
  }

  function openCategoryEditor(id) {
    editingCategoryId = id || null;
    const c = id ? categories.find((x) => x.id === id) : null;
    const titleEl = $("#cat-modal-title");
    if (titleEl) titleEl.textContent = c ? "تعديل التصنيف" : "إضافة تصنيف";
    const idEl = setField("#cat-id", c?.id || "");
    if (idEl) idEl.disabled = !!c;
    setField("#cat-title", categoryTitle(c));
    const iconEl = setField("#cat-icon", categoryIconKey(c));
    if (iconEl && !ALLOWED_ICONS.includes(iconEl.value)) iconEl.value = "star";
    setField("#cat-subtitle", categorySubtitle(c));
    setField("#cat-order", String(c?.order ?? 100));
    const published = $("#cat-published");
    if (published) published.checked = c ? c.published !== false : true;
    const modal = $("#category-modal");
    modal?.classList.add("open");
    modal?.setAttribute("aria-hidden", "false");
  }

  function closeCategoryEditor() {
    const modal = $("#category-modal");
    modal?.classList.remove("open");
    modal?.setAttribute("aria-hidden", "true");
    editingCategoryId = null;
  }

  async function saveCategory() {
    const idRaw = ($("#cat-id")?.value || "").trim().toLowerCase();
    const title = ($("#cat-title")?.value || "").trim();
    if (!idRaw || !title) {
      toast("أدخل المعرّف والعنوان");
      return;
    }
    if (!/^[a-z0-9_-]{2,30}$/.test(idRaw) || idRaw.startsWith("wird_")) {
      toast("المعرّف: 2–30 حرفاً إنجليزياً صغيراً أو رقماً أو _ أو -");
      return;
    }
    if (RESERVED_CATEGORY_IDS.includes(idRaw)) {
      toast("هذا المعرّف محجوز لتصنيف مدمج ولا يُحفظ في Firestore");
      return;
    }
    const icon = ALLOWED_ICONS.includes($("#cat-icon")?.value) ? $("#cat-icon").value : "star";
    const subtitle = ($("#cat-subtitle")?.value || "").trim().slice(0, 80);
    const order = Number($("#cat-order")?.value);
    const payload = {
      title: title.slice(0, 40),
      subtitle,
      icon,
      order: Number.isFinite(order) ? order : 100,
      published: !!$("#cat-published")?.checked,
      updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
    };
    if (editingCategoryId) {
      const existing = categories.find((c) => c.id === editingCategoryId);
      if (existing?.createdAt) payload.createdAt = existing.createdAt;
      await categoriesCol().doc(editingCategoryId).set(payload);
      toast("تم حفظ التصنيف");
    } else {
      const exists = categories.some((c) => c.id === idRaw);
      if (exists) {
        toast("المعرّف موجود مسبقاً");
        return;
      }
      await categoriesCol().doc(idRaw).set({
        ...payload,
        createdAt: firebase.firestore.FieldValue.serverTimestamp(),
      });
      toast("تمت إضافة التصنيف");
    }
    closeCategoryEditor();
  }
  async function deleteCategory(id) {
    if (!id || BUILTIN_CATEGORIES.some((b) => b.id === id)) {
      toast("لا يمكن حذف التصنيفات المدمجة");
      return;
    }
    const count = azkar.filter((z) => z.category === id).length;
    if (count > 0) {
      toast(`لا يمكن الحذف: يوجد ${count} ذكر مرتبط`);
      return;
    }
    if (!confirm("حذف هذا التصنيف؟")) return;
    await categoriesCol().doc(id).delete();
    toast("تم حذف التصنيف");
  }

  function listenCategories() {
    if (unsubCategories) unsubCategories();
    unsubCategories = categoriesCol().onSnapshot(
      (snap) => {
        const remote = snap.docs.map((doc) => {
          const d = doc.data() || {};
          return { id: doc.id, ...d };
        });
        mergeCategories(remote);
        renderCategories();
        renderTable();
      },
      (err) => {
        console.warn("categories listen", err);
        mergeCategories([]);
        renderCategories();
      }
    );
  }

  function renderTable() {
    const q = ($("#search")?.value || "").trim();
    const filter = $("#filter")?.value || "all";
    const status = $("#status-filter")?.value || "all";
    const rows = azkar
      .slice()
      .sort((a, b) => (a.order ?? 0) - (b.order ?? 0))
      .filter(
        (z) =>
          (filter === "all" || z.category === filter) &&
          (status === "all" || (status === "published" ? z.published : !z.published)) &&
          ((z.text || "") + (z.source || "")).includes(q)
      );

    const tbody = $("#azkar-table");
    if (!tbody) return;
    if (!rows.length) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:20px">لا توجد أذكار مطابقة.</td></tr>';
      return;
    }

    tbody.innerHTML = rows
      .map(
        (z) => `<tr class="draggable-row" draggable="true" data-id="${escapeHtml(z.id)}">
      <td class="order-cell"><span class="drag-handle" title="اسحب لإعادة الترتيب">⋮⋮</span><span class="order-num">${z.order ?? 0}</span></td>
      <td>${escapeHtml((z.text || "").slice(0, 90))}${(z.text || "").length > 90 ? "…" : ""}</td>
      <td><span class="pill">${escapeHtml(CATEGORY_LABEL[z.category] || z.category)}</span></td>
      <td>${Number(z.repeat) || 1}×</td>
      <td><span class="pill ${z.published ? "status-published" : "status-draft"}">${z.published ? "منشور" : "مسودة"}</span></td>
      <td>${escapeHtml(z.source || "—")}</td>
      <td>
        <button class="icon-btn" data-preview="${escapeHtml(z.id)}">معاينة</button> ·
        <button class="icon-btn" data-edit="${escapeHtml(z.id)}">تعديل</button> ·
        <button class="icon-btn" data-dup="${escapeHtml(z.id)}">نسخ</button> ·
        <button class="icon-btn" data-archive="${escapeHtml(z.id)}">${z.published ? "إخفاء" : "نشر"}</button>
      </td>
    </tr>`
      )
      .join("");

    bindTableActions();
    bindDrag();
  }

  function bindTableActions() {
    $$("[data-preview]").forEach((btn) =>
      btn.addEventListener("click", () => openPreview(btn.dataset.preview))
    );
    $$("[data-edit]").forEach((btn) =>
      btn.addEventListener("click", () => openEditor(btn.dataset.edit))
    );
    $$("[data-dup]").forEach((btn) =>
      btn.addEventListener("click", () => duplicateZekr(btn.dataset.dup))
    );
    $$("[data-archive]").forEach((btn) =>
      btn.addEventListener("click", () => togglePublished(btn.dataset.archive))
    );
  }

  function bindDrag() {
    const rows = $$("#azkar-table tr.draggable-row");
    let dragId = null;
    rows.forEach((row) => {
      row.addEventListener("dragstart", () => {
        dragId = row.dataset.id;
        row.classList.add("dragging");
      });
      row.addEventListener("dragend", () => row.classList.remove("dragging"));
      row.addEventListener("dragover", (e) => {
        e.preventDefault();
        row.classList.add("drag-over");
      });
      row.addEventListener("dragleave", () => row.classList.remove("drag-over"));
      row.addEventListener("drop", async (e) => {
        e.preventDefault();
        row.classList.remove("drag-over");
        const targetId = row.dataset.id;
        if (!dragId || dragId === targetId) return;
        await reorder(dragId, targetId);
      });
    });
  }

  async function reorder(fromId, toId) {
    const sorted = azkar.slice().sort((a, b) => (a.order ?? 0) - (b.order ?? 0));
    const fromIdx = sorted.findIndex((z) => z.id === fromId);
    const toIdx = sorted.findIndex((z) => z.id === toId);
    if (fromIdx < 0 || toIdx < 0) return;
    const [moved] = sorted.splice(fromIdx, 1);
    sorted.splice(toIdx, 0, moved);
    const batch = db.batch();
    sorted.forEach((z, i) => {
      batch.update(itemsCol().doc(z.id), { order: i + 1 });
    });
    await batch.commit();
    toast("تم حفظ الترتيب");
  }

  function listenAzkar() {
    if (unsubAzkar) unsubAzkar();
    unsubAzkar = itemsCol().onSnapshot(
      (snap) => {
        azkar = snap.docs.map((doc) => {
          const d = doc.data() || {};
          return {
            id: doc.id,
            text: d.text || "",
            category: d.category || "sabah",
            repeat: d.repeat || 1,
            fadl: d.fadl || "",
            source: d.source || "",
            published: d.published !== false,
            order: d.order ?? 0,
            numericId: d.id ?? 0,
          };
        });
        renderTable();
        renderStats();
        renderCategories();
      },
      (err) => toast("تعذر تحميل الأذكار: " + err.message)
    );
  }

  function listenFeedback() {
    if (unsubFeedback) unsubFeedback();
    unsubFeedback = db
      .collection("feedback")
      .orderBy("createdAt", "desc")
      .limit(100)
      .onSnapshot(
        (snap) => {
          feedbackItems = snap.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
          renderFeedbackList();
          renderStats();
        },
        () => {
          db.collection("feedback")
            .limit(100)
            .get()
            .then((snap) => {
              feedbackItems = snap.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
              renderFeedbackList();
              renderStats();
            })
            .catch((err) => toast("تعذر تحميل الرسائل: " + err.message));
        }
      );
  }

  function renderFeedbackList() {
    const list = $("#feedback-list");
    if (!list) return;
    if (!feedbackItems.length) {
      list.innerHTML = '<p class="empty-hint">لا رسائل بعد.</p>';
      return;
    }
    list.innerHTML = feedbackItems
      .map((f) => {
        const active = f.id === selectedFeedbackId ? " active" : "";
        const title = f.title || f.type || "رسالة";
        const preview = (f.message || "").slice(0, 80);
        return `<button class="feedback-item${active}" data-fid="${escapeHtml(f.id)}">
          <strong>${escapeHtml(title)}</strong>
          <small>${escapeHtml(f.userEmail || f.userId || "")} · ${escapeHtml(f.status || "new")}</small>
          <span>${escapeHtml(preview)}</span>
        </button>`;
      })
      .join("");
    $$("[data-fid]").forEach((btn) =>
      btn.addEventListener("click", () => {
        selectedFeedbackId = btn.dataset.fid;
        renderFeedbackList();
        renderFeedbackDetail();
      })
    );
    if (selectedFeedbackId) renderFeedbackDetail();
  }

  function renderFeedbackDetail() {
    const box = $("#feedback-detail");
    if (!box) return;
    const f = feedbackItems.find((x) => x.id === selectedFeedbackId);
    if (!f) {
      box.innerHTML = '<p class="empty-hint">اختر رسالة من القائمة.</p>';
      return;
    }
    box.innerHTML = `
      <p class="eyebrow">${escapeHtml(f.type || "رسالة")}</p>
      <h3>${escapeHtml(f.title || "بدون عنوان")}</h3>
      <small>${escapeHtml(f.userEmail || f.userId || "")}</small>
      <div class="feedback-body">${escapeHtml(f.message || "")}</div>
      ${f.adminReply ? `<div class="admin-reply"><strong>الرد السابق</strong><p>${escapeHtml(f.adminReply)}</p></div>` : ""}
      <label>الرد
        <textarea id="reply-text" rows="4" placeholder="اكتب الرد للمستخدم">${escapeHtml(f.adminReply || "")}</textarea>
      </label>
      <label>الحالة
        <select id="reply-status">
          <option value="new"${f.status === "new" ? " selected" : ""}>جديدة</option>
          <option value="in_progress"${f.status === "in_progress" ? " selected" : ""}>قيد المعالجة</option>
          <option value="replied"${f.status === "replied" ? " selected" : ""}>تم الرد</option>
          <option value="closed"${f.status === "closed" ? " selected" : ""}>مغلقة</option>
        </select>
      </label>
      <div class="modal-actions">
        <button class="primary" id="save-reply">حفظ الرد</button>
      </div>`;
    $("#save-reply")?.addEventListener("click", saveReply);
  }

  async function saveReply() {
    if (!selectedFeedbackId) return;
    const reply = $("#reply-text")?.value.trim() || "";
    const status = $("#reply-status")?.value || "replied";
    const f = feedbackItems.find((x) => x.id === selectedFeedbackId);
    await db.collection("feedback").doc(selectedFeedbackId).update({
      adminReply: reply,
      status,
      replyUnread: true,
      updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
    });
    if (f?.userId) {
      try {
        await db
          .collection("users")
          .doc(f.userId)
          .collection("feedback")
          .doc(selectedFeedbackId)
          .set(
            {
              adminReply: reply,
              status,
              replyUnread: true,
              updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
            },
            { merge: true }
          );
      } catch (_e) {
        /* user copy is best-effort */
      }
    }
    toast("تم حفظ الرد");
  }

  function openEditor(id) {
    editingId = id || null;
    const z = id ? azkar.find((x) => x.id === id) : null;
    const titleEl = $("#modal-title");
    if (titleEl) titleEl.textContent = z ? "تعديل الذكر" : "إضافة ذكر";
    setField("#field-text", z?.text || "");
    setField("#field-category", z?.category || "sabah");
    const catEl = $("#field-category");
    if (catEl && z?.category && ![...catEl.options].some((o) => o.value === z.category)) {
      catEl.value = catEl.options[0]?.value || "sabah";
    }
    setField("#field-repeat", String(z?.repeat || 1));
    setField("#field-fadl", z?.fadl || "");
    setField("#field-source", z?.source || "");
    setField("#field-audio-url", z?.audioUrl || "");
    setField("#field-audio-mishary", z?.audioUrlsByReciter?.mishary || "");
    setField("#field-audio-abdulbasit", z?.audioUrlsByReciter?.abdulbasit || "");
    setField("#field-audio-maher", z?.audioUrlsByReciter?.maher || "");
    setField("#field-audio-yasser", z?.audioUrlsByReciter?.yasser || "");
    const published = $("#field-published");
    if (published) published.checked = z ? !!z.published : true;
    const modal = $("#editor-modal");
    modal?.classList.add("open");
    modal?.setAttribute("aria-hidden", "false");
  }

  function closeEditor() {
    const modal = $("#editor-modal");
    modal?.classList.remove("open");
    modal?.setAttribute("aria-hidden", "true");
    editingId = null;
  }

  async function saveEditor() {
    const text = $("#field-text")?.value.trim();
    if (!text) {
      toast("يرجى كتابة نص الذكر");
      return;
    }
    const audioUrl = $("#field-audio-url")?.value.trim() || "";
    const audioUrlsByReciter = Object.fromEntries(
      [
        ["mishary", $("#field-audio-mishary")?.value.trim() || ""],
        ["abdulbasit", $("#field-audio-abdulbasit")?.value.trim() || ""],
        ["maher", $("#field-audio-maher")?.value.trim() || ""],
        ["yasser", $("#field-audio-yasser")?.value.trim() || ""],
      ].filter(([, url]) => url)
    );
    const suppliedAudioUrls = [audioUrl, ...Object.values(audioUrlsByReciter)];
    if (suppliedAudioUrls.filter(Boolean).some((url) => !/^https:\/\/\S+$/i.test(url))) {
      toast("روابط الصوت يجب أن تبدأ بـ HTTPS وألا تحتوي مسافات.");
      return;
    }
    const repeat = Math.min(1000, Math.max(1, Math.round(Number($("#field-repeat")?.value) || 1)));
    const category = $("#field-category")?.value || "sabah";
    const known = ["sabah", "masaa", "sleep", "travel", "worry", "rizq", "illness", "waking"];
    if (!known.includes(category) && !categories.some((c) => c.id === category && !c.builtin)) {
      toast("التصنيف غير موجود في Firestore. أضفه أولاً من شاشة التصنيفات.");
      return;
    }
    const payload = {
      text: text.slice(0, 3999),
      category,
      repeat,
      fadl: ($("#field-fadl")?.value || "").trim().slice(0, 3999),
      source: ($("#field-source")?.value || "").trim().slice(0, 499),
      audioUrl,
      audioUrlsByReciter,
      published: !!$("#field-published")?.checked,
      updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
    };
    if (editingId) {
      await itemsCol().doc(editingId).set(payload, { merge: true });
      toast("تم حفظ التعديل");
    } else {
      const maxOrder = azkar.reduce((m, z) => Math.max(m, Number(z.order) || 0), 0);
      const maxId = azkar.reduce((m, z) => Math.max(m, Number(z.numericId) || 0), 0);
      await itemsCol().add({
        ...payload,
        id: maxId + 1,
        order: maxOrder + 1,
        createdAt: firebase.firestore.FieldValue.serverTimestamp(),
      });
      toast("تمت إضافة الذكر");
    }
    closeEditor();
  }

  async function duplicateZekr(id) {
    const z = azkar.find((x) => x.id === id);
    if (!z) return;
    const maxOrder = azkar.reduce((m, item) => Math.max(m, Number(item.order) || 0), 0);
    const maxId = azkar.reduce((m, item) => Math.max(m, Number(item.numericId) || 0), 0);
    await itemsCol().add({
      text: z.text,
      category: z.category,
      repeat: z.repeat,
      fadl: z.fadl,
      source: z.source,
      published: false,
      id: maxId + 1,
      order: maxOrder + 1,
      createdAt: firebase.firestore.FieldValue.serverTimestamp(),
    });
    toast("تم نسخ الذكر كمسودة");
  }

  async function togglePublished(id) {
    const z = azkar.find((x) => x.id === id);
    if (!z) return;
    await itemsCol().doc(id).update({
      published: !z.published,
      updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
    });
    toast(z.published ? "أُخفي الذكر" : "نُشر الذكر");
  }

  function openPreview(id) {
    const z = (id && azkar.find((x) => x.id === id)) || azkar[0];
    if (!z) {
      toast("لا يوجد ذكر للمعاينة");
      return;
    }
    $("#preview-meta").textContent = `${CATEGORY_LABEL[z.category] || z.category} · ${z.repeat || 1}×`;
    $("#preview-zekr").textContent = z.text || "";
    $("#preview-fadl").textContent = z.fadl || "—";
    $("#preview-source").textContent = z.source || "—";
    const modal = $("#preview-modal");
    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
  }

  function closePreview() {
    const modal = $("#preview-modal");
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
  }

  function exportJson() {
    const blob = new Blob([JSON.stringify(azkar, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "baqiyat-azkar.json";
    a.click();
    URL.revokeObjectURL(url);
    toast("تم تصدير المكتبة");
  }

  async function sendNotify() {
    const now = Date.now();
    if (now - lastNotifyAt < 60000) {
      toast("انتظر دقيقة قبل إرسال إشعار آخر");
      return;
    }
    const title = $("#notify-title")?.value.trim();
    const body = $("#notify-body")?.value.trim();
    if (!title || !body) {
      toast("أدخل العنوان والنص");
      return;
    }
    await db.collection("notifications").add({
      title,
      body,
      status: "queued",
      audience: "all",
      createdBy: currentUser?.uid || "",
      createdAt: firebase.firestore.FieldValue.serverTimestamp(),
    });
    lastNotifyAt = now;
    $("#notify-title").value = "";
    $("#notify-body").value = "";
    toast("أُضيفت مهمة الإشعار");
  }

  async function startSession(user) {
    const check = await assertAdmin(user);
    if (!check.ok) {
      await auth.signOut();
      $("#auth-message").textContent = "هذا الحساب غير مدرج في مجموعة المشرفين.";
      setSessionUi(false);
      return;
    }
    currentUser = user;
    adminRole = check.role;
    if ($("#admin-email")) $("#admin-email").textContent = user.email || user.uid;
    if ($("#admin-role-badge")) $("#admin-role-badge").textContent = "دور: " + adminRole;
    setSessionUi(true);
    listenAzkar();
    listenFeedback();
    listenCategories();
    renderCategories();
  }

  function stopSession() {
    currentUser = null;
    if (unsubAzkar) unsubAzkar();
    if (unsubFeedback) unsubFeedback();
    if (unsubCategories) unsubCategories();
    unsubAzkar = unsubFeedback = unsubCategories = null;
    setSessionUi(false);
  }

  function friendlyAuthError(error) {
    const code = error?.code || "";
    if (code.includes("auth/api-key-not-valid")) {
      return "إعداد Firebase للويب غير صالح. تأكد أنك فتحت اللوحة من نطاق المشروع الصحيح.";
    }
    if (code.includes("auth/unauthorized-domain")) {
      return "هذا النطاق غير مصرّح به في Firebase Auth. أضفه من إعدادات Authentication → Settings.";
    }
    if (code.includes("auth/popup-closed-by-user") || code.includes("auth/cancelled-popup-request")) {
      return "أُغلق نافذة تسجيل الدخول قبل الإكمال.";
    }
    if (code.includes("auth/popup-blocked")) {
      return "المتصفح منع النافذة المنبثقة. سيتم المحاولة عبر إعادة التوجيه.";
    }
    if (code.includes("auth/network-request-failed")) {
      return "تعذر الاتصال بالشبكة. تحقق من الاتصال ثم أعد المحاولة.";
    }
    return error?.message || "تعذر تسجيل الدخول.";
  }

  async function signInWithGoogle() {
    const provider = new firebase.auth.GoogleAuthProvider();
    const message = $("#auth-message");
    try {
      await auth.signInWithPopup(provider);
    } catch (e) {
      const code = e?.code || "";
      if (code.includes("auth/popup-blocked") || code.includes("auth/popup-closed-by-user")) {
        try {
          await auth.signInWithRedirect(provider);
          return;
        } catch (re) {
          if (message) message.textContent = friendlyAuthError(re);
          return;
        }
      }
      if (message) message.textContent = friendlyAuthError(e);
    }
  }

  function bindUi() {
    $$("button[data-view]").forEach((btn) =>
      btn.addEventListener("click", () => go(btn.dataset.view))
    );
    $("#logout-btn")?.addEventListener("click", () => auth?.signOut());
    $("#google-login-btn")?.addEventListener("click", () => signInWithGoogle());
    $("#search")?.addEventListener("input", renderTable);
    $("#filter")?.addEventListener("change", renderTable);
    $("#status-filter")?.addEventListener("change", renderTable);
    $("#add-zekr")?.addEventListener("click", () => openEditor(null));
    $("#preview-btn")?.addEventListener("click", () => openPreview(null));
    $("#add-category")?.addEventListener("click", () => openCategoryEditor(null));
    $("#save-zekr")?.addEventListener("click", () => saveEditor().catch((e) => toast(e.message || "تعذر الحفظ")));
    $("#close-modal")?.addEventListener("click", closeEditor);
    $("#cancel-modal")?.addEventListener("click", closeEditor);
    $("#save-category")?.addEventListener("click", () => saveCategory().catch((e) => toast(e.message || "تعذر حفظ التصنيف")));
    $("#close-cat-modal")?.addEventListener("click", closeCategoryEditor);
    $("#cancel-cat-modal")?.addEventListener("click", closeCategoryEditor);
    $("#close-preview")?.addEventListener("click", closePreview);
    $("#export-json")?.addEventListener("click", exportJson);
    $("#send-notify")?.addEventListener("click", () => sendNotify().catch((e) => toast(e.message || "تعذر الإرسال")));
    ["#editor-modal", "#category-modal", "#preview-modal"].forEach((sel) => {
      $(sel)?.addEventListener("click", (event) => {
        if (event.target !== event.currentTarget) return;
        if (sel === "#editor-modal") closeEditor();
        else if (sel === "#category-modal") closeCategoryEditor();
        else closePreview();
      });
    });
  }

  const initializeAdminPanel = async () => {
    if (window.firebaseHostingConfigPromise) {
      await window.firebaseHostingConfigPromise.catch(() => null);
    }
    const cfg = window.firebaseConfig || window.AZKAR_FIREBASE_CONFIG;
    if (!window.firebase || !cfg) {
      console.error("Firebase not loaded");
      const msg = document.querySelector("#auth-message");
      if (msg) {
        msg.textContent =
          "تعذر تحميل إعداد Firebase. افتح اللوحة من Firebase Hosting أو عرّف window.firebaseConfig.";
      }
      return;
    }
    if (!firebase.apps.length) {
      firebase.initializeApp(cfg);
    }
    db = firebase.firestore();
    auth = firebase.auth();
    bindUi();
    auth.onAuthStateChanged((user) => {
      if (user) startSession(user).catch((e) => toast(e.message));
      else stopSession();
    });
  };
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initializeAdminPanel, { once: true });
  } else {
    initializeAdminPanel();
  }
})();
