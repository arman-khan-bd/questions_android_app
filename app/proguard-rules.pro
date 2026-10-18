# Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# Gson
-keepattributes EnclosingMethod
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.govtexam.prep.data.model.** { *; }
-keep class com.govtexam.prep.data.remote.response.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Glide
-keep public class * implements com.github.bumptech.glide.module.GlideModule
-keep class * extends com.github.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.github.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
