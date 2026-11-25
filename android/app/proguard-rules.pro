# Add project specific ProGuard rules here.
-keep class com.chaquo.python.** { *; }
-keep class com.cortex.app.bridge.** { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
