      <td><span class="pill">${escapeHtml(CATEGORY_LABEL[z.category] || z.category)}</span></td>
      <td>${Number(z.repeat) || 1}×</td>
      <td><span class="pill ${z.published ? "status-published" : "status-draft"}">${z.published ? "منشور" : "مسودة"}</span></td>
      <td>${escapeHtml(z.source || "—")}</td>
      <td>
        <button class="icon-btn" data-preview="${escapeHtml(z.id)}">معاينة</button> ·
        <button class="icon-btn" data-edit="${escapeHtml(z.id)}">تعديل</button> ·
        <button class="icon-btn" data-dup="${escapeHtml(z.id)}">نسخ</button> ·
        <button class="icon-btn" data-archive="${escapeHtml(z.id)}">${z.published ? "إخفاء" : "نشر"}</button>
      </td>
    </tr>`
      )
      .join("");

    bindTableActions();
    bindDrag();
  }

  function bindTableActions() {
    $$("[data-preview]").forEach((btn) =>
      btn.addEventListener("click", () => openPreview(btn.dataset.preview))
    );
    $$("[data-edit]").forEach((btn) =>
      btn.addEventListener("click", () => openEditor(btn.dataset.edit))
    );
    $$("[data-dup]").forEach((btn) =>
      btn.addEventListener("click", () => duplicateZekr(btn.dataset.dup))
    );
