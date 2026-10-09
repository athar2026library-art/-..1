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
