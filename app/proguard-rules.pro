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
-keepattributes Signature
-keep class android.support.v7.widget.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

-keep class pl.szczodrzynski.edziennik.utils.models.** { *; }
-keep class pl.szczodrzynski.edziennik.data.enums.* { *; }
-keep class pl.szczodrzynski.edziennik.data.db.entity.Event { *; }
-keep class pl.szczodrzynski.edziennik.data.db.full.EventFull { *; }
-keep class pl.szczodrzynski.edziennik.data.db.entity.FeedbackMessage { *; }
-keep class pl.szczodrzynski.edziennik.data.db.entity.Note { *; }
-keep class pl.szczodrzynski.edziennik.ui.home.HomeCardModel { *; }
-keepclassmembers class pl.szczodrzynski.edziennik.ui.widgets.WidgetConfig { public *; }
-keepnames class pl.szczodrzynski.edziennik.ui.widgets.timetable.WidgetTimetableProvider
-keepnames class pl.szczodrzynski.edziennik.ui.widgets.notifications.WidgetNotificationsProvider
-keepnames class pl.szczodrzynski.edziennik.ui.widgets.luckynumber.WidgetLuckyNumberProvider
-keep class pl.szczodrzynski.edziennik.data.config.AppData { *; }
-keep class pl.szczodrzynski.edziennik.data.config.AppData$** { *; }
-keep class pl.szczodrzynski.edziennik.core.manager.TextStylingManager$HtmlMode { *; }

-keepnames class androidx.appcompat.view.menu.MenuBuilder { setHeaderTitleInt(java.lang.CharSequence); }
-keepnames class androidx.appcompat.view.menu.MenuPopupHelper { showPopup(int, int, boolean, boolean); }
-keepclassmembernames class androidx.appcompat.view.menu.StandardMenuPopup { private *; }
-keepclassmembernames class androidx.appcompat.view.menu.MenuItemImpl { private *; }

-keepclassmembernames class com.mikepenz.materialdrawer.widget.MiniDrawerSliderView { private *; }
-keepclassmembernames class com.mikepenz.iconics.internal.IconicsViewsAttrsApplier {
    <fields>;
    readIconicsTextView(android.content.Context, android.util.AttributeSet, com.mikepenz.iconics.internal.CompoundIconsBundle);
    getIconicsImageViewDrawable(android.content.Context, android.util.AttributeSet);
}

-keepclassmembernames class com.mikepenz.iconics.internal.CompoundIconsBundle {
    setIcons(android.widget.TextView);
}

# for RecyclerTabView
-keepclassmembernames class com.google.android.material.tabs.TabLayout {
    tabIndicatorInterpolator;
}
-keepclassmembernames class com.google.android.material.tabs.TabLayout$TabView {
    tab;
    updateTab();
}
-keepclassmembernames class com.google.android.material.tabs.TabIndicatorInterpolator {
    updateIndicatorForOffset(com.google.android.material.tabs.TabLayout, android.view.View, android.view.View, float, android.graphics.drawable.Drawable);
}

-keep class .R
-keep class **.R$* {
    <fields>;
}

-keepattributes SourceFile,LineNumberTable
#-printmapping mapping.txt

-keep class okhttp3.** { *; }

-keep class com.google.android.material.tabs.** {*;}

# Exclude AgendaCalendarView
# Preserve generic type information for EventRenderer and its subclasses
-keepclassmembers class * extends com.github.tibolte.agendacalendarview.render.EventRenderer {
    <fields>;
    <methods>;
}

# Keep the EventRenderer class itself and all its subclasses
-keep class com.github.tibolte.agendacalendarview.render.EventRenderer
-keep class * extends com.github.tibolte.agendacalendarview.render.EventRenderer

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

-keepclassmembers class pl.szczodrzynski.edziennik.ui.login.qr.* { *; }
-keepclassmembers class pl.szczodrzynski.edziennik.data.api.szkolny.request.** { *; }
-keepclassmembers class pl.szczodrzynski.edziennik.data.api.szkolny.response.** { *; }
-keepclassmembernames class pl.szczodrzynski.edziennik.ui.login.LoginInfo$Platform { *; }

-keepclassmembernames class pl.szczodrzynski.fslogin.realm.RealmData { *; }
-keepclassmembernames class pl.szczodrzynski.fslogin.realm.RealmData$Type { *; }
