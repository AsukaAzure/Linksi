# Linksi ProGuard Rules

# Keep Room entities
-keep class com.linksi.app.data.db.** { *; }

# Keep domain models (Parcelable)
-keep class com.linksi.app.domain.model.** { *; }

# Keep Hilt generated code
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Jsoup
-keep class org.jsoup.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Coil
-keep class coil.** { *; }

-dontwarn org.jspecify.annotations.NullMarked