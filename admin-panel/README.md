# لوحة إدارة الباقيات

لوحة ويب عربية RTL لإدارة محتوى تطبيق Android عبر **Google Auth + Firestore RBAC**.

## الأمان

- لا توجد كلمة مرور ثابتة في الكود.
- الدخول عبر حساب Google فقط.
- يُسمح بالمتابعة إذا وُجد مستند `admins/{uid}` أو مطالبة مخصصة `admin: true`.
- أنشئ أول مشرف يدوياً من Firebase Console:
  1. Authentication → انسخ UID لحسابك.
  2. Firestore → مجموعة `admins` → مستند بالمعرّف = UID.
  3. الحقول: `role: super_admin` و `email: ...`.
- انشر `firestore.rules` من جذر المستودع قبل فتح اللوحة على الإنتاج.

## التشغيل المحلي

من جذر المستودع:

```bash
python3 -m http.server 4173 --directory admin-panel
```

ثم افتح الصفحة محلياً. أضف نطاق `localhost` في Firebase Authentication → Authorized domains.

## الإشعارات

`functions-fcm.example.js` مثال لـ Cloud Function تقرأ `admin_jobs` وترسل إلى موضوع FCM `all`. انسخه إلى `functions/index.js` وانشره يدوياً — لا يُنشر تلقائياً.

## الخصوصية

`robots.txt` و`noindex` يمنعان الفهرسة. هذا **لا يغني** عن RBAC أعلاه.
