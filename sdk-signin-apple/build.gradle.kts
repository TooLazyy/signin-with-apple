/*
 * MIT License
 *
 * Copyright (c) 2025 shinhyo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
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
