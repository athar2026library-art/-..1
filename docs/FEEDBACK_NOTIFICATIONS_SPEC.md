# نظام الشكاوى والاقتراحات والإشعارات

## خريطة القدرات

| الوحدة | المسؤولية | تعتمد على |
|---|---|---|
| identity | تسجيل دخول المستخدم وربط الطلبات به | — |
| feedback | المساعد الذكي، إرسال الشكوى، المتابعة والرد | identity |
| notifications | تسجيل أجهزة FCM وإنشاء حملات الإشعار وسجل الإرسال | identity |
| owner-console | معاينة وإدارة الشكاوى والإشعارات | feedback, notifications |

ترتيب التنفيذ: identity → feedback وnotifications → owner-console.

## الهدف
تمكين مستخدم تطبيق أذكار من وصف شكوى أو اقتراح عبر مساعد عربي، مراجعة الملخص والتصنيف قبل الإرسال، ثم متابعة الحالة والرد. وتمكين المالك من إدارة الطلبات وإرسال إشعارات للمستخدمين.

## قرار تسجيل الدخول
تسجيل الدخول إلزامي عند الإرسال والمتابعة، وليس عند فتح المساعد أو كتابة المسودة. يستخدم التطبيق Google/Firebase Auth الموجود، ويُحفظ الطلب تحت `users/{uid}/feedback/{feedbackId}` مع نسخة إدارية في `feedback/{feedbackId}`.

## عقد البيانات

`feedback/{feedbackId}` يحتوي: `userId`, `userEmail`, `type`, `title`, `message`, `aiSummary`, `aiCategory`, `priority`, `status`, `adminReply`, `internalNote`, `createdAt`, `updatedAt`, `resolvedAt`.

`notifications/{notificationId}` يحتوي: `title`, `body`, `audience`, `status`, `createdBy`, `createdAt`, `sentAt`, `deliveryCount`.

`users/{uid}` يحتوي: `fcmTokens`, `appVersion`, `notificationsEnabled`, `updatedAt`.

## الصلاحيات
المستخدم المسجل يقرأ ويحدّث طلباته الأساسية فقط. المشرف ذو Custom Claim `admin == true` يقرأ ويدير الشكاوى والإشعارات. الإرسال الفعلي إلى FCM يتم عبر Cloud Function/خدمة خلفية تستخدم Admin SDK؛ لا تُوضع مفاتيح الخدمة في Android أو المتصفح.

## تجربة المساعد
يفتح المستخدم المساعد، يكتب بحرية، ثم تظهر بطاقة تأكيد تعرض النوع والعنوان والملخص والأولوية. لا يتم إنشاء سجل Firestore إلا بعد تسجيل الدخول والتأكيد.

## الإشعارات
النسخة الأولى تدعم إشعاراً عاماً لجميع المستخدمين مع حفظ سجل الإرسال. تضاف الجماهير المخصصة لاحقاً بعد استقرار تسجيل FCM tokens.

## التحقق
- صفحة لوحة المالك تعرض تبويبي الشكاوى والإشعارات مع بيانات معاينة واضحة.
- `pnpm check` و`pnpm build` ينجحان.
- Android يبني مع Firebase Auth/Firestore/FCM.
- القواعد تمنع القراءة والكتابة غير المصرح بها.
- لا تُرفع أسرار Firebase Admin أو service account إلى Git.

## الحدود
- دائماً: التحقق من الحقول، إظهار حالة الطلب، وعدم إرسال AI نصاً دون تأكيد.
- يحتاج إعداداً خارجياً: نشر Cloud Function وربط Firebase Cloud Messaging، وتأكيد Authorized Domains.
- ممنوع: وضع Server Key أو Service Account في التطبيق أو الموقع.

## المسار التالي
إضافة Cloud Function فعلية للإرسال الجماعي بعد تزويد Firebase Functions/Cloud project بصلاحية النشر، ثم اختبار جهاز Android حقيقي.
