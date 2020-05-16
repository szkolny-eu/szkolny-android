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
-keep class pl.szczodrzynski.edziennik.data.db.entity.Event { *; }
-keep class pl.szczodrzynski.edziennik.data.db.full.EventFull { *; }
-keep class pl.szczodrzynski.edziennik.data.db.entity.FeedbackMessage { *; }
-keep class pl.szczodrzynski.edziennik.ui.modules.home.HomeCardModel { *; }
-keepclassmembers class pl.szczodrzynski.edziennik.ui.widgets.WidgetConfig { public *; }
-keepnames class pl.szczodrzynski.edziennik.ui.widgets.timetable.WidgetTimetableProvider
-keepnames class pl.szczodrzynski.edziennik.ui.widgets.notifications.WidgetNotificationsProvider
-keepnames class pl.szczodrzynski.edziennik.ui.widgets.luckynumber.WidgetLuckyNumberProvider

-keepnames class androidx.appcompat.view.menu.MenuBuilder { setHeaderTitleInt(java.lang.CharSequence); }
-keepclassmembernames class androidx.appcompat.view.menu.StandardMenuPopup { private *; }
-keepnames class androidx.appcompat.view.menu.MenuPopupHelper { showPopup(int, int, boolean, boolean); }

-keepclassmembernames class com.mikepenz.materialdrawer.widget.MiniDrawerSliderView { private *; }

-keep class .R
-keep class **.R$* {
    <fields>;
}

-keepattributes SourceFile,LineNumberTable
#-printmapping mapping.txt

-keep class okhttp3.** { *; }

-keep class com.google.android.material.tabs.** {*;}

# ServiceLoader support
        -keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Most of volatile fields are updated with AFU and should not be mangled
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class pl.szczodrzynski.edziennik.data.api.szkolny.interceptor.Signing { public final byte[] pleaseStopRightNow(java.lang.String, long); }

-keepclassmembernames class pl.szczodrzynski.edziennik.data.api.szkolny.request.** { *; }
-keepclassmembernames class pl.szczodrzynski.edziennik.data.api.szkolny.response.** { *; }
-keepclassmembernames class pl.szczodrzynski.edziennik.ui.modules.login.LoginInfo.Platform { *; }
