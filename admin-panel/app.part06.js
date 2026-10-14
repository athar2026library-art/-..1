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
    
    if($("#cat-modal-title")) $("#cat-modal-title").textContent = c ? "تعديل التصنيف" : "إضافة تصنيف";
    if($("#cat-id")) { $("#cat-id").value = c?.id || ""; $("#cat-id").disabled = !!c; }
    if($("#cat-title")) $("#cat-title").value = c?.title || c?.name || "";
    if($("#cat-icon")) $("#cat-icon").value = c?.icon || "book";
    if($("#cat-subtitle")) $("#cat-subtitle").value = c?.subtitle || c?.desc || "";
    if($("#cat-published")) $("#cat-published").checked = c ? c.published !== false : true;

    const modal = $("#category-modal");
    if(modal) { modal.classList.add("open"); modal.setAttribute("aria-hidden", "false"); }
  }

  function closeCategoryEditor() {
