# Proguard rules for MovieNas Native (R8 Full Shrinking & Obfuscation)

# Preserve all JSON models and reflection
-keep class com.movienas.data.** { *; }
-keepclassmembers class com.movienas.data.** { *; }

# Preserve Activities and View Holders
-keep class com.movienas.ui.** { *; }
-keep class com.movienas.ui.adapter.** { *; }
-keep class com.movienas.player.** { *; }

# Glide Image Loading
-keep class com.github.bumptech.glide.** { *; }
-dontwarn com.github.bumptech.glide.**
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Annotations and generic signatures
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
