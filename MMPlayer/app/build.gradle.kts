plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.mmplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mmplayer"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    
    // ExoPlayer for video/audio playback
    implementation(libs.exoplayer.core)
    implementation(libs.exoplayer.ui)
    implementation(libs.exoplayer.mediaSession)
    
    // UI Components
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.swiperefreshlayout)
    
    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel)
    
    // Navigation
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    
    // Image loading (Glide for thumbnails)
    implementation(libs.glide)
    
    // Permissions
    implementation(libs.permissions)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}