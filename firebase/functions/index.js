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
  const users = await db.collection("users").where("notificationsEnabled", "==", true).get();
  const tokens = users.docs.flatMap((user) => user.data().fcmTokens || []).filter(Boolean);

  if (!tokens.length) {
    await snapshot.ref.update({ status: "sent", deliveryCount: 0, sentAt: FieldValue.serverTimestamp() });
    return;
  }

  let successCount = 0;
  let failureCount = 0;
  for (let index = 0; index < tokens.length; index += 500) {
    const batch = tokens.slice(index, index + 500);
    const result = await getMessaging().sendEachForMulticast({
      tokens: batch,
      notification: { title: notification.title, body: notification.body },
      data: { notificationId: event.params.notificationId, title: notification.title, body: notification.body },
      android: { priority: "high", notification: { channelId: "owner_updates" } },
    });
    successCount += result.successCount;
    failureCount += result.failureCount;
  }

  await snapshot.ref.update({ status: "sent", deliveryCount: successCount, failedCount: failureCount, sentAt: FieldValue.serverTimestamp() });
});
