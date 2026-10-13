مشروع الباقيات — فهرس ملفات النصوص للمراجعة بواسطة أدوات ذكاء اصطناعي
====================================================================

Repository: https://github.com/athar2026library-art/-..1
Original branch: main
Export branch: ai-review-plain-text-export
صيغة التصدير: TXT UTF-8 غير مضغوط. كل جزء يحتوي مسار الملف الأصلي ومحتواه.

طريقة الاستخدام:
1) ارفع ملفات ADMIN-PANEL لمراجعة لوحة المالك.
2) ارفع كل ملفات ANDROID-APP-PART بالتتابع لمراجعة تطبيق أندرويد.
3) أرفق FIREBASE-BACKEND وROOT-CONFIG-AND-CI عند مراجعة التكامل أو البناء.
4) احتفظ بعناوين SOURCE FILE وEND SOURCE FILE عند اقتباس أي تعديل؛ فهي تحدد مسار الملف الحقيقي.

ملاحظات الاستبعاد:
- لم تُضمّن ملفات البناء الثنائية أو APK/AAB أو مجلدات build/node_modules/.gradle.
- لم تُضمّن package-lock/yarn/pnpm lockfiles لتقليل الحجم؛ لم تُحذف ملفات مصدر التطبيق بسبب ذلك.
- استُبعد app/google-services.json وlocal.properties وملفات المفاتيح/الأسرار. أعد تهيئة إعدادات Firebase محليًا عند البناء.
- لا يثبت هذا التصدير أن التطبيق اختُبر على جهاز فعلي؛ راجع تقرير حالة المشروع ضمن قسم ROOT-CONFIG-AND-CI إن كان مضمّنًا.

عدد الملفات المصدرية المضمّنة: 158

ملفات التصدير:
- ADMIN-PANEL-PART-01.txt — 71,854 bytes — SHA-256 6860e081615c5ff1d36998b2d675825778c67a8c43a86411ac96ead6dcb3839f
- ANDROID-APP-PART-01.txt — 93,323 bytes — SHA-256 d1406227243f6fd9afc1c4eecf44b4055349d2c8bf536df874badcc7e09265a0
- ANDROID-APP-PART-02.txt — 79,094 bytes — SHA-256 17854835b93d7e1ddc33b51c6ca78f6b14ed1b9608c7dc54a6842c2c64a45ef5
- ANDROID-APP-PART-03.txt — 84,618 bytes — SHA-256 d41643dc93260b111c237c9a42a44c8c35d080fdcdcc3464e7a9674931570652
- ANDROID-APP-PART-04.txt — 94,819 bytes — SHA-256 a276e262e481810d0515b8f9d5becbb01a4745167d004f6479b4427c272b9584
- ANDROID-APP-PART-05.txt — 72,520 bytes — SHA-256 0b2e634fd0e28327079f1cb9416fce161cc21fba77f866d6b366f2d1eb2ce9ea
- FIREBASE-BACKEND-PART-01.txt — 58,123 bytes — SHA-256 d0d9243da980bbd2bd45e8a6035205431ac0c33f3b31f8c3fc571ab88e960914
- ROOT-CONFIG-AND-CI-PART-01.txt — 71,749 bytes — SHA-256 2926d1373f1705ec70d1a0c693a1caf7a4f131f7e8fae3d0da27f4413c47e00a

قائمة ملفات المصدر المضمّنة:
- .env.example
- .github/workflows/android-validation.yml
- .github/workflows/deploy-admin-panel.yml
- .github/workflows/deploy-firestore-rules.yml
- .github/workflows/deploy-functions.yml
- BAQIYAT_PROJECT_STATUS_REPORT.md
- MERGE_RESOLUTION.md
- admin-panel/README.md
- admin-panel/app.js
- admin-panel/app.part01.js
- admin-panel/app.part02.js
- admin-panel/app.part03.js
- admin-panel/app.part04.js
- admin-panel/app.part05.js
- admin-panel/app.part06.js
- admin-panel/app.part07.js
- admin-panel/app.part08.js
- admin-panel/app.part09.js
- admin-panel/app.part10.js
- admin-panel/app.part11.js
- admin-panel/app.part12.js
- admin-panel/app.part13.js
- admin-panel/app.part14.js
- admin-panel/app.part15.js
- admin-panel/app.part16.js
- admin-panel/app.part17.js
- admin-panel/app.part18.js
- admin-panel/app.part19.js
- admin-panel/app.part20.js
- admin-panel/app.part21.js
- admin-panel/app.part22.js
- admin-panel/app.part23.js
- admin-panel/app.part24.js
- admin-panel/firebase-config.js
- admin-panel/functions-fcm.example.js
- admin-panel/index.html
- admin-panel/robots.txt
- admin-panel/styles.css
- app/build.gradle.kts
- app/proguard-rules.pro
- app/src/androidTest/java/com/example/AudioPlayerInstrumentedTest.kt
- app/src/androidTest/java/com/example/BaqiyatUiTest.kt
- app/src/androidTest/java/com/example/ExampleInstrumentedTest.kt
- app/src/debug/java/com/example/AppCheckInstaller.kt
- app/src/debug/java/com/example/AppCheckProviderInstaller.kt
- app/src/main/AndroidManifest.xml
- app/src/main/java/com/example/AzkarApplication.kt
- app/src/main/java/com/example/MainActivity.kt
- app/src/main/java/com/example/data/AiRepository.kt
- app/src/main/java/com/example/data/AppDatabase.kt
- app/src/main/java/com/example/data/ArabicText.kt
- app/src/main/java/com/example/data/AuthRepository.kt
- app/src/main/java/com/example/data/AzkarData.kt
- app/src/main/java/com/example/data/Categories.kt
- app/src/main/java/com/example/data/CustomWirds.kt
- app/src/main/java/com/example/data/FeedbackModels.kt
- app/src/main/java/com/example/data/FirestoreRepository.kt
- app/src/main/java/com/example/data/ProgressDao.kt
- app/src/main/java/com/example/data/ProgressRepository.kt
- app/src/main/java/com/example/data/SettingsRepository.kt
- app/src/main/java/com/example/data/TopicAzkarData.kt
- app/src/main/java/com/example/data/UserProgress.kt
- app/src/main/java/com/example/notifications/AzkarMessagingService.kt
- app/src/main/java/com/example/ui/AppNavGraph.kt
- app/src/main/java/com/example/ui/AppViewModel.kt
- app/src/main/java/com/example/ui/AudioPlayer.kt
- app/src/main/java/com/example/ui/CategoryIcons.kt
- app/src/main/java/com/example/ui/ChatMessage.kt
- app/src/main/java/com/example/ui/Haptics.kt
- app/src/main/java/com/example/ui/Journey.kt
- app/src/main/java/com/example/ui/NotificationWorker.kt
- app/src/main/java/com/example/ui/ShareHelper.kt
- app/src/main/java/com/example/ui/SyncWorker.kt
- app/src/main/java/com/example/ui/WirdSuggestion.kt
- app/src/main/java/com/example/ui/components/BaqiyatComponents.kt
- app/src/main/java/com/example/ui/screens/AiServicesScreen.kt
- app/src/main/java/com/example/ui/screens/AzkarScreen.kt
- app/src/main/java/com/example/ui/screens/FavoritesScreen.kt
- app/src/main/java/com/example/ui/screens/FeedbackScreen.kt
- app/src/main/java/com/example/ui/screens/HomeScreen.kt
- app/src/main/java/com/example/ui/screens/JourneyScreen.kt
- app/src/main/java/com/example/ui/screens/MoreScreen.kt
- app/src/main/java/com/example/ui/screens/ProgressStats.kt
- app/src/main/java/com/example/ui/screens/SearchScreen.kt
- app/src/main/java/com/example/ui/screens/SettingsScreen.kt
- app/src/main/java/com/example/ui/screens/StatsScreen.kt
- app/src/main/java/com/example/ui/screens/TasbihScreen.kt
- app/src/main/java/com/example/ui/screens/WirdEditorScreen.kt
- app/src/main/java/com/example/ui/screens/WirdsScreen.kt
- app/src/main/java/com/example/ui/theme/Color.kt
- app/src/main/java/com/example/ui/theme/Shapes.kt
- app/src/main/java/com/example/ui/theme/Theme.kt
- app/src/main/java/com/example/ui/theme/Type.kt
- app/src/main/java/com/example/widget/BaqiyatGlanceWidget.kt
- app/src/main/res/drawable/ic_launcher_background.xml
- app/src/main/res/drawable/ic_launcher_foreground.xml
- app/src/main/res/drawable/ic_stat_azkar.xml
- app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
- app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
- app/src/main/res/values/colors.xml
- app/src/main/res/values/font_certs.xml
- app/src/main/res/values/strings.xml
- app/src/main/res/values/themes.xml
- app/src/main/res/xml/backup_rules.xml
- app/src/main/res/xml/baqiyat_widget_info.xml
- app/src/main/res/xml/data_extraction_rules.xml
- app/src/main/res/xml/file_paths.xml
- app/src/release/java/com/example/AppCheckInstaller.kt
- app/src/release/java/com/example/AppCheckProviderInstaller.kt
- app/src/test/java/com/example/ArabicTextTest.kt
- app/src/test/java/com/example/BaqiyatComponentsScreenshotTest.kt
- app/src/test/java/com/example/CategoriesAndWirdsTest.kt
- app/src/test/java/com/example/ExampleRobolectricTest.kt
- app/src/test/java/com/example/ExampleUnitTest.kt
- app/src/test/java/com/example/JourneyTest.kt
- app/src/test/java/com/example/StreakCalculationTest.kt
- app/src/test/java/com/example/TimeModeTest.kt
- app/src/test/java/com/example/WirdSuggestionTest.kt
- app/src/test/java/com/example/data/TopicAzkarSearchTest.kt
- build.gradle.kts
- design/tokens.json
- docs/DEPLOY.md
- docs/FEEDBACK_NOTIFICATIONS_PLAN.md
- docs/FEEDBACK_NOTIFICATIONS_SPEC.md
- docs/GEMINI_LOCAL_TESTING.md
- docs/INTEGRATION_PR.md
- docs/PHASE1_DESIGN_SYSTEM.md
- docs/PHASE2_SECURITY.md
- docs/PHASE3_APPLY.md
- docs/PHASE4_APPLY.md
- docs/PHASE5_APPLY.md
- docs/PRODUCT_UPGRADE.md
- docs/RESOLVE_PR1_CONFLICTS.md
- docs/legal/delete-account.html
- docs/legal/privacy.html
- firebase/.firebaserc
- firebase/DEPLOY_NOTIFICATIONS.md
- firebase/README.md
- firebase/firebase.json
- firebase/firestore.indexes.json
- firebase/firestore.rules
- firebase/functions/README.md
- firebase/functions/index.js
- firebase/functions/lib/helpers.js
- firebase/functions/make_admin.js
- firebase/functions/package.json
- firebase/functions/test/helpers.test.js
- firebase/package.json
- firebase/storage.rules
- firebase/tests/rules.test.mjs
- firestore.rules
- gradle/libs.versions.toml
- gradle/wrapper/gradle-wrapper.properties
- gradle.properties
- gradlew
- gradlew.bat
- metadata.json
- settings.gradle.kts
