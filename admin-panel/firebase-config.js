// Static fallback for the Manus-hosted admin panel.
// Firebase Hosting auto-config below can override this when available.
window.AZKAR_FIREBASE_CONFIG = {
  apiKey: "AIzaSyBQPLAf_e9xWL3-MA5jtUPR5xGk75Id_xF0",
  authDomain: "svrpmtt.firebaseapp.com",
  projectId: "svrpmtt",
  storageBucket: "svrpmtt.firebasestorage.app",
  messagingSenderId: "372887186106",
  appId: "1:372887186106:web:458dec501ee157feed7083",
  measurementId: "G-8T2MNTMXQN"
};

(function () {
  function apply(cfg) {
    if (cfg && cfg.apiKey && cfg.projectId) {
      window.firebaseConfig = cfg;
      window.AZKAR_FIREBASE_CONFIG = cfg;
    }
  }

  apply(window.AZKAR_FIREBASE_CONFIG);
  apply(window.firebaseConfig);

  // Firebase Hosting provides /__/firebase/init.json on its project domain.
  // The static config remains available when this Manus-hosted panel is used.
  window.firebaseHostingConfigPromise = fetch("/__/firebase/init.json", {
    cache: "no-store",
  })
    .then(function (r) {
      return r.ok ? r.json() : null;
    })
    .then(function (cfg) {
      apply(cfg);
      return cfg;
    })
    .catch(function () {
      return null;
    });
})();
