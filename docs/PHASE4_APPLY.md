# المرحلة 4 — ما رُفع وما يبقى نسخه من الـ zip

## رُفع إلى `feature/private-admin-panel`

- `UserProgress.completedSleep` + Room v3 + `MIGRATION_2_3`
- `ProgressDao` / `ProgressRepository` (updateSleep, getAllProgressFlow, merge)
- `SettingsRepository` (weeklyGoal, accent)
- `Journey.kt` + `JourneyTest.kt` (سلسلة + عفو + شارات + heatmap)
- `ProgressStats.kt` (buildWeekChart / calculateStreak القديمة)
- `WirdSuggestion` يدعم sleepDone

## انسخ من `baqiyat-phase4-full-project.zip`

```text
app/src/main/java/com/example/ui/screens/JourneyScreen.kt
app/src/main/java/com/example/ui/theme/Color.kt
app/src/main/java/com/example/ui/theme/Theme.kt
app/src/main/java/com/example/ui/AppViewModel.kt
app/src/main/java/com/example/ui/AppNavGraph.kt
app/src/main/java/com/example/MainActivity.kt
app/src/main/java/com/example/ui/screens/AzkarScreen.kt
app/src/main/java/com/example/ui/screens/HomeScreen.kt
app/src/main/java/com/example/ui/screens/SettingsScreen.kt   # إن وُجد اختيار الثيم
firebase/firestore.rules
firebase/tests/rules.test.mjs
```

ثم:

```bash
git pull origin feature/private-admin-panel
# انسخ الملفات أعلاه
firebase deploy --only firestore:rules   # قبل إصدار التطبيق
./gradlew :app:assembleDebug test
```

**مهم:** انشر قواعد Firestore قبل إصدار نسخة التطبيق التي تكتب `completedSleep`، وإلا تُرفض المزامنة.
