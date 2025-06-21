plugins {
    id("shinhyo.android.library")
}

android {
    namespace = "io.github.shinhyo.signinwithapple"

    defaultConfig {
        minSdk = 24
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
//    implementation(libs.androidx.constraintlayout)

    // Activity Result API
    implementation(libs.androidx.activity.ktx)


    // Testing
//    testImplementation(libs.test.junit)
//    androidTestImplementation(libs.test.junit.ext)
//    androidTestImplementation(libs.test.espresso.core)
}
