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

import android.content.Context
import io.github.shinhyo.signinwithapple.model.AppleSignInResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CancellationException

@ExperimentalCoroutinesApi
class SignInWithAppleFlowTest {

    private lateinit var mockContext: Context
    private lateinit var mockCancellableSignIn: SignInWithApple.CancellableSignIn

    @Before
    fun setUp() {
        mockContext = mockk()
        mockCancellableSignIn = mockk(relaxed = true)
        mockkObject(SignInWithApple)

        // Initialize SignInWithApple to avoid IllegalStateException
        every { SignInWithApple.init(any(), any()) } returns Unit
        SignInWithApple.init("test.service.id", "https://test.com/callback")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `flow should emit AppleSignInResult on successful sign-in`() = runTest {
        // Given
        val nonce = "test-nonce"
        val expectedResult = AppleSignInResult(identityToken = "test-token")
        val callbackSlot = slot<(Result<AppleSignInResult>) -> Unit>()

        every {
            SignInWithApple.signInCancellable(
                context = mockContext,
                nonce = nonce,
                callback = capture(callbackSlot),
            )
        } answers {
            // Immediately invoke callback with success
            callbackSlot.captured.invoke(Result.success(expectedResult))
            mockCancellableSignIn
        }

        // When
        val flow = SignInWithApple.flow(mockContext, nonce)
        val result = flow.first()

        // Then
        assertEquals(expectedResult, result)
        verify {
            SignInWithApple.signInCancellable(
                context = mockContext,
                nonce = nonce,
                callback = any(),
            )
        }
    }

    @Test
    fun `flow should throw exception on failed sign-in`() = runTest {
        // Given
        val nonce = "test-nonce"
        val expectedException = RuntimeException("Sign-in failed")
        val callbackSlot = slot<(Result<AppleSignInResult>) -> Unit>()

        every {
            SignInWithApple.signInCancellable(
                context = mockContext,
                nonce = nonce,
                callback = capture(callbackSlot),
            )
        } answers {
            // Immediately invoke callback with failure
            callbackSlot.captured.invoke(Result.failure(expectedException))
            mockCancellableSignIn
        }

        // When & Then
        val flow = SignInWithApple.flow(mockContext, nonce)

        var caughtException: Throwable? = null
        try {
            flow.first()
        } catch (e: Throwable) {
            caughtException = e
        }

        assertTrue(caughtException is RuntimeException)
        assertEquals("Sign-in failed", caughtException?.message)
    }

    @Test
    fun `flow should handle cancellation exception`() = runTest {
        // Given
        val nonce = "test-nonce"
        val cancellationException = CancellationException("User cancelled")
        val callbackSlot = slot<(Result<AppleSignInResult>) -> Unit>()

        every {
            SignInWithApple.signInCancellable(
                context = mockContext,
                nonce = nonce,
                callback = capture(callbackSlot),
            )
        } answers {
            // Immediately invoke callback with cancellation
            callbackSlot.captured.invoke(Result.failure(cancellationException))
            mockCancellableSignIn
        }

        // When & Then
        val flow = SignInWithApple.flow(mockContext, nonce)

        var caughtException: Throwable? = null
        try {
            flow.first()
        } catch (e: Throwable) {
            caughtException = e
        }

        assertTrue(caughtException is CancellationException)
        assertEquals("User cancelled", caughtException?.message)
    }

    @Test
    fun `flow should handle multiple collections correctly`() = runTest {
        // Given
        val nonce = "test-nonce"
        val expectedResult = AppleSignInResult(identityToken = "test-token")
        val callbackSlot = slot<(Result<AppleSignInResult>) -> Unit>()

        every {
            SignInWithApple.signInCancellable(
                context = mockContext,
                nonce = nonce,
                callback = capture(callbackSlot),
            )
        } answers {
            // Immediately invoke callback with success
            callbackSlot.captured.invoke(Result.success(expectedResult))
            mockCancellableSignIn
        }

        // When - Collect from flow multiple times
        val flow = SignInWithApple.flow(mockContext, nonce)
        val result1 = flow.first()
        val result2 = flow.first()

        // Then
        assertEquals(expectedResult, result1)
        assertEquals(expectedResult, result2)

        // Verify that signInCancellable was called for each collection
        verify(exactly = 2) {
            SignInWithApple.signInCancellable(
                context = mockContext,
                nonce = nonce,
                callback = any(),
            )
        }
    }

    @Test
    fun `flow should handle empty identity token gracefully`() = runTest {
        // Given
        val nonce = "test-nonce"
        val emptyTokenResult = AppleSignInResult(identityToken = "")
        val callbackSlot = slot<(Result<AppleSignInResult>) -> Unit>()

        every {
            SignInWithApple.signInCancellable(
                context = mockContext,
                nonce = nonce,
                callback = capture(callbackSlot),
            )
        } answers {
            callbackSlot.captured.invoke(Result.success(emptyTokenResult))
            mockCancellableSignIn
        }

        // When
        val flow = SignInWithApple.flow(mockContext, nonce)
        val result = flow.first()

        // Then
        assertEquals(emptyTokenResult, result)
        assertEquals("", result.identityToken)
    }

    @Test
    fun `flow should handle different types of exceptions`() = runTest {
        // Given
        val nonce = "test-nonce"
        val networkException = Exception("Network error")
        val callbackSlot = slot<(Result<AppleSignInResult>) -> Unit>()

        every {
            SignInWithApple.signInCancellable(
                context = mockContext,
                nonce = nonce,
                callback = capture(callbackSlot),
            )
        } answers {
            callbackSlot.captured.invoke(Result.failure(networkException))
            mockCancellableSignIn
        }

        // When & Then
        val flow = SignInWithApple.flow(mockContext, nonce)

        var caughtException: Throwable? = null
        try {
            flow.first()
        } catch (e: Throwable) {
            caughtException = e
        }

        assertTrue(caughtException is Exception)
        assertEquals("Network error", caughtException?.message)
    }

    @Test
    fun `flow should work with different nonce values`() = runTest {
        // Given
        val nonce1 = "test-nonce-1"
        val nonce2 = "test-nonce-2"
        val expectedResult1 = AppleSignInResult(identityToken = "token-1")
        val expectedResult2 = AppleSignInResult(identityToken = "token-2")
        val callbackSlot = slot<(Result<AppleSignInResult>) -> Unit>()

        every {
            SignInWithApple.signInCancellable(
                context = mockContext,
                nonce = nonce1,
                callback = capture(callbackSlot),
            )
        } answers {
            callbackSlot.captured.invoke(Result.success(expectedResult1))
            mockCancellableSignIn
        }

        every {
            SignInWithApple.signInCancellable(
                context = mockContext,
                nonce = nonce2,
                callback = capture(callbackSlot),
            )
        } answers {
            callbackSlot.captured.invoke(Result.success(expectedResult2))
            mockCancellableSignIn
        }

        // When
        val flow1 = SignInWithApple.flow(mockContext, nonce1)
        val flow2 = SignInWithApple.flow(mockContext, nonce2)

        val result1 = flow1.first()
        val result2 = flow2.first()

        // Then
        assertEquals(expectedResult1, result1)
        assertEquals(expectedResult2, result2)

        verify {
            SignInWithApple.signInCancellable(
                context = mockContext,
                nonce = nonce1,
                callback = any(),
            )
        }

        verify {
            SignInWithApple.signInCancellable(
                context = mockContext,
                nonce = nonce2,
                callback = any(),
            )
        }
    }
}