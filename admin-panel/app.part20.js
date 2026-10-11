    const body = $("#notify-body")?.value.trim();
    if (!title || !body) {
      toast("أدخل العنوان والنص");
      return;
    }
    await db.collection("notifications").add({
      title,
      body,
      status: "queued",
      audience: "all",
      createdBy: currentUser?.uid || "",
      createdAt: firebase.firestore.FieldValue.serverTimestamp(),
    });
    lastNotifyAt = now;
    $("#notify-title").value = "";
    $("#notify-body").value = "";
    toast("أُضيفت مهمة الإشعار");
  }

  async function startSession(user) {
    const check = await assertAdmin(user);
    if (!check.ok) {
      await auth.signOut();
      $("#auth-message").textContent = "هذا الحساب غير مدرج في مجموعة المشرفين.";
      setSessionUi(false);
      return;
    }
    currentUser = user;
    adminRole = check.role;
    if ($("#admin-email")) $("#admin-email").textContent = user.email || user.uid;
    if ($("#admin-role-badge")) $("#admin-role-badge").textContent = "دور: " + adminRole;
    setSessionUi(true);
    listenAzkar();
    listenFeedback();
    listenCategories();
    renderCategories();
  }

  function stopSession() {
    currentUser = null;
    if (unsubAzkar) unsubAzkar();
