/**
 * Example Cloud Function (Node 18+) — deploy separately with Firebase Functions.
 * Listens to admin_jobs and sends FCM (topic "all" or per-user tokens).
 *
 * NOT deployed automatically — copy into functions/index.js and configure.
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.processAdminJobs = functions.firestore
  .document('admin_jobs/{jobId}')
  .onCreate(async (snap, context) => {
    const job = snap.data();
    if (!job || job.status !== 'pending') return null;

    const title = job.payload?.title || 'الباقيات';
    const body = job.payload?.body || '';

    try {
      // Broadcast to topic "all" — ensure the Android app subscribes to it.
      await admin.messaging().send({
        topic: 'all',
        notification: { title, body },
        data: {
          type: String(job.type || ''),
          feedbackId: String(job.payload?.feedbackId || ''),
          zekrId: String(job.payload?.zekrId || '')
        }
      });

      await snap.ref.update({
        status: 'done',
        processedAt: admin.firestore.FieldValue.serverTimestamp()
      });
    } catch (e) {
      console.error(e);
      await snap.ref.update({
        status: 'error',
        error: String(e.message || e),
        processedAt: admin.firestore.FieldValue.serverTimestamp()
      });
    }
    return null;
  });
