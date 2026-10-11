        <textarea id="reply-text" rows="4" placeholder="اكتب الرد للمستخدم">${escapeHtml(f.adminReply || "")}</textarea>
      </label>
      <label>الحالة
        <select id="reply-status">
          <option value="new"${f.status === "new" ? " selected" : ""}>جديدة</option>
          <option value="in_progress"${f.status === "in_progress" ? " selected" : ""}>قيد المعالجة</option>
          <option value="replied"${f.status === "replied" ? " selected" : ""}>تم الرد</option>
          <option value="closed"${f.status === "closed" ? " selected" : ""}>مغلقة</option>
        </select>
      </label>
      <div class="modal-actions">
        <button class="primary" id="save-reply">حفظ الرد</button>
      </div>`;
    $("#save-reply")?.addEventListener("click", saveReply);
  }

  async function saveReply() {
    if (!selectedFeedbackId) return;
    const reply = $("#reply-text")?.value.trim() || "";
    const status = $("#reply-status")?.value || "replied";
    const f = feedbackItems.find((x) => x.id === selectedFeedbackId);
    await db.collection("feedback").doc(selectedFeedbackId).update({
      adminReply: reply,
      status,
      replyUnread: true,
