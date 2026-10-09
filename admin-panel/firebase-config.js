// Prefer Firebase Hosting auto-config when available.
// Optional local/dev override: set window.firebaseConfig before this file loads.
(function () {
  function apply(cfg) {
    if (cfg && cfg.apiKey && cfg.projectId) {
      window.firebaseConfig = cfg;
      window.AZKAR_FIREBASE_CONFIG = cfg;
    }
  }

  // Keep any pre-injected config
  apply(window.firebaseConfig);

  // Hosting provides /__/firebase/init.json on the project domain
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
