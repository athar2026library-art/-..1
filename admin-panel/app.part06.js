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
    $("#cat-modal-title").textContent = c ? "تعديل التصنيف" : "إضافة تصنيف";
    $("#cat-field-id").value = c?.id || "";
    $("#cat-field-id").disabled = !!c;
    $("#cat-field-name").value = c?.name || "";
    $("#cat-field-icon").value = c?.icon || "📖";
    $("#cat-field-desc").value = c?.desc || "";
    $("#cat-field-order").value = c?.order ?? 100;
    $("#cat-field-published").checked = c ? c.published !== false : true;
    const modal = $("#category-modal");
    modal?.classList.add("open");
    modal?.setAttribute("aria-hidden", "false");
  }

  function closeCategoryEditor() {
