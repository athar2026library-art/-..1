# تطبيق بقية المرحلة 3

رُفعت إلى الفرع الأساسيات: `ArabicText`، `Haptics`، `Type`/`ReaderFont`، `SettingsRepository` (مفضلة/أهداف/خط)، `FavoritesScreen`، `MoreScreen`، ومسار `favorites` في `AppNavGraph`.

## انسخ من `baqiyat-phase3-full-project.zip` (أو طبّق `baqiyat-phase3.patch`)

```text
app/src/main/java/com/example/data/AzkarData.kt
app/src/main/java/com/example/ui/AppViewModel.kt
app/src/main/java/com/example/ui/AudioPlayer.kt
app/src/main/java/com/example/ui/screens/AzkarScreen.kt
app/src/main/java/com/example/ui/screens/HomeScreen.kt
app/src/main/java/com/example/ui/screens/SearchScreen.kt
app/src/main/java/com/example/ui/screens/TasbihScreen.kt
app/src/main/java/com/example/ui/screens/StatsScreen.kt   # buildWeekChart internal
app/src/main/res/font/scheherazade_regular.ttf
app/src/main/res/font/scheherazade_bold.ttf
```

## بعد النسخ

```bash
git pull origin feature/private-admin-panel
# انسخ الملفات أعلاه من الـ zip
./gradlew :app:assembleDebug
./gradlew test --tests com.example.ArabicTextTest
```

## ماذا تضيف الملفات المتبقية

- **AzkarData**: `byIds` + بحث بلا تشكيل + عنوان favorites
- **AppViewModel**: favorites / readerFont / dailyGoal / tasbihTarget + setters
- **AudioPlayer**: `currentRange` لتمييز الكلمة المنطوقة
- **AzkarScreen**: مفضلة، وضع تركيز، خط القراءة، تمييز النطق
- **TasbihScreen**: عبارات، هدف محفوظ، هدف يومي، Haptics، مخطط أسبوع
- **SearchScreen**: فلاتر + تمييز + قلب المفضلة
- **HomeScreen**: اختصار المفضلة + هدف اليوم من dailyGoal
