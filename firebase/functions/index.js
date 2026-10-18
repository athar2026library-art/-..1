const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { defineSecret } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getAuth } = require("firebase-admin/auth");
const { getMessaging } = require("firebase-admin/messaging");
const { getStorage } = require("firebase-admin/storage");
const { uniqueTokens, buildUserPrompt } = require("./lib/helpers");

initializeApp();

const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");
const GEMINI_MODEL = "gemini-3.5-flash-lite";
const REGION = "europe-west1";

const SYSTEM_INSTRUCTION =
  "أنت مساعد إسلامي متخصص في الأذكار والدعاء. اعتمد فقط على الأحاديث الصحيحة وكتاب حصن المسلم. " +
  "قدم إجاباتك بأسلوب ميسر، مختصر جداً، وهادئ. لا تفتِ ولا تصدر أحكاماً شرعية من عندك. " +
  "في نهاية كل إجابة أضف سطراً: (إجابة آلية، ليست فتوى).";

/** Codes that mean the device token is permanently unusable. */
const INVALID_FCM_CODES = new Set([
  "messaging/invalid-registration-token",
  "messaging/registration-token-not-registered",
  "messaging/invalid-argument",
]);

/**
 * Send FCM multicast and prune permanently invalid tokens from user docs.
 * Returns { successCount, failureCount, pruned }.
 */
async function sendToTokens(tokens, message) {
  const unique = uniqueTokens(tokens);
  let successCount = 0;
  let failureCount = 0;
  const invalidTokens = new Set();

  for (let index = 0; index < unique.length; index += 500) {
    const batch = unique.slice(index, index + 500);
    const result = await getMessaging().sendEachForMulticast({
      tokens: batch,
      ...message,
    });
    successCount += result.successCount;
    failureCount += result.failureCount;
    result.responses.forEach((resp, i) => {
      if (!resp.success && resp.error && INVALID_FCM_CODES.has(resp.error.code)) {
        invalidTokens.add(batch[i]);
      }
    });
  }

  let pruned = 0;
  if (invalidTokens.size > 0) {
    pruned = await pruneInvalidTokens([...invalidTokens]);
  }
  return { successCount, failureCount, pruned };
}

/** Remove invalid FCM tokens from users who still list them. */
async function pruneInvalidTokens(invalidList) {
  if (!invalidList.length) return 0;
  const db = getFirestore();
  const invalid = new Set(invalidList);
  let removed = 0;
  let cursor = null;
  for (let pageNum = 0; pageNum < 100; pageNum++) {
    let q = db.collection("users").where("notificationsEnabled", "==", true).orderBy("__name__").limit(300);
    if (cursor) q = q.startAfter(cursor);
    const page = await q.get();
    if (page.empty) break;
    const batch = db.batch();
    let ops = 0;
    for (const doc of page.docs) {
      const data = doc.data() || {};
      const tokens = Array.isArray(data.fcmTokens) ? data.fcmTokens : [];
      const next = tokens.filter((t) => typeof t === "string" && !invalid.has(t));
      if (next.length !== tokens.length) {
        batch.update(doc.ref, { fcmTokens: next, fcmTokensUpdatedAt: FieldValue.serverTimestamp() });
        removed += tokens.length - next.length;
        ops += 1;
      }
    }
    if (ops > 0) await batch.commit();
    cursor = page.docs[page.docs.length - 1];
    if (page.size < 300) break;
  }
  return removed;
}

async function getBroadcastTokens(audience = "all") {
  const db = getFirestore();
  const tokens = [];
  let cursor = null;
  let scannedUsers = 0;
  const maxUsers = 50000;

  while (scannedUsers < maxUsers) {
    let usersQuery = db.collection("users")
      .where("notificationsEnabled", "==", true)
      .orderBy("__name__")
      .limit(500);
    if (audience !== "all") {
      usersQuery = usersQuery.where("notificationAudience", "==", audience);
    }
    if (cursor) usersQuery = usersQuery.startAfter(cursor);
    const page = await usersQuery.get();
    if (page.empty) break;

    for (const user of page.docs) {
      const userTokens = user.data().fcmTokens;
      if (Array.isArray(userTokens)) {
        tokens.push(...userTokens.filter((token) => typeof token === "string" && token.length > 0));
      }
    }
    scannedUsers += page.size;
    cursor = page.docs[page.docs.length - 1];
    if (page.size < 500) break;
  }

  return uniqueTokens(tokens);
}

async function enforceRateLimit(uid, { perMinute = 3, perDay = 30 } = {}) {
  const db = getFirestore();
  const ref = db.collection("rateLimits").doc(uid);
  const now = Date.now();
  const minuteKey = Math.floor(now / 60000);
  const dayKey = Math.floor(now / 86400000);

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const data = snap.exists ? snap.data() : {};
    let minuteCount = data.minuteKey === minuteKey ? data.minuteCount || 0 : 0;
    let dayCount = data.dayKey === dayKey ? data.dayCount || 0 : 0;
    if (minuteCount >= perMinute) {
      throw new HttpsError("resource-exhausted", "تم تجاوز حد الاستخدام لهذه الدقيقة. حاول بعد قليل.");
    }
    if (dayCount >= perDay) {
      throw new HttpsError("resource-exhausted", "تم تجاوز الحد اليومي للمساعد.");
    }
    tx.set(ref, {
      minuteKey,
      minuteCount: minuteCount + 1,
      dayKey,
      dayCount: dayCount + 1,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
  });
}

exports.generateGemini = onCall(
  { region: REGION, secrets: [GEMINI_API_KEY], enforceAppCheck: true, timeoutSeconds: 60 },
  async (req) => {
    if (!req.auth?.uid) throw new HttpsError("unauthenticated", "سجّل الدخول أولاً");
    const mode = String(req.data?.mode || "");
    const text = String(req.data?.text || "").trim();
    if (!mode || !text) throw new HttpsError("invalid-argument", "mode");
    if (text.length < 2 || text.length > 2000) {
      throw new HttpsError("invalid-argument", "طول النص غير مقبول");
    }
    await enforceRateLimit(req.auth.uid);
    const userPrompt = buildUserPrompt(mode, text);
    const apiKey = GEMINI_API_KEY.value();
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent?key=${apiKey}`;
    const body = {
      systemInstruction: { parts: [{ text: SYSTEM_INSTRUCTION }] },
      contents: [{ role: "user", parts: [{ text: userPrompt }] }],
      generationConfig: { temperature: 0.4, maxOutputTokens: 1024 },
    };
    const res = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (!res.ok) {
      const errText = await res.text();
      console.error("gemini error", res.status, errText);
      throw new HttpsError("internal", "تعذر توليد الرد من المساعد.");
    }
    const json = await res.json();
    const reply = json?.candidates?.[0]?.content?.parts?.map((p) => p.text).join("") || "";
    return { reply: reply.trim() };
  },
);

exports.deleteAccount = onCall(
  { region: REGION, enforceAppCheck: true, timeoutSeconds: 120 },
  async (req) => {
    if (!req.auth?.uid) {
      throw new HttpsError("unauthenticated", "سجّل الدخول أولاً");
    }
    const uid = req.auth.uid;
    const db = getFirestore();
    const steps = [];

    try {
      try {
        await getStorage().bucket().deleteFiles({ prefix: `feedback/${uid}/` });
        steps.push("storage");
      } catch (storageErr) {
        const code = storageErr?.code || storageErr?.errorCode || "";
        if (code !== 404 && !String(storageErr?.message || "").includes("No such object")) {
          console.warn("deleteAccount storage", uid, storageErr);
        }
        steps.push("storage_skipped");
      }

      const fb = await db.collection("feedback").where("userId", "==", uid).get();
      for (const d of fb.docs) {
        await db.recursiveDelete(d.ref);
      }
      steps.push(`feedback:${fb.size}`);

      const userRef = db.collection("users").doc(uid);
      if ((await userRef.get()).exists) {
        await db.recursiveDelete(userRef);
      }
      steps.push("user");

      await Promise.all([
        db.collection("rateLimits").doc(uid).delete().catch(() => null),
        db.collection("admins").doc(uid).delete().catch(() => null),
      ]);
      steps.push("ancillary");

      await getAuth().deleteUser(uid);
      steps.push("auth");

      await db.collection("audit_logs").add({
        type: "delete_account",
        uid,
        steps,
        at: FieldValue.serverTimestamp(),
      }).catch(() => null);

      return { ok: true, steps };
    } catch (err) {
      console.error("deleteAccount failed", uid, steps, err);
      throw new HttpsError(
        "internal",
        "تعذر إكمال حذف الحساب. أعد المحاولة لاحقاً.",
        { steps, message: err?.message || String(err) },
      );
    }
  },
);

exports.dispatchOwnerNotification = onDocumentCreated(
  { document: "notifications/{notificationId}", region: REGION },
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;
    const notification = snapshot.data();
    if (notification.status !== "queued") return;

    const tokens = await getBroadcastTokens(notification.audience || "all");
    const result = await sendToTokens(tokens, {
      notification: { title: notification.title, body: notification.body },
      data: {
        notificationId: event.params.notificationId,
        title: notification.title,
        body: notification.body,
      },
      android: { priority: "high", notification: { channelId: "owner_updates" } },
    });
    await snapshot.ref.update({
      status: "sent",
      deliveryCount: result.successCount,
      failedCount: result.failureCount,
      prunedTokens: result.pruned || 0,
      sentAt: FieldValue.serverTimestamp(),
    });
    await getFirestore().collection("audit_logs").add({
      type: "broadcast",
      notificationId: event.params.notificationId,
      recipients: result.successCount,
      failed: result.failureCount,
      pruned: result.pruned || 0,
      at: FieldValue.serverTimestamp(),
    });
  },
);

exports.processAdminJob = onDocumentCreated(
  { document: "admin_jobs/{jobId}", region: REGION },
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;
    const job = snapshot.data();
    if (job.status && job.status !== "pending") return;

    try {
      await snapshot.ref.update({ status: "processing", startedAt: FieldValue.serverTimestamp() });
      const payload = job.payload || job;
      let tokens = [];
      let message = {};

      if (job.type === "broadcast") {
        tokens = await getBroadcastTokens(payload.audience || "all");
        message = {
          notification: { title: payload.title || "تحديث من الباقيات", body: payload.body || "لديك تحديث جديد" },
          data: { type: "broadcast", jobId: event.params.jobId },
          android: { priority: "high", notification: { channelId: "owner_updates" } },
        };
      } else if (job.type === "feedback_reply") {
        if (!payload.userId) throw new Error("feedback_reply requires userId");
        const user = await getFirestore().collection("users").doc(payload.userId).get();
        tokens = user.data()?.fcmTokens || [];
        message = {
          notification: { title: payload.title || "رد جديد من الباقيات", body: payload.body || "لديك رد جديد" },
          data: { type: "feedback_reply", feedbackId: payload.feedbackId || "", jobId: event.params.jobId },
          android: { priority: "high", notification: { channelId: "owner_updates" } },
        };
      } else {
        throw new Error(`Unsupported admin job type: ${job.type}`);
      }

      const result = await sendToTokens(tokens, message);
      await snapshot.ref.update({
        status: "completed",
        deliveryCount: result.successCount,
        failedCount: result.failureCount,
        prunedTokens: result.pruned || 0,
        completedAt: FieldValue.serverTimestamp(),
      });
    } catch (error) {
      console.error("admin_jobs processing failed", event.params.jobId, error);
      await snapshot.ref.update({
        status: "failed",
        error: error.message || "Unknown error",
        failedAt: FieldValue.serverTimestamp(),
      });
    }
  },
);

exports.notifyUserOnFeedbackReply = onDocumentUpdated(
  { document: "feedback/{feedbackId}", region: REGION },
  async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();
    if (!after || !after.adminReply || after.adminReply === before.adminReply || !after.userId) return;
    const db = getFirestore();
    const userRef = db.collection("users").doc(after.userId);
    await event.data.after.ref.update({ replyUnread: true });
    const user = await userRef.get();
    const title = "تم الرد على شكواك";
    const body = String(after.adminReply).slice(0, 160);
    await userRef.collection("notifications").add({
      title,
      body,
      feedbackId: event.params.feedbackId,
      createdAt: FieldValue.serverTimestamp(),
    });
    await sendToTokens(user.data()?.fcmTokens || [], {
      notification: { title, body },
      data: { feedbackId: event.params.feedbackId, type: "feedback_reply" },
      android: { priority: "high", notification: { channelId: "owner_updates" } },
    });
  },
);
