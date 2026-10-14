    const modal = $("#category-modal");
    modal?.classList.remove("open");
    modal?.setAttribute("aria-hidden", "true");
    editingCategoryId = null;
  }

  async function saveCategory() {
    const idRaw = ($("#cat-id")?.value || "").trim().toLowerCase();
    const title = ($("#cat-title")?.value || "").trim();
    if (!idRaw || !title) {
      toast("أدخل المعرّف والعنوان");
      return;
    }
    if (!/^[a-z0-9_-]{2,30}$/.test(idRaw) || idRaw.startsWith("wird_")) {
      toast("المعرّف: 2–30 حرفاً إنجليزياً صغيراً أو رقماً أو _ أو -");
      return;
    }
    if (RESERVED_CATEGORY_IDS.includes(idRaw)) {
      toast("هذا المعرّف محجوز لتصنيف مدمج ولا يُحفظ في Firestore");
      return;
    }
    const icon = ALLOWED_ICONS.includes($("#cat-icon")?.value) ? $("#cat-icon").value : "star";
    const subtitle = ($("#cat-subtitle")?.value || "").trim().slice(0, 80);
    const order = Number($("#cat-order")?.value);
    const payload = {
      title: title.slice(0, 40),
      subtitle,
      icon,
      order: Number.isFinite(order) ? order : 100,
      published: !!$("#cat-published")?.checked,
      updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
    };
    if (editingCategoryId) {
      const existing = categories.find((c) => c.id === editingCategoryId);
      if (existing?.createdAt) payload.createdAt = existing.createdAt;
      await categoriesCol().doc(editingCategoryId).set(payload);
      toast("تم حفظ التصنيف");
    } else {
      const exists = categories.some((c) => c.id === idRaw);
      if (exists) {
        toast("المعرّف موجود مسبقاً");
        return;
      }
      await categoriesCol().doc(idRaw).set({
        ...payload,
        createdAt: firebase.firestore.FieldValue.serverTimestamp(),
      });
      toast("تمت إضافة التصنيف");
    }
    closeCategoryEditor();
  }
