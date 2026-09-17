# نشر الإشعارات والمرفقات

## نشر القواعد

من مجلد Firebase الذي يحتوي على `firebase.json`:

```bash
firebase deploy --only firestore:rules,storage
```

تأكد أن `firebase.json` يشير إلى `firebase/storage.rules` ضمن قسم `storage.rules`.

## نشر Cloud Function

```bash
cd firebase/functions
npm install
firebase deploy --only functions:dispatchOwnerNotification
```

## اختبار الإشعار

بعد نشر الوظيفة وتسجيل جهاز Android بحساب مستخدم، افتح لوحة المالك وأنشئ إشعاراً بعنوان:

```text
تجربة الإشعارات
```

ونص:

```text
هذا إشعار تجريبي من لوحة المالك
```

اختر "جميع المستخدمين". سيُحفظ السجل بحالة `queued`، ثم تحوله Cloud Function إلى `sent`.

## ملاحظة أمنية

لا تستخدم Server Key أو Service Account داخل Android أو لوحة الويب. الوظيفة وحدها تستخدم Firebase Admin SDK.
