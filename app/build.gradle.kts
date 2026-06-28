/*
 * StudySpace — App-Level Build Configuration
 * Applies Firebase, Room, Maps, CameraX, ML Kit, and Navigation dependencies.
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Google Services plugin — required for Firebase (T04, T05, T06)
    alias(libs.plugins.google.services)
    // KSP (Kotlin Symbol Processing) for Room annotation processing (T03)
    id("com.google.devtools.ksp") version "1.9.24-1.0.20"
}

android {
    namespace = "com.example.studyspace"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.studyspace"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    // Enable View Binding — cleaner way to access XML views without findViewById
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // --- AndroidX Core ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // --- Firebase (T04, T05, T06) ---
    // Using BOM ensures all Firebase libraries use compatible versions
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)       // T05 — Authentication
    implementation(libs.firebase.firestore)  // T04 — Online Database
    implementation(libs.firebase.messaging)  // T06 — Push Notifications

    // --- Room Database (T03 — Local Database) ---
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler) // KSP annotation processor for Room

    // --- Google Maps SDK (T08 — Maps) ---
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // --- CameraX (T09 — Camera & QR) ---
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation("com.google.guava:guava:31.1-android")

    // --- ML Kit Barcode Scanner (T09 — QR Code Reader) ---
    implementation(libs.mlkit.barcode.scanning)

    // --- Lifecycle & ViewModel ---
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.runtime.ktx)

    // --- Coroutines ---
    implementation(libs.kotlinx.coroutines.android)

    // --- Navigation Component ---
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    // --- Glide (Image Loading) ---
    implementation(libs.glide)

    // --- Swipe Refresh Layout ---
    implementation(libs.swiperefreshlayout)

    // --- Testing ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
