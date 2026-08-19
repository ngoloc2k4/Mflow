# Conscrypt
-keep class org.conscrypt.** { *; }

# Ktor & OkHttp
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-keep class io.ktor.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer();
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Media3
-keep class androidx.media3.** { *; }
