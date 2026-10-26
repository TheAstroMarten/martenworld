# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.martenworld.app.**$$serializer { *; }
-keepclassmembers class com.martenworld.app.** { *** Companion; }
-keepclasseswithmembers class com.martenworld.app.** { kotlinx.serialization.KSerializer serializer(...); }
