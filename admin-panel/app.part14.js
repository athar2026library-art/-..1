          <strong>${escapeHtml(title)}</strong>
          <small>${escapeHtml(f.userEmail || f.userId || "")} · ${escapeHtml(f.status || "new")}</small>
          <span>${escapeHtml(preview)}</span>
        </button>`;
      })
      .join("");
    $$("[data-fid]").forEach((btn) =>
      btn.addEventListener("click", () => {
        selectedFeedbackId = btn.dataset.fid;
        renderFeedbackList();
        renderFeedbackDetail();
      })
    );
    if (selectedFeedbackId) renderFeedbackDetail();
  }

  function renderFeedbackDetail() {
    const box = $("#feedback-detail");
    if (!box) return;
    const f = feedbackItems.find((x) => x.id === selectedFeedbackId);
    if (!f) {
      box.innerHTML = '<p class="empty-hint">اختر رسالة من القائمة.</p>';
      return;
    }
    box.innerHTML = `
      <p class="eyebrow">${escapeHtml(f.type || "رسالة")}</p>
      <h3>${escapeHtml(f.title || "بدون عنوان")}</h3>
      <small>${escapeHtml(f.userEmail || f.userId || "")}</small>
      <div class="feedback-body">${escapeHtml(f.message || "")}</div>
      ${f.adminReply ? `<div class="admin-reply"><strong>الرد السابق</strong><p>${escapeHtml(f.adminReply)}</p></div>` : ""}
      <label>الرد
