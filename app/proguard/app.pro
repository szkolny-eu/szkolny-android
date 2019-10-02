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
-keep class android.support.v7.widget.** { *; }

-keep class pl.szczodrzynski.edziennik.utils.models.** { *; }
-keep class pl.szczodrzynski.edziennik.data.db.modules.events.Event { *; }
-keep class pl.szczodrzynski.edziennik.data.db.modules.events.EventFull { *; }
-keepclassmembers class pl.szczodrzynski.edziennik.widgets.WidgetConfig { public *; }
-keepnames class pl.szczodrzynski.edziennik.WidgetTimetable
-keepnames class pl.szczodrzynski.edziennik.notifications.WidgetNotifications
-keepnames class pl.szczodrzynski.edziennik.luckynumber.WidgetLuckyNumber

-keep class .R
-keep class **.R$* {
    <fields>;
}

-keepattributes SourceFile,LineNumberTable
#-printmapping mapping.txt

-keep class okhttp3.** { *; }

-keep class com.google.android.material.tabs.** {*;}