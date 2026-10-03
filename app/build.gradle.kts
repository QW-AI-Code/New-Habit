plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val keystorePath: String = System.getenv("SIGNING_STORE_FILE").orEmpty()
val keystoreFile: java.io.File? = if (keystorePath.isNotBlank()) file(keystorePath) else null
val hasReleaseKeystore: Boolean = keystoreFile != null && keystoreFile.isFile && keystoreFile.length() > 0L &&
    !System.getenv("SIGNING_STORE_PASSWORD").isNullOrBlank() && !System.getenv("SIGNING_KEY_ALIAS").isNullOrBlank()

if (!hasReleaseKeystore) logger.warn("No release keystore configured. Release APKs will use the debug key.")

android {
    namespace = "com.duck.twominute"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.duck.twominute"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
    }
    signingConfigs {
        if (hasReleaseKeystore) create("release") {
            storeFile = keystoreFile
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: System.getenv("SIGNING_STORE_PASSWORD")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseKeystore) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
        debug { isMinifyEnabled = false }
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    lint { abortOnError = false; checkReleaseBuilds = false }
    packaging { resources.excludes += setOf("/META-INF/AL2.0", "/META-INF/LGPL2.1", "/META-INF/*.kotlin_module") }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}
