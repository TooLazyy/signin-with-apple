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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.shinhyo.signinwithapple.databinding.ActivityAppleSignInWebviewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Apple Sign-In WebView Activity
 *
 * Displays the Apple OAuth authentication page in a WebView and
 * detects the redirect URL to deliver the authentication result via ResultReceiver.
 *
 * Follows MVVM architecture pattern:
 * - View (Activity): Handles UI interactions and lifecycle
 * - ViewModel: Manages UI state and business logic
 * - Model: Data handling is done through the ViewModel
 */
internal class AppleSignInWebViewActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_CLIENT_ID = "client_id"
        private const val EXTRA_REDIRECT_URI = "redirect_uri"
        private const val EXTRA_NONCE = "nonce"
        private const val EXTRA_RESULT_RECEIVER = "extra_result_receiver"

        // Legacy support
        private const val EXTRA_AUTH_URL = "auth_url"

        /**
         * Creates an intent to start this activity with Apple Sign-In configuration.
         */
        fun createIntent(
            context: Context,
            clientId: String,
            redirectUri: String,
            nonce: String,
            resultReceiver: ResultReceiver,
        ): Intent {
            return Intent(context, AppleSignInWebViewActivity::class.java).apply {
                putExtra(EXTRA_CLIENT_ID, clientId)
                putExtra(EXTRA_REDIRECT_URI, redirectUri)
                putExtra(EXTRA_NONCE, nonce)
                putExtra(EXTRA_RESULT_RECEIVER, resultReceiver)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        /**
         * Creates an intent to start this activity (legacy method).
         */
        @Deprecated("Use the new createIntent with individual parameters")
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

    private lateinit var binding: ActivityAppleSignInWebviewBinding
    private var resultReceiver: ResultReceiver? = null
    private val viewModel: AppleSignInWebViewViewModel by viewModels()
    private val jsInterface = FormInterceptorInterface()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppleSignInWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        extractIntentData()
        setupWebView()
        setupObservers()
        setupBackPressHandler()
    }

    /**
     * Extracts necessary data from Intent
     */
    private fun extractIntentData() {
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

        // Check if using new parameter structure
        val clientId = intent.getStringExtra(EXTRA_CLIENT_ID)
        val redirectUri = intent.getStringExtra(EXTRA_REDIRECT_URI)
        val nonce = intent.getStringExtra(EXTRA_NONCE)

        if (clientId != null && redirectUri != null && nonce != null) {
            // New structure: initialize with configuration
            viewModel.initializeAppleSignIn(clientId, redirectUri, nonce)
        }
        jsInterface.expectedState = viewModel.getState() ?: ""
    }

    /**
     * Sets up WebView configuration and client
     */
    private fun setupWebView() {
        binding.webview.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportMultipleWindows(true)
        }
        binding.webview.addJavascriptInterface(
            jsInterface,
            FormInterceptorInterface.NAME
        )

        binding.webview.webViewClient = object : WebViewClient() {

            private val mainHandler= Handler(Looper.getMainLooper())

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                viewModel.updateWebViewState(binding.webview.canGoBack())
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url.toString()
                val redirectUri = viewModel.getRedirectUri() ?: SignInWithApple.getRedirectUri()
                if(request.method == "POST" && url.contains(redirectUri)){
                    try {
                        Thread.currentThread().interrupt()
                    } catch (ex: Exception){}

                    mainHandler.post {
                        view?.stopLoading()
                        view?.loadUrl("javascript:${FormInterceptorInterface.JS_TO_INJECT}")
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean {
                val url = request.url.toString()
                val redirectUri = viewModel.getRedirectUri() ?: SignInWithApple.getRedirectUri()

                if (redirectUri.isEmpty()) {
                    finish() // Gracefully close the activity
                    return false
                }
                if (url.startsWith(redirectUri)) {
                    viewModel.handleRedirectUrl(url)
                    return true
                }
                return false
            }
        }
    }

    /**
     * Sets up observers for ViewModel state and events
     */
    private fun setupObservers() {
        jsInterface.onSuccess = {
            handleEvent(
                AppleSignInEvent.Success(
                    mapOf("id_token" to it)
                )
            )
        }
        jsInterface.onError = {
            handleEvent(AppleSignInEvent.Error(it, null))
        }
        jsInterface.onCancel = {
            handleEvent(AppleSignInEvent.Cancelled)
        }
        // Observe UI State
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { uiState ->
                handleUiState(uiState)
            }
        }

        // Observe Events
        lifecycleScope.launch {
            viewModel.events.collectLatest { event ->
                event?.let { handleEvent(it) }
            }
        }
    }

    /**
     * Handles UI state changes
     */
    private fun handleUiState(uiState: UiState) {
        // Load auth URL when available
        uiState.authUrl?.let { url ->
            if (binding.webview.url == null || binding.webview.url != url) {
                binding.webview.loadUrl(url)
            }
        }
    }

    /**
     * Handles events from ViewModel
     */
    private fun handleEvent(event: AppleSignInEvent) {
        when (event) {
            is AppleSignInEvent.Success -> {
                val resultBundle = mapToBundle(event.resultData)
                sendResultAndFinish(RESULT_OK, resultBundle)
            }

            is AppleSignInEvent.Error -> {
                val errorBundle = event.errorData?.let { mapToBundle(it) } ?: Bundle().apply {
                    putString("error", "redirect_processing_error")
                    putString("error_description", event.message)
                }
                sendResultAndFinish(RESULT_CANCELED, errorBundle)
            }

            is AppleSignInEvent.Cancelled -> {
                sendResultAndFinish(RESULT_CANCELED, null)
            }
        }
        viewModel.clearEvent()
    }

    /**
     * Converts Map to Bundle for result delivery
     */
    private fun mapToBundle(data: Map<String, String>): Bundle {
        return Bundle().apply {
            data.forEach { (key, value) ->
                putString(key, value)
            }
        }
    }

    /**
     * Sets up back press handling
     */
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this) {
            if (binding.webview.canGoBack()) {
                binding.webview.goBack()
            } else {
                viewModel.onUserCancelled()
            }
        }
    }

    /**
     * Sends the result and finishes the activity.
     */
    private fun sendResultAndFinish(resultCode: Int, resultData: Bundle?) {
        resultReceiver?.send(resultCode, resultData)
        finish()
    }
}