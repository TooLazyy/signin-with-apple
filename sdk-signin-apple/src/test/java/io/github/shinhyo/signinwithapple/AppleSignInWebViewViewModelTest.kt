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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class AppleSignInWebViewViewModelTest {

    private lateinit var viewModel: AppleSignInWebViewViewModel

    @Before
    fun setUp() {
        viewModel = AppleSignInWebViewViewModel()
    }

    @Test
    fun `updateWebViewState should update canGoBack in UI state`() = runTest {
        // Given
        val canGoBack = true

        // When
        viewModel.updateWebViewState(canGoBack)

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals(canGoBack, uiState.canGoBack)
    }

    @Test
    fun `onUserCancelled should emit Cancelled event`() = runTest {
        // When
        viewModel.onUserCancelled()

        // Then
        val event = viewModel.events.first()
        assertTrue(event is AppleSignInEvent.Cancelled)
    }

    @Test
    fun `clearEvent should clear current event`() = runTest {
        // Given
        viewModel.onUserCancelled()
        assertNotNull(viewModel.events.first())

        // When
        viewModel.clearEvent()

        // Then
        val event = viewModel.events.first()
        assertNull(event)
    }

    @Test
    fun `extractUrlFragment should return fragment when hash exists`() {
        // Given
        val url = "https://example.com/callback#code=abc123&state=xyz"

        // When
        val fragment = viewModel.extractUrlFragment(url)

        // Then
        assertEquals("code=abc123&state=xyz", fragment)
    }

    @Test
    fun `extractUrlFragment should return null when no hash exists`() {
        // Given
        val url = "https://example.com/callback"

        // When
        val fragment = viewModel.extractUrlFragment(url)

        // Then
        assertNull(fragment)
    }

    @Test
    fun `parseQueryParameters should parse parameters correctly`() {
        // Given
        val queryString = "code=abc123&state=xyz&id_token=token123"

        // When
        val params = viewModel.parseQueryParameters(queryString)

        // Then
        assertEquals(3, params.size)
        assertEquals("abc123", params["code"])
        assertEquals("xyz", params["state"])
        assertEquals("token123", params["id_token"])
    }

    @Test
    fun `parseQueryParameters should handle empty parameters`() {
        // Given
        val queryString = "key1=&key2=value2&key3"

        // When
        val params = viewModel.parseQueryParameters(queryString)

        // Then
        assertEquals(1, params.size) // empty parameters should be filtered out
        assertEquals("value2", params["key2"])
    }

    @Test
    fun `createSuccessData should create map with correct values`() {
        // Given
        val params = mapOf(
            "code" to "abc123",
            "state" to "xyz", // state is only for validation and is excluded from the result
            "id_token" to "token123",
        )

        // When
        val data = viewModel.createSuccessData(params)

        // Then
        assertEquals(2, data.size) // state is excluded, so only 2
        assertEquals("abc123", data["code"])
        assertEquals("token123", data["id_token"])
        assertNull(data["state"]) // state is not included in the result
    }

    @Test
    fun `createErrorData should create map with error info`() {
        // Given
        val error = "invalid_request"
        val errorDescription = "The request is invalid"

        // When
        val data = viewModel.createErrorData(error, errorDescription)

        // Then
        assertEquals(error, data["error"])
        assertEquals(errorDescription, data["error_description"])
    }

    @Test
    fun `handleAuthenticationSuccess should update UI state and emit success event`() = runTest {
        // Given
        val params = mapOf(
            "code" to "abc123",
            "state" to "xyz", // state is only for validation
        )

        // When
        viewModel.handleAuthenticationSuccess(params)

        // Then
        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isSuccess)

        val event = viewModel.events.first()
        assertTrue(event is AppleSignInEvent.Success)
        val successEvent = event as AppleSignInEvent.Success
        assertEquals("abc123", successEvent.resultData["code"])
        assertNull(successEvent.resultData["state"]) // state is not included in the result
    }

    @Test
    fun `handleAuthenticationError should update UI state and emit error event`() = runTest {
        // Given
        val params = mapOf(
            "error" to "access_denied",
            "error_description" to "User cancelled the authentication",
        )

        // When
        viewModel.handleAuthenticationError(params)

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals("User cancelled the authentication", uiState.errorMessage)

        val event = viewModel.events.first()
        assertTrue(event is AppleSignInEvent.Error)
        val errorEvent = event as AppleSignInEvent.Error
        assertEquals("User cancelled the authentication", errorEvent.message)
        assertNotNull(errorEvent.errorData)
    }

    @Test
    fun `processAuthenticationResult should handle success case`() = runTest {
        // Given
        val params = mapOf("code" to "abc123")

        // When
        viewModel.processAuthenticationResult(params)

        // Then
        val event = viewModel.events.first()
        assertTrue(event is AppleSignInEvent.Success)
    }

    @Test
    fun `processAuthenticationResult should handle error case`() = runTest {
        // Given
        val params = mapOf("error" to "access_denied")

        // When
        viewModel.processAuthenticationResult(params)

        // Then
        val event = viewModel.events.first()
        assertTrue(event is AppleSignInEvent.Error)
    }

    @Test
    fun `handleRedirectUrl should handle valid URL with fragment`() = runTest {
        // Given
        val url = "https://example.com/callback#code=abc123&state=xyz"

        // When
        viewModel.handleRedirectUrl(url)

        // Then
        val event = viewModel.events.first()
        assertTrue(event is AppleSignInEvent.Success)
    }

    @Test
    fun `handleRedirectUrl should handle invalid URL without fragment`() = runTest {
        // Given
        val url = "https://example.com/callback"

        // When
        viewModel.handleRedirectUrl(url)

        // Then
        val event = viewModel.events.first()
        assertTrue(event is AppleSignInEvent.Error)
        val errorEvent = event as AppleSignInEvent.Error
        assertEquals("Invalid redirect URL format", errorEvent.message)
    }

    @Test
    fun `emitError should emit error event with message`() = runTest {
        // Given
        val errorMessage = "Something went wrong"

        // When
        viewModel.emitError(errorMessage)

        // Then
        val event = viewModel.events.first()
        assertTrue(event is AppleSignInEvent.Error)
        val errorEvent = event as AppleSignInEvent.Error
        assertEquals(errorMessage, errorEvent.message)
    }

    @Test
    fun `initializeAppleSignIn should build auth URL and set UI state`() = runTest {
        // Given
        val clientId = "test.client.id"
        val redirectUri = "https://test.com/callback"
        val nonce = "test-nonce"

        // When
        viewModel.initializeAppleSignIn(clientId, redirectUri, nonce)

        // Then
        val uiState = viewModel.uiState.first()
        assertNotNull(uiState.authUrl)
        assertTrue(uiState.authUrl!!.contains("client_id=$clientId"))
        assertTrue(uiState.authUrl!!.contains("nonce=$nonce"))

        assertEquals(redirectUri, viewModel.getRedirectUri())
        assertNotNull(viewModel.getState())
    }

    @Test
    fun `buildAuthUrl should create correct Apple OAuth URL`() {
        // Given - Since this is an internal method, test it in a different way
        val clientId = "test.client.id"
        val redirectUri = "https://test.com/callback"
        val nonce = "test-nonce"

        // When
        viewModel.initializeAppleSignIn(clientId, redirectUri, nonce)

        // Then
        val uiState = viewModel.uiState.value
        val authUrl = uiState.authUrl!!

        assertTrue(authUrl.startsWith("https://appleid.apple.com/auth/authorize"))
        assertTrue(authUrl.contains("client_id=test.client.id"))
        assertTrue(authUrl.contains("redirect_uri="))
        assertTrue(authUrl.contains("response_type=code%20id_token"))
        assertTrue(authUrl.contains("response_mode=fragment"))
        assertTrue(authUrl.contains("nonce=test-nonce"))
        assertTrue(authUrl.contains("state="))
    }

    @Test
    fun `processAuthenticationResult should validate state and handle success`() = runTest {
        // Given
        viewModel.initializeAppleSignIn("test.client.id", "https://test.com/callback", "test-nonce")
        val state = viewModel.getState()!!
        val params = mapOf(
            "code" to "abc123",
            "state" to state,
        )

        // When
        viewModel.processAuthenticationResult(params)

        // Then
        val event = viewModel.events.first()
        assertTrue(event is AppleSignInEvent.Success)
    }

    @Test
    fun `processAuthenticationResult should reject invalid state`() = runTest {
        // Given
        viewModel.initializeAppleSignIn("test.client.id", "https://test.com/callback", "test-nonce")
        val params = mapOf(
            "code" to "abc123",
            "state" to "invalid-state",
        )

        // When
        viewModel.processAuthenticationResult(params)

        // Then
        val event = viewModel.events.first()
        assertTrue(event is AppleSignInEvent.Error)
        val errorEvent = event as AppleSignInEvent.Error
        assertTrue(errorEvent.message.contains("state mismatch"))
    }
}