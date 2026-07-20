# Compose
-keepclassmembers class * extends androidx.compose.runtime.snapshots.SnapshotState { *; }

# Coil
-dontwarn coil3.**

# OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers class okhttp3.internal.publicsuffix.PublicSuffixDatabase {
    private java.lang.String[] publicSuffixList;
    private java.lang.String[] publicSuffixExceptionList;
}
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Tink / Security-Crypto
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**

# JNA is used by the generated UniFFI Rust bindings. Its native dispatcher
# resolves Java classes and fields by their original JNI names at runtime.
-keep class com.sun.jna.** { *; }
# UniFFI's generated bindings include JNA-reflected library interfaces,
# structures, callbacks, converters, and error types. Keep the generated
# boundary intact so R8 cannot rename or remove members required by JNA.
-keep class com.yunfie.illustia.rust.** { *; }
-keep interface com.yunfie.illustia.rust.** { *; }
-keep class * extends com.sun.jna.Structure { *; }
-keep interface * extends com.sun.jna.Library { *; }
-keep interface * extends com.sun.jna.Callback { *; }
-dontwarn com.sun.jna.**
