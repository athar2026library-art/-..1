        return `<article class="category-card" data-cat-id="${escapeHtml(c.id)}">
        <div class="symbol">${escapeHtml(c.icon || "📖")}</div>
        <h3>${escapeHtml(c.name)}</h3>
        <p>${escapeHtml(c.desc || "")}</p>
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
        .map((c) => `<option value="${escapeHtml(c.id)}">${escapeHtml(c.name)}</option>`)
        .join("");
