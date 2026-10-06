plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val aiFusionIconB64 = layout.projectDirectory.file("src/main/icon/ic_ai_fusion.webp.b64")
val aiFusionGeneratedRes = layout.buildDirectory.dir("generated/res/aiFusionIcon/main")
val generateAiFusionIcon = tasks.register("generateAiFusionIcon") {
    inputs.file(aiFusionIconB64)
    outputs.file(aiFusionGeneratedRes.map { it.file("drawable/ic_ai_fusion_image.webp") })

    doLast {
        val output = aiFusionGeneratedRes.get().file("drawable/ic_ai_fusion_image.webp").asFile
        output.parentFile.mkdirs()
        val decoded = java.util.Base64.getDecoder().decode(aiFusionIconB64.asFile.readText().trim())
        output.writeBytes(decoded)
    }
}

android {
    namespace = "com.aifusion.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aifusion.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "4.4.0"

        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${System.getenv("GOOGLE_WEB_CLIENT_ID") ?: System.getenv("GOOGLE_CLIENT_ID") ?: ""}\""
        )
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets.getByName("main").res.srcDir(aiFusionGeneratedRes)

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(generateAiFusionIcon)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.runtime:runtime-saveable")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.1")

    implementation("dev.ffmpegkit-maintained:llama-android:0.1.1")
    implementation("dev.ffmpegkit-maintained:tesseract-android:5.5.0")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.30.0")
    implementation("com.google.ai.edge.litert:litert:1.4.1")
    implementation("com.google.ai.edge.litert:litert-gpu:1.4.1")

    implementation(project(":engine"))
}
