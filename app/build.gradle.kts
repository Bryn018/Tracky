plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.tracky.app"
    compileSdk = 35

    // Release signing configuration — reads from keystore.properties or env vars
    fun loadKeystoreProperty(key: String, envKey: String): String {
        val propFile = rootProject.file("../.tracky-keystore/keystore.properties")
        if (propFile.exists()) {
            try {
                val lines = propFile.readLines()
                val prefix = "$key="
                lines.firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix)?.trim()
                    ?.takeIf { it.isNotEmpty() }?.let { return it }
            } catch (_: Exception) {}
        }
        return System.getenv(envKey) ?: ""
    }

    signingConfigs {
        create("release") {
            storeFile = file(loadKeystoreProperty("storeFile", "TRACKY_STORE_FILE")
                .ifEmpty { "/home/admins/.tracky-keystore/release.keystore" })
            storePassword = loadKeystoreProperty("storePassword", "TRACKY_STORE_PASSWORD")
            keyAlias = loadKeystoreProperty("keyAlias", "TRACKY_KEY_ALIAS")
            keyPassword = loadKeystoreProperty("keyPassword", "TRACKY_KEY_PASSWORD")
        }
    }

    defaultConfig {
        applicationId = "com.tracky.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room schema export directory for migration validation
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)

    // DataStore
    implementation(libs.datastore.preferences)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}