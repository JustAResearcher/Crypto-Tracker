# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.cryptotracker.data.remote.dto.** { *; }
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes AnnotationDefault,RuntimeVisibleAnnotations

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
