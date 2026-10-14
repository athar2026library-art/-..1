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
