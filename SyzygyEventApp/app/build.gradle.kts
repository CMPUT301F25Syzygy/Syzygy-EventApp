plugins {
    alias(libs.plugins.android.application)

    // For Dokka docs generation (modern Javadoc alternative)
    id("org.jetbrains.dokka") version "1.9.20"

    // For Firebase
    id("com.google.gms.google-services")
}

// Dokka docs generation configuration
tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    outputDirectory.set(file("${rootProject.projectDir}/doc/javadoc"))

    dokkaSourceSets.register("androidMain") {
        displayName.set("Android App")
        includeNonPublic.set(true)
        reportUndocumented.set(true)
        skipDeprecated.set(false)

        // Source our application code
        sourceRoots.from(file("src/main/java"))

        // Avoid documenting Android internal packages
        perPackageOption {
            matchingRegex.set("android\\..*")
            suppress.set(true)
        }

        // Avoid documenting Java internal packages
        perPackageOption {
            matchingRegex.set("java\\.lang")
            suppress.set(true)
        }
    }
}

android {
    namespace = "com.example.syzygy_eventapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.syzygy_eventapp"
        minSdk = 24
        targetSdk = 36
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-firestore")
}