  }

  function renderTable() {
    const q = ($("#search")?.value || "").trim();
    const filter = $("#filter")?.value || "all";
    const status = $("#status-filter")?.value || "all";
    const rows = azkar
      .slice()
      .sort((a, b) => (a.order ?? 0) - (b.order ?? 0))
      .filter(
        (z) =>
          (filter === "all" || z.category === filter) &&
          (status === "all" || (status === "published" ? z.published : !z.published)) &&
          ((z.text || "") + (z.source || "")).includes(q)
      );

    const tbody = $("#azkar-table");
    if (!tbody) return;
    if (!rows.length) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:20px">لا توجد أذكار مطابقة.</td></tr>';
      return;
    }

    tbody.innerHTML = rows
      .map(
        (z) => `<tr class="draggable-row" draggable="true" data-id="${escapeHtml(z.id)}">
      <td class="order-cell"><span class="drag-handle" title="اسحب لإعادة الترتيب">⋮⋮</span><span class="order-num">${z.order ?? 0}</span></td>
      <td>${escapeHtml((z.text || "").slice(0, 90))}${(z.text || "").length > 90 ? "…" : ""}</td>
