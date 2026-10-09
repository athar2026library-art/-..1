    const toIdx = sorted.findIndex((z) => z.id === toId);
    if (fromIdx < 0 || toIdx < 0) return;
    const [moved] = sorted.splice(fromIdx, 1);
    sorted.splice(toIdx, 0, moved);
    const batch = db.batch();
    sorted.forEach((z, i) => {
      batch.update(itemsCol().doc(z.id), { order: i + 1 });
    });
    await batch.commit();
    toast("تم حفظ الترتيب");
  }

  function listenAzkar() {
    if (unsubAzkar) unsubAzkar();
    unsubAzkar = itemsCol().onSnapshot(
      (snap) => {
        azkar = snap.docs.map((doc) => {
          const d = doc.data() || {};
          return {
            id: doc.id,
            text: d.text || "",
            category: d.category || "sabah",
            repeat: d.repeat || 1,
            fadl: d.fadl || "",
            source: d.source || "",
            published: d.published !== false,
            order: d.order ?? 0,
            numericId: d.id ?? 0,
          };
        });
        renderTable();
        renderStats();
        renderCategories();
      },
      (err) => toast("تعذر تحميل الأذكار: " + err.message)
    );
  }

  function listenFeedback() {
    if (unsubFeedback) unsubFeedback();
    unsubFeedback = db
