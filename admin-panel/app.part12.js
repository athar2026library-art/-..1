    const toIdx = sorted.findIndex((z) => z.id === toId);
    if (fromIdx < 0 || toIdx < 0) return;
    const [moved] = sorted.splice(fromIdx, 1);
    sorted.splice(toIdx, 0, moved);
    const leftOrder = toIdx > 0 ? Number(sorted[toIdx - 1].order ?? toIdx) : null;
    const rightOrder = toIdx < sorted.length - 1 ? Number(sorted[toIdx + 1].order ?? toIdx + 2) : null;
    let nextOrder;
    if (leftOrder === null) nextOrder = rightOrder === null ? 1 : rightOrder - 1;
    else if (rightOrder === null) nextOrder = leftOrder + 1;
    else nextOrder = (leftOrder + rightOrder) / 2;
    if (!Number.isFinite(nextOrder) || (leftOrder !== null && nextOrder <= leftOrder) || (rightOrder !== null && nextOrder >= rightOrder)) {
      const batch = db.batch();
      sorted.forEach((z, i) => batch.update(itemsCol().doc(z.id), { order: i + 1 }));
      await batch.commit();
    } else {
      await itemsCol().doc(fromId).update({ order: nextOrder });
    }
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
