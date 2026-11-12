# Retrofit / OkHttp / Moshi
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions
-keep class com.hashcore.pool.data.remote.dto.** { *; }
-keep class com.hashcore.pool.data.remote.dto.**JsonAdapter { *; }
-keepclassmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep class * extends com.squareup.moshi.JsonAdapter
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn com.squareup.moshi.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep BuildConfig
-keep class com.hashcore.pool.BuildConfig { *; }

# Security crypto / Tink
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
# Optional Tink deps not on classpath (KeysDownloader / annotations)
-dontwarn com.google.api.client.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.joda.time.**
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**

# ZXing (QR) — keep encoder entry points
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Biometric / lifecycle
-dontwarn androidx.biometric.**

# Strip verbose logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
