# المرحلة 5 — حالة الرفع على `feature/private-admin-panel`

## تم رفعه

- `Categories.kt` / `CustomWirds.kt` / `CategoryIcons.kt`
- `CategoriesAndWirdsTest.kt`
- `firestore.rules` (`validCategory` + `content/categories/items`)
- `SettingsRepository` (أوراد مخصصة)
- `AppViewModel` (`customWirds`, `categories`, حفظ/حذف الورد)
- `FirestoreRepository.observeCategories` + `completedSleep` في النسخ الاحتياطي
- `AppNavGraph` (wirds / wird_edit / azkar/wird_*)
- `WirdsScreen` / `WirdEditorScreen`
- `MoreScreen` (رابط أوردي)
- `AiServicesScreen` (رقائق الشعور)
- `AzkarData.builtInOrEmpty` + عنوان المفضلة
- `HomeScreen` (أقسام ديناميكية من `viewModel.categories`)
- `JourneyScreen` (رحلتي)

## انسخ يدوياً من `baqiyat-phase5-full-project.zip` إن احتجت النسخة الكاملة

```text
admin-panel/app.js
admin-panel/index.html
app/src/main/java/com/example/ui/screens/AzkarScreen.kt   # wird_ + حالات فارغ/تحميل
firebase/tests/rules.test.mjs
```

```bash
git pull origin feature/private-admin-panel
firebase deploy --only firestore:rules
# انشر اللوحة بعد نسخ admin-panel
./gradlew :app:assembleDebug test
```

**قبل إضافة تصنيف من اللوحة:** انشر القواعد أولاً.
