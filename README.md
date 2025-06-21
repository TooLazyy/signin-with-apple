# 🍎 SignInWithApple Android SDK

[![](https://jitpack.io/v/shinhyo/signin-with-apple.svg)](https://jitpack.io/#shinhyo/signin-with-apple)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Android library** for Apple Sign-In integration. Since Apple doesn't provide an official Kotlin/Android SDK for "Sign in with Apple", this library fills that gap. 

A simple, secure, and modern Android library for integrating Apple ID sign-in ("Sign in with Apple")
into your app. This library focuses on **retrieving Apple's Identity Token (JWT)** that must be verified
on your backend server for secure authentication.

**🎨 UI Flexibility**: This library only handles the authentication flow - you can integrate it with any UI
framework or design system (Android Views, Jetpack Compose, custom layouts, or even dialogs). The sign-in process
uses a WebView overlay, so your app's UI remains completely customizable and can be presented in any form factor.

## Features

- Easy Apple ID sign-in integration for Android
- **Identity Token (JWT) retrieval** - Get Apple's signed JWT for server verification
- Secure nonce handling for replay attack prevention
- **Flexible UI integration** - Works with any UI framework (Views, Compose, custom layouts, dialogs)
- WebView-based OAuth flow (no custom tabs required)
- Result callback with Apple ID token (JWT)
- Sample app with both View and Compose UI
- JWT payload parsing and nonce validation example
- Kotlin-first with full Java compatibility

## Requirements

- **Android API Level 24+** (Android 7.0 Nougat)
- **JDK 17** or higher

## Modules

- `sdk-signin-apple`: The reusable Apple Sign-In SDK
- `sample`: Example Android app using the SDK (View & Compose)

---

## Getting Started

### 1. Add the SDK to your project

Add JitPack repository to your project (`settings.gradle.kts` or `build.gradle.kts`):

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}
```

Add the dependency to your app-level `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.shinhyo:signin-with-apple:latest")
}
```

> **Note:** Replace `latest` with a specific version (e.g., `1.0.0`) for production use.

### 2. Configure Apple Developer Console

Before using this library, you need to set up **Sign in with Apple** in your Apple Developer Console:

#### Step 1: Access Apple Developer Console
1. Go to [Apple Developer Console](https://developer.apple.com/account/resources/identifiers/list)
2. Sign in with your Apple Developer account

#### Step 2: Create App ID (if needed)
1. Create an **App ID** for your iOS app (if you have one)
2. Enable **Sign in with Apple** capability for the App ID

#### Step 3: Create Service ID
1. Navigate to **Identifiers** → **Services IDs**
2. Click the **+** button to create a new Service ID
3. Enter a **Description** (e.g., "My App Sign in with Apple")
4. Enter an **Identifier** (e.g., `com.yourcompany.yourapp.service`) - **This is your Service ID**
5. Enable **Sign in with Apple**
6. Click **Configure** next to "Sign in with Apple"
7. Select your **Primary App ID**
8. Add your **Website URLs** and **Return URLs** (redirect URIs)
   - Website URL: Your domain (e.g., `https://yourapp.com`)
   - Return URL: Your redirect URI (e.g., `https://yourapp.com/auth/apple/callback`)

> **Important:** Use the **Service ID** (the identifier you created, e.g., `com.yourcompany.yourapp.service`) as the `serviceId` parameter in your Android app. Do NOT use the App ID. Service IDs are specifically designed for web and non-iOS platforms like Android.

### 3. Initialize the SDK

In your Application or Activity:

```kotlin
SignInWithApple.init(
    serviceId = "com.yourcompany.yourapp.service", // Your Apple Service ID from step 2
    redirectUri = "https://yourapp.com/auth/apple/callback" // Your registered redirect URI
)
```

> **Remember:** Use the **Service ID** you created in Apple Developer Console, not the App ID.

### 4. Start the Sign-In Flow

You must generate a secure, random nonce for each sign-in attempt and pass it to the SDK:

```kotlin
val nonce = UUID.randomUUID().toString() // Or use a cryptographically secure generator
SignInWithApple.signIn(context, nonce) { result ->
    result.onSuccess { appleSignInResult ->
        // appleSignInResult.identityToken (JWT) - send to your backend for verification
    }.onFailure { error ->
        // Handle error
    }
}
```

### 5. Server-Side Verification ⚠️ **CRITICAL**

**🚨 Security Requirements:**

- **Always verify the `identityToken` (JWT) on your backend server** - Never trust client-side tokens
- **Nonce verification is MANDATORY** - Always check that the `nonce` in the JWT payload matches the one you generated and sent
- **Validate JWT signature and claims** according to Apple's documentation
- **Never skip server-side validation** - This is essential for preventing security vulnerabilities

**🛡️ Built-in Security Features:**

- **State Parameter Validation**: The library automatically generates and validates state parameters to prevent CSRF attacks
- **Secure Nonce Handling**: Cryptographically secure nonce generation and validation
- **URL Validation**: Strict redirect URI validation to prevent redirect attacks

**Why server-side verification is required:**

- Client-side validation can be bypassed by attackers
- The Identity Token contains sensitive user information that must be verified
- Nonce validation prevents replay attacks and ensures token freshness

#### Getting Access Token and Refresh Token (AT/RT)

After successfully verifying the Identity Token on your server, you need to obtain Access Token and Refresh Token from Apple:

1. **Exchange Identity Token for Access/Refresh Tokens:**
   ```http
   POST https://appleid.apple.com/auth/token
   Content-Type: application/x-www-form-urlencoded

   client_id=com.yourcompany.yourapp.service
   client_secret=YOUR_CLIENT_SECRET_JWT
   code=AUTHORIZATION_CODE_FROM_IDENTITY_TOKEN
   grant_type=authorization_code
   ```

2. **Generate Client Secret (JWT):**
   - Create a JWT signed with your Apple private key
   - Include required claims: `iss`, `iat`, `exp`, `aud`, `sub`
   - Use ES256 algorithm for signing

3. **Store Tokens Securely:**
   - **Access Token**: Short-lived token for API calls
   - **Refresh Token**: Long-lived token to refresh access tokens
   - Store both tokens securely on your backend server

4. **Token Refresh:**
   ```http
   POST https://appleid.apple.com/auth/token
   Content-Type: application/x-www-form-urlencoded

   client_id=com.yourcompany.yourapp.service
   client_secret=YOUR_CLIENT_SECRET_JWT
   refresh_token=YOUR_REFRESH_TOKEN
   grant_type=refresh_token
   ```

> **Important:** The authorization code in the Identity Token can only be used once to obtain the initial AT/RT pair. After that, use the refresh token to get new access tokens.

---

## Sample App

The `sample` module demonstrates:

- How to use the SDK in a traditional Activity (View system)
- How to use the SDK in a Jetpack Compose screen
- How to parse and pretty-print the JWT payload
- How to validate the nonce for security

---

## License

```
MIT License
Copyright (c) 2025 shinhyo
```

See [LICENSE](LICENSE) for details.