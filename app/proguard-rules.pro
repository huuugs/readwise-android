# ProGuard rules for ReadWise

# Keep model classes
-keep class com.readwise.core.model.** { *; }
-keep class com.readwise.core.database.entity.** { *; }

# Keep for serialization
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep class * extends android.app.Application

# PDFium
-keep class com.shockwave.pdfium.** { *; }
-keepclassmembers class com.shockwave.pdfium.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
