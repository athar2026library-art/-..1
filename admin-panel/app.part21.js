    if (unsubFeedback) unsubFeedback();
    if (unsubCategories) unsubCategories();
    unsubAzkar = unsubFeedback = unsubCategories = null;
    setSessionUi(false);
  }

  function friendlyAuthError(error) {
    const code = error?.code || "";
    if (code.includes("auth/api-key-not-valid")) {
      return "إعداد Firebase للويب غير صالح. تأكد أنك فتحت اللوحة من نطاق المشروع الصحيح.";
    }
    if (code.includes("auth/unauthorized-domain")) {
      return "هذا النطاق غير مصرّح به في Firebase Auth. أضفه من إعدادات Authentication → Settings.";
    }
    if (code.includes("auth/popup-closed-by-user") || code.includes("auth/cancelled-popup-request")) {
      return "أُغلق نافذة تسجيل الدخول قبل الإكمال.";
    }
    if (code.includes("auth/popup-blocked")) {
      return "المتصفح منع النافذة المنبثقة. سيتم المحاولة عبر إعادة التوجيه.";
    }
    if (code.includes("auth/network-request-failed")) {
      return "تعذر الاتصال بالشبكة. تحقق من الاتصال ثم أعد المحاولة.";
    }
    return error?.message || "تعذر تسجيل الدخول.";
  }

  async function signInWithGoogle() {
    const provider = new firebase.auth.GoogleAuthProvider();
    const message = $("#auth-message");
    try {
