# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /sdk/tools/proguard/proguard-android.txt

# Keep data models used with Firestore / serialization
-keep class com.baqiyat.azkar.data.UserProgress { *; }
-keep class com.baqiyat.azkar.data.Zekr { *; }
-keep class com.baqiyat.azkar.data.FeedbackItem { *; }
-keep class com.baqiyat.azkar.data.FeedbackDraft { *; }
