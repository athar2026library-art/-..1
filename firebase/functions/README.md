# Cloud Functions للباقيات

تحتوي الحزمة على الدوال التالية:

- `generateGemini`: مساعد الأذكار؛ يستخدم Secret Manager ويُقيَّد بتسجيل الدخول وApp Check وحد الاستخدام.
- `recordAzkarRead`: يسجل الذكر عند إكماله ويحدّث إحصاءات لوحة المالك مع حدّ معدل منفصل.
- `dispatchOwnerNotification` و`processAdminJob`: معالجة الإشعارات ومهام المالك.
- `deleteAccount`: حذف الحساب وبياناته عبر الخادم.

## تهيئة المساعد والإحصاءات

من مجلد `firebase/`:

```bash
firebase functions:secrets:set GEMINI_API_KEY
firebase deploy --only functions
```

أدخل المفتاح في الطلب التفاعلي داخل Firebase CLI فقط؛ لا تضعه في ملفات المستودع أو إعدادات Android. فعّل مزوّد Anonymous في Firebase Authentication إذا كنت تريد إتاحة المساعد وتسجيل الإحصاءات قبل ربط المستخدم بحساب Google. انشر قواعد Firestore من `firebase/firestore.rules` حتى يستطيع المشرف قراءة `owner_metrics/summary`، بينما تظل الكتابة محصورة في Cloud Functions.

**ملاحظة:** إحصاءات لوحة المالك تتجمع من تاريخ نشر هذه الدوال؛ لا يتم توليد بيانات تاريخية للقراءات أو الأسئلة التي حدثت قبل التفعيل.

## الإشعارات

هذه الوظيفة تراقب `notifications/{notificationId}`. عندما تكون قيمة `status` مساوية لـ`queued`، تجمع رموز FCM من المستخدمين الذين فعّلوا الإشعارات، وترسل الرسالة عبر Firebase Admin SDK، ثم تحدّث سجل الإشعار إلى `sent` مع أعداد النجاح والفشل.

## النشر

من جذر إعداد Firebase:

```bash
cd firebase/functions
npm install
firebase deploy --only functions:dispatchOwnerNotification
```

لا تضع Service Account JSON أو أي مفتاح خاص داخل المستودع. يجب أن يتم النشر من حساب يملك صلاحية Firebase Functions وCloud Build.
