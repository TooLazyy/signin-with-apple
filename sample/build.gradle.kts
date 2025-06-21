/*
 * Copyright 2025 shinhyo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
plugins {
    id("shinhyo.android.application")
}

android {
    namespace = "io.github.shinhyo.signinwithapple.sample"

    buildFeatures {
        viewBinding = true
    }

}

dependencies {
    // Reference library from JitPack (for public distribution)
    // Uncomment the following line when distributing the application publicly via JitPack.
     implementation(libs.signin.with.apple)

    // For local development - uncomment this line and comment out the JitPack line above if needed
//    implementation(project(":sdk-signin-apple"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.animation)


    testImplementation(libs.junit)
}