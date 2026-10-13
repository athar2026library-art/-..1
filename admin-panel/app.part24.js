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
  };
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initializeAdminPanel, { once: true });
  } else {
    initializeAdminPanel();
  }
})();
