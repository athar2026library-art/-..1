# Firestore / Room models — keep field names for toObjects and rules validation
-keepclassmembers class com.example.data.UserProgress { *; }
-keep class com.example.data.UserProgress { *; }
-keepattributes SourceFile,LineNumberTable,*Annotation*

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
