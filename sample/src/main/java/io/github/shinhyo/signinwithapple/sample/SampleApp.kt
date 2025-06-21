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
package io.github.shinhyo.signinwithapple.sample

import android.app.Application
import io.github.shinhyo.signinwithapple.SignInWithApple

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize SignInWithApple SDK
        // Configure your Apple Service ID and redirect URI
        // See README.md for detailed setup instructions
        SignInWithApple.init(
            serviceId = "", // Your Apple Service ID from Apple Developer Console
            redirectUri = "", // Your registered redirect URI
        )
    }
}