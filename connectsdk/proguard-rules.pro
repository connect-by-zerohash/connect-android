# Connect SDK ProGuard Rules
# F-008: isMinifyEnabled = true — internal classes are obfuscated by default.
# Only the public API surface and the JavaScript bridge must be kept.

# Keep public API
-keep public class xyz.connect.sdk.ConnectSDK { *; }
-keep public class xyz.connect.sdk.ConnectSDKTypes** { *; }
-keep public class xyz.connect.sdk.ConnectAllowList { *; }
-keep public class xyz.connect.sdk.auth.AuthTypes** { *; }
-keep public interface xyz.connect.sdk.** { *; }

# Keep JavaScript interface
-keepclassmembers class xyz.connect.sdk.ui.WebViewMessageHandler {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep WebView callbacks for the SDK's own clients only — do not keep methods
# on partner-app WebViewClient subclasses (those rules belong in the host app).
-keepclassmembers class xyz.connect.sdk.** extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class xyz.connect.sdk.** extends android.webkit.WebChromeClient {
    public void *(android.webkit.WebView, java.lang.String);
}

# Suppress R8 warning for JDK 9+ string concatenation class not present in
# the Android SDK. Java 17 source + older minSdk generates invokedynamic
# string concat bytecode that references this class; R8 handles the
# desugaring but flags the missing reference unless explicitly suppressed.
-dontwarn java.lang.invoke.StringConcatFactory

# Preserve annotations and generic type signatures (needed for reflection)
-keepattributes Signature
-keepattributes *Annotation*

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
