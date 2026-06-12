# OpenCV loads its native library through JNI; keep the Java bindings.
-keep class org.opencv.** { *; }

# Tesseract4Android JNI bindings.
-keep class com.googlecode.tesseract.android.** { *; }
-keep class com.googlecode.leptonica.android.** { *; }

# PdfBox-Android reflects on font and codec internals.
-keep class com.tom_roush.** { *; }
-dontwarn com.gemalto.jp2.**
-dontwarn com.tom_roush.**

# Kotlinx serialization for type-safe navigation routes and OCR word payloads.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.scanni.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.scanni.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
