# Firebase deployment contract

ضع ملف `google-services.json` في جذر `app/` بعد تنزيله من Firebase Console. لا ترفع الملف أو أي مفاتيح سرية إلى الدردشة أو ملفات الواجهة. ملف Android هذا ليس بديلاً عن إعداد Web App للوحة.

## التفعيل المطلوب

- Firebase Authentication: Google provider.
- Cloud Firestore.
- Custom claim للمشرف: `admin: true`، ويُمنح من Cloud Function أو Admin SDK موثوق فقط.
- نشر `firestore.rules` قبل إدخال بيانات الإنتاج.

## النشر

```bash
firebase deploy --only firestore:rules
```

لا تستخدم `firebase deploy` قبل تسجيل الدخول وتحديد المشروع الصحيح. تحقق من مشروع Firebase قبل كل نشر لتجنب الكتابة في مشروع غير مقصود.
