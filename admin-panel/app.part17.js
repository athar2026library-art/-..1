    modal.setAttribute("aria-hidden", "false");
  }

  function closeEditor() {
    const modal = $("#editor-modal");
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
    editingId = null;
  }

  async function saveEditor() {
    const text = $("#field-text")?.value.trim();
    if (!text) {
      toast("يرجى كتابة نص الذكر");
      return;
    }
    const payload = {
      text,
      category: $("#field-category")?.value || "sabah",
      repeat: Number($("#field-repeat")?.value) || 1,
      fadl: $("#field-fadl")?.value.trim() || "",
      source: $("#field-source")?.value.trim() || "",
      published: !!$("#field-published")?.checked,
      updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
    };
    if (editingId) {
      await itemsCol().doc(editingId).set(payload, { merge: true });
      toast("تم حفظ التعديل");
    } else {
      const maxOrder = azkar.reduce((m, z) => Math.max(m, Number(z.order) || 0), 0);
      const maxId = azkar.reduce((m, z) => Math.max(m, Number(z.numericId) || 0), 0);
      await itemsCol().add({
        ...payload,
        id: maxId + 1,
        order: maxOrder + 1,
