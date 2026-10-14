// Static fallback for the Manus-hosted admin panel.
// Firebase Hosting auto-config below can override this when available.
window.AZKAR_FIREBASE_CONFIG = {
  apiKey: "AIzaSyBQPLa_e9xWL3-MABjtUPR5xGk75Ig_sF0",
  authDomain: "svrpmtt.firebaseapp.com",
  projectId: "svrpmtt",
  storageBucket: "svrpmtt.firebasestorage.app",
  messagingSenderId: "372887186106",
  appId: "1:372887186106:web:4506ec501ee157feed7083",
  measurementId: "G-BT2MNTMKQW"
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

  var host = location.hostname || "";
  var onFirebaseHosting = /(^|\.)firebaseapp\.com$|(^|\.)web\.app$/.test(host);
  if (!onFirebaseHosting) {
    window.firebaseHostingConfigPromise = Promise.resolve(null);
    return;
  }

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
