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
    { id: "sabah", icon: "sun", name: "أذكار الصباح", title: "أذكار الصباح", desc: "ورد الصباح", subtitle: "ورد الصباح", builtin: true },
    { id: "masaa", icon: "moon", name: "أذكار المساء", title: "أذكار المساء", desc: "ورد المساء", subtitle: "ورد المساء", builtin: true },
    { id: "sleep", icon: "bed", name: "أذكار النوم", title: "أذكار النوم", desc: "طمأنينة قبل النوم", subtitle: "طمأنينة قبل النوم", builtin: true },
    { id: "travel", icon: "car", name: "أذكار السفر", title: "أذكار السفر", desc: "حفظ وأمان", subtitle: "حفظ وأمان", builtin: true },
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
      .replace(/&/g, "&")
      .replace(/</g, "<")
      .replace(/>/g, ">")
      .replace(/"/g, """)
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

  // ... REST OF FILE IS IN LOCAL - use push_files for full
  console.error("BUNDLE INCOMPLETE - re-push required");
})();
