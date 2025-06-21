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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.shinhyo.signinwithapple.SignInWithApple
import io.github.shinhyo.signinwithapple.model.AppleSignInResult
import io.github.shinhyo.signinwithapple.sample.util.JsonUtil
import io.github.shinhyo.signinwithapple.sample.util.JwtUtil
import kotlinx.serialization.serializer
import org.json.JSONObject
import timber.log.Timber
import java.util.UUID

class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var log by remember { mutableStateOf("Press the button above to start.") }
            val scrollState = rememberScrollState()
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                ) {
                    Button(
                        onClick = {
                            handleAppleSignIn(
                                onSuccess = { credential, nonce ->
                                    val json = JsonUtil.toPrettyJson(
                                        credential,
                                        serializer<AppleSignInResult>(),
                                    )
                                    val jwtPretty =
                                        JwtUtil.parseJwtPayload(credential.identityToken)
                                    val jwtPayload = try {
                                        JSONObject(jwtPretty)
                                    } catch (e: Exception) {
                                        val errorMsg = "onFailure\n\nJWT parse error: ${e.message}"
                                        Timber.e(errorMsg)
                                        log = errorMsg
                                        null
                                    }
                                    if (jwtPayload != null) {
                                        val jwtNonce = jwtPayload.optString("nonce")
                                        val nonceCheck =
                                            if (jwtNonce == nonce) "✅ nonce matches" else "❌ nonce mismatch"
                                        val message = buildString {
                                            appendLine("onSuccess")
                                            appendLine()
                                            appendLine(json)
                                            appendLine()
                                            appendLine("[JWT Payload]")
                                            appendLine(jwtPretty)
                                            appendLine()
                                            appendLine("[Nonce Check]")
                                            append(nonceCheck)
                                        }
                                        Timber.i(message)
                                        log = message
                                    }
                                },
                                onFailure = { error ->
                                    val jsonMap = JsonUtil.toPrettyJsonMap(
                                        mapOf(
                                            "error" to (error.message ?: "Unknown error"),
                                        ),
                                    )
                                    val message = "onFailure\n\n$jsonMap"
                                    Timber.e(message)
                                    log = message
                                },
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                    ) {
                        Text("Apple Sign")
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(4.dp)
                            .background(Color(0xFFEEEEEE)),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        Text(
                            text = log,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF222222),
                        )
                    }
                }
            }
        }
    }

    private fun handleAppleSignIn(
        onSuccess: (AppleSignInResult, String) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        // nonce is a cryptographically random value used to prevent replay attacks and to verify the integrity of the authentication response.
        // It must be generated by the client, sent to Apple, and then compared with the value in the id_token payload after authentication.
        // If the nonce in the JWT payload does not match the one you generated, the response should be considered invalid.
        val nonce = UUID.randomUUID().toString()
        SignInWithApple.signIn(this, nonce) { result ->
            result.onSuccess {
                onSuccess(it, nonce)
            }.onFailure {
                onFailure(it)
            }
        }
    }

    @Composable
    @Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
    private fun ComposeAppleSignPreview() {
        var log by remember { mutableStateOf("Press the button above to start.") }
        val scrollState = rememberScrollState()
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            ) {
                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                ) {
                    Text("Apple Sign")
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(4.dp)
                        .background(Color(0xFFEEEEEE)),
                    contentAlignment = Alignment.TopStart,
                ) {
                    Text(
                        text = log,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF222222),
                    )
                }
            }
        }
    }
}