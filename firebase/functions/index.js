const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

exports.dispatchOwnerNotification = onDocumentCreated("notifications/{notificationId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) return;
  const notification = snapshot.data();
  if (notification.status !== "queued") return;

  const db = getFirestore();
  const users = await db.collection("users").where("notificationsEnabled", "==", true).limit(500).get();
  const tokens = users.docs.flatMap((user) => user.data().fcmTokens || []).filter(Boolean);

  if (!tokens.length) {
    await snapshot.ref.update({ status: "sent", deliveryCount: 0, sentAt: FieldValue.serverTimestamp() });
    return;
  }

  const result = await getMessaging().sendEachForMulticast({
    tokens,
    notification: { title: notification.title, body: notification.body },
    data: { notificationId: event.params.notificationId, title: notification.title, body: notification.body },
    android: { priority: "high", notification: { channelId: "owner_updates" } },
  });

  await snapshot.ref.update({ status: "sent", deliveryCount: result.successCount, failedCount: result.failureCount, sentAt: FieldValue.serverTimestamp() });
});
