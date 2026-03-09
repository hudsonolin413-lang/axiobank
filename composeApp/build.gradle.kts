import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // alias(libs.plugins.composeHotReload)  // Temporarily disabled due to compatibility issue
    alias(libs.plugins.kotlinSerialization)
    // id("com.google.gms.google-services")  // Only needed for Firebase in production
}

kotlin {
    jvmToolchain(17)

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xjdk-release=17")
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xjdk-release=17")
        }
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            mainClass.set("org.dals.project.MainKt")
        }
    }

    // iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(kotlin("stdlib"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation("io.coil-kt.coil3:coil-compose:3.0.4")
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.0.4")
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.android)

            // Firebase - Commented out until google-services plugin is enabled
            // Uncomment when you have proper Firebase configuration
            // implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
            // implementation("com.google.firebase:firebase-analytics")
            // implementation("com.google.firebase:firebase-auth")
            // implementation("com.google.firebase:firebase-firestore")
            // implementation("com.google.firebase:firebase-messaging")
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.cio)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation("com.google.zxing:core:3.5.3")
            implementation("com.google.zxing:javase:3.5.3")
            implementation("com.github.sarxos:webcam-capture:0.3.12")
        }
    }
}

android {
    namespace = "org.dals.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.dals.project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        // Enable desugaring for Java 8+ APIs (like java.time)
        multiDexEnabled = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isDebuggable = false
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        // Enable desugaring to use Java 8+ APIs on older Android versions
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    // Core library desugaring for Java 8+ API support on Android
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}

compose.resources {
    publicResClass = true
    packageOfResClass = "org.dals.project.resources"
    generateResClass = always
}

compose.desktop {
    application {
        mainClass = "org.dals.project.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.dals.project"
            packageVersion = "1.0.0"
        }
    }
}

