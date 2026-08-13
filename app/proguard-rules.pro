# Kotlin + kotlinx.serialization
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations, AnnotationDefault
-dontwarn kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Serializable models of this app
-keep,includedescriptorclasses class com.duck.twominute.**$$serializer { *; }
-keepclassmembers class com.duck.twominute.** { *** Companion; }
-keepclasseswithmembers class com.duck.twominute.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Entry points declared in the manifest
-keep class com.duck.twominute.TwoMinuteApp { *; }
-keep class com.duck.twominute.MainActivity { *; }
-keep class com.duck.twominute.alarms.** { *; }

# AndroidX / DataStore internals
-dontwarn androidx.datastore.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
