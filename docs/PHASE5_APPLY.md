# المرحلة 5 — مكتمل تقريباً على `feature/private-admin-panel`

## تم رفعه بالكامل

| المكوّن | الحالة |
|---------|--------|
| Categories / CustomWirds / CategoryIcons | ✅ |
| firestore.rules (validCategory + items) | ✅ |
| rules.test.mjs (اختبارات التصنيفات) | ✅ |
| SettingsRepository (أوراد) | ✅ |
| AppViewModel (categories + customWirds) | ✅ |
| FirestoreRepository.observeCategories | ✅ |
| AppNavGraph (wirds / wird_edit) | ✅ |
| WirdsScreen / WirdEditorScreen | ✅ |
| MoreScreen | ✅ |
| AiServicesScreen (رقائق الشعور) | ✅ |
| AzkarData.builtInOrEmpty | ✅ |
| HomeScreen (تصنيفات ديناميكية) | ✅ |
| AzkarScreen (wird_ / favorites / فارغ / تحميل) | ✅ |
| JourneyScreen | ✅ |
| admin-panel/index.html (مودال التصنيف) | ✅ |

## ملف واحد انسخه يدوياً من الـ zip

```bash
# من baqiyat-phase5-full-project.zip
cp admin-panel/app.js  <repo>/admin-panel/app.js
git add admin-panel/app.js && git commit -m "feat(phase5): admin-panel dynamic categories CRUD"
```

السبب: حجم `app.js` (~27KB) يتجاوز حد استدعاء الرفع الآمن في هذه الجلسة؛ باقي اللوحة (`index.html`) مرفوع.

## بعد السحب

```bash
git pull origin feature/private-admin-panel
# انسخ app.js إن لم يكن محدّثاً
firebase deploy --only firestore:rules
# ثم Hosting للوحة إن لزم
./gradlew :app:assembleDebug test
```

**شرط الخروج:** مشرف يضيف تصنيفاً وأذكاره من اللوحة → تظهر في التطبيق بلا إصدار.
