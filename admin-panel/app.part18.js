        createdAt: firebase.firestore.FieldValue.serverTimestamp(),
      });
      toast("تمت إضافة الذكر");
    }
    closeEditor();
  }

  async function duplicateZekr(id) {
    const z = azkar.find((x) => x.id === id);
    if (!z) return;
    const maxOrder = azkar.reduce((m, item) => Math.max(m, Number(item.order) || 0), 0);
    const maxId = azkar.reduce((m, item) => Math.max(m, Number(item.numericId) || 0), 0);
    await itemsCol().add({
      text: z.text,
      category: z.category,
      repeat: z.repeat,
      fadl: z.fadl,
      source: z.source,
      published: false,
      id: maxId + 1,
      order: maxOrder + 1,
      createdAt: firebase.firestore.FieldValue.serverTimestamp(),
    });
    toast("تم نسخ الذكر كمسودة");
  }

  async function togglePublished(id) {
    const z = azkar.find((x) => x.id === id);
    if (!z) return;
    await itemsCol().doc(id).update({
      published: !z.published,
      updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
    });
    toast(z.published ? "أُخفي الذكر" : "نُشر الذكر");
  }

  function openPreview(id) {
    const z = (id && azkar.find((x) => x.id === id)) || azkar[0];
    if (!z) {
