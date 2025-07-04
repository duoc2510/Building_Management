import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.kapt")
    id ("kotlin-parcelize")

}

val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(localPropsFile.inputStream())
}

android {
    namespace = "com.app.buildingmanagement"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.app.buildingmanagement"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val apiKey = requireNotNull(localProps.getProperty("API_KEY")) { "API_KEY missing from local.properties" }
        val clientId = requireNotNull(localProps.getProperty("CLIENT_ID")) { "CLIENT_ID missing from local.properties" }
        val signature = requireNotNull(localProps.getProperty("SIGNATURE")) { "SIGNATURE missing from local.properties" }

        buildConfigField("String", "API_KEY", "\"$apiKey\"")
        buildConfigField("String", "CLIENT_ID", "\"$clientId\"")
        buildConfigField("String", "SIGNATURE", "\"$signature\"")
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}


dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.webkit:webkit:1.14.0")
    implementation ("com.google.android.material:material:1.11.0")
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database.ktx)
    implementation(libs.mpandroidchart)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.messaging)
    implementation(libs.play.services.analytics.impl)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation ("com.google.firebase:firebase-firestore-ktx:24.10.3")
    implementation ("androidx.recyclerview:recyclerview:1.3.2")

    // Picasso
    implementation("com.squareup.picasso:picasso:2.71828")

    // Cloudinary
    implementation("com.cloudinary:cloudinary-android:2.3.1")

    // Desugar
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

}
