# تقرير حالة مشروع الباقيات

**تاريخ التقرير:** 21 سبتمبر 2026
**الفرع:** `feature/private-admin-panel`
**آخر commit:** `f45f403` — `fix: modernize audio player continuations`

## الخلاصة التنفيذية

أصبح مشروع **الباقيات** في حالة تطوير متقدمة مقارنة بالنسخة الأولية. يحتوي تطبيق Android الآن على تجربة قراءة حديثة للأذكار، أذكار صباح ومساء مكتملة بالمصادر والتكرارات، حفظ محسّن للتقدم، اهتزازات تفاعلية، شريط إنجاز، دعم الوضع الليلي، المساعد الذكي، تسجيل الدخول بحساب Google، نظام الشكاوى والاقتراحات، والإشعارات. كما توجد لوحة مالك عربية بتصميم RTL ومتصلة بـ Firebase لإدارة المحتوى والشكاوى والإشعارات.

تم في آخر مرحلة إصلاح استخدام `CancellableContinuation.resume` القديم داخل مشغل الصوت. يستخدم `AudioPlayer` الآن overload الحديث مع `onCancellation`، كما تم تحديث إنشاء اللغة العربية إلى `Locale.forLanguageTag("ar")`. أُضيف اختبار Android lifecycle يتحقق من إنشاء مشغل النطق وإغلاقه بأمان. نجح بناء التطبيق ونجح تجميع اختبارات Android، بينما لم يُنفّذ تشغيل فعلي على جهاز Android لأن بيئة العمل لا تحتوي على جهاز أو محاكي متصل.

## الحالة الحالية حسب النظام

| النظام | الحالة الحالية | الدليل أو الملاحظة |
|---|---|---|
| تطبيق Android | جاهز للبناء والتثبيت | `:app:assembleDebug` نجح |
| شاشة الأذكار | محسّنة للأداء والتفاعل | الحفظ لا يحدث مع كل نقرة، والبطاقة كاملة قابلة للنقر |
| المساعد الذكي | متصل بـ Gemini | النموذج `gemini-2.5-flash` والمفتاح من `BuildConfig.GEMINI_API_KEY` |
| تسجيل الدخول Google | مهيأ | Web Client ID مأخوذ من `client_type = 3` في `google-services.json` |
| Firebase | متصل | Authentication وFirestore وStorage وFCM مستخدمة في التطبيق واللوحة |
| لوحة المالك | منشورة ومتصلة | إدارة المحتوى والشكاوى والإشعارات عبر Firestore |
| اختبارات UI | مضافة ومجمّعة | اختبارات Compose لتسجيل الدخول والأذكار |
| اختبار الصوت | مضاف ومجمّع | اختبار lifecycle لمشغل `AudioPlayer` |
| اختبار جهاز فعلي | لم يُنفذ بعد | لا توجد أجهزة متصلة عبر ADB |

## تحسينات واجهة الأذكار وتجربة القراءة

أُعيد تصميم شاشة القراءة حول التفاعل المباشر. أصبحت بطاقة الذكر بأكملها منطقة النقر بدل الاعتماد على زر صغير، ويؤدي النقر إلى إنقاص العداد فورًا مع اهتزاز خفيف عند تفعيل الاهتزازات. عند وصول العداد إلى الصفر يصدر اهتزاز تأكيدي ثم ينتقل التطبيق إلى الذكر التالي بحركة قصيرة.

أُضيف شريط تقدم علوي يعرض نسبة الإنجاز داخل الورد الحالي. كما أُضيفت أزرار واضحة للانتقال إلى الذكر السابق أو التالي وإعادة ضبط العداد. تظهر حالة الإكمال عند نهاية الورد مع خيار العودة إلى الصفحة الرئيسية.

تم إصلاح نقطة أداء مهمة في `AzkarScreen.kt`. لم يعد التطبيق يستدعي `saveLastReadState` مع كل نقرة تسبيح. يُحفظ التقدم عند تغير `currentIndex` أو عند مغادرة الشاشة عبر `DisposableEffect`. يقلل ذلك عمليات DataStore المتكررة ويمنع منافسة عمليات الإدخال مع عمليات I/O على الواجهة.

اكتملت نصوص الأذكار المشهورة في أذكار الصباح والمساء، ومنها آية الكرسي، المعوذات، سيد الاستغفار، وذكر «أصبحنا وأصبح الملك لله»، مع المصادر وفضائل الذكر وعدد التكرار. كما تم فحص حقول البيانات للتأكد من عدم وجود نصوص أساسية فارغة.

## المساعد الذكي

يستخدم التطبيق نموذج `gemini-2.5-flash` حصراً، ويقرأ مفتاح API من `BuildConfig.GEMINI_API_KEY` بدل تضمينه في منطق الشاشة. تُنفذ طلبات Gemini خارج خيط الواجهة باستخدام `Dispatchers.IO`.

ضُبطت مهلات الاتصال والقراءة والكتابة على 20 ثانية. تعالج طبقة المستودع حالات انقطاع الشبكة وانتهاء المهلة برسالة عربية واضحة، كما تعالج HTTP 429 برسالة تشير إلى تجاوز حد الاستخدام المؤقت. وتحتوي طبقة `AppViewModel` على حالة تحميل مستقلة حتى لا تبقى شاشة المساعد في حالة انتظار بعد فشل الطلب.

تقدم شاشة المساعد رسائل عربية مختصرة، وتطلب من النموذج الاعتماد على المصادر الموثوقة وعدم إصدار فتاوى أو أحكام شرعية من عنده. يظل هذا النظام مساعدًا للمعلومة والتدبر، ولا يحل محل التحقق العلمي أو سؤال أهل الاختصاص.

## تسجيل الدخول بحساب Google

يستخدم التطبيق Credential Manager مع Google ID Token ثم يربط الاعتماد بحساب Firebase عبر `GoogleAuthProvider`. تم التحقق من أن القيمة في `strings.xml` هي Web Client ID الحقيقي:

```text
372887186106-ori5bj4fde42ovqabuh34eedt3f1dcta.apps.googleusercontent.com
```

وتطابق القيمة الموجودة في `app/google-services.json` تحت `client_type = 3`. أُضيفت علامة اختبار مستقرة لزر تسجيل الدخول:

```text
google-sign-in-button
```

يؤكد اختبار Compose أن الزر ظاهر ومفعّل في شاشة الإعدادات. لا يفتح الاختبار مزود Google الفعلي تلقائيًا، لأن ذلك يحتاج حسابًا حقيقيًا وتفاعلًا خارجيًا على جهاز اختبار.

## الصوت وقراءة الأذكار

كان `AudioPlayer.kt` يستخدم الاستدعاء القديم:

```kotlin
continuation.resume(value, null)
```

تم استبداله بالواجهة الحديثة التي تستقبل callback للإلغاء:

```kotlin
continuation.resume(value) { _, _, _ -> }
```

طُبق التغيير على إكمال النطق، الأخطاء، الإيقاف، وحالات فشل التهيئة أو فشل `speak`. كما تم تحديث اللغة العربية من `Locale("ar")` إلى:

```kotlin
Locale.forLanguageTag("ar")
```

أُضيف اختبار Android باسم `AudioPlayerInstrumentedTest`. ينشئ الاختبار مشغل الصوت، يتحقق من أن الحالة الأولية ليست قيد التشغيل، ثم يستدعي `shutdown()` داخل `finally` لضمان تحرير موارد Text-to-Speech حتى عند فشل assertion.

هذا اختبار lifecycle آمن ولا يدعي نجاح إخراج صوت فعلي. التحقق من النطق العربي، اختيار محرك TTS، جودة الصوت، واستمرار القراءة يحتاج جهازًا أو محاكيًا مزودًا بمحرك Text-to-Speech فعلي.

## اختبارات Android المضافة

أُضيفت اختبارات Compose في `BaqiyatUiTest.kt` تغطي:

1. ظهور زر تسجيل الدخول باستخدام Google والتأكد من قابليته للتفاعل.
2. فتح أذكار الصباح.
3. ظهور بطاقة الذكر والعداد والتفاعل مع البطاقة بالنقر.

أُضيفت علامات اختبار مستقرة إلى الواجهة:

```text
google-sign-in-button
dhikr-card
dhikr-counter
```

تم تجميع اختبارات Android بنجاح عبر:

```bash
:app:compileDebugAndroidTestKotlin
:app:assembleDebugAndroidTest
```

## حالة البناء والتحذيرات

آخر عملية بناء نفذت المهام التالية بنجاح:

```bash
:app:assembleDebug
:app:compileDebugAndroidTestKotlin
:app:assembleDebugAndroidTest
```

نتجت الملفات التالية:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`

اختفت تحذيرات `AudioPlayer` المتعلقة بـ `CancellableContinuation` و`Locale`. يوجد تحذير مستقل في `MainActivity.kt` متعلق بـ `FirebaseMessaging.getToken()` ووسمه deprecated في إصدار Firebase Messaging الحالي. هذا التحذير لا يرتبط بنظام الصوت، ولا يوجد في الواجهة العامة الحالية بديل مباشر مكافئ داخل نفس API. يمكن عزل هذا الاستدعاء أو تحديث مكتبة Firebase في مرحلة مستقلة بعد اختبار FCM على جهاز حقيقي.

## لوحة المالك وFirebase

تتصل لوحة المالك بـ Firebase Authentication وFirestore وStorage. تشمل وظائفها إدارة محتوى الأذكار، حالات المسودة والنشر، صندوق الشكاوى والاقتراحات، البحث والتصفية، عرض المرفقات، السجل الزمني، الرد المباشر، تحديث حالة الشكوى، وإعداد الإشعارات.

تستخدم اللوحة تسجيل الدخول بالبريد الإلكتروني وكلمة المرور، مع رسائل Toast للنجاح والفشل، إظهار وإخفاء كلمة المرور، خيار تذكر الجلسة، وإعادة تعيين كلمة المرور. تم حفظ جلسة Firebase محليًا عند تفعيل التذكر، أو ضمن جلسة المتصفح عند تعطيله.

## القيود الحالية

لم يُنفذ اختبار تثبيت وتشغيل فعلي على جهاز Android في هذه البيئة لأن `adb devices` لم يعرض أي جهاز أو محاكي. لذلك لا يمكن من بيئة البناء وحدها تأكيد جودة صوت TTS الفعلية أو نجاح نافذة Google Credential Manager أو سلاسة الانتقال على عتاد محدد.

كما أن اختبار UI الحالي يعتمد على شاشة التطبيق الحقيقية وعلى بيانات Firebase المحلية أو الشبكية. ولرفع موثوقية الاختبارات في CI، يُستحسن لاحقًا توفير fake repositories وحقن dependencies بدل تهيئة Firebase وDataStore وWorkManager أثناء كل اختبار.

## التوصيات التالية

يوصى أولًا بتوصيل جهاز Android فعلي وتشغيل الاختبارات التالية:

```bash
adb devices
./gradlew connectedDebugAndroidTest
```

بعد ذلك يُستحسن اختبار تسجيل الدخول بحساب Google حقيقي على جهاز يحتوي على Google Play Services، ثم تجربة أذكار الصباح والمساء مع محرك TTS عربي، وتغيير مستوى الصوت، وإيقاف التطبيق واستئناف القراءة.

يوصى أيضًا بعزل FCM token registration عن بدء التطبيق أو نقله إلى worker مخصص مع معالجة صريحة لحالات عدم توفر Google Play Services. وأخيرًا، يمكن إضافة fake `AudioEngine` واختبار وحدة يثبت أن الاستمرار يعود عند `onDone` و`onError` و`stop` دون الحاجة إلى محرك صوت حقيقي.

## الملفات الرئيسية

- [مشغل الصوت](app/src/main/java/com/example/ui/AudioPlayer.kt)
- [اختبار مشغل الصوت](app/src/androidTest/java/com/example/AudioPlayerInstrumentedTest.kt)
- [اختبارات واجهة التطبيق](app/src/androidTest/java/com/example/BaqiyatUiTest.kt)
- [شاشة الأذكار](app/src/main/java/com/example/ui/screens/AzkarScreen.kt)
- [مستودع المساعد الذكي](app/src/main/java/com/example/data/AiRepository.kt)
- [ملف إعداد Web Client ID](app/src/main/res/values/strings.xml)

## References

[1]: https://developer.android.com/develop/ui/compose/testing "Android Compose testing documentation"
[2]: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-cancellable-continuation/ "Kotlin Coroutines CancellableContinuation API"
[3]: https://firebase.google.com/docs/cloud-messaging/android/client "Firebase Cloud Messaging Android client documentation"
[4]: https://developer.android.com/reference/android/speech/tts/TextToSpeech "Android TextToSpeech API reference"
