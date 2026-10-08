# المرحلة 5 — ما رُفع وما يبقى من الـ zip

## رُفع إلى `feature/private-admin-panel`

- `Categories.kt` / `CategoryDefaults` / `CustomWirds.kt`
- `CategoryIcons.kt`
- `CategoriesAndWirdsTest.kt`
- `firestore.rules`: `validCategory` + مسار `content/categories/items` + `validZekr` يقبل تصنيفاً ديناميكياً إن وُجد المستند

## انسخ من `baqiyat-phase5-full-project.zip`

```text
admin-panel/app.js
admin-panel/index.html
app/src/main/java/com/example/data/AzkarData.kt
app/src/main/java/com/example/data/FirestoreRepository.kt
app/src/main/java/com/example/data/SettingsRepository.kt
app/src/main/java/com/example/ui/AppNavGraph.kt
app/src/main/java/com/example/ui/AppViewModel.kt
app/src/main/java/com/example/ui/screens/AiServicesScreen.kt
app/src/main/java/com/example/ui/screens/AzkarScreen.kt
app/src/main/java/com/example/ui/screens/HomeScreen.kt
app/src/main/java/com/example/ui/screens/MoreScreen.kt
app/src/main/java/com/example/ui/screens/WirdsScreen.kt
app/src/main/java/com/example/ui/screens/WirdEditorScreen.kt
firebase/tests/rules.test.mjs
```

```bash
git pull origin feature/private-admin-panel
# انسخ الملفات أعلاه
firebase deploy --only firestore:rules
# ثم انشر اللوحة (Hosting) إن لزم
./gradlew :app:assembleDebug test
```

**شرط الخروج:** مشرف يضيف تصنيفاً وأذكاره من اللوحة → تظهر في التطبيق بلا إصدار جديد.
