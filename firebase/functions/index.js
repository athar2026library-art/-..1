const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { defineSecret } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");
const GEMINI_MODEL = "gemini-2.5-flash";

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

exports.generateGemini = onCall(
  { region: "us-central1", enforceAppCheck: true, secrets: [GEMINI_API_KEY], timeoutSeconds: 20 },
  async (request) => {
    if (!request.auth?.uid) {
      throw new HttpsError("unauthenticated", "يجب تسجيل الدخول لاستخدام المساعد الذكي.");
    }
    const prompt = typeof request.data?.prompt === "string" ? request.data.prompt.trim() : "";
    if (!prompt || prompt.length > 4000) {
      throw new HttpsError("invalid-argument", "نص الطلب غير صالح أو طويل جداً.");
    }

    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent?key=${encodeURIComponent(GEMINI_API_KEY.value())}`,
      {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }],
          systemInstruction: {
            parts: [{ text: "أنت مساعد إسلامي متخصص في الأذكار والدعاء. اعتمد فقط على الأحاديث الصحيحة وكتاب حصن المسلم. قدم إجاباتك بأسلوب ميسر، مختصر وهادئ. لا تفتي ولا تصدر أحكاماً شرعية." }],
          },
          generationConfig: { temperature: 0.4 },
        }),
      },
    );
    if (!response.ok) {
      const status = response.status === 429 ? "resource-exhausted" : "internal";
      throw new HttpsError(status, "تعذر الحصول على إجابة من المساعد الذكي.");
    }
    const payload = await response.json();
    const text = payload.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!text) throw new HttpsError("internal", "لم يتم العثور على إجابة صالحة.");
    return { text };
  },
);

exports.dispatchOwnerNotification = onDocumentCreated("notifications/{notificationId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) return;
  const notification = snapshot.data();
  if (notification.status !== "queued") return;

  const tokens = await getBroadcastTokens(notification.audience || "all");
  const result = await sendToTokens(tokens, {
    notification: { title: notification.title, body: notification.body },
    data: { notificationId: event.params.notificationId, title: notification.title, body: notification.body },
    android: { priority: "high", notification: { channelId: "owner_updates" } },
  });
  await snapshot.ref.update({
    status: "sent",
    deliveryCount: result.successCount,
    failedCount: result.failureCount,
    sentAt: FieldValue.serverTimestamp(),
  });
});

exports.processAdminJob = onDocumentCreated("admin_jobs/{jobId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) return;
  const job = snapshot.data();
  if (job.status && job.status !== "pending") return;

  await snapshot.ref.update({ status: "processing", processingStartedAt: FieldValue.serverTimestamp() });
  try {
    const payload = job.payload || {};
    let tokens = [];
    let message;
    if (job.type === "broadcast") {
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
    console.error("admin_jobs processing failed", event.params.jobId, error);
    await snapshot.ref.update({
      status: "failed",
      error: error.message || "Unknown error",
      failedAt: FieldValue.serverTimestamp(),
    });
  }
});

exports.notifyUserOnFeedbackReply = onDocumentUpdated("feedback/{feedbackId}", async (event) => {
  const before = event.data.before.data();
  const after = event.data.after.data();
  if (!after || !after.adminReply || after.adminReply === before.adminReply || !after.userId) return;
  const db = getFirestore();
  const userRef = db.collection("users").doc(after.userId);
  await event.data.after.ref.update({ replyUnread: true });
  const user = await userRef.get();
  const title = "تم الرد على شكواك";
  const body = after.adminReply.slice(0, 160);
  await userRef.collection("notifications").add({ title, body, feedbackId: event.params.feedbackId, createdAt: FieldValue.serverTimestamp() });
  await sendToTokens(user.data()?.fcmTokens || [], {
    notification: { title, body },
    data: { feedbackId: event.params.feedbackId, type: "feedback_reply" },
    android: { priority: "high", notification: { channelId: "owner_updates" } },
  });
});
