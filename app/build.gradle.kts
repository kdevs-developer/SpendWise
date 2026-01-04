plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.ksp)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.kdev.spendwise"
    compileSdk = 36 // Required for core-ktx 1.17.0

    compileOptions {
        // Sets the Java compiler version
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        // Sets the Kotlin JVM target version to match
        jvmTarget = "17"
    }

    kotlin {
        jvmToolchain(21)
    }

    defaultConfig {
        applicationId = "com.kdev.spendwise"
        minSdk = 26
        targetSdk = 36
    }
    buildTypes {
        release {

            isMinifyEnabled = true
            isShrinkResources = true

            // Use the "optimize" proguard file for more aggressive size reduction
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.unit)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.animation.core)
    // These should now resolve correctly
    val bom = platform(libs.androidx.compose.bom)
    implementation(bom)
    androidTestImplementation(bom)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material.icons.extended)

    // Firebase BOM (Bill of Materials)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // Firebase Auth & Firestore
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    implementation("androidx.core:core-splashscreen:1.2.0")

    implementation("com.google.android.gms:play-services-auth:21.4.0")

    implementation("androidx.biometric:biometric:1.1.0")

    // Room Persistence
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler) // KSP handles the annotation processing
}