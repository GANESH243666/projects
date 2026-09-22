import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) localPropertiesFile.inputStream().use(localProperties::load)

android {
    namespace = "com.max.assistant"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.max.assistant"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    defaultConfig {
        buildConfigField("String", "LLM_API_KEY", "\"${localProperties.getProperty("llm_api_key", "")}\"")
        buildConfigField("String", "LLM_API_URL", "\"${localProperties.getProperty("llm_api_url", "https://api.openai.com/v1/chat/completions")}\"")
        buildConfigField("String", "LLM_MODEL", "\"${localProperties.getProperty("llm_model", "gpt-4o-mini")}\"")
        buildConfigField("String", "CLOUD_TTS_API_KEY", "\"${localProperties.getProperty("cloud_tts_api_key", "")}\"")
        buildConfigField("String", "CLOUD_TTS_API_URL", "\"${localProperties.getProperty("cloud_tts_api_url", "https://api.openai.com/v1/audio/speech")}\"")
        buildConfigField("String", "CLOUD_TTS_MODEL", "\"${localProperties.getProperty("cloud_tts_model", "gpt-4o-mini-tts")}\"")
        buildConfigField("String", "CLOUD_TTS_VOICE", "\"${localProperties.getProperty("cloud_tts_voice", "alloy")}\"")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.alphacephei:vosk-android:0.3.47")
    
}
