          "تعذر تحميل إعداد Firebase. افتح اللوحة من Firebase Hosting أو عرّف window.firebaseConfig.";
      }
      return;
    }
    if (!firebase.apps.length) {
      firebase.initializeApp(cfg);
    }
    db = firebase.firestore();
    auth = firebase.auth();
    bindUi();
    auth.onAuthStateChanged((user) => {
      if (user) startSession(user).catch((e) => toast(e.message));
      else stopSession();
    });
  });

  async function loadOwnerMetrics() {
    if (!db || !currentUser) return;
    const todayUtc = new Date().toISOString().slice(0, 10);
    try {
      const [usersSnapshot, metricsSnapshot] = await Promise.all([
        db.collection("users").get(),
        db.collection("owner_metrics").doc("summary").get(),
      ]);
      const users = usersSnapshot.docs.map((doc) => doc.data() || {});
      const summary = metricsSnapshot.exists ? metricsSnapshot.data() || {} : {};
      const counts = {
        "#stat-users": String(usersSnapshot.size),
        "#stat-active-today": String(users.filter((user) => user.lastActiveDate === todayUtc).length),
        "#stat-azkar-reads": String(Number(summary.totalAzkarReads) || 0),
        "#stat-ai-questions": String(Number(summary.aiQuestions) || 0),
      };
      Object.entries(counts).forEach(([selector, value]) => {
        const element = $(selector);
        if (element) element.textContent = value;
      });

      const popular = Object.entries(summary.azkarReadsById || {})
        .sort((a, b) => (Number(b[1]?.count) || 0) - (Number(a[1]?.count) || 0))
        .slice(0, 3)
        .map(([id, item]) => {
          const label = String(item?.text || ("ذكر " + id)).slice(0, 100);
          return label + " (" + (Number(item?.count) || 0) + ")";
        });
      const topElement = $("#stat-top-read");
      if (topElement) {
        topElement.textContent = popular.length ? popular.join(" • ") : "لا توجد قراءات مسجلة بعد";
      }
    } catch (error) {
      console.error("Owner usage metrics failed", error);
      toast("تعذر تحميل إحصاءات الاستخدام. تحقق من صلاحيات Firestore.");
    }
  }
})();
