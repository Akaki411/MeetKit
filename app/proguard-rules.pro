# Keep Gson serialized fields and model classes
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.livekit.meetkit.data.models.** { *; }
