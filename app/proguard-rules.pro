# AgroLynch Proguard Rules

# Hilt rules
-keep public class * extends android.app.Service
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.view.View

# Firebase rules
-keep class com.google.firebase.** { *; }

# Room rules
-keep class * extends androidx.room.RoomDatabase
-keep class * { @androidx.room.Dao *; }
-keep class * { @androidx.room.Entity *; }

# ML Kit rules
-keep class com.google.mlkit.** { *; }

# Coroutines rules
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext$HandlerPost {
    private final android.os.Handler handler;
}

# Preserve Line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
