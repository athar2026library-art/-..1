    $("#add-category")?.addEventListener("click", () => openCategoryEditor(null));
    $("#save-zekr")?.addEventListener("click", () => saveEditor().catch((e) => toast(e.message || "تعذر الحفظ")));
    $("#close-modal")?.addEventListener("click", closeEditor);
    $("#cancel-modal")?.addEventListener("click", closeEditor);
    $("#save-category")?.addEventListener("click", () => saveCategory().catch((e) => toast(e.message || "تعذر حفظ التصنيف")));
    $("#close-cat-modal")?.addEventListener("click", closeCategoryEditor);
    $("#cancel-cat-modal")?.addEventListener("click", closeCategoryEditor);
    $("#close-preview")?.addEventListener("click", closePreview);
    $("#export-json")?.addEventListener("click", exportJson);
    $("#send-notify")?.addEventListener("click", () => sendNotify().catch((e) => toast(e.message || "تعذر الإرسال")));
    ["#editor-modal", "#category-modal", "#preview-modal"].forEach((sel) => {
      $(sel)?.addEventListener("click", (event) => {
        if (event.target !== event.currentTarget) return;
        if (sel === "#editor-modal") closeEditor();
        else if (sel === "#category-modal") closeCategoryEditor();
        else closePreview();
      });
    });
  }

  const initializeAdminPanel = async () => {
    if (window.firebaseHostingConfigPromise) {
      await window.firebaseHostingConfigPromise.catch(() => null);
    }
    const cfg = window.firebaseConfig || window.AZKAR_FIREBASE_CONFIG;
    if (!window.firebase || !cfg) {
      console.error("Firebase not loaded");
      const msg = document.querySelector("#auth-message");
      if (msg) {
        msg.textContent =
