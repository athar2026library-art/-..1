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

  function setSignedOutUI(messageText = "سجّل الدخول بحساب Google المصرّح له.") {
    const adminSection = $("#admin-section");
    const btnIn = $("#btn-sign-in");
    const btnOut = $("#btn-sign-out");
    const message = $("#auth-message");
    if (message) message.textContent = messageText;
    if (btnIn) btnIn.hidden = false;
    if (btnOut) btnOut.hidden = true;
    if (adminSection) adminSection.hidden = true;
  }

  async function verifyAdmin(user) {
    if (!user) return false;

    try {
      const token = await user.getIdTokenResult(true);
      if (token.claims?.admin === true) return true;

      // The Firestore rules allow a signed-in user to read only their own
      // admins/{uid} document, so this check cannot grant access by itself.
      const adminDoc = await db.collection("admins").doc(user.uid).get();
      return adminDoc.exists &&
        ["admin", "super_admin"].includes(adminDoc.data()?.role);
    } catch (error) {
      console.error("Admin verification failed", error);
      return false;
    }
  }

  async function loadDashboard() {
    const container = $("#admin-content");
    if (!container) return;
    container.textContent = "جاري تحميل الإحصائيات…";

    try {
      const [usersSnapshot, feedbackSnapshot] = await Promise.all([
        db.collection("users").get(),
        db.collection("feedback").get()
      ]);
      const newComplaints = feedbackSnapshot.docs.filter((doc) => {
        const data = doc.data() || {};
        return data.status === "new" &&
          ["complaint", "technical", "bug"].includes(data.type);
      }).length;

      const stats = [
        ["إجمالي المستخدمين", String(usersSnapshot.size)],
        ["الشكاوى والمشكلات الجديدة", String(newComplaints)],
        ["النشطون اليوم", "غير متاح بعد — لا يُسجَّل نشاط يومي موثوق"],
        ["عدد قراءات الأذكار", "غير متاح بعد — يلزم تسجيل أحداث القراءة"],
        ["أسئلة المساعد", "غير متاح بعد — يلزم تفعيل إحصاءات آمنة"]
      ];
      container.replaceChildren();
      const grid = document.createElement("div");
      grid.className = "stats-grid";
      stats.forEach(([label, value]) => {
        const card = document.createElement("article");
        card.className = "stat-card";
        const heading = document.createElement("h3");
        heading.textContent = label;
        const metric = document.createElement("p");
        metric.textContent = value;
        card.append(heading, metric);
        grid.append(card);
      });
      container.append(grid);
    } catch (error) {
      console.error("Dashboard loading failed", error);
      container.textContent =
        "تعذر تحميل الإحصائيات. تحقق من صلاحيات Firestore وقواعد الأمان.";
    }
  }

  async function handleAuthState(user) {
    const message = $("#auth-message");
    const adminSection = $("#admin-section");
    const btnIn = $("#btn-sign-in");
    const btnOut = $("#btn-sign-out");

    if (!user) {
      setSignedOutUI();
      return;
    }

    if (message) message.textContent = "جارٍ التحقق من صلاحيات المالك…";
    if (btnIn) btnIn.hidden = true;
    if (btnOut) btnOut.hidden = false;
    if (adminSection) adminSection.hidden = true;

    const authorized = await verifyAdmin(user);
    if (!authorized) {
      setSignedOutUI("هذا الحساب لا يملك صلاحية المالك. اطلب من مسؤول المشروع منحه صلاحية الإدارة.");
      if (btnOut) btnOut.hidden = false;
      return;
    }

    if (message) message.textContent = `مرحباً، ${user.displayName || user.email}`;
    if (adminSection) adminSection.hidden = false;
    const status = $("#admin-status");
    if (status) status.textContent = "تم التحقق من صلاحيات الإدارة.";
    await loadDashboard();
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

    auth.onAuthStateChanged((user) => { void handleAuthState(user); });
  }

  document.addEventListener("DOMContentLoaded", init);
})();
