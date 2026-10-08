/**
 * Firestore rules — path-by-path tests matching the Android app + admin panel.
 * Run from firebase/: npm run test:rules
 */
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { after, before, beforeEach, describe, it } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  addDoc,
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  setDoc,
  updateDoc,
  where,
} from 'firebase/firestore';

const __dir = dirname(fileURLToPath(import.meta.url));
const rules = readFileSync(join(__dir, '../firestore.rules'), 'utf8');

const ZEK_PUB = {
  text: 'سبحان الله وبحمده',
  category: 'sabah',
  published: true,
  repeat: 1,
  fadl: '',
  source: 'مسلم',
};
const ZEK_DRAFT = { ...ZEK_PUB, published: false, text: 'مسودة غير ظاهرة' };
const FEEDBACK = {
  userId: 'user-a',
  message: 'اقتراح لتحسين الورد',
  title: 'اقتراح مستخدم',
  type: 'suggestion',
  status: 'new',
  adminReply: '',
  replyUnread: false,
};

let env;

function unauth() {
  return env.unauthenticatedContext().firestore();
}
function user(uid = 'user-a', claims = {}) {
  return env.authenticatedContext(uid, { email: `${uid}@mail.test`, email_verified: true, ...claims }).firestore();
}
function adminClaim() {
  return user('claim-admin', { admin: true, email: 'claim-admin@mail.test' });
}
function ownerEmail() {
  return env.authenticatedContext('owner-mail', {
    email: 'someone@gmail.com',
    email_verified: true,
  }).firestore();
}
function staff() {
  return user('staff1', { email: 'staff@mail.test' });
}
function superStaff() {
  return user('super1', { email: 'super@mail.test' });
}

async function seed() {
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.doc('content/azkar/items/pub1').set(ZEK_PUB);
    await db.doc('content/azkar/items/draft1').set(ZEK_DRAFT);
    await db.doc('admins/staff1').set({ role: 'admin', email: 'staff@mail.test' });
    await db.doc('admins/super1').set({ role: 'super_admin' });
    await db.doc('users/user-a').set({ fcmTokens: ['tok-a'], updatedAt: new Date() });
    await db.doc('users/user-a/progress/2026-09-26').set({
      date: '2026-09-26',
      completedSabah: true,
      completedMasaa: false,
      totalTasbeeh: 12,
    });
    await db.doc('feedback/fb1').set(FEEDBACK);
    await db.doc('users/user-a/feedback/fb1').set(FEEDBACK);
  });
}

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'baqiyat-rules-test',
    firestore: { rules, host: '127.0.0.1', port: 8181 },
  });
});

after(async () => {
  await env?.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
  await seed();
});

describe('content / azkar', () => {
  it('زائر يقرأ ذكراً منشوراً', async () => {
    await assertSucceeds(getDoc(doc(unauth(), 'content', 'azkar', 'items', 'pub1')));
  });

  it('زائر لا يقرأ مسودة', async () => {
    await assertFails(getDoc(doc(unauth(), 'content', 'azkar', 'items', 'draft1')));
  });

  it('استعلام المنشورة ينجح للزائر', async () => {
    const q = query(collection(unauth(), 'content', 'azkar', 'items'), where('published', '==', true));
    await assertSucceeds(getDocs(q));
  });

  it('استعلام بلا فلتر published يفشل للزائر', async () => {
    await assertFails(getDocs(collection(unauth(), 'content', 'azkar', 'items')));
  });

  it('مستخدم عادي لا يكتب أذكاراً', async () => {
    await assertFails(setDoc(doc(user(), 'content', 'azkar', 'items', 'x'), ZEK_PUB));
  });

  it('مشرف يضيف ذكراً صالحاً', async () => {
    await assertSucceeds(setDoc(doc(staff(), 'content', 'azkar', 'items', 'new1'), ZEK_PUB));
  });

  it('مشرف يقرأ المسودة', async () => {
    await assertSucceeds(getDoc(doc(staff(), 'content', 'azkar', 'items', 'draft1')));
  });

  it('مشرف لا يضيف تصنيفاً بلا مستند تصنيف', async () => {
    await assertFails(setDoc(doc(staff(), 'content', 'azkar', 'items', 'bad'), { ...ZEK_PUB, category: 'free' }));
  });

  it('مشرف لا يضيف نصاً فارغاً', async () => {
    await assertFails(setDoc(doc(staff(), 'content', 'azkar', 'items', 'bad'), { ...ZEK_PUB, text: '' }));
  });
});

describe('content / categories (ديناميكية)', () => {
  const CAT = { title: 'أذكار المنزل', subtitle: 'عند الدخول والخروج', icon: 'home', order: 5, published: true };

  it('زائر يقرأ تصنيفاً منشوراً', async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), 'content', 'categories', 'items', 'home'), CAT);
    });
    await assertSucceeds(getDoc(doc(unauth(), 'content', 'categories', 'items', 'home')));
  });

  it('مشرف ينشئ تصنيفاً صالحاً', async () => {
    await assertSucceeds(setDoc(doc(staff(), 'content', 'categories', 'items', 'home'), CAT));
  });

  it('مستخدم عادي لا ينشئ تصنيفاً', async () => {
    await assertFails(setDoc(doc(user(), 'content', 'categories', 'items', 'home'), CAT));
  });

  it('معرّفات محجوزة ومخالفة مرفوضة', async () => {
    for (const id of ['sabah', 'favorites', 'wird_x', 'ABC', 'a']) {
      await assertFails(setDoc(doc(staff(), 'content', 'categories', 'items', id), CAT));
    }
  });

  it('أيقونة أو حقل غير معروف مرفوض', async () => {
    await assertFails(setDoc(doc(staff(), 'content', 'categories', 'items', 'home'), { ...CAT, icon: 'rocket' }));
    await assertFails(setDoc(doc(staff(), 'content', 'categories', 'items', 'home'), { ...CAT, extra: 1 }));
  });

  it('ذكر في تصنيف ديناميكي ينجح فقط إن وُجد التصنيف', async () => {
    await assertFails(setDoc(doc(staff(), 'content', 'azkar', 'items', 'z-home'), { ...ZEK_PUB, category: 'home' }));
    await assertSucceeds(setDoc(doc(staff(), 'content', 'categories', 'items', 'home'), CAT));
    await assertSucceeds(setDoc(doc(staff(), 'content', 'azkar', 'items', 'z-home'), { ...ZEK_PUB, category: 'home' }));
  });
});

describe('users / profile', () => {
  it('المالك يدمج توكن FCM', async () => {
    await assertSucceeds(setDoc(doc(user(), 'users', 'user-a'), { fcmTokens: ['tok-b'], updatedAt: new Date() }, { merge: true }));
  });

  it('المالك لا يمنح نفسه role', async () => {
    await assertFails(setDoc(doc(user(), 'users', 'user-a'), { role: 'admin' }, { merge: true }));
  });

  it('مستخدم لا يكتب ملف غيره', async () => {
    await assertFails(setDoc(doc(user('user-b'), 'users', 'user-a'), { fcmTokens: ['x'] }));
  });

  it('مشرف يقرأ ملف المستخدم', async () => {
    await assertSucceeds(getDoc(doc(staff(), 'users', 'user-a')));
  });

  it('مشرف لا يكتب ملف المستخدم', async () => {
    await assertFails(setDoc(doc(staff(), 'users', 'user-a'), { fcmTokens: ['hijack'] }, { merge: true }));
  });
});

describe('users / progress', () => {
  it('المالك يكتب تقدماً صالحاً', async () => {
    await assertSucceeds(setDoc(doc(user(), 'users', 'user-a', 'progress', '2026-09-27'), {
      date: '2026-09-27',
      completedSabah: false,
      completedMasaa: true,
      totalTasbeeh: 3,
    }));
  });

  it('تاريخ المستند يجب أن يطابق الحقل', async () => {
    await assertFails(setDoc(doc(user(), 'users', 'user-a', 'progress', '2026-09-27'), {
      date: '2026-01-01',
      completedSabah: false,
      completedMasaa: false,
      totalTasbeeh: 0,
    }));
  });

  it('تسبيح سالب مرفوض', async () => {
    await assertFails(setDoc(doc(user(), 'users', 'user-a', 'progress', '2026-09-27'), {
      date: '2026-09-27',
      completedSabah: false,
      completedMasaa: false,
      totalTasbeeh: -1,
    }));
  });

  it('لا مجموعات فرعية عشوائية تحت المستخدم', async () => {
    await assertFails(setDoc(doc(user(), 'users', 'user-a', 'secrets', 'k'), { v: 1 }));
  });
});

describe('feedback', () => {
  it('المالك ينشئ شكوى صالحة (technical)', async () => {
    await assertSucceeds(addDoc(collection(user(), 'feedback'), {
      ...FEEDBACK,
      type: 'technical',
      message: 'مشكلة تقنية في الصوت',
    }));
  });

  it('لا إنشاء برد إداري مزيف', async () => {
    await assertFails(addDoc(collection(user(), 'feedback'), { ...FEEDBACK, adminReply: 'تم' }));
  });

  it('لا إنشاء بحالة closed', async () => {
    await assertFails(addDoc(collection(user(), 'feedback'), { ...FEEDBACK, status: 'closed' }));
  });

  it('المالك يؤشر الرد كمقروء', async () => {
    await assertSucceeds(updateDoc(doc(user(), 'feedback', 'fb1'), { replyUnread: true }));
  });

  it('المالك لا يغيّر رد المشرف', async () => {
    await assertFails(updateDoc(doc(user(), 'feedback', 'fb1'), { adminReply: 'أنا المشرف' }));
  });

  it('مستخدم آخر لا يقرأ الشكوى', async () => {
    await assertFails(getDoc(doc(user('user-b'), 'feedback', 'fb1')));
  });

  it('مشرف يقرأ ويرد', async () => {
    await assertSucceeds(getDoc(doc(staff(), 'feedback', 'fb1')));
    await assertSucceeds(updateDoc(doc(staff(), 'feedback', 'fb1'), {
      adminReply: 'جزاك الله خيراً',
      status: 'replied',
      replyUnread: true,
    }));
  });

  it('مشرف لا يغيّر userId', async () => {
    await assertFails(updateDoc(doc(staff(), 'feedback', 'fb1'), { userId: 'user-b', adminReply: 'x' }));
  });
});

describe('notifications + admin_jobs + system', () => {
  it('مستخدم لا ينشئ إشعاراً عاماً', async () => {
    await assertFails(addDoc(collection(user(), 'notifications'), {
      title: 'تجربة', body: 'نص', status: 'queued',
    }));
  });

  it('مشرف ينشئ إشعاراً queued', async () => {
    await assertSucceeds(addDoc(collection(staff(), 'notifications'), {
      title: 'تجربة الإشعارات', body: 'هذا إشعار تجريبي', status: 'queued',
    }));
  });

  it('مشرف لا ينشئ إشعاراً بحالة sent', async () => {
    await assertFails(addDoc(collection(staff(), 'notifications'), {
      title: 'x', body: 'y', status: 'sent',
    }));
  });

  it('مشرف لا يحدّث الإشعار (الدالة وحدها)', async () => {
    await env.withSecurityRulesDisabled(async (ctx) => {
      await ctx.firestore().doc('notifications/n1').set({ title: 'a', body: 'b', status: 'queued' });
    });
    await assertFails(updateDoc(doc(staff(), 'notifications', 'n1'), { status: 'sent' }));
  });

  it('مشرف ينشئ admin_jobs pending', async () => {
    await assertSucceeds(addDoc(collection(staff(), 'admin_jobs'), {
      type: 'fcm_broadcast', status: 'pending',
    }));
  });

  it('مشرف لا ينشئ مهمة done', async () => {
    await assertFails(addDoc(collection(staff(), 'admin_jobs'), {
      type: 'fcm_broadcast', status: 'done',
    }));
  });

  it('مشرف لا يكتب system/fcm_rate', async () => {
    await assertFails(setDoc(doc(staff(), 'system', 'fcm_rate'), { count: 0 }));
  });
});

describe('admins RBAC', () => {
  it('مستخدم لا ينشئ مشرفاً', async () => {
    await assertFails(setDoc(doc(user(), 'admins', 'user-a'), { role: 'super_admin' }));
  });

  it('مشرف عادي لا يضيف مشرفاً', async () => {
    await assertFails(setDoc(doc(staff(), 'admins', 'new-admin'), { role: 'admin' }));
  });

  it('super_admin يضيف مشرفاً', async () => {
    await assertSucceeds(setDoc(doc(superStaff(), 'admins', 'new-admin'), { role: 'admin' }));
  });

  it('إيميل معروف بلا مطالبة ولا مستند admins لا يكفي', async () => {
    await assertFails(setDoc(doc(ownerEmail(), 'admins', 'seeded'), { role: 'super_admin' }));
  });

  it('مطالبة admin تمنح صلاحية كتابة ذكر', async () => {
    await assertSucceeds(setDoc(doc(adminClaim(), 'content', 'azkar', 'items', 'from-claim'), ZEK_PUB));
  });

  it('زائر لا يقرأ مجموعة المشرفين', async () => {
    await assertFails(getDocs(collection(unauth(), 'admins')));
  });
});
