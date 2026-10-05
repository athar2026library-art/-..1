const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

const email = (process.argv[2] || "mgedh.9ali@gmail.com").trim().toLowerCase();
if (!email || !email.includes("@")) {
  console.error("Usage: node make_admin.js owner@example.com");
  process.exit(2);
}

initializeApp({ projectId: process.env.GCLOUD_PROJECT || process.env.GCP_PROJECT || "svrpmtt" });

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
