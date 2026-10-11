    const modal = $("#category-modal");
    modal?.classList.remove("open");
    modal?.setAttribute("aria-hidden", "true");
    editingCategoryId = null;
  }

  async function saveCategory() {
    const idRaw = $("#cat-field-id")?.value.trim();
    const name = $("#cat-field-name")?.value.trim();
    if (!idRaw || !name) {
      toast("أدخل المعرّف والاسم");
      return;
    }
    const id = idRaw.replace(/[^a-zA-Z0-9_\u0600-\u06FF-]/g, "_").slice(0, 40);
    if (!id) {
      toast("معرّف غير صالح");
      return;
    }
    const payload = {
      id,
      name,
      icon: $("#cat-field-icon")?.value.trim() || "📖",
      desc: $("#cat-field-desc")?.value.trim() || "",
      order: Number($("#cat-field-order")?.value) || 100,
      published: !!$("#cat-field-published")?.checked,
      builtin: BUILTIN_CATEGORIES.some((b) => b.id === id),
      updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
    };
    if (editingCategoryId) {
      await categoriesCol().doc(editingCategoryId).set(payload, { merge: true });
      toast("تم حفظ التصنيف");
    } else {
      const exists = categories.some((c) => c.id === id);
      if (exists) {
        toast("المعرّف موجود مسبقاً");
