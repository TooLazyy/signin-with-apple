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
package io.github.shinhyo.signinwithapple

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URLEncoder
import java.util.UUID

/**
 * UI State for Apple Sign-In WebView
 */
internal data class UiState(
    val authUrl: String? = null,
    val canGoBack: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Events for Apple Sign-In WebView
 */
internal sealed class AppleSignInEvent {
    data class Success(val resultData: Map<String, String>) : AppleSignInEvent()
    data class Error(val message: String, val errorData: Map<String, String>? = null) :
        AppleSignInEvent()

    object Cancelled : AppleSignInEvent()
}

/**
 * Apple Sign-In configuration
 */
internal data class AppleSignInConfig(
    val clientId: String,
    val redirectUri: String,
    val nonce: String,
    val state: String = UUID.randomUUID().toString(),
)

/**
 * ViewModel for Apple Sign-In WebView Activity
 *
 * Manages the UI state and business logic for the Apple authentication flow.
 * Follows MVVM pattern by separating business logic from UI concerns.
 */
internal class AppleSignInWebViewViewModel() : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<AppleSignInEvent?>(null)
    val events: StateFlow<AppleSignInEvent?> = _events.asStateFlow()

    private var config: AppleSignInConfig? = null

    /**
     * Initializes Apple Sign-In with configuration and starts authentication
     */
    fun initializeAppleSignIn(
        clientId: String,
        redirectUri: String,
        nonce: String,
    ) {
        val newConfig = AppleSignInConfig(
            clientId = clientId,
            redirectUri = redirectUri,
            nonce = nonce,
        )
        this.config = newConfig

        val authUrl = buildAuthUrl(newConfig)
        _uiState.value = _uiState.value.copy(authUrl = authUrl)
    }

    /**
     * Gets the redirect URI from configuration
     */
    fun getRedirectUri(): String? = config?.redirectUri

    /**
     * Gets the state for validation
     */
    fun getState(): String? = config?.state

    /**
     * Builds Apple OAuth authentication URL
     * https://developer.apple.com/documentation/signinwithapplerestapi/generate_and_validate_tokens
     */
    internal fun buildAuthUrl(config: AppleSignInConfig): String {
        val encodedRedirectUri = URLEncoder.encode(config.redirectUri, "UTF-8")
        return "https://appleid.apple.com/auth/authorize" +
            "?client_id=${config.clientId}" +
            "&redirect_uri=$encodedRedirectUri" +
            "&response_type=code%20id_token" +
            "&response_mode=fragment" +
            "&nonce=${config.nonce}" +
            "&state=${config.state}"
    }

    /**
     * Handles the redirect URL from Apple's authentication flow
     */
    fun handleRedirectUrl(url: String) {
        try {
            val fragment = extractUrlFragment(url)
            if (fragment != null) {
                val params = parseQueryParameters(fragment)
                processAuthenticationResult(params)
            } else {
                emitError("Invalid redirect URL format")
            }
        } catch (e: Exception) {
            emitError("Failed to process redirect: ${e.message}")
        }
    }

    /**
     * Updates WebView navigation state
     */
    fun updateWebViewState(canGoBack: Boolean) {
        _uiState.value = _uiState.value.copy(canGoBack = canGoBack)
    }

    /**
     * Handles user cancellation
     */
    fun onUserCancelled() {
        _events.value = AppleSignInEvent.Cancelled
    }

    /**
     * Clears the current event after it's been handled
     */
    fun clearEvent() {
        _events.value = null
    }

    /**
     * Extracts URL fragment for testing
     */
    internal fun extractUrlFragment(url: String): String? {
        val hashIndex = url.indexOf('#')
        return if (hashIndex != -1) {
            url.substring(hashIndex + 1)
        } else {
            null
        }
    }

    /**
     * Processes authentication result based on parameters
     */
    internal fun processAuthenticationResult(params: Map<String, String>) {
        // Validate state to prevent CSRF attacks
        val receivedState = params["state"]
        val expectedState = config?.state

        if (receivedState != null && expectedState != null && receivedState != expectedState) {
            emitError("Security validation failed (state mismatch)")
            return
        }

        if (params.containsKey("error")) {
            handleAuthenticationError(params)
        } else {
            handleAuthenticationSuccess(params)
        }
    }

    /**
     * Handles successful authentication
     */
    internal fun handleAuthenticationSuccess(params: Map<String, String>) {
        val resultData = createSuccessData(params)

        _uiState.value = _uiState.value.copy(isSuccess = true)
        _events.value = AppleSignInEvent.Success(resultData)
    }

    /**
     * Handles authentication errors
     */
    internal fun handleAuthenticationError(params: Map<String, String>) {
        val error = params["error"] ?: "unknown_error"
        val errorDescription = params["error_description"] ?: "Authentication failed"
        val errorData = createErrorData(error, errorDescription)

        _uiState.value = _uiState.value.copy(errorMessage = errorDescription)
        _events.value = AppleSignInEvent.Error(errorDescription, errorData)
    }

    /**
     * Creates success result data
     */
    internal fun createSuccessData(params: Map<String, String>): Map<String, String> {
        return buildMap {
            params["code"]?.let { put("code", it) }
            params["id_token"]?.let { put("id_token", it) }
        }
    }

    /**
     * Creates error result data
     */
    internal fun createErrorData(error: String, errorDescription: String): Map<String, String> {
        return mapOf(
            "error" to error,
            "error_description" to errorDescription,
        )
    }

    /**
     * Emits error event
     */
    internal fun emitError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
        _events.value = AppleSignInEvent.Error(message)
    }

    /**
     * Parses query parameters from URL fragment
     */
    internal fun parseQueryParameters(queryString: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        queryString.split("&").forEach { param ->
            val keyValue = param.split("=", limit = 2)
            if (keyValue.size == 2) {
                val key = keyValue[0]
                val value = keyValue[1]
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    params[key] = value
                }
            }
        }
        return params
    }
}