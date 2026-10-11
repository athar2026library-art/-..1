# Firebase deployment contract

ضع ملف `google-services.json` في جذر `app/` بعد تنزيله من Firebase Console. لا ترفع الملف أو أي مفاتيح سرية إلى الدردشة أو ملفات الواجهة. ملف Android هذا ليس بديلاً عن إعداد Web App للوحة.

## التفعيل المطلوب

- Firebase Authentication: Google provider.
- Cloud Firestore.
- Custom claim للمشرف: `admin: true`، ويُمنح من Cloud Function أو Admin SDK موثوق فقط.
- **أو** مستند `admins/{uid}` في Firestore (`role: super_admin` لأول مشرف).
- **لا يوجد بريد مالك ثابت في القواعد.** أول مشرف يُمهَّد من Console / Admin SDK.
- نشر `firestore.rules` قبل إدخال بيانات الإنتاج.

## اختبار القواعد (محاكي محلي)

من مجلد `firebase/`:

```bash
npm install
npm run test:rules
```

`tests/rules.test.mjs` — 39 اختبارًا تغطي: الأذكار المنشورة/المسودات، ملف المستخدم، التقدم، الشكاوى، الإشعارات، `admin_jobs`، `system`، وRBAC (claim / staff / super_admin / بريد عادي لا يكفي).

المحاكي يستمع على `127.0.0.1:8181` (انظر `firebase.json`) لتفادي تعارض المنفذ 8080.

## النشر

من مجلد `firebase/` (حيث `firebase.json`):

```bash
firebase deploy --only firestore:rules,firestore:indexes,storage
```

لا تستخدم `firebase deploy` قبل تسجيل الدخول وتحديد المشروع الصحيح. تحقق من مشروع Firebase قبل كل نشر لتجنب الكتابة في مشروع غير مقصود.
