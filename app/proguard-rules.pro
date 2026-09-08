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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# ZXing core (QR generation + decoding for offline playlist exchange)
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**
# Strip android.util.Log from release builds.
#
# The app logs heavily for debugging - including one Log.e per song during the library scan,
# which writes every file path on the device into logcat. That is noise at best and a small
# privacy leak at worst, and any app on the device with log access can read it.
#
# -assumenosideeffects lets R8 delete the calls entirely, and because it also drops the now-
# unused arguments, the string concatenation that built each message goes with them - so this
# saves the work of building the strings too, not just the write.
#
# Note this removes Log.e as well, not only debug levels, because the codebase uses Log.e for
# ordinary debug output. Genuine production diagnostics should go through Crashlytics
# (FirebaseCrashlytics.log / recordException), which is unaffected by this rule.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static boolean isLoggable(...);
    public static java.lang.String getStackTraceString(...);
}
