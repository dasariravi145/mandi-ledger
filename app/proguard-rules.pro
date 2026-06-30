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
-dontwarn com.google.firebase.**
# Keep Firebase data models
-keep class com.dasariravi145.agrolynch.data.remote.model.** { *; }

# Room rules
-keep class * extends androidx.room.RoomDatabase
-keep class * { @androidx.room.Dao *; }
-keep class * { @androidx.room.Entity *; }
-keep class com.dasariravi145.agrolynch.data.local.entity.** { *; }
-keep class com.dasariravi145.agrolynch.data.local.dao.** { *; }
-dontwarn androidx.room.**

# ML Kit rules
-keep class com.google.mlkit.** { *; }

# AdMob rules
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.gms.ads.**
-dontwarn com.google.android.ump.**

# Gson / JSON models
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.dasariravi145.agrolynch.domain.model.** { *; }

# Coroutines rules
-dontwarn kotlinx.coroutines.**
-dontwarn kotlinx.coroutines.android.**

# Preserve Line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable

# R8 missing rules from build output
-dontwarn com.google.api.client.http.GenericUrl
-dontwarn com.google.api.client.http.HttpHeaders
-dontwarn com.google.api.client.http.HttpRequest
-dontwarn com.google.api.client.http.HttpRequestFactory
-dontwarn com.google.api.client.http.HttpResponse
-dontwarn com.google.api.client.http.HttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport$Builder
-dontwarn com.google.api.client.http.javanet.NetHttpTransport
-dontwarn com.google.firebase.ktx.Firebase
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn org.joda.time.Instant

# Timber for release (if used)
-keep class timber.log.Timber { *; }
-keep class timber.log.Timber$Tree { *; }
