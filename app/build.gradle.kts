plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.testapplication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.testapplication"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["file_provider"] = "com.example.testapplication"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures{
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11

    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation (libs.chat.sdk.android)
    implementation(libs.calls.sdk.android.v406)
    implementation(libs.glide)
    implementation(libs.androidx.recyclerview)
    implementation(libs.gson)
    testImplementation(libs.junit)
    implementation (libs.appcompat.v7)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.chat.uikit.android)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation (libs.logging.interceptor)

    //Firebase dependencies
    implementation (platform(libs.firebase.bom.v3281))
    implementation (libs.firebase.analytics.ktx)
    implementation (libs.firebase.messaging)
    implementation (libs.firebase.core)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}