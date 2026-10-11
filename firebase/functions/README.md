# إشعارات المالك

هذه الوظيفة تراقب `notifications/{notificationId}`. عندما تكون قيمة `status` مساوية لـ`queued`، تجمع رموز FCM من المستخدمين الذين فعّلوا الإشعارات، وترسل الرسالة عبر Firebase Admin SDK، ثم تحدّث سجل الإشعار إلى `sent` مع أعداد النجاح والفشل.

## النشر

من جذر إعداد Firebase:

```bash
cd firebase/functions
npm install
firebase deploy --only functions:dispatchOwnerNotification
```

لا تضع Service Account JSON أو أي مفتاح خاص داخل المستودع. يجب أن يتم النشر من حساب يملك صلاحية Firebase Functions وCloud Build.
