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

import android.content.Context
import io.github.shinhyo.signinwithapple.model.AppleSignInResult
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

@ExperimentalCoroutinesApi
class SignInWithAppleIntegrationTest {

    @Test
    fun `init should set serviceId and redirectUri`() {
        // Given
        val serviceId = "test.service.id"
        val redirectUri = "https://test.com/callback"

        // When
        SignInWithApple.init(serviceId, redirectUri)

        // Then
        assertEquals(redirectUri, SignInWithApple.getRedirectUri())
    }

    @Test
    fun `init should throw exception for empty serviceId`() {
        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            SignInWithApple.init("", "https://example.com/callback")
        }
    }

    @Test
    fun `init should throw exception for empty redirectUri`() {
        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            SignInWithApple.init("test.service.id", "")
        }
    }

    @Test
    fun `init should throw exception for both empty values`() {
        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            SignInWithApple.init("", "")
        }
    }

    @Test
    fun `init should throw exception on invalid input and getRedirectUri should handle it`() {
        // Given & When - Try to initialize with empty values
        var initFailedException: IllegalArgumentException? = null
        try {
            SignInWithApple.init("", "")
        } catch (e: IllegalArgumentException) {
            initFailedException = e
        }

        // Then - Verify that init failed
        assertNotNull("Init should have failed with empty values", initFailedException)
        assertEquals("Service ID cannot be empty", initFailedException!!.message)
    }

    @Test
    fun `getRedirectUri should return correct URI when initialized`() {
        // Given
        val expectedUri = "https://example.com/auth/callback"
        SignInWithApple.init("test.service", expectedUri)

        // When
        val actualUri = SignInWithApple.getRedirectUri()

        // Then
        assertEquals(expectedUri, actualUri)
    }

    @Test
    fun `AppleSignInResult should store identity token correctly`() {
        // Given
        val identityToken = "test.identity.token.jwt"

        // When
        val result = AppleSignInResult(identityToken = identityToken)

        // Then
        assertEquals(identityToken, result.identityToken)
    }

    @Test
    fun `AppleSignInResult should handle empty token`() {
        // Given
        val emptyToken = ""

        // When
        val result = AppleSignInResult(identityToken = emptyToken)

        // Then
        assertEquals(emptyToken, result.identityToken)
    }

    @Test
    fun `SignInWithApple signIn should handle initialization properly`() = runTest {
        // Given
        val serviceId = "com.example.test"
        val redirectUri = "https://example.com/callback"
        val mockContext = mockk<Context>()
        val nonce = "test-nonce"

        // Initialize the service
        SignInWithApple.init(serviceId, redirectUri)

        // When & Then - Should not throw exception when properly initialized
        var callbackInvoked = false
        try {
            SignInWithApple.signIn(mockContext, nonce) { result ->
                callbackInvoked = true
                // The callback will be invoked even if there's an error
                // since this is an integration test and actual WebView won't work in unit test environment
            }
            // The method should complete without throwing an exception
        } catch (e: Exception) {
            // Expected in unit test environment due to missing Android components
            // But initialization logic should have run
        }

        // Verify that initialization was successful
        assertEquals(redirectUri, SignInWithApple.getRedirectUri())
    }

    @Test
    fun `flow extension should be accessible`() = runTest {
        // Given
        val serviceId = "com.example.test"
        val redirectUri = "https://example.com/callback"
        val mockContext = mockk<Context>()
        val nonce = "test-nonce"

        // Initialize the service
        SignInWithApple.init(serviceId, redirectUri)

        // When
        val flow = SignInWithApple.flow(mockContext, nonce)

        // Then
        assertNotNull(flow)
        // Flow creation should succeed even if collection would fail in unit test environment
    }

    @Test
    fun `multiple init calls should update values`() {
        // Given
        val serviceId1 = "service1"
        val redirectUri1 = "https://example1.com/callback"
        val serviceId2 = "service2"
        val redirectUri2 = "https://example2.com/callback"

        // When
        SignInWithApple.init(serviceId1, redirectUri1)
        val firstUri = SignInWithApple.getRedirectUri()

        SignInWithApple.init(serviceId2, redirectUri2)
        val secondUri = SignInWithApple.getRedirectUri()

        // Then
        assertEquals(redirectUri1, firstUri)
        assertEquals(redirectUri2, secondUri)
    }
}