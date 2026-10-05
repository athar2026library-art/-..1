# سطور الأخطاء مقروءة في تقارير الانهيار
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Firestore يقرأ ويكتب الحقول بأسمائها؛ بدون keep تفشل hasOnly في القواعد بعد R8
-keep class com.example.data.UserProgress { *; }
-keep class com.example.data.Zekr { *; }
-keep class com.example.data.FeedbackItem { *; }
-keep class com.example.data.FeedbackDraft { *; }

# Firebase / Play Services
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Credential Manager / Google ID
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.libraries.identity.googleid.**
-keep class androidx.credentials.** { *; }

-dontwarn kotlinx.coroutines.**
-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }
