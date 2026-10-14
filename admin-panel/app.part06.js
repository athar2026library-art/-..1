      if (categories.some((c) => c.id === current)) sel.value = current;
    }
    const filter = $("#filter");
    if (filter) {
      const cur = filter.value;
      filter.innerHTML =
        '<option value="all">الكل</option>' +
        categories.map((c) => `<option value="${escapeHtml(c.id)}">${escapeHtml(c.name)}</option>`).join("");
      if (cur === "all" || categories.some((c) => c.id === cur)) filter.value = cur;
    }
  }

    function openCategoryEditor(id) {
    editingCategoryId = id || null;
    const c = id ? categories.find((x) => x.id === id) : null;
    const titleEl = $("#cat-modal-title");
    if (titleEl) titleEl.textContent = c ? "تعديل التصنيف" : "إضافة تصنيف";

    const idInput = $("#cat-field-id") || $("#cat-id");
    if (idInput) {
      idInput.value = c?.id || "";
      idInput.disabled = !!c;
    }

    const nameInput = $("#cat-field-name") || $("#cat-title");
    if (nameInput) nameInput.value = c?.title || c?.name || "";

    const iconInput = $("#cat-field-icon") || $("#cat-icon");
    if (iconInput) iconInput.value = c?.icon || "star";

    const descInput = $("#cat-field-desc") || $("#cat-subtitle");
    if (descInput) descInput.value = c?.subtitle || c?.desc || "";

    const orderInput = $("#cat-field-order") || $("#cat-order");
    if (orderInput) orderInput.value = c?.order ?? 100;

    const pubInput = $("#cat-field-published") || $("#cat-published");
    if (pubInput) pubInput.checked = c ? c.published !== false : true;

    const modal = $("#category-modal");
    modal?.classList.add("open");
    modal?.setAttribute("aria-hidden", "false");
  }

  function closeCategoryEditor() {
