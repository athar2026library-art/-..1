/**
 * One-shot script: set Firebase Auth custom claim { admin: true } for a user.
 *
 * Prerequisites (run from firebase/functions after firebase login):
 *   npm install
 *   export GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json
 *   # or: gcloud auth application-default login
 *
 * Usage:
 *   node make_admin.js
 *
 * Then edit TARGET_UID below to your Firebase Auth UID (Console → Authentication).
 */
const admin = require("firebase-admin");

// ========== عدّل هذا فقط ==========
const TARGET_UID = "REPLACE_WITH_YOUR_FIREBASE_AUTH_UID";
// ==================================

if (!admin.apps.length) {
  admin.initializeApp({
    projectId: process.env.GCLOUD_PROJECT || process.env.GCP_PROJECT || "svrpmtt",
  });
}

async function main() {
  if (!TARGET_UID || TARGET_UID.startsWith("REPLACE_")) {
    console.error("Edit TARGET_UID in make_admin.js to your real Firebase Auth UID.");
    process.exit(1);
  }

  await admin.auth().setCustomUserClaims(TARGET_UID, { admin: true });
  const user = await admin.auth().getUser(TARGET_UID);
  console.log("OK — custom claims for", TARGET_UID, ":", user.customClaims);
  console.log("User must sign out and sign in again for the claim to appear in the ID token.");
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
