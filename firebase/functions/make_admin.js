const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

const email = (process.argv[2] || "").trim().toLowerCase();
const projectId = (process.env.GCLOUD_PROJECT || process.env.GCP_PROJECT || "").trim();
if (!email || !email.includes("@") || !projectId) {
  console.error("Usage: GCLOUD_PROJECT=your-firebase-project node make_admin.js owner@example.com");
  process.exit(2);
}

initializeApp({ projectId });

(async () => {
  const auth = getAuth();
  const user = await auth.getUserByEmail(email);
  await auth.setCustomUserClaims(user.uid, { ...(user.customClaims || {}), admin: true });
  await getFirestore().collection("admins").doc(user.uid).set(
    { email, role: "super_admin", updatedAt: FieldValue.serverTimestamp() },
    { merge: true },
  );
  console.log(`Admin claims and admins/${user.uid} updated for ${email}`);
  console.log("The user must sign out and sign in again for the claim to refresh.");
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
