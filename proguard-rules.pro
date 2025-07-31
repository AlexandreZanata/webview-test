# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep JavaScript interface methods
-keepclassmembers class br.com.alexandre.WebViewInterface {
    @android.webkit.JavascriptInterface <methods>;
}

-keepclassmembers class br.com.alexandre.MainActivity$WebAppInterface {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep photo manager methods that might be called via reflection
-keep class br.com.alexandre.PhotoManager {
    public <methods>;
}

# Keep notification manager for proper service operation
-keep class br.com.alexandre.CustomNotificationManager {
    public <methods>;
}

# Keep message polling service
-keep class br.com.alexandre.MessagePollingService {
    public <methods>;
}

# Keep permission helper enums and data classes
-keep class br.com.alexandre.utils.PermissionType
-keep class br.com.alexandre.utils.PermissionResult

# Keep WebView related classes
-keep class android.webkit.** { *; }
-keep class androidx.webkit.** { *; }

# Keep notification related classes
-keep class androidx.core.app.NotificationCompat** { *; }

# Keep file provider related classes
-keep class androidx.core.content.FileProvider { *; }

# Keep location related classes
-keep class android.location.** { *; }

# Standard Android rules
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep classes with main methods
-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}

# Keep activity classes
-keep public class * extends android.app.Activity
-keep public class * extends androidx.appcompat.app.AppCompatActivity

# Keep service classes
-keep public class * extends android.app.Service

# Keep broadcast receiver classes
-keep public class * extends android.content.BroadcastReceiver

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}