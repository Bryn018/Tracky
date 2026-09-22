# ==========================================
# Tracky ProGuard / R8 Rules
# ==========================================

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Room entities
-keep class com.tracky.app.data.local.entity.** { *; }

# Keep DAOs
-keep class com.tracky.app.data.local.dao.*

# ---- Hilt / Dagger ----
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Hilt injected classes
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }

# ---- Compose ----
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ---- Coroutines ----
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ---- DataStore ----
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { *; }

# ---- WorkManager ----
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }

# ---- Keep all ViewModels ----
-keep class com.tracky.app.ui.screens.** { *; }

# ---- Keep custom Application class ----
-keep class com.tracky.app.TrackyApplication { *; }

# ---- Remove logging in Release builds ----
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}