    modal?.setAttribute("aria-hidden", "false");
  }

  function closeEditor() {
    const modal = $("#editor-modal");
    modal?.classList.remove("open");
    modal?.setAttribute("aria-hidden", "true");
    editingId = null;
  }

  async function saveEditor() {
    const text = $("#field-text")?.value.trim();
    if (!text) {
      toast("يرجى كتابة نص الذكر");
      return;
    }
    const audioUrl = $("#field-audio-url")?.value.trim() || "";
    const audioUrlsByReciter = Object.fromEntries(
      [
        ["mishary", $("#field-audio-mishary")?.value.trim() || ""],
        ["abdulbasit", $("#field-audio-abdulbasit")?.value.trim() || ""],
        ["maher", $("#field-audio-maher")?.value.trim() || ""],
        ["yasser", $("#field-audio-yasser")?.value.trim() || ""],
      ].filter(([, url]) => url)
    );
    const suppliedAudioUrls = [audioUrl, ...Object.values(audioUrlsByReciter)];
    if (suppliedAudioUrls.filter(Boolean).some((url) => !/^https:\/\/\S+$/i.test(url))) {
      toast("روابط الصوت يجب أن تبدأ بـ HTTPS وألا تحتوي مسافات.");
      return;
    }
    const repeat = Math.min(1000, Math.max(1, Math.round(Number($("#field-repeat")?.value) || 1)));
    const category = $("#field-category")?.value || "sabah";
    const known = ["sabah", "masaa", "sleep", "travel", "worry", "rizq", "illness", "waking"];
    if (!known.includes(category) && !categories.some((c) => c.id === category && !c.builtin)) {
      toast("التصنيف غير موجود في Firestore. أضفه أولاً من شاشة التصنيفات.");
      return;
    }
    const payload = {
      text: text.slice(0, 3999),
      category,
      repeat,
      fadl: ($("#field-fadl")?.value || "").trim().slice(0, 3999),
      source: ($("#field-source")?.value || "").trim().slice(0, 499),
      audioUrl,
      audioUrlsByReciter,
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
