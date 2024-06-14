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
-keep class pl.szczodrzynski.edziennik.data.db.enums.* { *; }
-keep class pl.szczodrzynski.edziennik.data.db.entity.Event { *; }
-keep class pl.szczodrzynski.edziennik.data.db.full.EventFull { *; }
-keep class pl.szczodrzynski.edziennik.data.db.entity.FeedbackMessage { *; }
-keep class pl.szczodrzynski.edziennik.data.db.entity.Note { *; }
-keep class pl.szczodrzynski.edziennik.ui.home.HomeCardModel { *; }
-keepclassmembers class pl.szczodrzynski.edziennik.ui.widgets.WidgetConfig { public *; }
-keepnames class pl.szczodrzynski.edziennik.ui.widgets.timetable.WidgetTimetableProvider
-keepnames class pl.szczodrzynski.edziennik.ui.widgets.notifications.WidgetNotificationsProvider
-keepnames class pl.szczodrzynski.edziennik.ui.widgets.luckynumber.WidgetLuckyNumberProvider
-keep class pl.szczodrzynski.edziennik.config.AppData { *; }
-keep class pl.szczodrzynski.edziennik.config.AppData$** { *; }
-keep class pl.szczodrzynski.edziennik.utils.managers.TextStylingManager$HtmlMode { *; }

-keepnames class androidx.appcompat.view.menu.MenuBuilder { setHeaderTitleInt(java.lang.CharSequence); }
-keepnames class androidx.appcompat.view.menu.MenuPopupHelper { showPopup(int, int, boolean, boolean); }
-keepclassmembernames class androidx.appcompat.view.menu.StandardMenuPopup { private *; }
-keepclassmembernames class androidx.appcompat.view.menu.MenuItemImpl { private *; }

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

-keepclassmembers class pl.szczodrzynski.edziennik.ui.login.qr.* { *; }
-keepclassmembers class pl.szczodrzynski.edziennik.data.api.szkolny.request.** { *; }
-keepclassmembers class pl.szczodrzynski.edziennik.data.api.szkolny.response.** { *; }
-keepclassmembernames class pl.szczodrzynski.edziennik.ui.login.LoginInfo$Platform { *; }

-keepclassmembernames class pl.szczodrzynski.fslogin.realm.RealmData { *; }
-keepclassmembernames class pl.szczodrzynski.fslogin.realm.RealmData$Type { *; }

# Exclude Retrofit2
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>
-keep,allowobfuscation,allowshrinking class retrofit2.Response