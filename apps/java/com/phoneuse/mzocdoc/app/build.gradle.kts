plugins {
    id("com.android.application") version "7.4.2"
    id("org.jetbrains.kotlin.android") version "1.8.22"
    id("com.google.devtools.ksp") version "1.8.22-1.0.11"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
    }
}

android {
    namespace = "com.phoneuse.mzocdoc"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.phoneuse.mzocdoc"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
    
    packagingOptions {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.8"
    }
    
    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
    }
    
    // Disable Java compilation for pure Kotlin project
    tasks.withType<JavaCompile>().configureEach {
        enabled = false
    }
}

dependencies {
    // Room - temporarily disabled due to Java 21 compatibility
    // We'll use JSON loading and direct SQLite operations instead
    // val roomVersion = "2.5.0"
    // implementation("androidx.room:room-runtime:$roomVersion")
    // implementation("androidx.room:room-ktx:$roomVersion")
    // ksp("androidx.room:room-compiler:$roomVersion")
    
    // Compose
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.compose.ui:ui:1.4.3")
    implementation("androidx.compose.material3:material3:1.1.1")
    implementation("androidx.compose.foundation:foundation:1.4.3")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    
    // Core Android
    implementation("androidx.core:core-ktx:1.10.1")
}

