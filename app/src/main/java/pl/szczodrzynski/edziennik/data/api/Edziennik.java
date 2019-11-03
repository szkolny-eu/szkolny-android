package pl.szczodrzynski.edziennik.data.api;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.danimahardhika.cafebar.CafeBar;
import com.google.android.gms.common.util.ArrayUtils;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.BuildConfig;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.WidgetTimetable;
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface;
import pl.szczodrzynski.edziennik.data.api.interfaces.SyncCallback;
import pl.szczodrzynski.edziennik.data.db.modules.announcements.AnnouncementFull;
import pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance;
import pl.szczodrzynski.edziennik.data.db.modules.attendance.AttendanceFull;
import pl.szczodrzynski.edziennik.data.db.modules.events.Event;
import pl.szczodrzynski.edziennik.data.db.modules.events.EventFull;
import pl.szczodrzynski.edziennik.data.db.modules.events.EventType;
import pl.szczodrzynski.edziennik.data.db.modules.grades.GradeFull;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonFull;
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore;
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message;
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageFull;
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata;
import pl.szczodrzynski.edziennik.data.db.modules.notices.Notice;
import pl.szczodrzynski.edziennik.data.db.modules.notices.NoticeFull;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.ProfileFull;
import pl.szczodrzynski.edziennik.data.db.modules.teams.Team;
import pl.szczodrzynski.edziennik.network.ServerRequest;
import pl.szczodrzynski.edziennik.utils.Themes;
import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.models.Notification;
import pl.szczodrzynski.edziennik.widgets.luckynumber.WidgetLuckyNumber;
import pl.szczodrzynski.edziennik.widgets.notifications.WidgetNotifications;

import static android.content.Context.CLIPBOARD_SERVICE;
import static com.mikepenz.iconics.utils.IconicsConvertersKt.colorInt;
import static com.mikepenz.iconics.utils.IconicsConvertersKt.sizeDp;
import static pl.szczodrzynski.edziennik.App.APP_URL;
import static pl.szczodrzynski.edziennik.MainActivity.DRAWER_ITEM_HOME;
import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_OK;
import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_OTHER;
import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_PROFILE_ARCHIVED;
import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_PROFILE_NOT_FOUND;
import static pl.szczodrzynski.edziennik.data.api.AppError.stringErrorCode;
import static pl.szczodrzynski.edziennik.data.api.AppError.stringErrorType;
import static pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface.FEATURE_AGENDA;
import static pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface.FEATURE_ALL;
import static pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface.FEATURE_ANNOUNCEMENTS;
import static pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface.FEATURE_ATTENDANCE;
import static pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface.FEATURE_GRADES;
import static pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface.FEATURE_HOMEWORK;
import static pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface.FEATURE_MESSAGES_INBOX;
import static pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface.FEATURE_MESSAGES_OUTBOX;
import static pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface.FEATURE_NOTICES;
import static pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface.FEATURE_TIMETABLE;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_HOMEWORK;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_SEMESTER1_FINAL;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_SEMESTER1_PROPOSED;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_SEMESTER2_FINAL;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_SEMESTER2_PROPOSED;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_YEAR_FINAL;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_YEAR_PROPOSED;
import static pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore.LOGIN_TYPE_IUCZNIOWIE;
import static pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore.LOGIN_TYPE_LIBRUS;
import static pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore.LOGIN_TYPE_MOBIDZIENNIK;
import static pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore.LOGIN_TYPE_VULCAN;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_AUTO_ARCHIVING;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_LUCKY_NUMBER;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_NEW_ANNOUNCEMENT;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_NEW_ATTENDANCE;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_NEW_EVENT;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_NEW_GRADE;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_NEW_HOMEWORK;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_NEW_MESSAGE;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_NEW_NOTICE;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_NEW_SHARED_EVENT;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_NEW_SHARED_HOMEWORK;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_SERVER_MESSAGE;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_TIMETABLE_LESSON_CHANGE;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.REGISTRATION_ENABLED;
import static pl.szczodrzynski.edziennik.utils.Utils.d;
import static pl.szczodrzynski.edziennik.utils.Utils.ns;

public class Edziennik {
    //public static final int CODE_NULL = 0;
    private App app;

    private static final String TAG = "Edziennik";
    private static boolean registerEmpty;
    public static int oldLuckyNumber;

    public static EdziennikInterface getApi(App app, int loginType) {
        switch (loginType) {
            default:
            case LOGIN_TYPE_MOBIDZIENNIK:
                return app.apiMobidziennik;
            case LOGIN_TYPE_LIBRUS:
                return app.apiLibrus;
            case LOGIN_TYPE_IUCZNIOWIE:
                return app.apiIuczniowie;
            case LOGIN_TYPE_VULCAN:
                return app.apiVulcan;
        }
    }

    public Edziennik(App app) {
        this.app = app;
    }

    @SuppressWarnings("deprecation")
    public static void clearCookies(Context context, String url) {
        //Log.d(TAG, "Cookies: " + yahooCookies);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            //Log.d(TAG, "Using clearCookies code for API >=" + String.valueOf(Build.VERSION_CODES.LOLLIPOP_MR1));
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else {
            //Log.d(TAG, "Using clearCookies code for API <" + String.valueOf(Build.VERSION_CODES.LOLLIPOP_MR1));
            CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(context);
            cookieSyncManager.startSync();
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncManager.stopSync();
            cookieSyncManager.sync();
        }
    }

    public void initMessagesWebView(WebView webView, App app, boolean fullVersion, boolean clearCookies) {
        if (!app.profile.getEmpty() && app.profile.getLoginStoreType() == LoginStore.LOGIN_TYPE_MOBIDZIENNIK) {
            String url;
            if (fullVersion) {
                url = "https://" + app.profile.getLoginData("serverName", "") + ".mobidziennik.pl/mobile/wiadomosci";
            } else {
                url = "https://" + app.profile.getLoginData("serverName", "") + ".mobidziennik.pl/api/";
            }

            String str1 = "login=" + app.profile.getLoginData("username", "") + "&haslo=" + app.profile.getLoginData("password", "");

            if (!fullVersion) {
                str1 += "&ip=" + app.deviceId + "&wersja=20&token=&webview_wiadomosci=1";
            }


            if (-1L != -1L) {
                str1 += "&id_wiadomosci=" + -1L;
            }

            if (clearCookies)
                clearCookies(app, "https://" + app.profile.getLoginData("serverName", "") + ".mobidziennik.pl");

            //Toast.makeText(app, "URL "+url, Toast.LENGTH_SHORT).show();
            webView.postUrl(url, str1.getBytes());
        } else if (!app.profile.getEmpty() && app.profile.getLoginStoreType() == LoginStore.LOGIN_TYPE_IUCZNIOWIE) {
            String url = "https://iuczniowie.progman.pl/idziennik/mod_panelRodzica/Komunikator.aspx";
            webView.loadUrl(url);
            /*
            if (app.profile.loginServerName.equals("") || app.profile.loginUsername.equals("") || app.profile.loginPassword.equals("")) {
                webView.loadData("<h3>"+app.getString(R.string.api_error_code_invalid_login)+"</h3>", "text/html", "UTF-8");
                return;
            }

            if (app.appConfig.deviceId == null || app.appConfig.deviceId.equals("")) {
                app.appConfig.deviceId = Settings.Secure.getString(app.getContentResolver(), Settings.Secure.ANDROID_ID);
                app.appConfig.savePending = true;
            }

            //Ion.getDefault(app.getContext()).getCookieMiddleware().getCookieStore().removeAll(); // TODO remove only cookies for this domain
            String finalLoginServerName = app.profile.loginServerName;
            String finalLoginUsername = app.profile.loginUsername;
            String finalLoginPassword = app.profile.loginPassword;
            Ion.with(app.getContext())
                    .load("https://iuczniowie.progman.pl/idziennik/login.aspx")
                    .setTimeout(REQUEST_TIMEOUT)
                    .setHeader("User-Agent", Iuczniowie.userAgent)
                    //.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .asString()
                    .setCallback((e, result) -> {
                        if (e instanceof java.util.concurrent.TimeoutException) {
                            webView.loadData("<h3>"+app.getString(R.string.api_error_code_timeout)+"</h3>", "text/html", "UTF-8");
                            return;
                        }
                        app.profile.loggedIn = (result != null && result.equals("ok"));
                        if (result == null || result.equals("")) { // for safety
                            webView.loadData("<h3>"+app.getString(R.string.api_error_code_no_internet)+"</h3>", "text/html", "UTF-8");
                            return;
                        }
                        if (clearCookies)
                            clearCookies(app, "https://iuczniowie.progman.pl");

                        String post = "";
                        try {
                            post += "ctl00$ContentPlaceHolder$nazwaPrzegladarki="+URLEncoder.encode(Iuczniowie.userAgent, "UTF-8");
                            post += "ctl00$ContentPlaceHolder$NazwaSzkoly="+finalLoginServerName;
                            post += "ctl00$ContentPlaceHolder$UserName="+URLEncoder.encode(finalLoginUsername, "UTF-8");
                            post += "ctl00$ContentPlaceHolder$Password="+URLEncoder.encode(finalLoginPassword, "UTF-8");
                            post += "ctl00$ContentPlaceHolder$Logowanie=Zaloguj";
                        } catch (UnsupportedEncodingException e1) {
                            e1.printStackTrace();
                        }

                        webView.getSettings().setUserAgentString(Iuczniowie.userAgent);

                    });*/
        } else if (app.profile.getEmpty()) {
            webView.loadData("<h3>" + app.getString(R.string.sync_error_invalid_login) + "</h3>", "text/html", "UTF-8");
        } else {
            webView.loadData("<h3>" + app.getString(R.string.settings_register_login_not_implemented_text) + "</h3>", "text/html", "UTF-8");
        }
    }



    /*    _____                                                        _______        _
         |  __ \                               /\                     |__   __|      | |
         | |__) | __ ___   ___ ___  ___ ___   /  \   ___ _   _ _ __   ___| | __ _ ___| | __
         |  ___/ '__/ _ \ / __/ _ \/ __/ __| / /\ \ / __| | | | '_ \ / __| |/ _` / __| |/ /
         | |   | | | (_) | (_|  __/\__ \__ \/ ____ \\__ \ |_| | | | | (__| | (_| \__ \   <
         |_|   |_|  \___/ \___\___||___/___/_/    \_\___/\__, |_| |_|\___|_|\__,_|___/_|\_\
                                                          __/ |
                                                         |__*/

    /**
     * A task for creating notifications and downloading shared events.
     */
    private static class ProcessAsyncTask extends AsyncTask<Void, Void, Integer> {
        private App app;
        private WeakReference<Context> activityContext;
        private SyncCallback callback;
        private Exception e = null;
        private String apiResponse = null;
        private int profileId;
        private ProfileFull profile;

        public ProcessAsyncTask(App app, Context activityContext, SyncCallback callback, int profileId, ProfileFull profile) {
            //d(TAG, "Thread/ProcessAsyncTask/constructor/"+Thread.currentThread().getName());
            this.app = app;
            this.activityContext = new WeakReference<>(activityContext);
            this.callback = callback;
            this.profileId = profileId;
            this.profile = profile;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            Context activityContext = this.activityContext.get();
            //d(TAG, "Thread/ProcessAsyncTask/doInBackground/"+Thread.currentThread().getName());
            try {

                // UPDATE FCM TOKEN IF EMPTY
                if (app.appConfig.fcmToken == null || app.appConfig.fcmToken.equals("")) {
                    FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(instanceIdResult -> {
                        app.appConfig.fcmToken = instanceIdResult.getToken();
                        app.appConfig.savePending = true;
                    });
                }

                callback.onProgress(1);
                if (profile.getSyncNotifications()) {
                    new Handler(activityContext.getMainLooper()).post(() -> {
                        callback.onActionStarted(R.string.sync_action_creating_notifications);
                    });

                    for (LessonFull change : app.db.lessonChangeDao().getNotNotifiedNow(profileId)) {
                        String text = app.getContext().getString(R.string.notification_lesson_change_format, change.changeTypeStr(app.getContext()), change.lessonDate == null ? "" : change.lessonDate.getFormattedString(), change.subjectLongName);
                        app.notifier.add(new Notification(app.getContext(), text)
                                .withProfileData(profile.getId(), profile.getName())
                                .withType(TYPE_TIMETABLE_LESSON_CHANGE)
                                .withFragmentRedirect(MainActivity.DRAWER_ITEM_TIMETABLE)
                                .withLongExtra("timetableDate", change.lessonDate.getValue())
                                .withAddedDate(change.addedDate)
                        );
                    }
                    for (EventFull event : app.db.eventDao().getNotNotifiedNow(profileId)) {
                        String text;
                        if (event.type == TYPE_HOMEWORK)
                            text = app.getContext().getString(R.string.notification_homework_format, ns(app.getString(R.string.notification_event_no_subject), event.subjectLongName), event.eventDate.getFormattedString());
                        else
                            text = app.getContext().getString(R.string.notification_event_format, event.typeName, event.eventDate.getFormattedString(), ns(app.getString(R.string.notification_event_no_subject), event.subjectLongName));
                        app.notifier.add(new Notification(app.getContext(), text)
                                .withProfileData(profile.getId(), profile.getName())
                                .withType(event.type == TYPE_HOMEWORK ? TYPE_NEW_HOMEWORK : TYPE_NEW_EVENT)
                                .withFragmentRedirect(event.type == TYPE_HOMEWORK ? MainActivity.DRAWER_ITEM_HOMEWORK : MainActivity.DRAWER_ITEM_AGENDA)
                                .withLongExtra("eventId", event.id)
                                .withLongExtra("eventDate", event.eventDate.getValue())
                                .withAddedDate(event.addedDate)
                        );
                        // student's rights abuse - disabled, because this was useless
                        /*if (!event.addedManually && event.type == RegisterEvent.TYPE_EXAM && event.eventDate.combineWith(event.startTime) - event.addedDate < 7 * 24 * 60 * 60 * 1000) {
                            text = app.getContext().getString(R.string.notification_abuse_format, event.typeString(app, app.profile), event.subjectLongName, event.eventDate.getFormattedString());
                            app.notifier.add(new Notification(app.getContext(), text)
                                    .withProfileData(profile.id, profile.name)
                                    .withType(Notification.TYPE_GENERAL)
                                    .withFragmentRedirect(MainActivity.DRAWER_ITEM_NOTIFICATIONS)
                            );
                        }*/
                    }

                    Date today = Date.getToday();
                    int todayValue = today.getValue();
                    profile.setCurrentSemester(profile.dateToSemester(today));

                    for (GradeFull grade : app.db.gradeDao().getNotNotifiedNow(profileId)) {
                        String gradeName = grade.name;
                        if (grade.type == TYPE_SEMESTER1_PROPOSED
                                || grade.type == TYPE_SEMESTER2_PROPOSED) {
                            gradeName = (app.getString(R.string.grade_semester_proposed_format_2, grade.name));
                        } else if (grade.type == TYPE_SEMESTER1_FINAL
                                || grade.type == TYPE_SEMESTER2_FINAL) {
                            gradeName = (app.getString(R.string.grade_semester_final_format_2, grade.name));
                        } else if (grade.type == TYPE_YEAR_PROPOSED) {
                            gradeName = (app.getString(R.string.grade_year_proposed_format_2, grade.name));
                        } else if (grade.type == TYPE_YEAR_FINAL) {
                            gradeName = (app.getString(R.string.grade_year_final_format_2, grade.name));
                        }
                        String text = app.getContext().getString(R.string.notification_grade_format, gradeName, grade.subjectLongName);
                        app.notifier.add(new Notification(app.getContext(), text)
                                .withProfileData(profile.getId(), profile.getName())
                                .withType(TYPE_NEW_GRADE)
                                .withFragmentRedirect(MainActivity.DRAWER_ITEM_GRADES)
                                .withLongExtra("gradesSubjectId", grade.subjectId)
                                .withAddedDate(grade.addedDate)
                        );
                    }
                    for (NoticeFull notice : app.db.noticeDao().getNotNotifiedNow(profileId)) {
                        String noticeTypeStr = (notice.type == Notice.TYPE_POSITIVE ? app.getString(R.string.notification_notice_praise) : (notice.type == Notice.TYPE_NEGATIVE ? app.getString(R.string.notification_notice_warning) : app.getString(R.string.notification_notice_new)));
                        String text = app.getContext().getString(R.string.notification_notice_format, noticeTypeStr, notice.teacherFullName, Date.fromMillis(notice.addedDate).getFormattedString());
                        app.notifier.add(new Notification(app.getContext(), text)
                                .withProfileData(profile.getId(), profile.getName())
                                .withType(TYPE_NEW_NOTICE)
                                .withFragmentRedirect(MainActivity.DRAWER_ITEM_BEHAVIOUR)
                                .withLongExtra("noticeId", notice.id)
                                .withAddedDate(notice.addedDate)
                        );
                    }
                    for (AttendanceFull attendance : app.db.attendanceDao().getNotNotifiedNow(profileId)) {
                        String attendanceTypeStr = app.getString(R.string.notification_type_attendance);
                        switch (attendance.type) {
                            case Attendance.TYPE_ABSENT:
                                attendanceTypeStr = app.getString(R.string.notification_absence);
                                break;
                            case Attendance.TYPE_ABSENT_EXCUSED:
                                attendanceTypeStr = app.getString(R.string.notification_absence_excused);
                                break;
                            case Attendance.TYPE_BELATED:
                                attendanceTypeStr = app.getString(R.string.notification_belated);
                                break;
                            case Attendance.TYPE_BELATED_EXCUSED:
                                attendanceTypeStr = app.getString(R.string.notification_belated_excused);
                                break;
                            case Attendance.TYPE_RELEASED:
                                attendanceTypeStr = app.getString(R.string.notification_release);
                                break;
                        }
                        String text = app.getContext().getString(R.string.notification_attendance_format, attendanceTypeStr, attendance.subjectLongName, attendance.lessonDate.getFormattedString());
                        app.notifier.add(new Notification(app.getContext(), text)
                                .withProfileData(profile.getId(), profile.getName())
                                .withType(TYPE_NEW_ATTENDANCE)
                                .withFragmentRedirect(MainActivity.DRAWER_ITEM_ATTENDANCE)
                                .withLongExtra("attendanceId", attendance.id)
                                .withAddedDate(attendance.addedDate)
                        );
                    }
                    for (AnnouncementFull announcement : app.db.announcementDao().getNotNotifiedNow(profileId)) {
                        String text = app.getContext().getString(R.string.notification_announcement_format, announcement.subject);
                        app.notifier.add(new Notification(app.getContext(), text)
                                .withProfileData(profile.getId(), profile.getName())
                                .withType(TYPE_NEW_ANNOUNCEMENT)
                                .withFragmentRedirect(MainActivity.DRAWER_ITEM_ANNOUNCEMENTS)
                                .withLongExtra("announcementId", announcement.id)
                                .withAddedDate(announcement.addedDate)
                        );
                    }
                    for (MessageFull message : app.db.messageDao().getReceivedNotNotifiedNow(profileId)) {
                        String text = app.getContext().getString(R.string.notification_message_format, message.senderFullName, message.subject);
                        app.notifier.add(new Notification(app.getContext(), text)
                                .withProfileData(profile.getId(), profile.getName())
                                .withType(TYPE_NEW_MESSAGE)
                                .withFragmentRedirect(MainActivity.DRAWER_ITEM_MESSAGES)
                                .withLongExtra("messageType", Message.TYPE_RECEIVED)
                                .withLongExtra("messageId", message.id)
                                .withAddedDate(message.addedDate)
                        );
                    }

                    if (profile.getLuckyNumber() != oldLuckyNumber
                            && profile.getLuckyNumber() != -1
                            && profile.getLuckyNumberDate() != null
                            && profile.getLuckyNumberDate().getValue() >= todayValue) {
                        String text;
                        if (profile.getLuckyNumberDate().getValue() == todayValue) { // LN for today
                            text = app.getString((profile.getStudentNumber() != -1 && profile.getStudentNumber() == profile.getLuckyNumber() ? R.string.notification_lucky_number_yours_format : R.string.notification_lucky_number_format), profile.getLuckyNumber());
                        } else if (profile.getLuckyNumberDate().getValue() == todayValue + 1) { // LN for tomorrow
                            text = app.getString((profile.getStudentNumber() != -1 && profile.getStudentNumber() == profile.getLuckyNumber() ? R.string.notification_lucky_number_yours_tomorrow_format : R.string.notification_lucky_number_tomorrow_format), profile.getLuckyNumber());
                        } else { // LN for later
                            text = app.getString((profile.getStudentNumber() != -1 && profile.getStudentNumber() == profile.getLuckyNumber() ? R.string.notification_lucky_number_yours_later_format : R.string.notification_lucky_number_later_format), profile.getLuckyNumberDate().getFormattedString(), profile.getLuckyNumber());
                        }
                        app.notifier.add(new Notification(app.getContext(), text)
                                .withProfileData(profile.getId(), profile.getName())
                                .withType(TYPE_LUCKY_NUMBER)
                                .withFragmentRedirect(MainActivity.DRAWER_ITEM_HOME)
                        );
                        oldLuckyNumber = profile.getLuckyNumber();
                    }
                }


                app.db.metadataDao().setAllNotified(profileId, true);
                callback.onProgress(1);

                // SEND WEB PUSH, if registration allowed
                // otherwise, UNREGISTER THE USER
                if (profile.getRegistration() == REGISTRATION_ENABLED) {
                    new Handler(activityContext.getMainLooper()).post(() -> {
                        callback.onActionStarted(R.string.sync_action_syncing_shared_events);
                    });
                    //if (profile.registrationUsername == null || profile.registrationUsername.equals("")) {
                    //}
                    ServerRequest syncRequest = new ServerRequest(app, app.requestScheme + APP_URL + "main.php?sync", "Edziennik/REG", profile);

                    if (registerEmpty) {
                        syncRequest.setBodyParameter("first_run", "true");
                    }

                    // ALSO SEND NEW DATA TO BROWSER *excluding* all Shared Events !!!
                    // because they will be sent by the server, as soon as it's shared, by FCM

                    if (app.appConfig.webPushEnabled) {
                        int position = 0;
                        for (Notification notification : app.appConfig.notifications) {
                            //Log.d(TAG, notification.text);
                            if (!notification.notified) {
                                if (notification.type != TYPE_NEW_SHARED_EVENT
                                        && notification.type != TYPE_SERVER_MESSAGE
                                        && notification.type != TYPE_NEW_SHARED_HOMEWORK) // these are automatically sent to the browser by the server
                                {
                                    //Log.d(TAG, "Adding notify[" + position + "]");
                                    syncRequest.setBodyParameter("notify[" + position + "][type]", Integer.toString(notification.type));
                                    syncRequest.setBodyParameter("notify[" + position + "][title]", notification.title);
                                    syncRequest.setBodyParameter("notify[" + position + "][text]", notification.text);
                                    position++;
                                }
                            }
                        }
                    }

                    callback.onProgress(1);

                    if (app.appConfig.webPushEnabled || profile.getEnableSharedEvents()) {
                        JsonObject result = syncRequest.runSync();
                        callback.onProgress(1);
                        //Log.d(TAG, "Executed request");
                        if (result == null) {
                            return AppError.CODE_APP_SERVER_ERROR;
                        }
                        apiResponse = result.toString();
                        if (!result.get("success").getAsString().equals("true")) {
                            return AppError.CODE_APP_SERVER_ERROR;
                        }
                        // HERE PROCESS ALL THE RECEIVED EVENTS
                        // add them to the profile and create appropriate notifications
                        for (JsonElement jEventEl : result.getAsJsonArray("events")) {
                            JsonObject jEvent = jEventEl.getAsJsonObject();
                            String teamCode = jEvent.get("team").getAsString();
                            //d(TAG, "An event is there! "+jEvent.toString());
                            // get the target Team from teamCode
                            Team team = app.db.teamDao().getByCodeNow(profile.getId(), teamCode);
                            if (team != null) {
                                //d(TAG, "The target team is "+team.name+", ID "+team.id);
                                // create the event from Json. Add the missing teamId and !!profileId!!
                                Event event = app.gson.fromJson(jEvent.toString(), Event.class);
                                // proguard. disable for Event.class
                                if (event.eventDate == null) {
                                    apiResponse += "\n\nEventDate == null\n" + jEvent.toString();
                                    throw new Exception("null eventDate");
                                }
                                event.profileId = profile.getId();
                                event.teamId = team.id;
                                event.addedManually = true;
                                //d(TAG, "Created the event! "+event);

                                if (event.sharedBy != null && event.sharedBy.equals(profile.getUsernameId())) {
                                    //d(TAG, "Shared by self! Changing name");
                                    event.sharedBy = "self";
                                    event.sharedByName = profile.getStudentNameLong();
                                }

                                EventType type = app.db.eventTypeDao().getByIdNow(profileId, event.type);

                                //d(TAG, "Finishing adding event "+event);
                                app.db.eventDao().add(event);
                                Metadata metadata = new Metadata(profile.getId(), event.type == TYPE_HOMEWORK ? Metadata.TYPE_HOMEWORK : Metadata.TYPE_EVENT, event.id, registerEmpty, true, jEvent.get("addedDate").getAsLong());
                                long metadataId = app.db.metadataDao().add(metadata);
                                if (metadataId != -1 && !registerEmpty) {
                                    app.notifier.add(new Notification(app.getContext(), app.getString(R.string.notification_shared_event_format, event.sharedByName, type != null ? type.name : "wydarzenie", event.eventDate == null ? "nieznana data" : event.eventDate.getFormattedString(), event.topic))
                                            .withProfileData(profile.getId(), profile.getName())
                                            .withType(event.type == TYPE_HOMEWORK ? TYPE_NEW_SHARED_HOMEWORK : TYPE_NEW_SHARED_EVENT)
                                            .withFragmentRedirect(event.type == TYPE_HOMEWORK ? MainActivity.DRAWER_ITEM_HOMEWORK : MainActivity.DRAWER_ITEM_AGENDA)
                                            .withLongExtra("eventDate", event.eventDate.getValue())
                                    );
                                }
                            }
                        }
                        callback.onProgress(5);
                        return CODE_OK;
                    } else {
                        callback.onProgress(6);
                        return CODE_OK;
                    }
                } else {
                    // the user does not want to be registered
                    callback.onProgress(7);
                    return CODE_OK;
                }
            } catch (Exception e) {
                e.printStackTrace();
                this.e = e;
                return null;
            }
            //return null;
        }

        @Override
        protected void onPostExecute(Integer errorCode) {
            //d(TAG, "Thread/ProcessAsyncTask/onPostExecute/"+Thread.currentThread().getName());
            Context activityContext = this.activityContext.get();
            app.profileSaveFull(profile);
            if (app.profile != null && profile.getId() == app.profile.getId()) {
                app.profile = profile;
            }
            if (errorCode == null) {
                // this means an Exception was thrown
                callback.onError(activityContext, new AppError(TAG, 513, CODE_OTHER, e, apiResponse));
                return;
            }
            //Log.d(TAG, "Finishing");


            callback.onProgress(1);

            if (errorCode == CODE_OK)
                callback.onSuccess(activityContext, profile);
            else {
                try {
                    // oh that's useless
                    throw new RuntimeException(stringErrorCode(app, errorCode, ""));
                } catch (Exception e) {
                    callback.onError(activityContext, new AppError(TAG, 528, errorCode, e, (String) null));
                }
            }
            super.onPostExecute(errorCode);
        }
    }

    public void notifyAndReload() {
        // TODO \/

        Intent intent = new Intent(app.getContext(), WidgetTimetable.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(app).getAppWidgetIds(new ComponentName(app, WidgetTimetable.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        app.sendBroadcast(intent);

        intent = new Intent(app.getContext(), WidgetNotifications.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        ids = AppWidgetManager.getInstance(app).getAppWidgetIds(new ComponentName(app, WidgetNotifications.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        app.sendBroadcast(intent);

        intent = new Intent(app.getContext(), WidgetLuckyNumber.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        ids = AppWidgetManager.getInstance(app).getAppWidgetIds(new ComponentName(app, WidgetLuckyNumber.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        app.sendBroadcast(intent);
    }

    /*     _____
          / ____|
         | (___  _   _ _ __   ___
          \___ \| | | | '_ \ / __|
          ____) | |_| | | | | (__
         |_____/ \__, |_| |_|\___|
                  __/ |
                 |__*/
    // DataCallbacks that are *not* in Edziennik.sync need to be executed on the main thread.
    // EdziennikInterface.sync is executed on a worker thread
    // in Edziennik.sync/newCallback methods are called on a worker thread

    // callback passed to Edziennik.sync is executed on the main thread
    // thus, callback which is in guiSync is also on the main thread

    /**
     * Sync all Edziennik data.
     * Used in services, login form and {@code guiSync}
     * <p>
     * May be ran on worker thread.
     * {@link EdziennikInterface}.sync is ran always on worker thread.
     * Every callback is ran on the UI thread.
     *
     * @param app
     * @param activityContext
     * @param callback
     * @param profileId
     */
    public void sync(@NonNull App app, @NonNull Context activityContext, @NonNull SyncCallback callback, int profileId) {
        sync(app, activityContext, callback, profileId, (int[])null);
    }
    public void sync(@NonNull App app, @NonNull Context activityContext, @NonNull SyncCallback callback, int profileId, @Nullable int ... featureList) {
        // empty: no unread notifications, all shared events (current+past)
        // only if there is no data, and we are not logged in yet
        SyncCallback newCallback = new SyncCallback() {
            @Override
            public void onLoginFirst(List<Profile> profileList, LoginStore loginStore) {
                new Handler(activityContext.getMainLooper()).post(() -> {
                    callback.onLoginFirst(profileList, loginStore);
                });
            }

            @Override
            public void onSuccess(Context activityContext, ProfileFull profileFull) {
                new Handler(activityContext.getMainLooper()).post(() -> {
                    new ProcessAsyncTask(app, activityContext, callback, profileId, profileFull).execute();
                });
            }

            @Override
            public void onError(Context activityContext, AppError error) {
                new Handler(activityContext.getMainLooper()).post(() -> {
                    callback.onError(activityContext, error);
                });
            }

            @Override
            public void onProgress(int progressStep) {
                new Handler(activityContext.getMainLooper()).post(() -> {
                    callback.onProgress(progressStep);
                });
            }

            @Override
            public void onActionStarted(int stringResId) {
                new Handler(activityContext.getMainLooper()).post(() -> {
                    callback.onActionStarted(stringResId);
                });
            }
        };
        AsyncTask.execute(() -> {
            ProfileFull profile = app.db.profileDao().getFullByIdNow(profileId);
            if (profile != null) {

                if (profile.getArchived()) {
                    newCallback.onError(activityContext, new AppError(TAG, 678, CODE_PROFILE_ARCHIVED, profile.getName()));
                    return;
                }
                else if (profile.getDateYearEnd() != null && Date.getToday().getValue() >= profile.getDateYearEnd().getValue()) {
                    profile.setArchived(true);
                    app.notifier.add(new Notification(app.getContext(), app.getString(R.string.profile_auto_archiving_format, profile.getName(), profile.getDateYearEnd().getFormattedString()))
                            .withProfileData(profile.getId(), profile.getName())
                            .withType(TYPE_AUTO_ARCHIVING)
                            .withFragmentRedirect(DRAWER_ITEM_HOME)
                            .withLongExtra("autoArchiving", 1L)
                    );
                    app.notifier.postAll(null);
                    app.db.profileDao().add(profile);
                    if (App.profileId == profile.getId()) {
                        app.profile.setArchived(true);
                    }
                    newCallback.onSuccess(activityContext, profile);
                    return;
                }

                registerEmpty = profile.getEmpty();
                oldLuckyNumber = profile.getLuckyNumber();
                getApi(app, profile.getLoginStoreType()).syncFeature(activityContext, newCallback, profile, featureList);
            } else {
                new Handler(activityContext.getMainLooper()).post(() -> callback.onError(activityContext, new AppError(TAG, 609, CODE_PROFILE_NOT_FOUND, (String) null)));
            }
        });
    }

    /*     _____ _    _ _____
          / ____| |  | |_   _|
         | |  __| |  | | | |   __      ___ __ __ _ _ __  _ __   ___ _ __ ___
         | | |_ | |  | | | |   \ \ /\ / / '__/ _` | '_ \| '_ \ / _ \ '__/ __|
         | |__| | |__| |_| |_   \ V  V /| | | (_| | |_) | |_) |  __/ |  \__ \
          \_____|\____/|_____|   \_/\_/ |_|  \__,_| .__/| .__/ \___|_|  |___/
                                                  | |   | |
                                                  |_|   |*/
    /**
     * Sync all Edziennik data while showing a progress dialog.
     * A wrapper for {@code sync}
     *
     * Does not switch between threads.
     * All callbacks have to be executed on the UI thread.
     *
     * @param app an App singleton instance
     * @param activity a parent activity
     * @param profileId ID of the profile to sync
     * @param dialogTitle a title of the dialog to show
     * @param dialogText dialog's content
     * @param successText a toast to show on success
     */
    public void guiSync(@NonNull App app, @NonNull Activity activity, int profileId, @StringRes int dialogTitle, @StringRes int dialogText, @StringRes int successText) {
        guiSync(app, activity, profileId, dialogTitle, dialogText, successText, (int[])null);
    }
    public void guiSync(@NonNull App app, @NonNull Activity activity, int profileId, @StringRes int dialogTitle, @StringRes int dialogText, @StringRes int successText, int ... featureList) {
        MaterialDialog progressDialog = new MaterialDialog.Builder(activity)
                .title(dialogTitle)
                .content(dialogText)
                .progress(false, 110, false)
                .canceledOnTouchOutside(false)
                .show();
        SyncCallback guiSyncCallback = new SyncCallback() {
            @Override
            public void onLoginFirst(List<Profile> profileList, LoginStore loginStore) {

            }

            @Override
            public void onSuccess(Context activityContext, ProfileFull profileFull) {
                progressDialog.dismiss();
                Toast.makeText(activityContext, successText, Toast.LENGTH_SHORT).show();
                notifyAndReload();
                // profiles are saved automatically, during app.saveConfig in processFinish
                /*if (activityContext instanceof MainActivity) {
                    //((MainActivity) activityContext).reloadCurrentFragment("GuiSync");
                    ((MainActivity) activityContext).accountHeaderAddProfiles();
                }*/
            }

            @Override
            public void onError(Context activityContext, AppError error) {
                progressDialog.dismiss();
                guiShowErrorDialog((Activity) activityContext, error, R.string.sync_error_dialog_title);
            }

            @Override
            public void onProgress(int progressStep) {
                progressDialog.incrementProgress(progressStep);
            }

            @Override
            public void onActionStarted(int stringResId) {
                progressDialog.setContent(activity.getString(R.string.sync_action_format, activity.getString(stringResId)));
            }
        };
        app.apiEdziennik.sync(app, activity, guiSyncCallback, profileId, featureList);
    }
    /**
     * Sync all Edziennik data in background.
     * A callback is executed on main thread.
     * A wrapper for {@code sync}
     *
     * @param app an App singleton instance
     * @param activity a parent activity
     * @param profileId ID of the profile to sync
     * @param syncCallback a callback
     * @param feature a feature to sync
     */
    public void guiSyncSilent(@NonNull App app, @NonNull Activity activity, int profileId, SyncCallback syncCallback, int feature) {
        SyncCallback guiSyncCallback = new SyncCallback() {
            @Override
            public void onLoginFirst(List<Profile> profileList, LoginStore loginStore) {

            }

            @Override
            public void onSuccess(Context activityContext, ProfileFull profileFull) {
                notifyAndReload();
                syncCallback.onSuccess(activityContext, profileFull);
            }

            @Override
            public void onError(Context activityContext, AppError error) {
                syncCallback.onError(activityContext, error);
            }

            @Override
            public void onProgress(int progressStep) {
                syncCallback.onProgress(progressStep);
            }

            @Override
            public void onActionStarted(int stringResId) {
                syncCallback.onActionStarted(stringResId);
            }
        };
        app.apiEdziennik.sync(app, activity, guiSyncCallback, profileId, feature == FEATURE_ALL ? null : new int[]{feature});
    }

    /**
     * Show a dialog allowing the user to choose which features to sync.
     * Handles everything including pre-selecting the features basing on the current fragment.
     *
     * Will execute {@code sync} after the selection is made.
     *
     * A normal progress dialog is shown during the sync.
     *
     * @param app an App singleton instance
     * @param activity a parent activity
     * @param profileId ID of the profile to sync
     * @param dialogTitle a title of the dialog to show
     * @param dialogText dialog's content
     * @param successText a toast to show on success
     * @param currentFeature a feature id representing the currently opened fragment or caller
     */
    public void guiSyncFeature(@NonNull App app,
                               @NonNull Activity activity,
                               int profileId,
                               @StringRes int dialogTitle,
                               @StringRes int dialogText,
                               @StringRes int successText,
                               int currentFeature) {

        String[] items = new String[]{
                app.getString(R.string.menu_timetable),
                app.getString(R.string.menu_agenda),
                app.getString(R.string.menu_grades),
                app.getString(R.string.menu_homework),
                app.getString(R.string.menu_notices),
                app.getString(R.string.menu_attendance),
                app.getString(R.string.title_messages_inbox_single),
                app.getString(R.string.title_messages_sent_single),
                app.getString(R.string.menu_announcements)
        };
        int[] itemsIds = new int[]{
                FEATURE_TIMETABLE,
                FEATURE_AGENDA,
                FEATURE_GRADES,
                FEATURE_HOMEWORK,
                FEATURE_NOTICES,
                FEATURE_ATTENDANCE,
                FEATURE_MESSAGES_INBOX,
                FEATURE_MESSAGES_OUTBOX,
                FEATURE_ANNOUNCEMENTS
        };
        int[] selectedIndices;
        if (currentFeature == FEATURE_ALL) {
            selectedIndices = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
        }
        else {
            selectedIndices = new int[]{Arrays.binarySearch(itemsIds, currentFeature)};
        }

        MaterialDialog dialog = new MaterialDialog.Builder(activity)
                .title(R.string.sync_feature_title)
                .content(R.string.sync_feature_text)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .neutralText(R.string.sync_feature_all)
                .items(items)
                .itemsIds(itemsIds)
                .itemsCallbackMultiChoice(ArrayUtils.toWrapperArray(selectedIndices), (dialog1, which, text) -> {
                    dialog1.getActionButton(DialogAction.POSITIVE).setEnabled(which.length > 0);
                    return true;
                })
                .alwaysCallMultiChoiceCallback()
                .onPositive(((dialog1, which) -> {
                    List<Integer> featureList = new ArrayList<>();
                    for (int i: dialog1.getSelectedIndices()) {
                        featureList.add(itemsIds[i]);
                    }
                    guiSync(app, activity, profileId, dialogTitle, dialogText, successText, ArrayUtils.toPrimitiveArray(featureList));
                }))
                .onNeutral(((dialog1, which) -> {
                    guiSync(app, activity, profileId, dialogTitle, dialogText, successText);
                }))
                .show();



    }

    public void guiShowArchivedDialog(Activity activity, String profileName) {
        new MaterialDialog.Builder(activity)
                .title(R.string.profile_archived_dialog_title)
                .content(activity.getString(R.string.profile_archived_dialog_text_format, profileName))
                .positiveText(R.string.ok)
                .onPositive(((dialog, which) -> dialog.dismiss()))
                .autoDismiss(false)
                .show();
    }

    /*     _____ _    _ _____
          / ____| |  | |_   _|
         | |  __| |  | | | |     ___ _ __ _ __ ___  _ __ ___
         | | |_ | |  | | | |    / _ \ '__| '__/ _ \| '__/ __|
         | |__| | |__| |_| |_  |  __/ |  | | | (_) | |  \__ \
          \_____|\____/|_____|  \___|_|  |_|  \___/|_|  |__*/
    /**
     * Used for reporting an exception somewhere in the code that is not part of Edziennik APIs.
     *
     * @param activity a parent activity
     * @param errorLine the line of code where the error occurred
     * @param e an Exception object
     */
    public void guiReportException(Activity activity, int errorLine, Exception e) {
        guiReportError(activity, new AppError(TAG, errorLine, CODE_OTHER, "Błąd wewnętrzny aplikacji ("+errorLine+")", null, null, e, null), null);
    }

    public void guiShowErrorDialog(Activity activity, @NonNull AppError error, @StringRes int dialogTitle) {
        if (error.errorCode == CODE_PROFILE_ARCHIVED) {
            guiShowArchivedDialog(activity, error.errorText);
            return;
        }
        error.changeIfCodeOther();
        new MaterialDialog.Builder(activity)
                .title(dialogTitle)
                .content(error.asReadableString(activity))
                .positiveText(R.string.ok)
                .onPositive(((dialog, which) -> dialog.dismiss()))
                .neutralText(R.string.sync_error_dialog_report_button)
                .onNeutral(((dialog, which) -> {
                    guiReportError(activity, error, dialog);
                }))
                .autoDismiss(false)
                .show();
    }
    public void guiShowErrorSnackbar(MainActivity activity, @NonNull AppError error) {
        if (error.errorCode == CODE_PROFILE_ARCHIVED) {
            guiShowArchivedDialog(activity, error.errorText);
            return;
        }

        // TODO: 2019-08-28
        IconicsDrawable icon = new IconicsDrawable(activity)
                .icon(CommunityMaterial.Icon.cmd_alert_circle);
        sizeDp(icon, 20);
        colorInt(icon, Themes.INSTANCE.getPrimaryTextColor(activity));

        error.changeIfCodeOther();
        CafeBar.builder(activity)
                .to(activity.findViewById(R.id.coordinator))
                .content(error.asReadableString(activity))
                .icon(icon)
                .positiveText(R.string.more)
                .positiveColor(0xff4caf50)
                .negativeText(R.string.ok)
                .negativeColor(0x66ffffff)
                .onPositive((cafeBar -> guiReportError(activity, error, null)))
                .onNegative((cafeBar -> cafeBar.dismiss()))
                .autoDismiss(false)
                .swipeToDismiss(true)
                .floating(true)
                .show();
    }
    public void guiReportError(Activity activity, AppError error, @Nullable MaterialDialog parentDialogToDisableNeutral) {
        String errorDetails = error.getDetails(activity);
        String htmlErrorDetails = "<small>"+errorDetails+"</small>";
        htmlErrorDetails = htmlErrorDetails.replaceAll(activity.getPackageName(), "<font color='#4caf50'>"+activity.getPackageName()+"</font>");
        htmlErrorDetails = htmlErrorDetails.replaceAll("\n", "<br>");

        new MaterialDialog.Builder(activity)
                .title(R.string.sync_report_dialog_title)
                .content(Html.fromHtml(htmlErrorDetails))
                .typeface(null, "RobotoMono-Regular.ttf")
                .negativeText(R.string.close)
                .onNegative(((dialog1, which1) -> dialog1.dismiss()))
                .neutralText(R.string.copy_to_clipboard)
                .onNeutral((dialog1, which1) -> {
                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        ClipData clip = ClipData.newPlainText("Error report", errorDetails);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(activity, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    }
                })
                .autoDismiss(false)
                .positiveText(R.string.sync_report_dialog_button)
                .checkBoxPromptRes(R.string.sync_report_dialog_include_api_response, true, null)
                .onPositive(((dialog1, which1) -> AsyncTask.execute(() -> error.getApiResponse(activity, apiResponse -> {
                    new ServerRequest(app, app.requestScheme + APP_URL + "main.php?report", "Edziennik/Report")
                            .setBodyParameter("base64_encoded", Base64.encodeToString(errorDetails.getBytes(), Base64.DEFAULT))
                            .setBodyParameter("api_response", dialog1.isPromptCheckBoxChecked() ? Base64.encodeToString(apiResponse.getBytes(), Base64.DEFAULT) : "VW5jaGVja2Vk"/*Unchecked*/)
                            .run((e, result) -> {
                                new Handler(activity.getMainLooper()).post(() -> {
                                    if (result != null)
                                    {
                                        if (result.get("success").getAsBoolean()) {
                                            Toast.makeText(activity, activity.getString(R.string.crash_report_sent), Toast.LENGTH_SHORT).show();
                                            dialog1.getActionButton(DialogAction.POSITIVE).setEnabled(false);
                                            if (parentDialogToDisableNeutral != null)
                                                parentDialogToDisableNeutral.getActionButton(DialogAction.NEUTRAL).setEnabled(false);
                                        }
                                        else {
                                            Toast.makeText(activity, activity.getString(R.string.crash_report_cannot_send) + ": " + result.get("reason").getAsString(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                    else
                                    {
                                        Toast.makeText(activity, activity.getString(R.string.crash_report_cannot_send)+" brak internetu", Toast.LENGTH_LONG).show();
                                    }
                                });
                            });
                }))))
                .show();
    }

    /**
     * A method that displays a dialog allowing the user to report an error that has occurred.
     *
     * @param activity a parent activity
     * @param errorCode self-explanatory
     * @param errorText additional error information, that replaces text based on {@code errorCode} if it's {@code CODE_OTHER}
     * @param throwable a {@link Throwable} containing the error details
     * @param apiResponse response of the Edziennik API
     * @param parentDialogToDisableNeutral if not null, an instance of {@link MaterialDialog} in which the neutral button should be disabled after submitting an error report
     */
    public void guiReportError(Activity activity, int errorCode, String errorText, Throwable throwable, String apiResponse, @Nullable MaterialDialog parentDialogToDisableNeutral) {
        // build a string containing the stack trace and the device name + user's registration data
        String contentPlain = "Application Internal Error "+stringErrorType(errorCode)+":\n"+stringErrorCode(activity, errorCode, "")+"\n"+errorText+"\n\n";
        contentPlain += Log.getStackTraceString(throwable);
        String content = "<small>"+contentPlain+"</small>";
        content = content.replaceAll(activity.getPackageName(), "<font color='#4caf50'>"+activity.getPackageName()+"</font>");
        content = content.replaceAll("\n", "<br>");

        contentPlain += "\n"+Build.MANUFACTURER+"\n"+Build.BRAND+"\n"+Build.MODEL+"\n"+Build.DEVICE+"\n";
        if (app.profile != null && app.profile.getRegistration() == REGISTRATION_ENABLED) {
            contentPlain += "U: "+app.profile.getUsernameId()+"\nS: "+ app.profile.getStudentNameLong() +"\nT: "+app.profile.loginStoreType()+"\n";
        }
        contentPlain += BuildConfig.VERSION_NAME+" "+BuildConfig.BUILD_TYPE+"\nAndroid "+Build.VERSION.RELEASE;

        d(TAG, contentPlain);
        d(TAG, apiResponse == null ? "API Response = null" : apiResponse);


        // show a dialog containing the error details in HTML
        String finalContentPlain = contentPlain;
        new MaterialDialog.Builder(activity)
                .title(R.string.sync_report_dialog_title)
                .content(Html.fromHtml(content))
                .typeface(null, "RobotoMono-Regular.ttf")
                .negativeText(R.string.close)
                .onNegative(((dialog1, which1) -> dialog1.dismiss()))
                .neutralText(R.string.copy_to_clipboard)
                .onNeutral((dialog1, which1) -> {
                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        ClipData clip = ClipData.newPlainText("Error report", finalContentPlain);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(activity, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    }
                })
                .autoDismiss(false)
                .positiveText(R.string.sync_report_dialog_button)
                .checkBoxPromptRes(R.string.sync_report_dialog_include_api_response, true, null)
                .onPositive(((dialog1, which1) -> {
                    // send the error report
                    new ServerRequest(app, app.requestScheme + APP_URL + "main.php?report", "Edziennik/Report")
                            .setBodyParameter("base64_encoded", Base64.encodeToString(finalContentPlain.getBytes(), Base64.DEFAULT))
                            .setBodyParameter("api_response", dialog1.isPromptCheckBoxChecked() ? apiResponse == null ? Base64.encodeToString("NULL XD".getBytes(), Base64.DEFAULT) : Base64.encodeToString(apiResponse.getBytes(), Base64.DEFAULT) : "VW5jaGVja2Vk"/*Unchecked*/)
                            .run((e, result) -> {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    if (result != null)
                                    {
                                        if (result.get("success").getAsBoolean()) {
                                            Toast.makeText(activity, activity.getString(R.string.crash_report_sent), Toast.LENGTH_SHORT).show();
                                            dialog1.getActionButton(DialogAction.POSITIVE).setEnabled(false);
                                            if (parentDialogToDisableNeutral != null)
                                                parentDialogToDisableNeutral.getActionButton(DialogAction.NEUTRAL).setEnabled(false);
                                        }
                                        else {
                                            Toast.makeText(activity, activity.getString(R.string.crash_report_cannot_send) + ": " + result.get("reason").getAsString(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                    else
                                    {
                                        Toast.makeText(activity, activity.getString(R.string.crash_report_cannot_send)+" JsonObject equals null", Toast.LENGTH_LONG).show();
                                    }
                                });
                            });
                }))
                .show();
    }

    /*    _____            __ _ _                                           _
         |  __ \          / _(_) |                                         | |
         | |__) | __ ___ | |_ _| | ___   _ __ ___ _ __ ___   _____   ____ _| |
         |  ___/ '__/ _ \|  _| | |/ _ \ | '__/ _ \ '_ ` _ \ / _ \ \ / / _` | |
         | |   | | | (_) | | | | |  __/ | | |  __/ | | | | | (_) \ V / (_| | |
         |_|   |_|  \___/|_| |_|_|\___| |_|  \___|_| |_| |_|\___/ \_/ \__,_|*/
    public void guiRemoveProfile(MainActivity activity, int profileId, String profileName) {
        new MaterialDialog.Builder(activity)
                .title(R.string.profile_menu_remove_confirm)
                .content(activity.getString(R.string.profile_menu_remove_confirm_text_format, profileName, profileName))
                .positiveText(R.string.remove)
                .negativeText(R.string.cancel)
                .onPositive(((dialog, which) -> {
                    AsyncTask.execute(() -> {
                        removeProfile(profileId);
                        activity.runOnUiThread(() -> {
                            //activity.drawer.loadItem(DRAWER_ITEM_HOME, null, "ProfileRemoving");
                            //activity.recreate(DRAWER_ITEM_HOME);
                            activity.reloadTarget();
                            Toast.makeText(activity, "Profil został usunięty.", Toast.LENGTH_LONG).show();
                        });
                    });
                }))
                .show();
    }
    public void removeProfile(int profileId) {
        Profile profileObject = app.db.profileDao().getByIdNow(profileId);
        if (profileObject == null)
            return;
        app.db.announcementDao().clear(profileId);
        app.db.attendanceDao().clear(profileId);
        app.db.eventDao().clear(profileId);
        app.db.eventTypeDao().clear(profileId);
        app.db.gradeDao().clear(profileId);
        app.db.gradeCategoryDao().clear(profileId);
        app.db.lessonDao().clear(profileId);
        app.db.lessonChangeDao().clear(profileId);
        app.db.luckyNumberDao().clear(profileId);
        app.db.noticeDao().clear(profileId);
        app.db.subjectDao().clear(profileId);
        app.db.teacherDao().clear(profileId);
        app.db.teamDao().clear(profileId);
        app.db.messageRecipientDao().clear(profileId);
        app.db.messageDao().clear(profileId);
        app.db.endpointTimerDao().clear(profileId);
        app.db.attendanceTypeDao().clear(profileId);
        app.db.classroomDao().clear(profileId);
        app.db.lessonRangeDao().clear(profileId);
        app.db.noticeTypeDao().clear(profileId);
        app.db.teacherAbsenceDao().clear(profileId);
        app.db.teacherAbsenceTypeDao().clear(profileId);

        int loginStoreId = profileObject.getLoginStoreId();
        List<Integer> profilesUsingLoginStore = app.db.profileDao().getIdsByLoginStoreIdNow(loginStoreId);
        if (profilesUsingLoginStore.size() == 1) {
            app.db.loginStoreDao().remove(loginStoreId);
        }
        app.db.profileDao().remove(profileId);
        app.db.metadataDao().deleteAll(profileId);

        List<Notification> toRemove = new ArrayList<>();
        for (Notification notification: app.appConfig.notifications) {
            if (notification.profileId == profileId) {
                toRemove.add(notification);
            }
        }
        app.appConfig.notifications.removeAll(toRemove);

        app.profile = null;
        App.profileId = -1;
    }
}
