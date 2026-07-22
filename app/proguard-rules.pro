# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve line numbers for readable release crash traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Manifest components (activities, the widget provider) are kept automatically
# by AGP. These extra rules cover WebView surfaces in case JS bridges are added
# later, and keep the framework-invoked WebView client callbacks intact.
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public *;
}
-keepclassmembers class * extends android.webkit.WebViewClient {
    public *;
}
# Uncomment and set the class name if you ever add @JavascriptInterface methods:
#-keepclassmembers class com.example.minimalbrowser.YourBridge {
#   @android.webkit.JavascriptInterface <methods>;
#}