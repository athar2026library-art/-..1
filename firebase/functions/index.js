const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { defineSecret } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue, Timestamp } = require("firebase-admin/firestore");
const { getAuth } = require("firebase-admin/auth");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");
const GEMINI_MODEL = "gemini-2.5-flash";
const REGION = "europe-west1";

const SYSTEM_INSTRUCTION =
  "أنت مساعد إسلامي متخصص في الأذكار والدعاء. اعتمد فقط على الأحاديث الصحيحة وكتاب حصن المسلم. " +
  "قدم إجاباتك بأسلوب ميسر، مختصر جداً، وهادئ. لا تفتِ ولا تصدر أحكاماً شرعية من عندك. " +
  "في نهاية كل إجابة أضف سطراً: (إجابة آلية، ليست فتوى).";

function uniqueTokens(tokens) {
  return [...new Set((tokens || []).filter((token) => typeof token === "string" && token.trim()))];
}

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

/** Server-side rate limit: rateLimits/{uid} — clients cannot write (rules deny). */
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

function buildUserPrompt(mode, text) {
  if (mode === "explain") {
    return `قم بشرح وتدبر هذا الذكر بأسلوب إيماني، ميسر ومختصر جداً:\n\n"${text}"`;
  }
  return (
    `أشعر بـ (${text}) أو أحتاج إلى دعاء بهذا الخصوص. اقترح لي ذكراً أو دعاءً من الأحاديث الصحيحة وحصن المسلم يناسب حالتي.\n` +
    "نرجو الرد بالتنسيق التالي حصراً:\nالذكر: [النص]\nفضله: [شرح مبسط]\nالمصدر: [المرجع]"
  );
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
    // Do not log user text (may contain personal feelings).

    let response;
    try {
      response = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent?key=${encodeURIComponent(GEMINI_API_KEY.value())}`,
        {
          method: "POST",
          headers: { "content-type": "application/json" },
          body: JSON.stringify({
            contents: [{ parts: [{ text: userPrompt }] }],
            systemInstruction: { parts: [{ text: SYSTEM_INSTRUCTION }] },
            generationConfig: { temperature: 0.4 },
          }),
        },
      );
    } catch (_e) {
      throw new HttpsError("unavailable", "لا اتصال");
    }

    if (response.status === 429) {
      throw new HttpsError("resource-exhausted", "وصلت لحد الاستخدام. حاول بعد قليل.");
    }
    if (!response.ok) {
      throw new HttpsError("internal", "تعذر الحصول على إجابة من المساعد.");
    }

    const payload = await response.json();
    const out = payload.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!out) throw new HttpsError("internal", "لم يتم العثور على إجابة صالحة.");
    return { text: out, disclaimer: true };
  },
);

/** Delete account: recursive users/{uid} then Auth user. Requires recent login on client. */
exports.deleteAccount = onCall(
  { region: REGION, enforceAppCheck: true, timeoutSeconds: 60 },
  async (req) => {
    if (!req.auth?.uid) {
      throw new HttpsError("unauthenticated", "سجّل الدخول أولاً");
    }
    const uid = req.auth.uid;
    const db = getFirestore();
    await db.recursiveDelete(db.collection("users").doc(uid));
    // Best-effort: remove top-level feedback owned by user
    const fb = await db.collection("feedback").where("userId", "==", uid).get();
    const batch = db.batch();
    fb.docs.forEach((d) => batch.delete(d.ref));
    if (!fb.empty) await batch.commit();
    await db.collection("rateLimits").doc(uid).delete().catch(() => {});
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
