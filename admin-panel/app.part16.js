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
    $("#modal-title").textContent = z ? "تعديل الذكر" : "إضافة ذكر";
    $("#field-text").value = z?.text || "";
    $("#field-category").value = z?.category || "sabah";
    $("#field-repeat").value = z?.repeat || 1;
    $("#field-fadl").value = z?.fadl || "";
    $("#field-source").value = z?.source || "";
    $("#field-published").checked = z ? !!z.published : true;
    const modal = $("#editor-modal");
    modal.classList.add("open");
