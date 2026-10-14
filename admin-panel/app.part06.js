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
