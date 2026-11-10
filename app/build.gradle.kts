import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.jvm.toolchain.JavaLanguageVersion
import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
plugins {
    id("com.android.application")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}


android {
    namespace = "com.nanzbeatles.nanas"
    compileSdk = 36
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "com.nanzbeatles.nanas"
        minSdk = 23
        targetSdk = 36
        versionCode = 200
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        resourceConfigurations += listOf("in", "id")

        // LastFM API keys from GitHub Secrets
        val lastFmKey = localProperties.getProperty("LASTFM_API_KEY") ?: System.getenv("LASTFM_API_KEY") ?: ""
        val lastFmSecret = localProperties.getProperty("LASTFM_SECRET") ?: System.getenv("LASTFM_SECRET") ?: ""

        buildConfigField("String", "LASTFM_API_KEY", "\"$lastFmKey\"")
        buildConfigField("String", "LASTFM_SECRET", "\"$lastFmSecret\"")

        // BetterLyrics API key from GitHub Secrets
        val betterLyricsKey = localProperties.getProperty("BETTERLYRICS_API_KEY") ?: System.getenv("BETTERLYRICS_API_KEY") ?: ""
        buildConfigField("String", "BETTERLYRICS_API_KEY", "\"$betterLyricsKey\"")

        // Discord application ID used for OAuth2 login and Rich Presence
        buildConfigField("Long", "DISCORD_APP_ID", "1450052823617372160L")
    }
    
    externalNativeBuild {
        cmake {
            path("src/main/cpp/vibrafp/lib/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    flavorDimensions += listOf("variant", "ui", "abi")
    productFlavors {
        // FOSS variant (default) - F-Droid compatible, no Google Play Services
        create("foss") {
            dimension = "variant"
            isDefault = true
            buildConfigField("Boolean", "CAST_AVAILABLE", "false")
        }
        
        // GMS variant - with Google Cast support (requires Google Play Services)
        create("gms") {
            dimension = "variant"
            buildConfigField("Boolean", "CAST_AVAILABLE", "true")
        }

        // Architecture-specific release APKs. These intentionally use neither the FOSS nor
        // GMS flavor name; they share the non-Cast implementation without branding as FOSS.
        create("standalone") {
            dimension = "variant"
            buildConfigField("Boolean", "CAST_AVAILABLE", "false")
        }
        
        create("universal") {
            dimension = "abi"
            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            }
            buildConfigField("String", "ARCHITECTURE", "\"universal\"")
        }
        create("arm64") {
            dimension = "abi"
            ndk { abiFilters += "arm64-v8a" }
            buildConfigField("String", "ARCHITECTURE", "\"arm64\"")
        }
        create("armeabi") {
            dimension = "abi"
            ndk { abiFilters += "armeabi-v7a" }
            buildConfigField("String", "ARCHITECTURE", "\"armeabi\"")
        }
        create("x86") {
            dimension = "abi"
            ndk { abiFilters += "x86" }
            buildConfigField("String", "ARCHITECTURE", "\"x86\"")
        }
        create("x86_64") {
            dimension = "abi"
            ndk { abiFilters += "x86_64" }
            buildConfigField("String", "ARCHITECTURE", "\"x86_64\"")
        }

        create("mobile") {
            dimension = "ui"
            isDefault = true
        }

        create("tv") {
            dimension = "ui"
        }
    }

    sourceSets {
        getByName("main") {
            kotlin.srcDir("src/main/kotlin")
        }
        getByName("foss") {
            kotlin.srcDirs("src/foss/kotlin")
        }
        getByName("gms") {
            kotlin.srcDirs("src/gms/kotlin")
        }
        getByName("standalone") {
            kotlin.srcDirs("src/foss/kotlin")
            manifest.srcFile("src/foss/AndroidManifest.xml")
        }
        getByName("tv") {
            kotlin.srcDirs("src/tv/kotlin")
            manifest.srcFile("src/tv/AndroidManifest.xml")
        }
    }

    signingConfigs {
        // Load from local.properties for persistent debug and release builds
        create("persistentDebug") {
            val storeFilePath = localProperties.getProperty("debug.persistent.storeFile", "")
            if (storeFilePath.isNotEmpty()) {
                storeFile = file(storeFilePath)
            }
            storePassword = localProperties.getProperty("debug.persistent.storePassword", "android")
            keyAlias = localProperties.getProperty("debug.persistent.keyAlias", "androiddebugkey")
            keyPassword = localProperties.getProperty("debug.persistent.keyPassword", "android")
        }
        
        create("release") {
            val storeFilePath = localProperties.getProperty("release.storeFile", "")
            if (storeFilePath.isNotEmpty() && file(storeFilePath).exists()) {
                storeFile = file(storeFilePath)
                storePassword = localProperties.getProperty("release.storePassword", "android")
                keyAlias = localProperties.getProperty("release.keyAlias", "nanzbeatles")
                keyPassword = localProperties.getProperty("release.keyPassword", "android")
            } else if (file("keystore/release.keystore").exists()) {
                storeFile = file("keystore/release.keystore")
                storePassword = localProperties.getProperty("release.storePassword", "android")
                keyAlias = localProperties.getProperty("release.keyAlias", "nanzbeatles")
                keyPassword = localProperties.getProperty("release.keyPassword", "android")
            } else {
                storeFile = signingConfigs.getByName("debug").storeFile
                storePassword = signingConfigs.getByName("debug").storePassword
                keyAlias = signingConfigs.getByName("debug").keyAlias
                keyPassword = signingConfigs.getByName("debug").keyPassword
            }
        }
        
        getByName("debug") {
            val storeFilePath = localProperties.getProperty("debug.storeFile", "")
            if (storeFilePath.isNotEmpty() && file(storeFilePath).exists()) {
                storeFile = file(storeFilePath)
            }
            storePassword = localProperties.getProperty("debug.storePassword", "android")
            keyAlias = localProperties.getProperty("debug.keyAlias", "nanzbeatlesdebug")
            keyPassword = localProperties.getProperty("debug.keyPassword", "android")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = false
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            externalNativeBuild {
                cmake {
                    arguments += listOf(
                        "-DENABLE_LTO=ON",
                        "-DCMAKE_BUILD_TYPE=Release"
                    )
                }
            }
            ndk {
                debugSymbolLevel = "NONE"
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            externalNativeBuild {
                cmake {
                    arguments += listOf(
                        "-DENABLE_LTO=OFF",
                        "-DCMAKE_BUILD_TYPE=Debug"
                    )
                }
            }
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    // Custom APK naming for TV builds
    tasks.register("renameTvApk") {
        doLast {
            val apkDir = layout.buildDirectory.dir("outputs/apk").get().asFile
            apkDir.walkTopDown().filter {
                it.isFile && it.name.endsWith(".apk") &&
                it.name.contains("tv", ignoreCase = true) &&
                it.name.contains("release", ignoreCase = true)
            }.forEach { apkFile ->
                val newName = "Beatles-Tv.apk"
                val newFile = apkFile.parentFile.resolve(newName)
                apkFile.renameTo(newFile)
                println("Renamed TV APK: ${apkFile.name} -> $newName")
            }
        }
    }

    afterEvaluate {
        tasks.named("assembleFossTvUniversalRelease").configure {
            finalizedBy("renameTvApk")
        }
        tasks.named("assembleGmsTvUniversalRelease").configure {
            finalizedBy("renameTvApk")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        lintConfig = file("lint.xml")
        warningsAsErrors = false
        abortOnError = false
        checkDependencies = false
    }

    androidResources {
        generateLocaleConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
            keepDebugSymbols += listOf(
                "**/libandroidx.graphics.path.so",
                "**/libdatastore_shared_counter.so"
            )
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/CONTRIBUTORS.md"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn"
        )
        suppressWarnings.set(false)
    }
}

dependencies {
    implementation(libs.guava)
    implementation(libs.coroutines.guava)
    implementation(libs.concurrent.futures)
    implementation(libs.timber)
    implementation(libs.bundles.activity)
    implementation(libs.appcompat)
    implementation(libs.browser)

    implementation(libs.bundles.compose)
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.navigation)
    implementation(libs.materialKolor)
    implementation(libs.shimmer)
    implementation(libs.palette)

    implementation(libs.bundles.media3)
    implementation(libs.media3.session)
    implementation(libs.media3.okhttp)

    // Google Cast - only included in GMS flavor (not available in F-Droid/FOSS builds)
    "gmsImplementation"(libs.media3.cast)
    "gmsImplementation"(libs.mediarouter)
    "gmsImplementation"(libs.cast.framework)

    implementation(libs.room.runtime)
    implementation(libs.kuromoji.ipadic)
    implementation(libs.tinypinyin)
    ksp(libs.room.compiler)

    implementation(libs.bundles.hilt)
    ksp(libs.hilt.compiler)

    implementation(libs.bundles.ktor)
    implementation(libs.bundles.coil)
    implementation(libs.bundles.datastore)

    implementation(libs.apacheLang3)
    implementation(libs.brotli)
    implementation(libs.jsoup)
    implementation(libs.newpipeextractor)
    implementation(libs.json)
    implementation(libs.ucrop)
    implementation(libs.reorderable)

    coreLibraryDesugaring(libs.desugaring)

    implementation(project(":innertube"))
    implementation(project(":kugou"))
    implementation(project(":lrclib"))
    implementation(project(":auravideo"))
    implementation(project(":lastfm"))
    implementation(project(":betterlyrics"))
    implementation(project(":simpmusic"))
    implementation(project(":shazamkit"))
    implementation(project(":rush"))
    implementation(project(":paxsenix"))
    implementation(project(":musixmatch"))

    implementation(libs.mlkit)
    implementation(libs.protobuf)

    testImplementation(libs.junit)
}
