# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# ==============================================================================
# R8 Anti-Decompilation & Aggressive Obfuscation Hardening
# ==============================================================================

# Flatten all non-kept obfuscated classes into a single package 'o'
-repackageclasses 'o'
-allowaccessmodification

# Use lookalike identifier dictionary (I/l/O/0/1) to confuse decompilers (JADX/CFR)
-obfuscationdictionary proguard-dict.txt
-classobfuscationdictionary proguard-dict.txt
-packageobfuscationdictionary proguard-dict.txt

# Reuse identical obfuscated names for methods with differing signatures
-overloadaggressively

# Aggressive multi-pass optimization (inlining, class merging, dead code stripping)
-optimizationpasses 5

# Erase original .kt/.java source file names from DEX metadata
-renamesourcefileattribute SourceFile

# Strip verbose/debug logging calls in release builds (void methods only)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
    public static void i(...);
    public static void w(...);
}
-assumenosideeffects class timber.log.Timber$Tree {
    public void v(...);
    public void d(...);
    public void i(...);
    public void w(...);
}

# ==============================================================================
# Required Framework & Reflection Keep Rules
# ==============================================================================

# Ignore missing service warnings
-dontwarn javax.script.**
-dontwarn com.google.re2j.**
-dontwarn java.beans.**

# Keep required runtime attributes for Compose/Hilt/Serialization/Room
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Kotlin Serialization
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class **$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class * {
    public static kotlinx.serialization.KSerializer serializer(...);
}
-keep class *$$serializer { *; }

# Keep data classes for serialization
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    <fields>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep JNI native bindings (must never be repackaged or renamed)
-keep class com.nanzbeatles.nanas.recognition.VibraSignature { *; }

# Keep runtime Class.forName reflection targets
-keep class com.nanzbeatles.nanas.TvMainActivity { *; }
-keep class com.nanzbeatles.nanas.tv.TvRecommendationHelper { *; }
-keep class com.nanzbeatles.nanas.lyrics.LanguageDetectionHelper { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# VOSK
-keep class org.vosk.** { *; }
-keep class com.alphacephei.** { *; }
-dontwarn org.vosk.**

# JNA (used by Vosk for native library access)
-keep class com.sun.jna.** { *; }
-keep class java.lang.reflect.Field { *; }
-dontwarn com.sun.jna.**

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Jsoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# Coil
-keep class coil3.** { *; }

# Protobuf
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

# Kuromoji loads dictionary .bin files via Class.getResourceAsStream with
# package-relative paths. If R8 obfuscates these classes/packages, release
# builds look for the dictionaries under the obfuscated package and crash.
-keep class com.atilika.kuromoji.** { *; }
-dontwarn com.atilika.kuromoji.**

# Innertube / YouTube models
-keep class com.nanzbeatles.innertube.models.** { *; }

# App entities and DAOs
-keep class com.nanzbeatles.nanas.db.entities.** { *; }
-keep class com.nanzbeatles.nanas.db.dao.** { *; }
-keep class com.nanzbeatles.nanas.db.MusicDatabase { *; }

# ML Kit Selfie Segmentation
-keep class com.google.mlkit.vision.segmentation.selfie.** { *; }
-keep class com.google.mlkit.vision.segmentation.** { *; }
-dontwarn com.google.mlkit.**
