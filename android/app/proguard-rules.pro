# Keep the WhiteBoardSpeedup class and its fields
-keep class com.nomivision.sys.WhiteBoardSpeedup { *; }
-keep class com.nomivision.sys.WhiteBoardSpeedup$* { *; }
-keep class com.nomivision.sys.input.** { *; }
-keepclassmembers class com.nomivision.sys.** { *; }

# Keep all native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep all classes in the app package
-keep class com.example.pview_delta.** { *; } 