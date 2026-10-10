plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.WalletSIOPM"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.subscriptiontracker"
        minSdk = 24
        targetSdk = 37
        versionCode = 6
        versionName = "2.3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("androidx.work:work-runtime:2.9.0")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
}