plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)

    kotlin("plugin.serialization") version "2.2.21"
}

android {
    namespace = "com.fortioridesign.moxykaroo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fortioridesign.moxykaroo"
        minSdk = 26
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 31
        versionCode = 13
        versionName = "1.0.1"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "karoo-moxymonitor-${variant.name}.apk"
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    /** Android */
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.androidx.lifeycle)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.hammerhead.karoo.ext)

    /** Compose */
    implementation(libs.bundles.compose.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    /** Glance for RemoteView in ride app */
    implementation(libs.androidx.glance.appwidget)

    /** Other */
    implementation(libs.ble.ktx)
    implementation(libs.ble.scanner)

    /** Koin */
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    /** Testing */
    testImplementation(libs.testng)
}