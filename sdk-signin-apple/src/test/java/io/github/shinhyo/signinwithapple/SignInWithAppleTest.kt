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
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class SignInWithAppleTest {

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
    fun `signIn should invoke callback with success result`() = runTest {
        // Given
        val nonce = "test-nonce"
        val expectedResult = AppleSignInResult(identityToken = "test-token")
        val callbackSlot = slot<(Result<AppleSignInResult>) -> Unit>()
        val latch = CountDownLatch(1)
        var actualResult: Result<AppleSignInResult>? = null

        every {
            SignInWithApple.signInCancellable(
                context = mockContext,
                nonce = nonce,
                callback = capture(callbackSlot),
            )
        } answers {
            // Simulate async callback
            callbackSlot.captured.invoke(Result.success(expectedResult))
            mockCancellableSignIn
        }

        // When
        SignInWithApple.signIn(mockContext, nonce) { result ->
            actualResult = result
            latch.countDown()
        }

        // Wait for callback
        assertTrue("Callback should be invoked within timeout", latch.await(1, TimeUnit.SECONDS))

        // Then
        assertNotNull(actualResult)
        assertTrue("Result should be success", actualResult!!.isSuccess)
        assertEquals(expectedResult, actualResult!!.getOrNull())

        verify {
            SignInWithApple.signInCancellable(
                context = mockContext,
                nonce = nonce,
                callback = any(),
            )
        }
    }

    @Test
    fun `signIn should invoke callback with failure result`() = runTest {
        // Given
        val nonce = "test-nonce"
        val expectedException = RuntimeException("Sign-in failed")
        val callbackSlot = slot<(Result<AppleSignInResult>) -> Unit>()
        val latch = CountDownLatch(1)
        var actualResult: Result<AppleSignInResult>? = null

        every {
            SignInWithApple.signInCancellable(
                context = mockContext,
                nonce = nonce,
                callback = capture(callbackSlot),
            )
        } answers {
            callbackSlot.captured.invoke(Result.failure(expectedException))
            mockCancellableSignIn
        }

        // When
        SignInWithApple.signIn(mockContext, nonce) { result ->
            actualResult = result
            latch.countDown()
        }

        // Wait for callback
        assertTrue("Callback should be invoked within timeout", latch.await(1, TimeUnit.SECONDS))

        // Then
        assertNotNull(actualResult)
        assertTrue("Result should be failure", actualResult!!.isFailure)
        assertTrue("Exception should be RuntimeException", actualResult!!.exceptionOrNull() is RuntimeException)
        assertEquals("Sign-in failed", actualResult!!.exceptionOrNull()?.message)
    }

    @Test
    fun `signIn should handle cancellation exception`() = runTest {
        // Given
        val nonce = "test-nonce"
        val cancellationException = CancellationException("User cancelled")
        val callbackSlot = slot<(Result<AppleSignInResult>) -> Unit>()
        val latch = CountDownLatch(1)
        var actualResult: Result<AppleSignInResult>? = null

        every {
            SignInWithApple.signInCancellable(
                context = mockContext,
                nonce = nonce,
                callback = capture(callbackSlot),
            )
        } answers {
            callbackSlot.captured.invoke(Result.failure(cancellationException))
            mockCancellableSignIn
        }

        // When
        SignInWithApple.signIn(mockContext, nonce) { result ->
            actualResult = result
            latch.countDown()
        }

        // Wait for callback
        assertTrue("Callback should be invoked within timeout", latch.await(1, TimeUnit.SECONDS))

        // Then
        assertNotNull(actualResult)
        assertTrue("Result should be failure", actualResult!!.isFailure)
        assertTrue("Exception should be CancellationException", actualResult!!.exceptionOrNull() is CancellationException)
        assertEquals("User cancelled", actualResult!!.exceptionOrNull()?.message)
    }

    @Test
    fun `signIn should work with multiple simultaneous calls`() = runTest {
        // Given
        val nonce1 = "test-nonce-1"
        val nonce2 = "test-nonce-2"
        val expectedResult1 = AppleSignInResult(identityToken = "test-token-1")
        val expectedResult2 = AppleSignInResult(identityToken = "test-token-2")
        val callbackSlot1 = slot<(Result<AppleSignInResult>) -> Unit>()
        val callbackSlot2 = slot<(Result<AppleSignInResult>) -> Unit>()
        val latch = CountDownLatch(2)
        val actualResults = mutableListOf<Result<AppleSignInResult>>()

        every {
            SignInWithApple.signInCancellable(
                context = mockContext,
                nonce = nonce1,
                callback = capture(callbackSlot1),
            )
        } answers {
            callbackSlot1.captured.invoke(Result.success(expectedResult1))
            mockCancellableSignIn
        }

        every {
            SignInWithApple.signInCancellable(
                context = mockContext,
                nonce = nonce2,
                callback = capture(callbackSlot2),
            )
        } answers {
            callbackSlot2.captured.invoke(Result.success(expectedResult2))
            mockCancellableSignIn
        }

        // When
        SignInWithApple.signIn(mockContext, nonce1) { result ->
            actualResults.add(result)
            latch.countDown()
        }

        SignInWithApple.signIn(mockContext, nonce2) { result ->
            actualResults.add(result)
            latch.countDown()
        }

        // Wait for both callbacks
        assertTrue("Both callbacks should be invoked within timeout", latch.await(2, TimeUnit.SECONDS))

        // Then
        assertEquals(2, actualResults.size)
        assertTrue("All results should be successful", actualResults.all { it.isSuccess })

        val tokens = actualResults.mapNotNull { it.getOrNull()?.identityToken }
        assertTrue("Should contain token-1", tokens.contains("test-token-1"))
        assertTrue("Should contain token-2", tokens.contains("test-token-2"))
    }

    @Test
    fun `signIn should handle empty identity token`() = runTest {
        // Given
        val nonce = "test-nonce"
        val emptyTokenResult = AppleSignInResult(identityToken = "")
        val callbackSlot = slot<(Result<AppleSignInResult>) -> Unit>()
        val latch = CountDownLatch(1)
        var actualResult: Result<AppleSignInResult>? = null

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
        SignInWithApple.signIn(mockContext, nonce) { result ->
            actualResult = result
            latch.countDown()
        }

        // Wait for callback
        assertTrue("Callback should be invoked within timeout", latch.await(1, TimeUnit.SECONDS))

        // Then
        assertNotNull(actualResult)
        assertTrue("Result should be success", actualResult!!.isSuccess)
        val result = actualResult!!.getOrNull()
        assertNotNull(result)
        assertEquals("", result!!.identityToken)
    }

    @Test
    fun `signIn should handle different exception types`() = runTest {
        // Given
        val nonce = "test-nonce"
        val networkException = Exception("Network error")
        val callbackSlot = slot<(Result<AppleSignInResult>) -> Unit>()
        val latch = CountDownLatch(1)
        var actualResult: Result<AppleSignInResult>? = null

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

        // When
        SignInWithApple.signIn(mockContext, nonce) { result ->
            actualResult = result
            latch.countDown()
        }

        // Wait for callback
        assertTrue("Callback should be invoked within timeout", latch.await(1, TimeUnit.SECONDS))

        // Then
        assertNotNull(actualResult)
        assertTrue("Result should be failure", actualResult!!.isFailure)
        assertTrue("Exception should be generic Exception", actualResult!!.exceptionOrNull() is Exception)
        assertEquals("Network error", actualResult!!.exceptionOrNull()?.message)
    }

    @Test
    fun `signIn should work with different nonce values sequentially`() = runTest {
        // Given
        val nonce1 = "nonce-1"
        val nonce2 = "nonce-2"
        val expectedResult1 = AppleSignInResult(identityToken = "token-for-nonce-1")
        val expectedResult2 = AppleSignInResult(identityToken = "token-for-nonce-2")
        val callbackSlot1 = slot<(Result<AppleSignInResult>) -> Unit>()
        val callbackSlot2 = slot<(Result<AppleSignInResult>) -> Unit>()
        val latch1 = CountDownLatch(1)
        val latch2 = CountDownLatch(1)
        var actualResult1: Result<AppleSignInResult>? = null
        var actualResult2: Result<AppleSignInResult>? = null

        every {
            SignInWithApple.signInCancellable(
                context = mockContext,
                nonce = nonce1,
                callback = capture(callbackSlot1),
            )
        } answers {
            callbackSlot1.captured.invoke(Result.success(expectedResult1))
            mockCancellableSignIn
        }

        every {
            SignInWithApple.signInCancellable(
                context = mockContext,
                nonce = nonce2,
                callback = capture(callbackSlot2),
            )
        } answers {
            callbackSlot2.captured.invoke(Result.success(expectedResult2))
            mockCancellableSignIn
        }

        // When
        SignInWithApple.signIn(mockContext, nonce1) { result ->
            actualResult1 = result
            latch1.countDown()
        }

        assertTrue("First callback should be invoked", latch1.await(1, TimeUnit.SECONDS))

        SignInWithApple.signIn(mockContext, nonce2) { result ->
            actualResult2 = result
            latch2.countDown()
        }

        assertTrue("Second callback should be invoked", latch2.await(1, TimeUnit.SECONDS))

        // Then
        assertNotNull(actualResult1)
        assertNotNull(actualResult2)
        assertTrue("First result should be success", actualResult1!!.isSuccess)
        assertTrue("Second result should be success", actualResult2!!.isSuccess)
        assertEquals("token-for-nonce-1", actualResult1!!.getOrNull()?.identityToken)
        assertEquals("token-for-nonce-2", actualResult2!!.getOrNull()?.identityToken)
    }
}