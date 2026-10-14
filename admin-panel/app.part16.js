      updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
    });
    if (f?.userId) {
      try {
        await db
          .collection("users")
          .doc(f.userId)
          .collection("feedback")
          .doc(selectedFeedbackId)
          .set(
            {
              adminReply: reply,
              status,
              replyUnread: true,
              updatedAt: firebase.firestore.FieldValue.serverTimestamp(),
            },
            { merge: true }
          );
      } catch (_e) {
        /* user copy is best-effort */
      }
    }
    toast("تم حفظ الرد");
  }

  function openEditor(id) {
    editingId = id || null;
    const z = id ? azkar.find((x) => x.id === id) : null;
    const titleEl = $("#modal-title");
    if (titleEl) titleEl.textContent = z ? "تعديل الذكر" : "إضافة ذكر";
    setField("#field-text", z?.text || "");
    setField("#field-category", z?.category || "sabah");
    const catEl = $("#field-category");
    if (catEl && z?.category && ![...catEl.options].some((o) => o.value === z.category)) {
      catEl.value = catEl.options[0]?.value || "sabah";
    }
    setField("#field-repeat", String(z?.repeat || 1));
    setField("#field-fadl", z?.fadl || "");
    setField("#field-source", z?.source || "");
    setField("#field-audio-url", z?.audioUrl || "");
    setField("#field-audio-mishary", z?.audioUrlsByReciter?.mishary || "");
    setField("#field-audio-abdulbasit", z?.audioUrlsByReciter?.abdulbasit || "");
    setField("#field-audio-maher", z?.audioUrlsByReciter?.maher || "");
    setField("#field-audio-yasser", z?.audioUrlsByReciter?.yasser || "");
    const published = $("#field-published");
    if (published) published.checked = z ? !!z.published : true;
    const modal = $("#editor-modal");
    modal?.classList.add("open");
