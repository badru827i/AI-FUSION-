plugins {
    id("com.android.library")
}

val engineRoot = rootProject.file("engine")

android {
    namespace = "org.the3deer.android.engine"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles(engineRoot.resolve("proguard-rules.pro"))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                engineRoot.resolve("proguard-rules.pro")
            )
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs(
                engineRoot.resolve("src/main/java"),
                engineRoot.resolve("src/obj/java"),
                engineRoot.resolve("src/fbx/java")
            )
            jniLibs.srcDir(engineRoot.resolve("src/fbx/cpp"))
        }
    }

    externalNativeBuild {
        cmake {
            path = engineRoot.resolve("src/fbx/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
}
