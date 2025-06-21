package io.github.shinhyo.signinwithapple

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import io.github.shinhyo.signinwithapple.model.AppleIdCredential
import timber.log.Timber
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Apple Sign-In Service
 *
 * This is the Apple ID authentication service used by external modules.
 * It uses a WebView to perform Apple OAuth authentication and safely delivers the result via ResultReceiver.
 *
 * Usage:
 * - Simply call the signIn(context, callback) method.
 * - No need to handle onActivityResult separately.
 * - Works independently of the Activity lifecycle.
 */
object SignInWithApple {
    private const val TAG = "AppleSignIn"

    private lateinit var clientId: String
    private lateinit var redirectUri: String

    /**
     * Initializes the Apple Sign-In library.
     *
     * @param clientId The Apple service client ID
     * @param redirectUri The redirect URI registered with Apple
     */
    fun init(clientId: String, redirectUri: String) {
        this.clientId = clientId
        this.redirectUri = redirectUri
    }

    /**
     * Starts Apple Sign-In.
     *
     * @param context Context (Activity or Application Context)
     * @param callback Login result callback (on success: AppleIdCredential, on failure: Exception)
     */
    fun signIn(context: Context, callback: (Result<AppleIdCredential>) -> Unit) {
        try {
            if (!::clientId.isInitialized || !::redirectUri.isInitialized) {
                throw IllegalStateException("SignInWithApple must be initialized before use.")
            }
            // Generate state and nonce for security
            val state = UUID.randomUUID().toString()
            val nonce = UUID.randomUUID().toString()

            // Create ResultReceiver
            val resultReceiver = createResultReceiver(callback, state)

            // Build Apple OAuth authentication URL
            val authUrl = buildAuthUrl(state, nonce)

            // Start WebView Activity with ResultReceiver
            val intent = AppleSignInWebViewActivity.createIntent(
                context = context,
                authUrl = authUrl,
                resultReceiver = resultReceiver
            )
            context.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "🍎 [$TAG] Failed to start sign-in")
            callback(Result.failure(e))
        }
    }

    /**
     * Creates a ResultReceiver.
     */
    private fun createResultReceiver(
        callback: (Result<AppleIdCredential>) -> Unit,
        expectedState: String
    ): ResultReceiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            try {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        handleSuccessResult(resultData, callback, expectedState)
                    }

                    Activity.RESULT_CANCELED -> {
                        callback(Result.failure(CancellationException("User canceled the login")))
                    }

                    else -> {
                        callback(Result.failure(Exception("Unknown result code: $resultCode")))
                    }
                }
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }
    }

    /**
     * Handles the success result.
     */
    private fun handleSuccessResult(
        resultData: Bundle?,
        callback: (Result<AppleIdCredential>) -> Unit,
        expectedState: String
    ) {
        try {
            val bundle = resultData ?: throw Exception("No result data")

            val idToken = bundle.getString("id_token")
            val error = bundle.getString("error")
            val receivedState = bundle.getString("state")

            // Validate state (to prevent CSRF attacks)
            if (receivedState != null && receivedState != expectedState) {
                callback(Result.failure(Exception("Security validation failed (state mismatch)")))
                return
            }

            // Handle result based on presence of idToken or error
            when {
                !idToken.isNullOrEmpty() -> {
                    val credential = AppleIdCredential(identityToken = idToken)
                    callback(Result.success(credential))
                }

                !error.isNullOrEmpty() -> {
                    callback(Result.failure(Exception("Apple login error: $error")))
                }

                else -> {
                    callback(Result.failure(Exception("Unknown Apple login result")))
                }
            }
        } catch (e: Exception) {
            callback(Result.failure(e))
        }
    }

    /**
     * Builds the Apple OAuth authentication URL.
     */
    private fun buildAuthUrl(state: String, nonce: String): String {
        // Apply URL encoding
        val encodedRedirectUri = URLEncoder.encode(redirectUri, "UTF-8")

        return "https://appleid.apple.com/auth/authorize" +
            "?client_id=$clientId" +
            "&redirect_uri=$encodedRedirectUri" +
            "&response_type=code%20id_token" +
            "&response_mode=fragment" +
            "&nonce=$nonce" +
            "&state=$state"
    }

    /**
     * Gets the redirect URI.
     */
    fun getRedirectUri(): String = redirectUri
}
