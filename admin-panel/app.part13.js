      .collection("feedback")
      .orderBy("createdAt", "desc")
      .limit(100)
      .onSnapshot(
        (snap) => {
          feedbackItems = snap.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
          renderFeedbackList();
          renderStats();
        },
        () => {
          db.collection("feedback")
            .limit(100)
            .get()
            .then((snap) => {
              feedbackItems = snap.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
              renderFeedbackList();
              renderStats();
            })
            .catch((err) => toast("تعذر تحميل الرسائل: " + err.message));
        }
      );
  }

  function renderFeedbackList() {
    const list = $("#feedback-list");
    if (!list) return;
    if (!feedbackItems.length) {
      list.innerHTML = '<p class="empty-hint">لا رسائل بعد.</p>';
      return;
    }
    list.innerHTML = feedbackItems
      .map((f) => {
        const active = f.id === selectedFeedbackId ? " active" : "";
        const title = f.title || f.type || "رسالة";
        const preview = (f.message || "").slice(0, 80);
        return `<button class="feedback-item${active}" data-fid="${escapeHtml(f.id)}">
