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
const GEMINI_MODEL = "gemini-2.5-flash";
const REGION = "europe-west1";

const SYSTEM_INSTRUCTION =
  "أنت مساعد إسلامي متخصص في الأذكار والدعاء. اعتمد فقط على الأحاديث الصحيحة وكتاب حصن المسلم. " +
  "قدم إجاباتك بأسلوب ميسر، مختصر جداً، وهادئ. لا تفتِ ولا تصدر أحكاماً شرعية من عندك. " +
  "في نهاية كل إجابة أضف سطراً: (إجابة آلية، ليست فتوى).";

async function sendToTokens(tokens, message) {
  const unique = uniqueTokens(tokens);
  let successCount = 0;
  let failureCount = 0;
  for (let index = 0; index < unique.length; index += 500) {
    const result = await getMessaging().sendEachForMulticast({
      tokens: unique.slice(index, index + 500),
      ...message,
    });
    successCount += result.successCount;
    failureCount += result.failureCount;
  }
  return { successCount, failureCount };
}

async function getBroadcastTokens(audience = "all") {
  const db = getFirestore();
  let usersQuery = db.collection("users").where("notificationsEnabled", "==", true);
  if (audience !== "all") {
    usersQuery = usersQuery.where("notificationAudience", "==", audience);
  }
  const users = await usersQuery.get();
  return users.docs.flatMap((user) => user.data().fcmTokens || []);
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
    if (minuteCount >= perMinute || dayCount >= perDay) {
      throw new HttpsError("resource-exhausted", "وصلت لحد الاستخدام. حاول بعد قليل.");
    }
    tx.set(
      ref,
      {
        minuteKey,
        minuteCount: minuteCount + 1,
        dayKey,
        dayCount: dayCount + 1,
        updatedAt: FieldValue.serverTimestamp(),
      },
      { merge: true },
    );
  });
}

/** Refund one unit if Gemini call failed after consuming quota. */
async function refundRateLimit(uid) {
  const db = getFirestore();
  const ref = db.collection("rateLimits").doc(uid);
  const now = Date.now();
  const minuteKey = Math.floor(now / 60000);
  const dayKey = Math.floor(now / 86400000);
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists) return;
    const data = snap.data() || {};
    const minuteCount =
      data.minuteKey === minuteKey ? Math.max(0, (data.minuteCount || 0) - 1) : data.minuteCount || 0;
    const dayCount =
      data.dayKey === dayKey ? Math.max(0, (data.dayCount || 0) - 1) : data.dayCount || 0;
    tx.set(ref, { minuteCount, dayCount, updatedAt: FieldValue.serverTimestamp() }, { merge: true });
  });
}

exports.generateGemini = onCall(
  {
    region: REGION,
    enforceAppCheck: true,
    secrets: [GEMINI_API_KEY],
    timeoutSeconds: 30,
    maxInstances: 10,
  },
  async (req) => {
    if (!req.auth?.uid) {
      throw new HttpsError("unauthenticated", "سجّل الدخول أولاً");
    }

    const { mode, text } = req.data || {};
    if (!["explain", "suggest"].includes(mode)) {
      throw new HttpsError("invalid-argument", "mode");
    }
    if (typeof text !== "string" || text.trim().length < 2 || text.length > 500) {
      throw new HttpsError("invalid-argument", "طول النص غير مقبول");
    }

    await enforceRateLimit(req.auth.uid, { perMinute: 3, perDay: 30 });

    const userPrompt = buildUserPrompt(mode, text.trim());

    let response;
    try {
      response = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent`,
        {
          method: "POST",
          headers: {
            "content-type": "application/json",
            "x-goog-api-key": GEMINI_API_KEY.value(),
          },
          body: JSON.stringify({
            contents: [{ parts: [{ text: userPrompt }] }],
            systemInstruction: { parts: [{ text: SYSTEM_INSTRUCTION }] },
            generationConfig: { temperature: 0.4 },
          }),
          signal: AbortSignal.timeout(20000),
        },
      );
    } catch (_e) {
      await refundRateLimit(req.auth.uid).catch(() => {});
      throw new HttpsError("unavailable", "لا اتصال");
    }

    if (response.status === 429) {
      await refundRateLimit(req.auth.uid).catch(() => {});
      throw new HttpsError("resource-exhausted", "وصلت لحد الاستخدام. حاول بعد قليل.");
    }
    if (!response.ok) {
      await refundRateLimit(req.auth.uid).catch(() => {});
      throw new HttpsError("internal", "تعذر الحصول على إجابة من المساعد.");
    }

    const payload = await response.json();
    const out = payload.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!out) {
      await refundRateLimit(req.auth.uid).catch(() => {});
      throw new HttpsError("internal", "لم يتم العثور على إجابة صالحة.");
    }
    return { text: out, disclaimer: true };
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

    // 1) Feedback (+ timeline subcollections) before Auth delete
    const fb = await db.collection("feedback").where("userId", "==", uid).get();
    for (const d of fb.docs) {
      await db.recursiveDelete(d.ref);
    }

    // 2) User tree
    await db.recursiveDelete(db.collection("users").doc(uid));

    // 3) Ancillary docs
    await db.collection("rateLimits").doc(uid).delete().catch(() => {});
    await db.collection("admins").doc(uid).delete().catch(() => {});

    // 4) Storage attachments
    await getStorage()
      .bucket()
      .deleteFiles({ prefix: `feedback/${uid}/` })
      .catch(() => {});

    // 5) Auth last so retries remain possible if earlier steps fail
    await getAuth().deleteUser(uid);
    return { ok: true };
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
      sentAt: FieldValue.serverTimestamp(),
    });
    await getFirestore().collection("audit_logs").add({
      type: "broadcast",
      notificationId: event.params.notificationId,
      recipients: result.successCount,
      failed: result.failureCount,
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

    await snapshot.ref.update({ status: "processing", processingStartedAt: FieldValue.serverTimestamp() });
    try {
      const payload = job.payload || {};
      let tokens = [];
      let message;
      if (job.type === "broadcast" || job.type === "fcm_broadcast") {
        tokens = await getBroadcastTokens(payload.audience || "all");
        message = {
          notification: { title: payload.title || "الباقيات", body: payload.body || "لديك تحديث جديد" },
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
        completedAt: FieldValue.serverTimestamp(),
      });
    } catch (error) {
      console.error("admin_jobs processing failed", event.params.jobId);
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
