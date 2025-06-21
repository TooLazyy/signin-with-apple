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
package io.github.shinhyo.signinwithapple

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@ExperimentalCoroutinesApi
class AppleSignInWebViewViewModelIntegrationTest {

    @Test
    fun `buildAuthUrl should create valid Apple OAuth URL`() = runTest {
        // Given
        val viewModel = AppleSignInWebViewViewModel()
        val clientId = "com.example.test"
        val redirectUri = "https://example.com/callback"
        val nonce = "test-nonce-123"
        val state = "test-state-456"

        val config = AppleSignInConfig(
            clientId = clientId,
            redirectUri = redirectUri,
            nonce = nonce,
            state = state,
        )

        // When
        val authUrl = viewModel.buildAuthUrl(config)

        // Then
        assertTrue(
            "URL should start with Apple's OAuth endpoint",
            authUrl.startsWith("https://appleid.apple.com/auth/authorize"),
        )
        assertTrue("URL should contain client_id", authUrl.contains("client_id=$clientId"))
        assertTrue("URL should contain redirect_uri", authUrl.contains("redirect_uri="))
        assertTrue("URL should contain nonce", authUrl.contains("nonce=$nonce"))
        assertTrue("URL should contain state", authUrl.contains("state=$state"))
        assertTrue("URL should contain response_type", authUrl.contains("response_type=code%20id_token"))
        assertTrue("URL should contain response_mode", authUrl.contains("response_mode=fragment"))
    }

    @Test
    fun `parseQueryParameters should parse URL parameters correctly`() {
        // Given
        val viewModel = AppleSignInWebViewViewModel()
        val queryString = "code=abc123&state=xyz789&id_token=token456&empty_param="

        // When
        val params = viewModel.parseQueryParameters(queryString)

        // Then
        assertEquals(3, params.size) // empty_param should be filtered out
        assertEquals("abc123", params["code"])
        assertEquals("xyz789", params["state"])
        assertEquals("token456", params["id_token"])
    }

    @Test
    fun `parseQueryParameters should handle malformed query string`() {
        // Given
        val viewModel = AppleSignInWebViewViewModel()
        val malformedQuery = "invalid=&=value&key_only&proper=value"

        // When
        val params = viewModel.parseQueryParameters(malformedQuery)

        // Then
        assertEquals(1, params.size)
        assertEquals("value", params["proper"])
    }

    @Test
    fun `extractUrlFragment should extract fragment from URL`() {
        // Given
        val viewModel = AppleSignInWebViewViewModel()
        val urlWithFragment = "https://example.com/callback#code=abc&state=xyz"
        val urlWithoutFragment = "https://example.com/callback"

        // When
        val fragment = viewModel.extractUrlFragment(urlWithFragment)
        val noFragment = viewModel.extractUrlFragment(urlWithoutFragment)

        // Then
        assertEquals("code=abc&state=xyz", fragment)
        assertNull(noFragment)
    }

    @Test
    fun `createSuccessData should filter out state parameter`() {
        // Given
        val viewModel = AppleSignInWebViewViewModel()
        val inputParams = mapOf(
            "code" to "authorization_code",
            "state" to "security_state",
            "id_token" to "identity_token",
        )

        // When
        val successData = viewModel.createSuccessData(inputParams)

        // Then
        assertEquals(2, successData.size)
        assertEquals("authorization_code", successData["code"])
        assertEquals("identity_token", successData["id_token"])
        assertFalse("State should be filtered out", successData.containsKey("state"))
    }

    @Test
    fun `createErrorData should create error map`() {
        // Given
        val viewModel = AppleSignInWebViewViewModel()
        val error = "access_denied"
        val errorDescription = "User denied access"

        // When
        val errorData = viewModel.createErrorData(error, errorDescription)

        // Then
        assertEquals(2, errorData.size)
        assertEquals(error, errorData["error"])
        assertEquals(errorDescription, errorData["error_description"])
    }

    @Test
    fun `processAuthenticationResult should validate state correctly`() = runTest {
        // Given
        val viewModel = AppleSignInWebViewViewModel()
        val clientId = "test.client"
        val redirectUri = "https://test.com/callback"
        val nonce = "test-nonce"

        // Initialize with known state
        viewModel.initializeAppleSignIn(clientId, redirectUri, nonce)
        val validState = viewModel.getState()

        val validParams = mapOf(
            "code" to "auth_code",
            "state" to validState!!,
        )

        val invalidParams = mapOf(
            "code" to "auth_code",
            "state" to "invalid_state",
        )

        // When & Then - Valid state should succeed
        viewModel.processAuthenticationResult(validParams)
        val successEvent = viewModel.events.first()
        assertTrue("Should be success event", successEvent is AppleSignInEvent.Success)

        // Invalid state should fail
        viewModel.processAuthenticationResult(invalidParams)
        val errorEvent = viewModel.events.first()
        assertTrue("Should be error event", errorEvent is AppleSignInEvent.Error)
        assertTrue(
            "Error should mention state mismatch",
            (errorEvent as AppleSignInEvent.Error).message.contains("state mismatch"),
        )
    }

    @Test
    fun `initializeAppleSignIn should set up ViewModel state correctly`() = runTest {
        // Given
        val viewModel = AppleSignInWebViewViewModel()
        val clientId = "test.client.id"
        val redirectUri = "https://test.example.com/callback"
        val nonce = "secure-nonce-value"

        // When
        viewModel.initializeAppleSignIn(clientId, redirectUri, nonce)

        // Then
        val uiState = viewModel.uiState.first()
        assertNotNull("Auth URL should be set", uiState.authUrl)
        assertFalse("Should not be success initially", uiState.isSuccess)
        assertNull("No error message initially", uiState.errorMessage)

        assertEquals(redirectUri, viewModel.getRedirectUri())
        assertNotNull("State should be generated", viewModel.getState())

        // Verify auth URL contains expected parameters
        val authUrl = uiState.authUrl!!
        assertTrue("Should contain client_id", authUrl.contains("client_id=$clientId"))
        assertTrue("Should contain nonce", authUrl.contains("nonce=$nonce"))
    }

    @Test
    fun `updateWebViewState should update canGoBack`() = runTest {
        // Given
        val viewModel = AppleSignInWebViewViewModel()

        // When
        viewModel.updateWebViewState(true)
        val uiStateTrue = viewModel.uiState.first()

        viewModel.updateWebViewState(false)
        val uiStateFalse = viewModel.uiState.first()

        // Then
        assertTrue("Should update canGoBack to true", uiStateTrue.canGoBack)
        assertFalse("Should update canGoBack to false", uiStateFalse.canGoBack)
    }

    @Test
    fun `emitError should set error message and emit error event`() = runTest {
        // Given
        val viewModel = AppleSignInWebViewViewModel()
        val errorMessage = "Something went wrong"

        // When
        viewModel.emitError(errorMessage)

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals(errorMessage, uiState.errorMessage)

        val event = viewModel.events.first()
        assertTrue("Should emit error event", event is AppleSignInEvent.Error)
        assertEquals(errorMessage, (event as AppleSignInEvent.Error).message)
    }
}