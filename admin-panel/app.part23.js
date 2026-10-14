    $("#btn-add-category")?.addEventListener("click", () => openCategoryEditor(null));
    $("#save-editor")?.addEventListener("click", () => saveEditor().catch((e) => toast(e.message)));
    $("#close-editor")?.addEventListener("click", closeEditor);
    $("#save-category")?.addEventListener("click", () => saveCategory().catch((e) => toast(e.message)));
    $("#close-category")?.addEventListener("click", closeCategoryEditor);
    $("#close-preview")?.addEventListener("click", closePreview);
    $("#export-json")?.addEventListener("click", exportJson);
    $("#send-notify")?.addEventListener("click", () => sendNotify().catch((e) => toast(e.message)));
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

    $("#save-zekr")?.addEventListener("click", () => saveEditor().catch(e => toast(e.message)));
    $("#save-category")?.addEventListener("click", () => saveCategory().catch(e => toast(e.message)));
    $("#close-modal")?.addEventListener("click", closeEditor);
    $("#cancel-modal")?.addEventListener("click", closeEditor);
    $("#close-cat-modal")?.addEventListener("click", closeCategoryEditor);
    $("#cancel-cat-modal")?.addEventListener("click", closeCategoryEditor);
