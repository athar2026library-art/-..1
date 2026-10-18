# اختبار Gemini على Android عبر Firebase AI Logic

يستخدم تطبيق أندرويد حاليًا Firebase AI Logic مع Gemini Developer API. لا يحتاج هذا المسار إلى وضع مفتاح Gemini داخل التطبيق أو في `local.properties`، ولا يستدعي Cloud Function `generateGemini` للمحادثة العادية.

## إعداد المشروع مرة واحدة

1. افتح مشروع `svrpmtt` في [Firebase Console](https://console.firebase.google.com/project/svrpmtt).
2. من **AI Logic** اختر **Get started**، ثم اختر **Gemini Developer API** كمزوّد.
3. من **App Check** اختر تطبيق Android ذي المعرّف `com.baqiyat.azkar`.
4. في نسخة `debug` يستخدم التطبيق `DebugAppCheckProviderFactory`. شغّل التطبيق وافتح Logcat، وابحث عن رسالة Firebase التي تعرض Debug token. سجّل الرمز في Firebase Console ضمن **Manage debug tokens** إذا لم يكن مسجّلًا بالفعل.
5. من **App Check → APIs** تأكد من إعداد سياسة Firebase AI Logic. لا تفعّل enforcement قبل تسجيل رمز Debug الذي تحتاجه للاختبار؛ وإلا فقد تُرفض الطلبات.
6. أعد تشغيل التطبيق بعد حفظ الإعدادات، وجرّب سؤالًا قصيرًا للمساعد.

## التحقق عند استمرار الفشل

- تأكد أن التطبيق يستخدم `app/google-services.json` الذي يحتوي على العميل `com.baqiyat.azkar` ضمن المشروع `svrpmtt`.
- راجع Logcat وابحث عن `Firebase AI Logic request failed` أو رسائل App Check. لا تنسخ Debug token أو أي بيانات اعتماد إلى Issues أو مستودع عام.
- تأكد من اختيار **Gemini Developer API**؛ اختيار Agent Platform / Vertex AI له متطلبات فوترة مختلفة.
- اختبر الاتصال بالإنترنت، ثم جرّب سؤالًا بسيطًا مثل: `ما أذكار الصباح؟`.

## لا تفعل ذلك

- لا تضف مفتاح Gemini إلى `BuildConfig` أو `local.properties` لهذا التكامل.
- لا ترفع Debug token أو ملفات مفاتيح حساب الخدمة إلى Git.
- لا تستخدم نسخة `debug` كتوزيعة إنتاج. يستخدم إصدار `release` مزوّد Play Integrity بدل مزوّد Debug.

> نجاح ترجمة APK لا يثبت وحده أن طلب Gemini الحي يعمل. يجب إكمال إعداد Firebase ثم التحقق من وجود إجابة فعلية داخل التطبيق.
