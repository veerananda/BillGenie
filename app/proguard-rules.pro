# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep class * extends androidx.room.TypeConverter

# Keep all model classes (entities)
-keep class com.billgenie.data.entity.* { *; }
-keep class com.billgenie.data.dao.* { *; }
-keep class com.billgenie.data.database.* { *; }

# Gson - Comprehensive rules
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Keep all model classes and their fields (critical for Gson)
-keep class com.billgenie.model.** { *; }
-keep class com.billgenie.BillItemDisplay { *; }
-keep class com.billgenie.MenuItem { *; }

# Keep all classes that might be used via reflection
-keep class com.billgenie.** { *; }

# Keep View binding classes
-keep class com.billgenie.databinding.* { *; }

# Material Design Components
-keep class com.google.android.material.** { *; }
-keep interface com.google.android.material.** { *; }

# AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }