/**
 * Example Cloud Function (Node 18+) — deploy separately with Firebase Functions.
 * Listens to admin_jobs and sends FCM (topic "all").
 *
 * NOT deployed automatically — copy into functions/index.js and configure.
 *
 * Rate limit: at most 20 broadcasts per rolling minute via system/fcm_rate.
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

const MAX_PER_MINUTE = 20;

exports.processAdminJobs = functions.firestore
  .document('admin_jobs/{jobId}')
  .onCreate(async (snap, context) => {
    const job = snap.data();
    if (!job || job.status !== 'pending') return null;

    const rateRef = admin.firestore().collection('system').doc('fcm_rate');
    const allowed = await admin.firestore().runTransaction(async (tx) => {
      const rateSnap = await tx.get(rateRef);
      const now = Date.now();
      const data = rateSnap.exists ? rateSnap.data() : { windowStart: now, count: 0 };
      const windowStart = data.windowStart || now;
      let count = data.count || 0;
      if (now - windowStart > 60000) {
        count = 0;
        tx.set(rateRef, { windowStart: now, count: 1 }, { merge: true });
        return true;
      }
      if (count >= MAX_PER_MINUTE) return false;
      tx.set(rateRef, { windowStart, count: count + 1 }, { merge: true });
      return true;
    });

    if (!allowed) {
      await snap.ref.update({
        status: 'rate_limited',
        processedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      return null;
    }

    const title = job.payload?.title || 'الباقيات';
    const body = job.payload?.body || '';

    try {
      await admin.messaging().send({
        topic: 'all',
        notification: { title, body },
        data: {
          type: String(job.type || ''),
          feedbackId: String(job.payload?.feedbackId || ''),
          zekrId: String(job.payload?.zekrId || ''),
        },
      });

      await snap.ref.update({
        status: 'done',
        processedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    } catch (e) {
      console.error(e);
      await snap.ref.update({
        status: 'error',
        error: String(e.message || e),
        processedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }
    return null;
  });
