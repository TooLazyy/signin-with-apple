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
package io.github.shinhyo.signinwithapple.sample.util

import android.util.Base64
import org.json.JSONObject

object JwtUtil {
    fun parseJwtPayload(jwt: String?): String {
        if (jwt.isNullOrBlank()) return "No JWT token"
        val parts = jwt.split(".")
        if (parts.size != 3) return "Invalid JWT format"
        return try {
            val payload = parts[1]
            val decoded = String(
                Base64.decode(
                    payload,
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
                ),
            )
            val json = JSONObject(decoded)
            json.toString(4) // pretty print with indent
        } catch (e: Exception) {
            "JWT parse error: ${e.message}"
        }
    }
}