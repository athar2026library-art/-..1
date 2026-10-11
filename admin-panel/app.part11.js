    $$("[data-archive]").forEach((btn) =>
      btn.addEventListener("click", () => togglePublished(btn.dataset.archive))
    );
  }

  function bindDrag() {
    const rows = $$("#azkar-table tr.draggable-row");
    let dragId = null;
    rows.forEach((row) => {
      row.addEventListener("dragstart", () => {
        dragId = row.dataset.id;
        row.classList.add("dragging");
      });
      row.addEventListener("dragend", () => row.classList.remove("dragging"));
      row.addEventListener("dragover", (e) => {
        e.preventDefault();
        row.classList.add("drag-over");
      });
      row.addEventListener("dragleave", () => row.classList.remove("drag-over"));
      row.addEventListener("drop", async (e) => {
        e.preventDefault();
        row.classList.remove("drag-over");
        const targetId = row.dataset.id;
        if (!dragId || dragId === targetId) return;
        await reorder(dragId, targetId);
      });
    });
  }

  async function reorder(fromId, toId) {
    const sorted = azkar.slice().sort((a, b) => (a.order ?? 0) - (b.order ?? 0));
    const fromIdx = sorted.findIndex((z) => z.id === fromId);
