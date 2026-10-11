      if (gate) gate.style.display = "none";
      shell?.classList.add("ready");
    } else {
      gate?.classList.remove("hidden");
      if (gate) gate.style.display = "grid";
      shell?.classList.remove("ready");
    }
  }

  async function assertAdmin(user) {
    const token = await user.getIdTokenResult(true);
    if (token.claims.admin === true) {
      return { ok: true, role: "super_admin" };
    }
    const snap = await db.collection("admins").doc(user.uid).get();
    if (!snap.exists) return { ok: false, role: "" };
    const role = snap.data()?.role || "admin";
    return { ok: true, role };
  }

  function renderStats() {
    const published = azkar.filter((z) => z.published).length;
    if ($("#stat-total")) $("#stat-total").textContent = String(azkar.length);
    if ($("#stat-published")) $("#stat-published").textContent = String(published);
    if ($("#stat-draft")) $("#stat-draft").textContent = String(azkar.length - published);
    const unread = feedbackItems.filter((f) => f.status === "new" || !f.adminReply).length;
    if ($("#stat-feedback")) $("#stat-feedback").textContent = String(unread);
    const badge = $("#feedback-badge");
    if (badge) {
