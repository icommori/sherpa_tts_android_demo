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

# Preserve line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep all sherpa-onnx JNI classes, data structures, and methods
# This is crucial because C++ code calls these classes via JNI and expects exact names
-keep class com.k2fsa.sherpa.onnx.** { *; }

# Keep all native methods
-keepclasseswithmembernames class * {
    native <methods>;
}