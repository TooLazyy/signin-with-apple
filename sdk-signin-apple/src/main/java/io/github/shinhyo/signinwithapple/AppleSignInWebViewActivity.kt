package io.github.shinhyo.signinwithapple

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity

/**
 * Apple Sign-In WebView Activity
 *
 * Displays the Apple OAuth authentication page in a WebView and
 * detects the redirect URL to deliver the authentication result via ResultReceiver.
 */
class AppleSignInWebViewActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_AUTH_URL = "auth_url"
        private const val EXTRA_RESULT_RECEIVER = "extra_result_receiver"

        /**
         * Creates an intent to start this activity.
         */
        fun createIntent(
            context: Context,
            authUrl: String,
            resultReceiver: ResultReceiver,
        ): Intent {
            return Intent(context, AppleSignInWebViewActivity::class.java).apply {
                putExtra(EXTRA_AUTH_URL, authUrl)
                putExtra(EXTRA_RESULT_RECEIVER, resultReceiver)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    private lateinit var webView: WebView
    private var resultReceiver: ResultReceiver? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apple_sign_in_webview)

        // Extract necessary data from Intent
        val authUrl = intent.getStringExtra(EXTRA_AUTH_URL) ?: run {
            sendResultAndFinish(RESULT_CANCELED, null)
            return
        }

        @Suppress("DEPRECATION")
        resultReceiver = if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_RESULT_RECEIVER, ResultReceiver::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_RECEIVER)
        }
        if (resultReceiver == null) {
            finish()
            return
        }

        // WebView settings
        webView = findViewById(R.id.webview)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportMultipleWindows(true)
        }

        // Load URL and handle redirection
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean {
                val url = request.url.toString()

                // Check if the URL is the redirect URI
                if (url.startsWith(SignInWithApple.getRedirectUri())) {
                    handleRedirectUrl(url)
                    return true
                }
                return false
            }
        }

        // Start loading the WebView
        webView.loadUrl(authUrl)

        onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                sendResultAndFinish(RESULT_CANCELED, null)
            }
        }
    }

    /**
     * Handles the redirect URL.
     */
    private fun handleRedirectUrl(url: String) {
        try {
            // Extract fragment from URL
            val hashIndex = url.indexOf('#')
            if (hashIndex != -1) {
                val fragment = url.substring(hashIndex + 1)
                val params = parseQueryParameters(fragment)

                // Create result Bundle
                val resultBundle = Bundle().apply {
                    params["state"]?.let { putString("state", it) }
                    params["code"]?.let { putString("code", it) }
                    params["id_token"]?.let { putString("id_token", it) }
                    params["error"]?.let { putString("error", it) }
                }

                sendResultAndFinish(RESULT_OK, resultBundle)
            } else {
                sendResultAndFinish(RESULT_CANCELED, null)
            }
        } catch (e: Exception) {
            val errorBundle = Bundle().apply {
                putString("error", "redirect_processing_error")
                putString("error_description", e.message)
            }
            sendResultAndFinish(RESULT_CANCELED, errorBundle)
        }
    }

    /**
     * Sends the result and finishes the activity.
     */
    private fun sendResultAndFinish(resultCode: Int, resultData: Bundle?) {
        resultReceiver?.send(resultCode, resultData)
        finish()
    }

    /**
     * Parses the query string and returns a parameter map.
     */
    private fun parseQueryParameters(queryString: String): Map<String, String> {
        val params = mutableMapOf<String, String>()

        queryString.split("&").forEach { param ->
            val keyValue = param.split("=", limit = 2)
            if (keyValue.size == 2) {
                val key = keyValue[0]
                val value = keyValue[1]
                params[key] = value
            }
        }

        return params
    }
}
