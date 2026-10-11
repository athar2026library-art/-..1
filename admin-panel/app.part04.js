      badge.textContent = String(unread);
      badge.classList.toggle("hidden", unread === 0);
    }
  }

  function mergeCategories(remote) {
    const map = new Map();
    BUILTIN_CATEGORIES.forEach((c) => map.set(c.id, { ...c }));
    (remote || []).forEach((c) => {
      if (!c || !c.id) return;
      map.set(c.id, {
        id: c.id,
        icon: c.icon || "📖",
        name: c.name || c.id,
        desc: c.desc || "",
        order: c.order ?? 100,
        published: c.published !== false,
        builtin: !!c.builtin || BUILTIN_CATEGORIES.some((b) => b.id === c.id),
      });
    });
    categories = Array.from(map.values()).sort((a, b) => (a.order ?? 0) - (b.order ?? 0));
    // refresh label map for table
    categories.forEach((c) => {
      CATEGORY_LABEL[c.id] = c.name;
    });
  }

  function renderCategories() {
    const grid = $("#category-grid");
    if (!grid) return;
    grid.innerHTML = categories
      .map((c) => {
        const count = azkar.filter((z) => z.category === c.id).length;
        const badge = c.builtin ? '<span class="pill">مدمج</span>' : '<span class="pill status-published">ديناميكي</span>';
