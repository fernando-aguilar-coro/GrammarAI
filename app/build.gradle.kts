import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val envProps = Properties().apply {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.inputStream().use { load(it) }
    }
}
val geminiApiKey: String = System.getenv("GEMINI_API_KEY") ?: envProps.getProperty("GEMINI_API_KEY", "AQ.Ab8RN6IqQtqG8DvVvAx7QHKVRBXPYPZKvpw9dLnQgwGUCkYi9Q")

// Dynamic versionCode based on current timestamp divided by X (1000ms = 1s) to ensure a unique value per build
val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toInt() ?: 1
val versionPrefix = 1791897819
val autoVersionCode: Int = versionPrefix + runNumber

android {
    namespace = "com.app.grammarai"
    compileSdk = 36
 
    signingConfigs {
        create("release") {
            val prodKey = rootProject.file("@felinode__felinode-keystore.bak.jks")
            if (prodKey.exists()) {
                storeFile = prodKey
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                    ?: envProps.getProperty("RELEASE_KEYSTORE_PASSWORD", "b8d5d8315f9034aef1838b0e33a2e6c9")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                    ?: envProps.getProperty("RELEASE_KEY_ALIAS", "9f3369b34725d52eb7b565bb13b69faf")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
                    ?: envProps.getProperty("RELEASE_KEY_PASSWORD", "1422314a2abc557bdd0f2ec363c6b80b")
                enableV1Signing = true
                enableV2Signing = true
            }
        }
        getByName("debug") {
            val prodKey = rootProject.file("@felinode__felinode-keystore.bak.jks")
            if (prodKey.exists()) {
                storeFile = prodKey
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                    ?: envProps.getProperty("RELEASE_KEYSTORE_PASSWORD", "b8d5d8315f9034aef1838b0e33a2e6c9")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                    ?: envProps.getProperty("RELEASE_KEY_ALIAS", "9f3369b34725d52eb7b565bb13b69faf")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
                    ?: envProps.getProperty("RELEASE_KEY_PASSWORD", "1422314a2abc557bdd0f2ec363c6b80b")
            } else {
                storeFile = file("debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    defaultConfig {
        applicationId = "com.felinode.app"
        minSdk = 24
        targetSdk = 36
        versionCode = autoVersionCode
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
            ndk {
                debugSymbolLevel = "symbol_table"
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    assetPacks += listOf(
        // Spanish packs
        ":audio_pack_es_11_20",
        ":audio_pack_es_21_30",
        ":audio_pack_es_31_40",
        ":audio_pack_es_41_50",
        ":audio_pack_es_51_60",
        ":audio_pack_es_61_70",
        ":audio_pack_es_71_80",
        ":audio_pack_es_81_90",
        // English packs
        ":audio_pack_en_11_20",
        ":audio_pack_en_21_30",
        ":audio_pack_en_31_40",
        ":audio_pack_en_41_50",
        ":audio_pack_en_51_60",
        ":audio_pack_en_61_70",
        ":audio_pack_en_71_80",
        ":audio_pack_en_81_90"
    )
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.android.youtube.player)
    implementation(libs.play.services.auth)
    implementation(libs.mlkit.translate)
    implementation(libs.play.asset.delivery)
    implementation(libs.play.asset.delivery.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}