# —— نماذج Room / Firestore (أسماء الحقول يجب أن تبقى للمزامنة والقواعد) ——
-keep class com.example.data.UserProgress { *; }
-keepclassmembers class com.example.data.UserProgress { *; }
-keep class com.example.data.Zekr { *; }
-keep class com.example.data.FeedbackItem { *; }
-keep class com.example.data.FeedbackDraft { *; }

# —— Firebase ——
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Credential Manager / Google ID
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.libraries.identity.googleid.**
-keep class androidx.credentials.** { *; }

# Kotlin / Coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# R8 full mode: keep enum values used in serialization
-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }
