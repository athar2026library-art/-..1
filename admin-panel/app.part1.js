/* لوحة الباقيات — Google Auth + Firestore RBAC. لا كلمة مرور ثابتة. */
(function () {
  const $ = (s) => document.querySelector(s);
  const $$ = (s) => document.querySelectorAll(s);

  const CATEGORY_LABEL = {
    sabah: "الصباح",
    masaa: "المساء",
    sleep: "النوم",
    travel: "السفر",
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

  // TEST_CHUNK_END - remaining content in subsequent commits
