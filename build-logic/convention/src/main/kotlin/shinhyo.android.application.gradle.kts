import com.android.build.api.dsl.ApplicationExtension
import io.github.shinhyo.buildlogic.configureAndroidCompose
import io.github.shinhyo.buildlogic.configureHiltAndroid
import io.github.shinhyo.buildlogic.configureKotlinAndroid
import io.github.shinhyo.buildlogic.findLibrary
import io.github.shinhyo.buildlogic.findVersion

with(pluginManager) {
    apply("com.android.application")
}

extensions.configure<ApplicationExtension> {
    namespace = "io.github.shinhyo.signinwithapple.sample"

    defaultConfig.applicationId = "io.github.shinhyo.signinwithapple.sample"
    defaultConfig.targetSdk = findVersion("targetSdkVer").toInt()
    defaultConfig.versionCode = findVersion("versionCode").toInt()
    defaultConfig.versionName = findVersion("versionName")
    defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    defaultConfig.vectorDrawables.useSupportLibrary = true

    buildFeatures.buildConfig = true

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
        }

        getByName("release") {
            isDebuggable = false
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    configureKotlinAndroid()
    configureAndroidCompose()
    configureHiltAndroid()
}

dependencies {
    add("implementation", findLibrary("androidx.startup"))
    add("implementation", findLibrary("timber"))
    add("implementation", findLibrary("coil.kt"))
}
