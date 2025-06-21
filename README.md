# SignInWithApple Android SDK

A simple, secure, and modern Android library for integrating Apple ID sign-in ("Sign in with Apple")
into your app. This project provides both a reusable SDK and a sample app demonstrating usage with
both traditional Android Views and Jetpack Compose.

## Features

- Easy Apple ID sign-in integration for Android
- Secure nonce handling for replay attack prevention
- WebView-based OAuth flow (no custom tabs required)
- Result callback with Apple ID token (JWT)
- Sample app with both View and Compose UI
- JWT payload parsing and nonce validation example

## Modules

- `sdk-signin-apple`: The reusable Apple Sign-In SDK
- `sample`: Example Android app using the SDK (View & Compose)

---

## Getting Started

### 1. Add the SDK to your project

Clone this repository and include the `sdk-signin-apple` module in your project settings.

### 2. Initialize the SDK

In your Application or Activity:

```kotlin
SignInWithApple.init(
    clientId = "<YOUR_APPLE_CLIENT_ID>",
    redirectUri = "<YOUR_REGISTERED_REDIRECT_URI>"
)
```

### 3. Start the Sign-In Flow

You must generate a secure, random nonce for each sign-in attempt and pass it to the SDK:

```kotlin
val nonce = UUID.randomUUID().toString() // Or use a cryptographically secure generator
SignInWithApple.signIn(context, nonce) { result ->
    result.onSuccess { credential ->
        // credential.identityToken (JWT) - send to your backend for verification
    }.onFailure { error ->
        // Handle error
    }
}
```

### 4. Server-Side Verification

- Always verify the `identityToken` (JWT) on your backend.
- Check that the `nonce` in the JWT payload matches the one you generated and sent.
- Validate the JWT signature and claims according to Apple's documentation.

---

## Sample App

The `sample` module demonstrates:

- How to use the SDK in a traditional Activity (View system)
- How to use the SDK in a Jetpack Compose screen
- How to parse and pretty-print the JWT payload
- How to validate the nonce for security

---

## Security Notes

- **Always generate the nonce on the client and verify it on your backend.**
- Never trust the identity token (JWT) without server-side validation.
- Do not use predictable or static nonces.

---

## License

```
Apache License 2.0
Copyright 2025 shinhyo
```

See [LICENSE](LICENSE) for details.

