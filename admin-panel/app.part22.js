      await auth.signInWithPopup(provider);
    } catch (e) {
      const code = e?.code || "";
      if (code.includes("auth/popup-blocked") || code.includes("auth/popup-closed-by-user")) {
        try {
          await auth.signInWithRedirect(provider);
          return;
        } catch (re) {
          if (message) message.textContent = friendlyAuthError(re);
          return;
        }
      }
      if (message) message.textContent = friendlyAuthError(e);
    }
  }

  function bindUi() {
    $$(".nav-item").forEach((btn) =>
      btn.addEventListener("click", () => go(btn.dataset.view))
    );
    $("#btn-logout")?.addEventListener("click", () => auth.signOut());
    $("#logout-btn")?.addEventListener("click", () => auth.signOut());
    $("#btn-google")?.addEventListener("click", () => signInWithGoogle());
    $("#google-login-btn")?.addEventListener("click", () => signInWithGoogle());
    $("#search")?.addEventListener("input", renderTable);
    $("#filter")?.addEventListener("change", renderTable);
    $("#status-filter")?.addEventListener("change", renderTable);
    $("#btn-add")?.addEventListener("click", () => openEditor(null));
