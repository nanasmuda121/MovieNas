# Proguard rules for MovieNas Native
-keep class com.movienas.data.** { *; }
-keepclassmembers class * {
    @org.json.** <fields>;
    @org.json.** <methods>;
}
-keep class com.github.bumptech.glide.** { *; }
-keep class androidx.media3.** { *; }
