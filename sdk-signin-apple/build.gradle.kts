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
    id("shinhyo.android.library")
    id("maven-publish")
}

android {
    namespace = "io.github.shinhyo.signinwithapple"

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            consumerProguardFiles("proguard-rules.pro")
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Activity Result API
    implementation(libs.androidx.activity.ktx)
    implementation(libs.kotlinx.serialization.json)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}

// Function to get version from git tag
fun getVersionName(): String {
    return try {
        val tagOutput = providers.exec {
            commandLine("git", "describe", "--tags", "--abbrev=0")
        }.standardOutput.asText.get().trim()
        
        // Remove 'v' prefix if present (e.g., v1.0.0 -> 1.0.0)
        if (tagOutput.startsWith("v")) {
            tagOutput.substring(1)
        } else {
            tagOutput
        }
    } catch (e: Exception) {
        // Fallback to property or throw error if not found
        findProperty("VERSION_NAME") as String? 
            ?: error("VERSION_NAME property not found and no git tag available. Please set VERSION_NAME in gradle.properties or create a git tag.")
    }
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                
                groupId = "io.github.shinhyo"
                artifactId = "signin-with-apple"
                version = getVersionName()
                
                pom {
                    name.set("SignInWithApple Android SDK")
                    description.set("A simple, secure, and modern Android library for integrating Apple ID sign-in")
                    url.set("https://github.com/shinhyo/SignInWithApple")
                    
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    
                    developers {
                        developer {
                            id.set("shinhyo")
                            name.set("shinhyo")
                            email.set("wonshinhyo@gmail.com")
                        }
                    }
                    
                    scm {
                        connection.set("scm:git:git://github.com/shinhyo/SignInWithApple.git")
                        developerConnection.set("scm:git:ssh://git@github.com:shinhyo/SignInWithApple.git")
                        url.set("https://github.com/shinhyo/SignInWithApple")
                    }
                }
            }
        }
    }
}
