(function () {
  const $ = (sel) => document.querySelector(sel);

  let auth = null;
  let db = null;

  async function resolveFirebaseConfig() {
    const hostingConfig = await Promise.resolve(
      window.firebaseHostingConfigPromise
    ).catch(() => null);

    if (
      hostingConfig?.apiKey &&
      hostingConfig?.projectId &&
      hostingConfig?.appId
    ) {
      return hostingConfig;
    }

    const localConfig =
      window.firebaseConfig ||
      window.AZKAR_FIREBASE_CONFIG;

    if (
      localConfig?.apiKey &&
      localConfig?.projectId &&
      localConfig?.appId
    ) {
      return localConfig;
    }

    return null;
  }

  function friendlyAuthError(error) {
    const code = error?.code || "";

    if (code.includes("auth/api-key-not-valid")) {
      return "إعداد Firebase للويب غير صالح. تأكد أنك فتحت اللوحة من نطاق المشروع الصحيح.";
    }

    if (code.includes("auth/unauthorized-domain")) {
      return "هذا النطاق غير مسموح به في Firebase Authentication. أضفه ضمن Authorized domains.";
    }

    if (code.includes("auth/popup-blocked")) {
      return "المتصفح منع نافذة Google. اضغط مرة أخرى أو اسمح بالنوافذ المنبثقة.";
    }

    if (code.includes("auth/popup-closed-by-user")) {
      return "أُغلقت نافذة Google قبل إكمال تسجيل الدخول.";
    }

    return error?.message || "تعذر تسجيل الدخول. حاول مرة أخرى.";
  }

  async function signInAdmin() {
    const message = $("#auth-message");

    if (message) {
      message.textContent = "جاري فتح تسجيل Google…";
    }

    const provider = new firebase.auth.GoogleAuthProvider();
    provider.setCustomParameters({ prompt: "select_account" });

    try {
      await auth.signInWithPopup(provider);
    } catch (error) {
      const code = error?.code || "";

      if (code.includes("auth/popup-blocked")) {
        try {
          await auth.signInWithRedirect(provider);
          return;
        } catch (redirectError) {
          if (message) {
            message.textContent = friendlyAuthError(redirectError);
          }
          return;
        }
      }

      if (message) {
        message.textContent = friendlyAuthError(error);
      }
    }
  }

  function setSignedInUI(user) {
    const authSection = $("#auth-section");
    const adminSection = $("#admin-section");
    const btnIn = $("#btn-sign-in");
    const btnOut = $("#btn-sign-out");
    const message = $("#auth-message");
    const status = $("#admin-status");

    if (user) {
      if (message) message.textContent = `مرحباً، ${user.displayName || user.email}`;
      if (btnIn) btnIn.hidden = true;
      if (btnOut) btnOut.hidden = false;
      if (adminSection) adminSection.hidden = false;
      if (status) {
        status.textContent =
          "تم تسجيل الدخول. يمكنك متابعة إدارة المحتوى من هنا.";
      }
    } else {
      if (message) {
        message.textContent = "سجّل الدخول بحساب Google المصرّح له.";
      }
      if (btnIn) btnIn.hidden = false;
      if (btnOut) btnOut.hidden = true;
      if (adminSection) adminSection.hidden = true;
    }
  }

  async function init() {
    const config = await resolveFirebaseConfig();
    const message = $("#auth-message");

    if (!config) {
      if (message) {
        message.textContent =
          "تعذر تحميل إعداد Firebase. افتح اللوحة من Firebase Hosting أو عرّف window.firebaseConfig.";
      }
      return;
    }

    if (!firebase.apps.length) {
      firebase.initializeApp(config);
    }

    auth = firebase.auth();
    db = firebase.firestore();

    $("#btn-sign-in")?.addEventListener("click", signInAdmin);
    $("#btn-sign-out")?.addEventListener("click", () => auth.signOut());

    auth.onAuthStateChanged((user) => setSignedInUI(user));
  }

  document.addEventListener("DOMContentLoaded", init);
})();
