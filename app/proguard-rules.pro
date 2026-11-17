# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class my.hana.meena.**$$serializer { *; }
-keepclassmembers class my.hana.meena.** { *** Companion; }
-keepclasseswithmembers class my.hana.meena.** { kotlinx.serialization.KSerializer serializer(...); }

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**


# CameraX
-keep class androidx.camera.** { *; }

# Health Connect
-keep class androidx.health.connect.** { *; }
