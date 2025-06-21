# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep the SignInWithApple public API
-keep public class io.github.shinhyo.signinwithapple.SignInWithApple {
    public *;
}

# Keep the SignInWithApple extension functions
-keep public class io.github.shinhyo.signinwithapple.SignInWithAppleKt {
    public *;
}

# Keep the model classes
-keep public class io.github.shinhyo.signinwithapple.model.** {
    public *;
}

# Keep WebView activity for Apple sign-in
-keep public class io.github.shinhyo.signinwithapple.AppleSignInWebViewActivity {
    public *;
}

# Keep ResultReceiver for callbacks
-keep class * extends android.os.ResultReceiver {
    public *;
}

# Keep kotlinx.coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep kotlinx.serialization
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Keep Flow related classes
-keep class kotlinx.coroutines.flow.** { *; }

# Keep suspend functions
-keepclassmembers class * {
    @kotlin.jvm.JvmStatic *;
}

# Keep coroutines continuation
-keep class kotlin.coroutines.Continuation

# Keep annotation for reflection
-keepattributes *Annotation*

# Keep generic signatures
-keepattributes Signature

# Keep exceptions
-keepattributes Exceptions

# Keep InnerClasses
-keepattributes InnerClasses

# Android specific
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

# WebView
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String);
}
