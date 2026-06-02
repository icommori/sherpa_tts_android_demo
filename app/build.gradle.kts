import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
// 讀取 local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
android {
    namespace = "com.k2fsa.sherpa.onnx.tts.engine"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.k2fsa.sherpa.onnx.tts.engine"
        minSdk = 21
        targetSdk = 34
        versionCode = 20260513
        versionName = "1.13.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    signingConfigs {
        create("releaseAndDefault") {
            storeFile = file("../innocomm.jks")
            storePassword = localProperties.getProperty("signing.storePassword")
            keyAlias = localProperties.getProperty("signing.keyAlias")
            keyPassword = localProperties.getProperty("signing.keyPassword")
            enableV1Signing = true
            enableV2Signing = true
        }
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("releaseAndDefault")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("releaseAndDefault")
        }

        // 重新命名輸出的 APK 檔名 (KTS 正確語法)
        applicationVariants.all {
            outputs.all {
                val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
                var newName = output.outputFile.name
                newName = newName.replace("app-", "SherpaOnnxTtsEngine_${versionName}_")
                output.outputFileName = newName
            }
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
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // kotlin-api source files are copied to app/src/main/java/com/k2fsa/sherpa/onnx/
    // No extra sourceSets needed
}

tasks.register("downloadModels") {
    val assetsDir = file("src/main/assets")
    val models = listOf(
        "kokoro-multi-lang-v1_1",
        "sherpa-onnx-supertonic-3-tts-int8-2026-05-11",
        "vits-piper-zh_CN-xiao_ya-medium-int8",
        "vits-piper-en_US-lessac-low-int8"
    )
    val baseUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"

    doLast {
        assetsDir.mkdirs()
        models.forEach { model ->
            val modelDir = file("${assetsDir.absolutePath}/$model")
            if (!modelDir.exists()) {
                println("Downloading $model...")
                val tarFile = file("${assetsDir.absolutePath}/${model}.tar.bz2")
                
                exec {
                    workingDir = assetsDir
                    commandLine = listOf("curl", "-L", "-o", tarFile.name, "$baseUrl/${model}.tar.bz2")
                }
                
                println("Extracting $model...")
                exec {
                    workingDir = assetsDir
                    commandLine = listOf("tar", "-xjf", tarFile.name)
                }
                
                tarFile.delete()
            } else {
                println("Model $model already exists in assets, skipping download.")
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn("downloadModels")
}


dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}