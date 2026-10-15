/* Minimal core + Firebase init. UI buttons and category save handled by app.overrides.js */
(function () {
  const $ = (s) => document.querySelector(s);
  const $$ = (s) => document.querySelectorAll(s);
  function toast(msg) {
    const el = $("#toast");
    if (!el) return;
    el.textContent = msg;
    el.classList.add("show");
    setTimeout(() => el.classList.remove("show"), 2400);
  }
  window.__adminToast = toast;

  const initializeAdminPanel = async () => {
    if (window.firebaseHostingConfigPromise) {
      await window.firebaseHostingConfigPromise.catch(() => null);
    }
    const cfg = window.firebaseConfig || window.AZKAR_FIREBASE_CONFIG;
    if (!window.firebase || !cfg) {
      console.error("Firebase not loaded");
      const msg = document.querySelector("#auth-message");
      if (msg) msg.textContent = "تعذر تحميل إعداد Firebase.";
      return;
    }
    if (!firebase.apps.length) {
      firebase.initializeApp(cfg);
    }
    const db = firebase.firestore();
    const auth = firebase.auth();
    window.__adminDb = db;
    window.__adminAuth = auth;

    auth.onAuthStateChanged(async (user) => {
      const gate = $("#auth-gate");
      const shell = $("#shell");
      if (!user) {
        gate?.classList.remove("hidden");
        if (gate) gate.style.display = "grid";
        shell?.classList.remove("ready");
        return;
      }
      try {
        const token = await user.getIdTokenResult(true);
        let ok = token.claims.super_admin === true || token.claims.admin === true;
        if (!ok) {
          const snap = await db.collection("admins").doc(user.uid).get();
          ok = snap.exists;
        }
        if (!ok) {
          toast("ليس لديك صلاحية مشرف");
          await auth.signOut();
          return;
        }
        gate?.classList.add("hidden");
        if (gate) gate.style.display = "none";
        shell?.classList.add("ready");
        if ($("#admin-email")) $("#admin-email").textContent = user.email || user.uid;
      } catch (e) {
        toast(e.message || "خطأ في التحقق");
      }
    });

    $("#google-login-btn")?.addEventListener("click", async () => {
      const provider = new firebase.auth.GoogleAuthProvider();
      try {
        await auth.signInWithPopup(provider);
      } catch (e) {
        if ((e.code || "").includes("popup")) {
          try { await auth.signInWithRedirect(provider); } catch (re) { toast(re.message); }
        } else toast(e.message || "فشل الدخول");
      }
    });
    $("#logout-btn")?.addEventListener("click", () => auth.signOut());
  };

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initializeAdminPanel, { once: true });
  } else {
    initializeAdminPanel();
  }
})();
