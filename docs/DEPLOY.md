# أوامر النشر والصلاحيات (نفّذها من جهازك المصادق)

```bash
# 1) المشروع
firebase use svrpmtt

# 2) سر Gemini (مرة واحدة)
firebase functions:secrets:set GEMINI_API_KEY

# 3) منح صلاحية أدمن (عدّل البريد أو استخدم UID)
cd firebase/functions && node make_admin.js YOUR_EMAIL@example.com

# 4) النشر
firebase deploy --only firestore:rules,storage,functions
```

## App Check
1. سجّل SHA-256 لمفتاح التوقيع وPlay App Signing في Firebase.
2. فعّل Play Integrity لتطبيق Android.
3. لنسخة debug: انسخ Debug token من Logcat وأضفه في Console.
4. لا تفعّل Enforce إلا بعد أسبوع مراقبة بلا رفض للنسخ الشرعية.

## تقييد مفاتيح Google Cloud
- مفتاح Android: قيّده بالحزمة `com.baqiyat.azkar` + بصمة SHA-1/256.
- مفتاح الويب: HTTP referrers فقط.

## تحقق بعد البناء
```bash
./gradlew :app:assembleDebug :app:assembleRelease
# لا يوجد GEMINI في الـ APK:
# unzip -p app/build/outputs/apk/release/*.apk | strings | grep -i gemini || echo clean
```
