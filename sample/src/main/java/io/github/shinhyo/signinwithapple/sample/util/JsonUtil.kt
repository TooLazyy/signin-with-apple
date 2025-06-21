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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object JsonUtil {
    private val prettyJson = Json { prettyPrint = true }
    fun <T> toPrettyJson(obj: T, serializer: KSerializer<T>): String {
        return prettyJson.encodeToString(serializer, obj)
    }

    fun toPrettyJsonMap(map: Map<String, String>): String {
        return prettyJson.encodeToString(
            MapSerializer(String.Companion.serializer(), String.serializer()),
            map,
        )
    }
}