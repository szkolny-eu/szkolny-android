package pl.szczodrzynski.edziennik.data.api;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.crashlytics.android.Crashlytics;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import im.wangchao.mhttp.Request;
import im.wangchao.mhttp.Response;
import im.wangchao.mhttp.body.MediaTypeUtils;
import im.wangchao.mhttp.callback.JsonArrayCallbackHandler;
import im.wangchao.mhttp.callback.JsonCallbackHandler;
import im.wangchao.mhttp.callback.TextCallbackHandler;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.BuildConfig;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.api.interfaces.AttachmentGetCallback;
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface;
import pl.szczodrzynski.edziennik.data.api.interfaces.LoginCallback;
import pl.szczodrzynski.edziennik.data.api.interfaces.MessageGetCallback;
import pl.szczodrzynski.edziennik.data.api.interfaces.RecipientListGetCallback;
import pl.szczodrzynski.edziennik.data.api.interfaces.SyncCallback;
import pl.szczodrzynski.edziennik.data.db.modules.announcements.Announcement;
import pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance;
import pl.szczodrzynski.edziennik.data.db.modules.events.Event;
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.Lesson;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange;
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore;
import pl.szczodrzynski.edziennik.data.db.modules.luckynumber.LuckyNumber;
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message;
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageFull;
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageRecipient;
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageRecipientFull;
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata;
import pl.szczodrzynski.edziennik.data.db.modules.notices.Notice;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.ProfileFull;
import pl.szczodrzynski.edziennik.data.db.modules.subjects.Subject;
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher;
import pl.szczodrzynski.edziennik.data.db.modules.teams.Team;
import pl.szczodrzynski.edziennik.ui.modules.messages.MessagesComposeInfo;
import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.models.Endpoint;
import pl.szczodrzynski.edziennik.utils.models.Time;
import pl.szczodrzynski.edziennik.utils.models.Week;

import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_INVALID_LOGIN;
import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_INVALID_SCHOOL_NAME;
import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_MAINTENANCE;
import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_OTHER;
import static pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance.TYPE_ABSENT;
import static pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance.TYPE_ABSENT_EXCUSED;
import static pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance.TYPE_BELATED;
import static pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance.TYPE_PRESENT;
import static pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance.TYPE_RELEASED;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_SEMESTER1_FINAL;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_SEMESTER1_PROPOSED;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_YEAR_FINAL;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_YEAR_PROPOSED;
import static pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange.TYPE_CANCELLED;
import static pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange.TYPE_CHANGE;
import static pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_DELETED;
import static pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_RECEIVED;
import static pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_SENT;
import static pl.szczodrzynski.edziennik.data.db.modules.notices.Notice.TYPE_NEGATIVE;
import static pl.szczodrzynski.edziennik.data.db.modules.notices.Notice.TYPE_NEUTRAL;
import static pl.szczodrzynski.edziennik.data.db.modules.notices.Notice.TYPE_POSITIVE;
import static pl.szczodrzynski.edziennik.utils.Utils.crc16;
import static pl.szczodrzynski.edziennik.utils.Utils.crc32;
import static pl.szczodrzynski.edziennik.utils.Utils.d;
import static pl.szczodrzynski.edziennik.utils.Utils.getWordGradeValue;

public class Iuczniowie implements EdziennikInterface {
    public Iuczniowie(App app) {
        this.app = app;
    }

    private static final String TAG = "api.Iuczniowie";
    private static String IDZIENNIK_URL = "https://iuczniowie.progman.pl/idziennik";
    private static final String userAgent = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36";

    private App app;
    private Context activityContext = null;
    private SyncCallback callback = null;
    private int profileId = -1;
    private Profile profile = null;
    private LoginStore loginStore = null;
    private boolean fullSync = true;
    private Date today = Date.getToday();
    private List<String> targetEndpoints = new ArrayList<>();

    // PROGRESS
    private static final int PROGRESS_LOGIN = 10;
    private int PROGRESS_COUNT = 1;
    private int PROGRESS_STEP = (90/PROGRESS_COUNT);

    private int onlyFeature = FEATURE_ALL;

    private List<Team> teamList;
    private List<Teacher> teacherList;
    private List<Subject> subjectList;
    private List<Lesson> lessonList;
    private List<LessonChange> lessonChangeList;
    private List<Grade> gradeList;
    private List<Event> eventList;
    private List<Notice> noticeList;
    private List<Attendance> attendanceList;
    private List<Announcement> announcementList;
    private List<Message> messageList;
    private List<MessageRecipient> messageRecipientList;
    private List<MessageRecipient> messageRecipientIgnoreList;
    private List<Metadata> metadataList;
    private List<Metadata> messageMetadataList;

    private static boolean fakeLogin = false;
    private String lastLogin = "";
    private long lastLoginTime = -1;
    private String lastResponse = null;
    private String loginSchoolName = null;
    private String loginUsername = null;
    private String loginPassword = null;
    private String loginBearerToken = null;
    private int loginRegisterId = -1;
    private int loginSchoolYearId = -1;
    private String loginStudentId = null;
    private int teamClassId = -1;

    private boolean prepare(@NonNull Context activityContext, @NonNull SyncCallback callback, int profileId, @Nullable Profile profile, @NonNull LoginStore loginStore) {
        this.activityContext = activityContext;
        this.callback = callback;
        this.profileId = profileId;
        // here we must have a login store: either with a correct ID or -1
        // there may be no profile and that's when onLoginFirst happens
        this.profile = profile;
        this.loginStore = loginStore;
        this.fullSync = profile == null || profile.getEmpty() || profile.shouldFullSync(activityContext);
        this.today = Date.getToday();

        this.loginSchoolName = loginStore.getLoginData("schoolName", "");
        this.loginUsername = loginStore.getLoginData("username", "");
        this.loginPassword = loginStore.getLoginData("password", "");
        if (loginSchoolName.equals("") || loginUsername.equals("") || loginPassword.equals("")) {
            finishWithError(new AppError(TAG, 162, CODE_INVALID_LOGIN, "Login field is empty"));
            return false;
        }
        fakeLogin = BuildConfig.DEBUG && loginUsername.startsWith("FAKE");
        IDZIENNIK_URL = fakeLogin ? "http://szkolny.eu/idziennik" : "https://iuczniowie.progman.pl/idziennik";

        teamList = profileId == -1 ? new ArrayList<>() : app.db.teamDao().getAllNow(profileId);
        teacherList = profileId == -1 ? new ArrayList<>() : app.db.teacherDao().getAllNow(profileId);
        subjectList = profileId == -1 ? new ArrayList<>() : app.db.subjectDao().getAllNow(profileId);
        lessonList = new ArrayList<>();
        lessonChangeList = new ArrayList<>();
        gradeList = new ArrayList<>();
        eventList = new ArrayList<>();
        noticeList = new ArrayList<>();
        attendanceList = new ArrayList<>();
        announcementList = new ArrayList<>();
        messageList = new ArrayList<>();
        messageRecipientList = new ArrayList<>();
        messageRecipientIgnoreList = new ArrayList<>();
        metadataList = new ArrayList<>();
        messageMetadataList = new ArrayList<>();

        return true;
    }

    @Override
    public void sync(@NonNull Context activityContext, @NonNull SyncCallback callback, int profileId, @Nullable Profile profile, @NonNull LoginStore loginStore) {
        if (!prepare(activityContext, callback, profileId, profile, loginStore))
            return;

        login(() -> {
            targetEndpoints = new ArrayList<>();
            targetEndpoints.add("LuckyNumberAndSemesterDates");
            targetEndpoints.add("Timetable");
            targetEndpoints.add("Grades");
            targetEndpoints.add("PropositionGrades");
            targetEndpoints.add("Exams");
            targetEndpoints.add("Notices");
            targetEndpoints.add("Announcements");
            targetEndpoints.add("Attendance");
            targetEndpoints.add("MessagesInbox");
            targetEndpoints.add("MessagesOutbox");
            targetEndpoints.add("Finish");
            PROGRESS_COUNT = targetEndpoints.size()-1;
            PROGRESS_STEP = (90/PROGRESS_COUNT);
            begin();
        });
    }
    @Override
    public void syncFeature(@NonNull Context activityContext, @NonNull SyncCallback callback, @NonNull ProfileFull profile, int ... featureList) {
        if (featureList == null) {
            sync(activityContext, callback, profile.getId(), profile, LoginStore.fromProfileFull(profile));
            return;
        }
        if (!prepare(activityContext, callback, profile.getId(), profile, LoginStore.fromProfileFull(profile)))
            return;

        login(() -> {
            targetEndpoints = new ArrayList<>();
            if (featureList.length == 1)
                onlyFeature = featureList[0];
            targetEndpoints.add("LuckyNumberAndSemesterDates");
            for (int feature: featureList) {
                switch (feature) {
                    case FEATURE_TIMETABLE:
                        targetEndpoints.add("Timetable");
                        break;
                    case FEATURE_AGENDA:
                        targetEndpoints.add("Exams");
                        break;
                    case FEATURE_GRADES:
                        targetEndpoints.add("Grades");
                        targetEndpoints.add("PropositionGrades");
                        break;
                    case FEATURE_HOMEWORK:
                        targetEndpoints.add("Homework");
                        break;
                    case FEATURE_NOTICES:
                        targetEndpoints.add("Notices");
                        break;
                    case FEATURE_ATTENDANCE:
                        targetEndpoints.add("Attendance");
                        break;
                    case FEATURE_MESSAGES_INBOX:
                        targetEndpoints.add("MessagesInbox");
                        break;
                    case FEATURE_MESSAGES_OUTBOX:
                        targetEndpoints.add("MessagesOutbox");
                        break;
                    case FEATURE_ANNOUNCEMENTS:
                        targetEndpoints.add("Announcements");
                        break;
                }
            }
            targetEndpoints.add("Finish");
            PROGRESS_COUNT = targetEndpoints.size()-1;
            PROGRESS_STEP = (90/PROGRESS_COUNT);
            begin();
        });
    }

    private void begin() {
        List<Cookie> cookieList = app.cookieJar.loadForRequest(HttpUrl.get(IDZIENNIK_URL));
        for (Cookie cookie: cookieList) {
            if (cookie.name().equalsIgnoreCase("Bearer")) {
                loginBearerToken = cookie.value();
            }
        }
        loginStudentId = profile.getStudentData("studentId", null);
        loginSchoolYearId = profile.getStudentData("schoolYearId", -1);
        loginRegisterId = profile.getStudentData("registerId", -1);

        if (loginRegisterId == -1) {
            finishWithError(new AppError(TAG, 212, CODE_OTHER, app.getString(R.string.error_register_id_not_found), "loginRegisterId == -1"));
            return;
        }
        if (loginSchoolYearId == -1) {
            finishWithError(new AppError(TAG, 216, CODE_OTHER, app.getString(R.string.error_school_year_not_found), "loginSchoolYearId == -1"));
            return;
        }
        if (loginStudentId == null) {
            if (lastResponse == null) {
                lastLoginTime = -1;
                lastLogin = "";
                finishWithError(new AppError(TAG, 223, CODE_OTHER, app.getString(R.string.error_student_id_not_found), "loginStudentId == null && lastResponse == null"));
                return;
            }
            Matcher selectMatcher = Pattern.compile("<select.*?name=\"ctl00\\$dxComboUczniowie\".*?</select>", Pattern.DOTALL).matcher(lastResponse);
            if (!selectMatcher.find()) {
                finishWithError(new AppError(TAG, 228, CODE_OTHER, app.getString(R.string.error_register_id_not_found), lastResponse));
                return;
            }
            Matcher idMatcher = Pattern.compile("<option.*?value=\""+loginRegisterId+"\"\\sdata-id-ucznia=\"([A-z0-9]+)\".*?>.*?</option>", Pattern.DOTALL).matcher(selectMatcher.group(0));
            while (idMatcher.find()) {
                loginStudentId = idMatcher.group(1);
                profile.putStudentData("studentId", loginStudentId);
            }
        }

        this.attendanceMonth = today.month;
        this.attendanceYear = today.year;
        this.attendancePrevMonthChecked = false;
        this.examsMonth = today.month;
        this.examsYear = today.year;
        this.examsMonthsChecked = 0;
        this.examsNextMonthChecked = false;

        callback.onProgress(PROGRESS_LOGIN);

        r("get", null);
    }

    private void r(String type, String endpoint) {
        // endpoint == null when beginning
        if (endpoint == null)
            endpoint = targetEndpoints.get(0);
        int index = -1;
        for (String request: targetEndpoints) {
            index++;
            if (request.equals(endpoint)) {
                break;
            }
        }
        if (type.equals("finish")) {
            // called when finishing the action
            callback.onProgress(PROGRESS_STEP);
            index++;
        }
        d(TAG, "Called r("+type+", "+endpoint+"). Getting "+targetEndpoints.get(index));
        switch (targetEndpoints.get(index)) {
            case "LuckyNumberAndSemesterDates":
                getLuckyNumberAndSemesterDates();
                break;
            case "Timetable":
                getTimetable();
                break;
            case "Grades":
                getGrades();
                break;
            case "PropositionGrades":
                getPropositionGrades();
                break;
            case "Exams":
                getExams();
                break;
            case "Notices":
                getNotices();
                break;
            case "Announcements":
                getAnnouncements();
                break;
            case "Attendance":
                getAttendance();
                break;
            case "MessagesInbox":
                getMessagesInbox();
                break;
            case "MessagesOutbox":
                getMessagesOutbox();
                break;
            case "Finish":
                finish();
                break;
        }
    }
    private void saveData() {
        if (teamList.size() > 0) {
            //app.db.teamDao().clear(profileId);
            app.db.teamDao().addAll(teamList);
        }
        if (teacherList.size() > 0)
            app.db.teacherDao().addAll(teacherList);
        if (subjectList.size() > 0)
            app.db.subjectDao().addAll(subjectList);
        if (lessonList.size() > 0) {
            app.db.lessonDao().clear(profileId);
            app.db.lessonDao().addAll(lessonList);
        }
        if (lessonChangeList.size() > 0)
            app.db.lessonChangeDao().addAll(lessonChangeList);
        if (gradeList.size() > 0) {
            app.db.gradeDao().clear(profileId);
            app.db.gradeDao().addAll(gradeList);
        }
        if (eventList.size() > 0) {
            app.db.eventDao().removeFuture(profileId, today);
            app.db.eventDao().addAll(eventList);
        }
        if (noticeList.size() > 0) {
            app.db.noticeDao().clear(profileId);
            app.db.noticeDao().addAll(noticeList);
        }
        if (attendanceList.size() > 0)
            app.db.attendanceDao().addAll(attendanceList);
        if (announcementList.size() > 0)
            app.db.announcementDao().addAll(announcementList);
        if (messageList.size() > 0)
            app.db.messageDao().addAllIgnore(messageList);
        if (messageRecipientList.size() > 0)
            app.db.messageRecipientDao().addAll(messageRecipientList);
        if (messageRecipientIgnoreList.size() > 0)
            app.db.messageRecipientDao().addAllIgnore(messageRecipientIgnoreList);
        if (metadataList.size() > 0)
            app.db.metadataDao().addAllIgnore(metadataList);
        if (messageMetadataList.size() > 0)
            app.db.metadataDao().setSeen(messageMetadataList);
    }
    private void finish() {
        try {
            saveData();
        }
        catch (Exception e) {
            finishWithError(new AppError(TAG, 363, CODE_OTHER, app.getString(R.string.sync_error_saving_data), null, null, e, null));
        }
        if (fullSync) {
            profile.setLastFullSync(System.currentTimeMillis());
            fullSync = false;
        }
        profile.setEmpty(false);
        callback.onSuccess(activityContext, new ProfileFull(profile, loginStore));
    }
    private void finishWithError(AppError error) {
        try {
            saveData();
        }
        catch (Exception e) {
            Crashlytics.logException(e);
        }
        callback.onError(activityContext, error);
    }

    /*    _                 _
         | |               (_)
         | |     ___   __ _ _ _ __
         | |    / _ \ / _` | | '_ \
         | |___| (_) | (_| | | | | |
         |______\___/ \__, |_|_| |_|
                       __/ |
                      |__*/
    private void login(@NonNull LoginCallback loginCallback) {
        if (lastLogin.equals(loginSchoolName +":"+ loginUsername)
                && System.currentTimeMillis() - lastLoginTime < 5 * 60 * 1000
                && profile != null) { // less than 5 minutes, use the already logged in account
            loginCallback.onSuccess();
            return;
        }
        app.cookieJar.clearForDomain("iuczniowie.progman.pl");
        callback.onActionStarted(R.string.sync_action_logging_in);
        Request.builder()
                .url(IDZIENNIK_URL +"/login.aspx")
                .userAgent(userAgent)
                .callback(new TextCallbackHandler() {
                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        finishWithError(new AppError(TAG, 389, CODE_OTHER, response, throwable));
                    }

                    @Override
                    public void onSuccess(String data1, Response response1) {
                        if (data1 == null || data1.equals("")) { // for safety
                            finishWithError(new AppError(TAG, 395, CODE_MAINTENANCE, response1));
                            return;
                        }
                        //Log.d(TAG, "r:"+data);
                        Request.Builder builder = Request.builder()
                                .url(IDZIENNIK_URL +"/login.aspx")
                                .userAgent(userAgent)
                                //.withClient(app.httpLazy)
                                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/png,*/*;q=0.8")
                                .addHeader("Cache-Control", "max-age=0")
                                .addHeader("Origin", "https://iuczniowie.progman.pl")
                                .addHeader("Referer", "https://iuczniowie.progman.pl/idziennik/login.aspx")
                                .addHeader("Upgrade-Insecure-Requests", "1")
                                .contentType(MediaTypeUtils.APPLICATION_FORM)
                                .addParameter("ctl00$ContentPlaceHolder$nazwaPrzegladarki", userAgent)
                                .addParameter("ctl00$ContentPlaceHolder$NazwaSzkoly", loginSchoolName)
                                .addParameter("ctl00$ContentPlaceHolder$UserName", loginUsername)
                                .addParameter("ctl00$ContentPlaceHolder$Password", loginPassword)
                                .addParameter("ctl00$ContentPlaceHolder$captcha", "")
                                .addParameter("ctl00$ContentPlaceHolder$Logowanie", "Zaloguj")
                                .post();

                        // extract hidden form fields __VIEWSTATE __VIEWSTATEGENERATOR __EVENTVALIDATION
                        //Pattern pattern = Pattern.compile("<.+?name=\"__VIEWSTATE\".+?value=\"([A-z0-9+/=]+)\".+?name=\"__VIEWSTATEGENERATOR\".+?value=\"([A-z0-9+/=]+)\".+?name=\"__EVENTVALIDATION\".+?value=\"([A-z0-9+/=]+)\".+?>", Pattern.DOTALL);
                        //Pattern pattern = Pattern.compile("<input type=\"hidden\"(?=[^>]* name=[\"']([^'\"]*)|)(?=[^>]* value=[\"']([^'\"]*)|)", Pattern.DOTALL);
                        Pattern pattern = Pattern.compile("<input type=\"hidden\".+?name=\"([A-z0-9_]+)?\".+?value=\"([A-z0-9_+-/=]+)?\".+?>", Pattern.DOTALL);
                        Matcher matcher = pattern.matcher(data1);
                        while (matcher.find()) {
                            //Log.d(TAG, "Match: "+matcher.group(1)+"="+matcher.group(2));
                            builder.addParameter(matcher.group(1), matcher.group(2));
                        }

                        builder.callback(new TextCallbackHandler() {
                            @Override
                            public void onSuccess(String data2, Response response2) {
                                callback.onProgress(PROGRESS_LOGIN);
                                Pattern errorPattern = Pattern.compile("id=\"spanErrorMessage\">(.*?)</", Pattern.DOTALL);
                                Matcher errorMatcher = errorPattern.matcher(data2);
                                if (errorMatcher.find()) {
                                    String error = errorMatcher.group(1);
                                    d(TAG, errorMatcher.group(0));
                                    if (error.equals("")) {
                                        finishWithError(new AppError(TAG, 443, CODE_MAINTENANCE, error, response2, data2));
                                        return;
                                    }
                                    if (error.contains("nieprawidłową nazwę szkoły")) {
                                        finishWithError(new AppError(TAG, 447, CODE_INVALID_SCHOOL_NAME, error, response2, data2));
                                        return;
                                    }
                                    if (error.contains("nieprawidłowy login lub hasło")) {
                                        finishWithError(new AppError(TAG, 451, AppError.CODE_INVALID_LOGIN, error, response2, data2));
                                        return;
                                    }
                                    finishWithError(new AppError(TAG, 454, CODE_OTHER, error, response2, data2));
                                    return;
                                }

                                // a successful login

                                lastLogin = loginSchoolName +":"+ loginUsername;
                                lastLoginTime = System.currentTimeMillis();
                                lastResponse = data2;

                                // HERE we decide if it's the first login
                                // if it is, let's search for students and return them in onLoginFirst
                                // else, let's continue syncing having the profile object
                                if (profile != null) {
                                    loginRegisterId = profile.getStudentData("registerId", -1);
                                    loginSchoolYearId = profile.getStudentData("schoolYearId", -1);
                                    loginStudentId = profile.getStudentData("studentId", null);
                                    Matcher selectMatcher = Pattern.compile("<select.*?name=\"ctl00\\$dxComboUczniowie\".*?</select>", Pattern.DOTALL).matcher(data2);
                                    if (!selectMatcher.find()) {
                                        finishWithError(new AppError(TAG, 473, CODE_OTHER, app.getString(R.string.error_register_id_not_found), response2, data2));
                                        return;
                                    }
                                    Log.d(TAG, "g" + selectMatcher.group(0));
                                    Matcher idMatcher = Pattern.compile("<option.*?value=\"([0-9]+)\".+?>(.+?)\\s(.+?)\\s*\\((.+?),\\s*(.+?)\\)</option>", Pattern.DOTALL).matcher(selectMatcher.group(0));
                                    while (idMatcher.find()) {
                                        if (loginRegisterId != Integer.parseInt(idMatcher.group(1)))
                                            continue;
                                        String teamClassName = idMatcher.group(4) + " " + idMatcher.group(5);
                                        teamClassId = crc16(teamClassName.getBytes());
                                        app.db.teamDao().add(new Team(
                                                profileId,
                                                teamClassId,
                                                teamClassName,
                                                1,
                                                loginSchoolName+":"+teamClassName,
                                                -1
                                        ));
                                    }
                                    loginCallback.onSuccess();
                                    return;
                                }
                                try {
                                    Matcher yearMatcher = Pattern.compile("name=\"ctl00\\$dxComboRokSzkolny\".+?selected=\"selected\".*?value=\"([0-9]+)\"", Pattern.DOTALL).matcher(data2);
                                    if (yearMatcher.find()) {
                                        try {
                                            loginSchoolYearId = Integer.parseInt(yearMatcher.group(1));
                                        } catch (Exception ex) {
                                            finishWithError(new AppError(TAG, 501, CODE_OTHER, response2, ex, data2));
                                            return;
                                        }
                                    } else {
                                        if (data2.contains("Hasło dostępu do systemu wygasło")) {
                                            finishWithError(new AppError(TAG, 504, CODE_OTHER, app.getString(R.string.error_must_change_password), response2, data2));
                                            return;
                                        }
                                        finishWithError(new AppError(TAG, 507, CODE_OTHER, app.getString(R.string.error_school_year_not_found), response2, data2));
                                        return;
                                    }

                                    List<String> studentIds = new ArrayList<>();
                                    List<Integer> registerIds = new ArrayList<>();
                                    List<String> studentNamesLong = new ArrayList<>();
                                    List<String> studentNamesShort = new ArrayList<>();
                                    List<String> studentTeams = new ArrayList<>();

                                    Matcher selectMatcher = Pattern.compile("<select.*?name=\"ctl00\\$dxComboUczniowie\".*?</select>", Pattern.DOTALL).matcher(data2);
                                    if (!selectMatcher.find()) {
                                        finishWithError(new AppError(TAG, 519, CODE_OTHER, app.getString(R.string.error_register_id_not_found), response2, data2));
                                        return;
                                    }
                                    Log.d(TAG, "g" + selectMatcher.group(0));
                                    Matcher idMatcher = Pattern.compile("<option.*?value=\"([0-9]+)\"\\sdata-id-ucznia=\"([A-z0-9]+?)\".*?>(.+?)\\s(.+?)\\s*\\((.+?),\\s*(.+?)\\)</option>", Pattern.DOTALL).matcher(selectMatcher.group(0));
                                    while (idMatcher.find()) {
                                        registerIds.add(Integer.parseInt(idMatcher.group(1)));
                                        String studentId = idMatcher.group(2);
                                        String studentFirstName = idMatcher.group(3);
                                        String studentLastName = idMatcher.group(4);
                                        String teamClassName = idMatcher.group(5) + " " + idMatcher.group(6);
                                        studentIds.add(studentId);
                                        studentNamesLong.add(studentFirstName + " " + studentLastName);
                                        studentNamesShort.add(studentFirstName + " " + studentLastName.charAt(0) + ".");
                                        studentTeams.add(teamClassName);
                                    }
                                    Collections.reverse(studentIds);
                                    Collections.reverse(registerIds);
                                    Collections.reverse(studentNamesLong);
                                    Collections.reverse(studentNamesShort);
                                    Collections.reverse(studentTeams);

                                    List<Profile> profileList = new ArrayList<>();
                                    for (int index = 0; index < registerIds.size(); index++) {
                                        Profile newProfile = new Profile();
                                        newProfile.setStudentNameLong(studentNamesLong.get(index));
                                        newProfile.setStudentNameShort(studentNamesShort.get(index));
                                        newProfile.setName(newProfile.getStudentNameLong());
                                        newProfile.setSubname(loginUsername);
                                        newProfile.setEmpty(true);
                                        newProfile.putStudentData("studentId", studentIds.get(index));
                                        newProfile.putStudentData("registerId", registerIds.get(index));
                                        newProfile.putStudentData("schoolYearId", loginSchoolYearId);
                                        profileList.add(newProfile);
                                    }

                                    callback.onLoginFirst(profileList, loginStore);
                                } catch (Exception ex) {
                                    finishWithError(new AppError(TAG, 557, CODE_OTHER, response2, ex, data2));
                                }
                            }

                            @Override
                            public void onFailure(Response response, Throwable throwable) {
                                finishWithError(new AppError(TAG, 563, CODE_OTHER, response, throwable, data1));
                            }
                        }).build().enqueue();
                    }
                }).build().enqueue();
    }

    /*    _    _      _                                          _ _ _                _
         | |  | |    | |                       ___              | | | |              | |
         | |__| | ___| |_ __   ___ _ __ ___   ( _ )     ___ __ _| | | |__   __ _  ___| | _____
         |  __  |/ _ \ | '_ \ / _ \ '__/ __|  / _ \/\  / __/ _` | | | '_ \ / _` |/ __| |/ / __|
         | |  | |  __/ | |_) |  __/ |  \__ \ | (_>  < | (_| (_| | | | |_) | (_| | (__|   <\__ \
         |_|  |_|\___|_| .__/ \___|_|  |___/  \___/\/  \___\__,_|_|_|_.__/ \__,_|\___|_|\_\___/
                       | |
                       |*/
    private interface ApiRequestCallback {
        void onSuccess(JsonObject result, Response response);
    }
    private void apiRequest(Request.Builder requestBuilder, ApiRequestCallback apiRequestCallback) {
        requestBuilder.callback(new JsonCallbackHandler() {
            @Override
            public void onSuccess(JsonObject data, Response response) {
                if (data == null) {
                    finishWithError(new AppError(TAG, 578, CODE_MAINTENANCE, response));
                    return;
                }
                try {
                    apiRequestCallback.onSuccess(data, response);
                }
                catch (Exception e) {
                    finishWithError(new AppError(TAG, 583, CODE_OTHER, response, e, data));
                }
            }

            @Override
            public void onFailure(Response response, Throwable throwable) {
                finishWithError(new AppError(TAG, 592, CODE_OTHER, response, throwable));
            }
        })
        .build()
        .enqueue();
    }
    private interface ApiRequestArrayCallback {
        void onSuccess(JsonArray result, Response response);
    }
    private void apiRequestArray(Request.Builder requestBuilder, ApiRequestArrayCallback apiRequestArrayCallback) {
        requestBuilder.callback(new JsonArrayCallbackHandler() {
            @Override
            public void onSuccess(JsonArray data, Response response) {
                if (data == null) {
                    finishWithError(new AppError(TAG, 603, CODE_MAINTENANCE, response));
                    return;
                }
                try {
                    apiRequestArrayCallback.onSuccess(data, response);
                }
                catch (Exception e) {
                    finishWithError(new AppError(TAG, 610, CODE_OTHER, response, e, data.toString()));
                }
            }

            @Override
            public void onFailure(Response response, Throwable throwable) {
                finishWithError(new AppError(TAG, 616, CODE_OTHER, response, throwable));
            }
        })
                .build()
                .enqueue();
    }

    private Subject searchSubject(String name, long id, String shortName) {
        Subject subject;
        if (id == -1)
            subject = Subject.getByName(subjectList, name);
        else
            subject = Subject.getById(subjectList, id);

        if (subject == null) {
            subject = new Subject(profileId, (id == -1 ? crc16(name.getBytes()) : id), name, shortName);
            subjectList.add(subject);
        }
        return subject;
    }

    private Teacher searchTeacher(String firstName, String lastName) {
        Teacher teacher = Teacher.getByFullName(teacherList, firstName+" "+lastName);
        return validateTeacher(teacher, firstName, lastName);
    }
    private Teacher searchTeacher(char firstNameChar, String lastName) {
        Teacher teacher = Teacher.getByShortName(teacherList, firstNameChar+"."+lastName);
        return validateTeacher(teacher, String.valueOf(firstNameChar), lastName);
    }
    @NonNull
    private Teacher validateTeacher(Teacher teacher, String firstName, String lastName) {
        if (teacher == null) {
            teacher = new Teacher(profileId, -1, firstName, lastName);
            teacher.id = crc16(teacher.getShortName().getBytes());
            teacherList.add(teacher);
        }
        if (firstName.length() > 1)
            teacher.name = firstName;
        teacher.surname = lastName;
        return teacher;
    }

    private Teacher searchTeacherByLastFirst(String nameLastFirst) {
        String[] nameParts = nameLastFirst.split(" ", Integer.MAX_VALUE);
        if (nameParts.length == 1)
            return searchTeacher(nameParts[0], "");
        return searchTeacher(nameParts[1], nameParts[0]);
    }
    private Teacher searchTeacherByFirstLast(String nameFirstLast) {
        String[] nameParts = nameFirstLast.split(" ", Integer.MAX_VALUE);
        if (nameParts.length == 1)
            return searchTeacher(nameParts[0], "");
        return searchTeacher(nameParts[0], nameParts[1]);
    }
    private Teacher searchTeacherByFDotLast(String nameFDotLast) {
        String[] nameParts = nameFDotLast.split("\\.", Integer.MAX_VALUE);
        if (nameParts.length == 1)
            return searchTeacher(nameParts[0], "");
        return searchTeacher(nameParts[0].charAt(0), nameParts[1]);
    }
    private Teacher searchTeacherByFDotSpaceLast(String nameFDotSpaceLast) {
        String[] nameParts = nameFDotSpaceLast.split("\\. ", Integer.MAX_VALUE);
        if (nameParts.length == 1)
            return searchTeacher(nameParts[0], "");
        return searchTeacher(nameParts[0].charAt(0), nameParts[1]);
    }

    /*    _____        _          _____                            _
         |  __ \      | |        |  __ \                          | |
         | |  | | __ _| |_ __ _  | |__) |___  __ _ _   _  ___  ___| |_ ___
         | |  | |/ _` | __/ _` | |  _  // _ \/ _` | | | |/ _ \/ __| __/ __|
         | |__| | (_| | || (_| | | | \ \  __/ (_| | |_| |  __/\__ \ |_\__ \
         |_____/ \__,_|\__\__,_| |_|  \_\___|\__, |\__,_|\___||___/\__|___/
                                                | |
                                                |*/
    private void getTimetable() {
        callback.onActionStarted(R.string.sync_action_syncing_timetable);
        Date weekStart = Week.getWeekStart();
        if (Date.getToday().getWeekDay() > 4) {
            weekStart.stepForward(0, 0, 7);
        }
        apiRequest(Request.builder()
                .url(IDZIENNIK_URL +"/mod_panelRodzica/plan/WS_Plan.asmx/pobierzPlanZajec")
                .userAgent(userAgent)
                .addParameter("idPozDziennika", loginRegisterId)
                .addParameter("pidRokSzkolny", loginSchoolYearId)
                .addParameter("data", weekStart.getStringY_m_d()+"T10:00:00.000Z")
                .postJson(), (result, response) -> {
            JsonObject data = result.getAsJsonObject("d");
            if (data == null) {
                finishWithError(new AppError(TAG, 697, CODE_MAINTENANCE, response, result));
                return;
            }
            List<Pair<Time, Time>> lessonHours = new ArrayList<>();
            for (JsonElement jLessonHourEl : data.getAsJsonArray("GodzinyLekcyjne")) {
                JsonObject jLessonHour = jLessonHourEl.getAsJsonObject();
                // jLessonHour
                lessonHours.add(new Pair<>(Time.fromH_m(jLessonHour.get("Poczatek").getAsString()), Time.fromH_m(jLessonHour.get("Koniec").getAsString())));
            }

            for (JsonElement jLessonEl : data.getAsJsonArray("Przedmioty")) {
                JsonObject jLesson = jLessonEl.getAsJsonObject();
                // jLesson
                Subject rSubject = searchSubject(jLesson.get("Nazwa").getAsString(), jLesson.get("Id").getAsInt(), jLesson.get("Skrot").getAsString());
                Teacher rTeacher = searchTeacherByFDotLast(jLesson.get("Nauczyciel").getAsString());

                int weekDay = jLesson.get("DzienTygodnia").getAsInt() - 1;
                Pair<Time, Time> lessonHour = lessonHours.get(jLesson.get("Godzina").getAsInt());
                if (lessonHour == null || lessonHour.first == null || lessonHour.second == null)
                    continue;
                Lesson lessonObject = new Lesson(
                        profileId,
                        weekDay,
                        lessonHour.first,
                        lessonHour.second
                );
                lessonObject.subjectId = rSubject.id;
                lessonObject.teacherId = rTeacher.id;
                lessonObject.teamId = teamClassId;
                lessonObject.classroomName = jLesson.get("NazwaSali").getAsString();

                lessonList.add(lessonObject);

                int type = jLesson.get("TypZastepstwa").getAsInt();
                if (type != -1) {
                    // we have a lesson change to process
                    LessonChange lessonChangeObject = new LessonChange(
                            profileId,
                            weekStart.clone().stepForward(0, 0, weekDay),
                            lessonObject.startTime,
                            lessonObject.endTime
                    );

                    lessonChangeObject.teamId = lessonObject.teamId;
                    lessonChangeObject.teacherId = lessonObject.teacherId;
                    lessonChangeObject.subjectId = lessonObject.subjectId;
                    lessonChangeObject.classroomName = lessonObject.classroomName;
                    switch (type) {
                        case 0:
                            lessonChangeObject.type = TYPE_CANCELLED;
                            break;
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                            lessonChangeObject.type = TYPE_CHANGE;
                            String newTeacher = jLesson.get("NauZastepujacy").getAsString();
                            String newSubject = jLesson.get("PrzedmiotZastepujacy").getAsString();
                            if (!newTeacher.equals("")) {
                                lessonChangeObject.teacherId = searchTeacherByFDotLast(newTeacher).id;
                            }
                            if (!newSubject.equals("")) {
                                lessonChangeObject.subjectId = searchSubject(newSubject, -1, "").id;
                            }
                            break;
                    }

                    lessonChangeList.add(lessonChangeObject);
                    metadataList.add(new Metadata(profileId, Metadata.TYPE_LESSON_CHANGE, lessonChangeObject.id, profile.getEmpty(), profile.getEmpty(), System.currentTimeMillis()));
                }
            }
            r("finish", "Timetable");
        });
    }

    private void getGrades() {
        callback.onActionStarted(R.string.sync_action_syncing_grades);
        apiRequest(Request.builder()
                .url(IDZIENNIK_URL +"/mod_panelRodzica/oceny/WS_ocenyUcznia.asmx/pobierzOcenyUcznia")
                .userAgent(userAgent)
                .addParameter("idPozDziennika", loginRegisterId)
                .postJson(), (result, response) -> {
            JsonObject data = result.getAsJsonObject("d");
            if (data == null) {
                finishWithError(new AppError(TAG, 782, CODE_MAINTENANCE, response, result));
                return;
            }
            JsonArray jSubjects = data.getAsJsonArray("Przedmioty");
            for (JsonElement jSubjectEl : jSubjects) {
                JsonObject jSubject = jSubjectEl.getAsJsonObject();
                // jSubject
                Subject rSubject = searchSubject(jSubject.get("Przedmiot").getAsString(), jSubject.get("IdPrzedmiotu").getAsInt(), jSubject.get("Przedmiot").getAsString());
                for (JsonElement jGradeEl : jSubject.getAsJsonArray("Oceny")) {
                    JsonObject jGrade = jGradeEl.getAsJsonObject();
                    // jGrade
                    Teacher rTeacher = searchTeacherByLastFirst(jGrade.get("Wystawil").getAsString());

                    boolean countToTheAverage = jGrade.get("DoSredniej").getAsBoolean();
                    float value = jGrade.get("WartoscDoSred").getAsFloat();

                    String gradeColor = jGrade.get("Kolor").getAsString();
                    int colorInt = 0xff2196f3;
                    if (!gradeColor.isEmpty()) {
                        colorInt = Color.parseColor("#"+gradeColor);
                    }

                    Grade gradeObject = new Grade(
                            profileId,
                            jGrade.get("idK").getAsLong(),
                            jGrade.get("Kategoria").getAsString(),
                            colorInt,
                            "",
                            jGrade.get("Ocena").getAsString(),
                            value,
                            value > 0 && countToTheAverage ? jGrade.get("Waga").getAsFloat() : 0,
                            jGrade.get("Semestr").getAsInt(),
                            rTeacher.id,
                            rSubject.id);

                    switch (jGrade.get("Typ").getAsInt()) {
                        case 0:
                            JsonElement historyEl = jGrade.get("Historia");
                            JsonArray history;
                            if (historyEl instanceof JsonArray && (history = historyEl.getAsJsonArray()).size() > 0) {
                                float sum = gradeObject.value * gradeObject.weight;
                                float count = gradeObject.weight;
                                for (JsonElement historyItemEl: history) {
                                    JsonObject historyItem = historyItemEl.getAsJsonObject();

                                    countToTheAverage = historyItem.get("DoSredniej").getAsBoolean();
                                    value = historyItem.get("WartoscDoSred").getAsFloat();
                                    float weight = historyItem.get("Waga").getAsFloat();

                                    if (value > 0 && countToTheAverage) {
                                        sum += value * weight;
                                        count += weight;
                                    }

                                    Grade historyObject = new Grade(
                                            profileId,
                                            gradeObject.id * -1,
                                            historyItem.get("Kategoria").getAsString(),
                                            Color.parseColor("#"+historyItem.get("Kolor").getAsString()),
                                            historyItem.get("Uzasadnienie").getAsString(),
                                            historyItem.get("Ocena").getAsString(),
                                            value,
                                            value > 0 && countToTheAverage ? weight * -1 : 0,
                                            historyItem.get("Semestr").getAsInt(),
                                            rTeacher.id,
                                            rSubject.id);
                                    historyObject.parentId = gradeObject.id;

                                    gradeList.add(historyObject);
                                    metadataList.add(new Metadata(profileId, Metadata.TYPE_GRADE, historyObject.id, true, true, Date.fromY_m_d(historyItem.get("Data_wystaw").getAsString()).getInMillis()));
                                }
                                // update the current grade's value with an average of all historical grades and itself
                                if (sum > 0 && count > 0) {
                                    gradeObject.value = sum / count;
                                }
                                gradeObject.isImprovement = true; // gradeObject is the improved grade. Originals are historyObjects
                            }
                            break;
                        case 1:
                            gradeObject.type = TYPE_SEMESTER1_FINAL;
                            gradeObject.name = Integer.toString((int) gradeObject.value);
                            gradeObject.weight = 0;
                            break;
                        case 2:
                            gradeObject.type = TYPE_YEAR_FINAL;
                            gradeObject.name = Integer.toString((int) gradeObject.value);
                            gradeObject.weight = 0;
                            break;
                    }

                    gradeList.add(gradeObject);
                    metadataList.add(new Metadata(profileId, Metadata.TYPE_GRADE, gradeObject.id, profile.getEmpty(), profile.getEmpty(), Date.fromY_m_d(jGrade.get("Data_wystaw").getAsString()).getInMillis()));
                }
            }
            r("finish", "Grades");
        });
    }

    private void getPropositionGrades() {
        callback.onActionStarted(R.string.sync_action_syncing_proposition_grades);
        apiRequest(Request.builder()
                .url(IDZIENNIK_URL +"/mod_panelRodzica/brak_ocen/WS_BrakOcenUcznia.asmx/pobierzBrakujaceOcenyUcznia")
                .userAgent(userAgent)
                .addParameter("idPozDziennika", loginRegisterId)
                .postJson(), (result, response) -> {
            JsonObject data = result.getAsJsonObject("d");
            if (data == null) {
                finishWithError(new AppError(TAG, 836, CODE_MAINTENANCE, response, result));
                return;
            }
            JsonArray jSubjects = data.getAsJsonArray("Przedmioty");
            for (JsonElement jSubjectEl : jSubjects) {
                JsonObject jSubject = jSubjectEl.getAsJsonObject();
                // jSubject
                Subject rSubject = searchSubject(jSubject.get("Przedmiot").getAsString(), -1, jSubject.get("Przedmiot").getAsString());
                String semester1Proposed = jSubject.get("OcenaSem1").getAsString();
                String semester2Proposed = jSubject.get("OcenaSem2").getAsString();
                int semester1Value = getWordGradeValue(semester1Proposed);
                int semester2Value = getWordGradeValue(semester2Proposed);
                long semester1Id = rSubject.id * -100 - 1;
                long semester2Id = rSubject.id * -100 - 2;

                if (!semester1Proposed.equals("")) {
                    Grade gradeObject = new Grade(
                            profileId,
                            semester1Id,
                            "",
                            -1,
                            "",
                            Integer.toString(semester1Value),
                            semester1Value,
                            0,
                            1,
                            -1,
                            rSubject.id);

                    gradeObject.type = TYPE_SEMESTER1_PROPOSED;

                    gradeList.add(gradeObject);
                    metadataList.add(new Metadata(profileId, Metadata.TYPE_GRADE, gradeObject.id, profile.getEmpty(), profile.getEmpty(), System.currentTimeMillis()));
                }

                if (!semester2Proposed.equals("")) {
                    Grade gradeObject = new Grade(
                            profileId,
                            semester2Id,
                            "",
                            -1,
                            "",
                            Integer.toString(semester2Value),
                            semester2Value,
                            0,
                            2,
                            -1,
                            rSubject.id);

                    gradeObject.type = TYPE_YEAR_PROPOSED;

                    gradeList.add(gradeObject);
                    metadataList.add(new Metadata(profileId, Metadata.TYPE_GRADE, gradeObject.id, profile.getEmpty(), profile.getEmpty(), System.currentTimeMillis()));
                }
            }
            r("finish", "PropositionGrades");
        });
    }

    private int examsYear = Date.getToday().year;
    private int examsMonth = Date.getToday().month;
    private int examsMonthsChecked = 0;
    private boolean examsNextMonthChecked = false; // TO DO temporary // no more // idk
    private void getExams() {
        callback.onActionStarted(R.string.sync_action_syncing_exams);
        JsonObject postData = new JsonObject();
        postData.addProperty("idP", loginRegisterId);
        postData.addProperty("rok", examsYear);
        postData.addProperty("miesiac", examsMonth);
        JsonObject param = new JsonObject();
        param.addProperty("strona", 1);
        param.addProperty("iloscNaStrone", "99");
        param.addProperty("iloscRekordow", -1);
        param.addProperty("kolumnaSort", "ss.Nazwa,sp.Data_sprawdzianu");
        param.addProperty("kierunekSort", 0);
        param.addProperty("maxIloscZaznaczonych", 0);
        param.addProperty("panelFiltrow", 0);
        postData.add("param", param);

        apiRequest(Request.builder()
                .url(IDZIENNIK_URL +"/mod_panelRodzica/sprawdziany/mod_sprawdzianyPanel.asmx/pobierzListe")
                .userAgent(userAgent)
                .setJsonBody(postData), (result, response) -> {
            JsonObject data = result.getAsJsonObject("d");
            if (data == null) {
                finishWithError(new AppError(TAG, 921, CODE_MAINTENANCE, response, result));
                return;
            }
            for (JsonElement jExamEl : data.getAsJsonArray("ListK")) {
                JsonObject jExam = jExamEl.getAsJsonObject();
                // jExam
                long eventId = jExam.get("_recordId").getAsLong();
                Subject rSubject = searchSubject(jExam.get("przedmiot").getAsString(), -1, "");
                Teacher rTeacher = searchTeacherByLastFirst(jExam.get("wpisal").getAsString());
                Date examDate = Date.fromY_m_d(jExam.get("data").getAsString());
                Lesson lessonObject = Lesson.getByWeekDayAndSubject(lessonList, examDate.getWeekDay(), rSubject.id);
                Time examTime = lessonObject == null ? null : lessonObject.startTime;

                int eventType = (jExam.get("rodzaj").getAsString().equals("sprawdzian/praca klasowa") ? Event.TYPE_EXAM : Event.TYPE_SHORT_QUIZ);
                Event eventObject = new Event(
                        profileId,
                        eventId,
                        examDate,
                        examTime,
                        jExam.get("zakres").getAsString(),
                        -1,
                        eventType,
                        false,
                        rTeacher.id,
                        rSubject.id,
                        teamClassId
                );

                eventList.add(eventObject);
                metadataList.add(new Metadata(profileId, Metadata.TYPE_EVENT, eventObject.id, profile.getEmpty(), profile.getEmpty(), System.currentTimeMillis()));
            }

            if (profile.getEmpty() && examsMonthsChecked < 3 /* how many months backwards to check? */) {
                examsMonthsChecked++;
                examsMonth--;
                if (examsMonth < 1) {
                    examsMonth = 12;
                    examsYear--;
                }
                r("get", "Exams");
            } else if (!examsNextMonthChecked /* get also one month forward */) {
                Date showDate = Date.getToday().stepForward(0, 1, 0);
                examsYear = showDate.year;
                examsMonth = showDate.month;
                examsNextMonthChecked = true;
                r("get", "Exams");
            } else {
                r("finish", "Exams");
            }
        });
    }

    private void getNotices() {
        callback.onActionStarted(R.string.sync_action_syncing_notices);
        apiRequest(Request.builder()
                .url(IDZIENNIK_URL +"/mod_panelRodzica/uwagi/WS_uwagiUcznia.asmx/pobierzUwagiUcznia")
                .userAgent(userAgent)
                .addParameter("idPozDziennika", loginRegisterId)
                .postJson(), (result, response) -> {
            JsonObject data = result.getAsJsonObject("d");
            if (data == null) {
                finishWithError(new AppError(TAG, 982, CODE_MAINTENANCE, response, result));
                return;
            }
            for (JsonElement jNoticeEl : data.getAsJsonArray("SUwaga")) {
                JsonObject jNotice = jNoticeEl.getAsJsonObject();
                // jExam
                long noticeId = crc16(jNotice.get("id").getAsString().getBytes());

                Teacher rTeacher = searchTeacherByLastFirst(jNotice.get("Nauczyciel").getAsString());
                Date addedDate = Date.fromY_m_d(jNotice.get("Data").getAsString());

                int nType = TYPE_NEUTRAL;
                String jType = jNotice.get("Typ").getAsString();
                if (jType.equals("n")) {
                    nType = TYPE_NEGATIVE;
                } else if (jType.equals("p")) {
                    nType = TYPE_POSITIVE;
                }

                Notice noticeObject = new Notice(
                        profileId,
                        noticeId,
                        jNotice.get("Tresc").getAsString(),
                        jNotice.get("Semestr").getAsInt(),
                        nType,
                        rTeacher.id);
                noticeList.add(noticeObject);
                metadataList.add(new Metadata(profileId, Metadata.TYPE_NOTICE, noticeObject.id, profile.getEmpty(), profile.getEmpty(), addedDate.getInMillis()));
            }
            r("finish", "Notices");
        });
    }

    private void getAnnouncements() {
        callback.onActionStarted(R.string.sync_action_syncing_announcements);
        if (loginStudentId == null) {
            r("finish", "Announcements");
            return;
        }
        JsonObject postData = new JsonObject();
        postData.addProperty("uczenId", loginStudentId);
        JsonObject param = new JsonObject();
        param.add("parametryFiltrow", new JsonArray());
        postData.add("param", param);

        apiRequest(Request.builder()
                .url(IDZIENNIK_URL +"/mod_panelRodzica/tabOgl/WS_tablicaOgloszen.asmx/GetOgloszenia")
                .userAgent(userAgent)
                .setJsonBody(postData), (result, response) -> {
            JsonObject data = result.getAsJsonObject("d");
            if (data == null) {
                finishWithError(new AppError(TAG, 1033, CODE_MAINTENANCE, response, result));
                return;
            }
            for (JsonElement jAnnouncementEl : data.getAsJsonArray("ListK")) {
                JsonObject jAnnouncement = jAnnouncementEl.getAsJsonObject();
                // jAnnouncement
                long announcementId = jAnnouncement.get("Id").getAsLong();

                Teacher rTeacher = searchTeacherByFirstLast(jAnnouncement.get("Autor").getAsString());
                long addedDate = Long.parseLong(jAnnouncement.get("DataDodania").getAsString().replaceAll("[^\\d]", ""));
                Date startDate = Date.fromMillis(Long.parseLong(jAnnouncement.get("DataWydarzenia").getAsString().replaceAll("[^\\d]", "")));

                Announcement announcementObject = new Announcement(
                        profileId,
                        announcementId,
                        jAnnouncement.get("Temat").getAsString(),
                        jAnnouncement.get("Tresc").getAsString(),
                        startDate,
                        null,
                        rTeacher.id
                );
                announcementList.add(announcementObject);
                metadataList.add(new Metadata(profileId, Metadata.TYPE_ANNOUNCEMENT, announcementObject.id, profile.getEmpty(), profile.getEmpty(), addedDate));
            }
            r("finish", "Announcements");
        });
    }

    private int attendanceYear;
    private int attendanceMonth;
    private boolean attendancePrevMonthChecked = false;
    private void getAttendance() {
        callback.onActionStarted(R.string.sync_action_syncing_attendance);
        apiRequest(Request.builder()
                .url(IDZIENNIK_URL +"/mod_panelRodzica/obecnosci/WS_obecnosciUcznia.asmx/pobierzObecnosciUcznia")
                .userAgent(userAgent)
                .addParameter("idPozDziennika", loginRegisterId)
                .addParameter("mc", attendanceMonth)
                .addParameter("rok", attendanceYear)
                .addParameter("dataTygodnia", "")
                .postJson(), (result, response) -> {
            JsonObject data = result.getAsJsonObject("d");
            if (data == null) {
                finishWithError(new AppError(TAG, 1076, CODE_MAINTENANCE, response, result));
                return;
            }
            for (JsonElement jAttendanceEl : data.getAsJsonArray("Obecnosci")) {
                JsonObject jAttendance = jAttendanceEl.getAsJsonObject();
                // jAttendance
                int attendanceTypeIdziennik = jAttendance.get("TypObecnosci").getAsInt();
                if (attendanceTypeIdziennik == 5 || attendanceTypeIdziennik == 7)
                    continue;
                Date attendanceDate = Date.fromY_m_d(jAttendance.get("Data").getAsString());
                Time attendanceTime = Time.fromH_m(jAttendance.get("OdDoGodziny").getAsString());
                if (attendanceDate.combineWith(attendanceTime) > System.currentTimeMillis())
                    continue;

                long attendanceId = crc16(jAttendance.get("IdLesson").getAsString().getBytes());
                Subject rSubject = searchSubject(jAttendance.get("Przedmiot").getAsString(), jAttendance.get("IdPrzedmiot").getAsLong(), "");
                Teacher rTeacher = searchTeacherByFDotSpaceLast(jAttendance.get("PrzedmiotNauczyciel").getAsString());

                String attendanceName = "obecność";
                int attendanceType = Attendance.TYPE_CUSTOM;

                switch (attendanceTypeIdziennik) {
                    case 1: /* nieobecność usprawiedliwiona */
                        attendanceName = "nieobecność usprawiedliwiona";
                        attendanceType = TYPE_ABSENT_EXCUSED;
                        break;
                    case 2: /* spóźnienie */
                        attendanceName = "spóźnienie";
                        attendanceType = TYPE_BELATED;
                        break;
                    case 3: /* nieobecność nieusprawiedliwiona */
                        attendanceName = "nieobecność nieusprawiedliwiona";
                        attendanceType = TYPE_ABSENT;
                        break;
                    case 4: /* zwolnienie */
                    case 9: /* zwolniony / obecny */
                        attendanceType = TYPE_RELEASED;
                        if (attendanceTypeIdziennik == 4)
                            attendanceName = "zwolnienie";
                        if (attendanceTypeIdziennik == 9)
                            attendanceName = "zwolnienie / obecność";
                        break;
                    case 0: /* obecny */
                    case 8: /* Wycieczka */
                        attendanceType = TYPE_PRESENT;
                        if (attendanceTypeIdziennik == 8)
                            attendanceName = "wycieczka";
                        break;
                }

                int semester = profile.dateToSemester(attendanceDate);

                Attendance attendanceObject = new Attendance(
                        profileId,
                        attendanceId,
                        rTeacher.id,
                        rSubject.id,
                        semester,
                        attendanceName,
                        attendanceDate,
                        attendanceTime,
                        attendanceType
                );

                attendanceList.add(attendanceObject);
                if (attendanceObject.type != TYPE_PRESENT) {
                    metadataList.add(new Metadata(profileId, Metadata.TYPE_ATTENDANCE, attendanceObject.id, profile.getEmpty(), profile.getEmpty(), System.currentTimeMillis()));
                }
            }

            int attendanceDateValue = attendanceYear *10000 + attendanceMonth *100;
            if (profile.getEmpty() && attendanceDateValue > profile.getSemesterStart(1).getValue()) {
                attendancePrevMonthChecked = true; // do not need to check prev month later
                attendanceMonth--;
                if (attendanceMonth < 1) {
                    attendanceMonth = 12;
                    attendanceYear--;
                }
                r("get", "Attendance");
            } else if (!attendancePrevMonthChecked /* get also the previous month */) {
                attendanceMonth--;
                if (attendanceMonth < 1) {
                    attendanceMonth = 12;
                    attendanceYear--;
                }
                attendancePrevMonthChecked = true;
                r("get", "Attendance");
            } else {
                r("finish", "Attendance");
            }
        });
    }

    private void getLuckyNumberAndSemesterDates() {
        if (profile.getLuckyNumberDate() != null && profile.getLuckyNumber() != -1 && profile.getLuckyNumberDate().getValue() == Date.getToday().getValue()) {
            r("finish", "LuckyNumberAndSemesterDates");
            return;
        }
        if (loginBearerToken == null || loginStudentId == null) {
            r("finish", "LuckyNumberAndSemesterDates");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_lucky_number);
        apiRequest(Request.builder()
                .url(IDZIENNIK_URL +"/api/Uczniowie/"+loginStudentId+"/AktualnyDziennik")
                .header("Authorization", "Bearer "+loginBearerToken)
                .userAgent(userAgent), (data, response) -> {
            JsonObject settings = data.getAsJsonObject("ustawienia");
            if (settings == null) {
                finishWithError(new AppError(TAG, 1188, CODE_MAINTENANCE, response, data));
                return;
            }
            // data, settings
            profile.setLuckyNumber(-1);
            profile.setLuckyNumberDate(today);
            JsonElement luckyNumberEl = data.get("szczesliwyNumerek");
            if (luckyNumberEl != null && !(luckyNumberEl instanceof JsonNull)) {
                profile.setLuckyNumber(luckyNumberEl.getAsInt());
                Time publishTime = Time.fromH_m(settings.get("godzinaPublikacjiSzczesliwegoLosu").getAsString());
                if (Time.getNow().getValue() > publishTime.getValue()) {
                    profile.getLuckyNumberDate().stepForward(0, 0, 1); // the lucky number is already for tomorrow
                }
                app.db.luckyNumberDao().add(new LuckyNumber(profileId, profile.getLuckyNumberDate(), profile.getLuckyNumber()));
            }

            profile.setDateSemester1Start(Date.fromY_m_d(settings.get("poczatekSemestru1").getAsString()));
            profile.setDateSemester2Start(Date.fromY_m_d(settings.get("koniecSemestru1").getAsString()).stepForward(0, 0, 1));
            profile.setDateYearEnd(Date.fromY_m_d(settings.get("koniecSemestru2").getAsString()));

            r("finish", "LuckyNumberAndSemesterDates");
        });
    }

    private void getMessagesInbox() {
        if (loginBearerToken == null) {
            r("finish", "MessagesInbox");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_messages);
        apiRequestArray(Request.builder()
                .url(IDZIENNIK_URL +"/api/Wiadomosci/Odebrane")
                .header("Authorization", "Bearer "+loginBearerToken)
                .userAgent(userAgent), (data, response) -> {
            for (JsonElement jMessageEl: data) {
                JsonObject jMessage = jMessageEl.getAsJsonObject();

                String subject = jMessage.get("tytul").getAsString();
                if (subject.contains("(") && subject.startsWith("iDziennik - "))
                    continue;
                if (subject.startsWith("Uwaga dla ucznia (klasa:"))
                    continue;

                String messageIdStr = jMessage.get("id").getAsString();
                long messageId = crc32((messageIdStr+"0").getBytes());

                String body = "[META:"+messageIdStr+";-1]";
                body += jMessage.get("tresc").getAsString().replaceAll("\n", "<br>");

                long readDate = jMessage.get("odczytana").getAsBoolean() ? Date.fromIso(jMessage.get("wersjaRekordu").getAsString()) : 0;
                long sentDate = Date.fromIso(jMessage.get("dataWyslania").getAsString());

                JsonObject sender = jMessage.getAsJsonObject("nadawca");
                Teacher rTeacher = searchTeacher(sender.get("imie").getAsString(), sender.get("nazwisko").getAsString());
                rTeacher.loginId = sender.get("id").getAsString()+":"+sender.get("usr").getAsString();

                Message message = new Message(
                        profileId,
                        messageId,
                        subject,
                        body,
                        jMessage.get("rekordUsuniety").getAsBoolean() ? TYPE_DELETED : TYPE_RECEIVED,
                        rTeacher.id,
                        -1
                );

                MessageRecipient messageRecipient = new MessageRecipient(
                        profileId,
                        -1 /* me */,
                        -1,
                        readDate,
                        /*messageId*/ messageId
                );

                messageList.add(message);
                messageRecipientList.add(messageRecipient);
                messageMetadataList.add(new Metadata(profileId, Metadata.TYPE_MESSAGE, message.id, readDate > 0, readDate > 0 || profile.getEmpty(), sentDate));
            }

            r("finish", "MessagesInbox");
        });
    }

    private void getMessagesOutbox() {
        if (loginBearerToken == null) {
            r("finish", "MessagesOutbox");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_messages);
        apiRequestArray(Request.builder()
                .url(IDZIENNIK_URL +"/api/Wiadomosci/Wyslane")
                .header("Authorization", "Bearer "+loginBearerToken)
                .userAgent(userAgent), (data, response) -> {
            for (JsonElement jMessageEl: data) {
                JsonObject jMessage = jMessageEl.getAsJsonObject();

                String messageIdStr = jMessage.get("id").getAsString();
                long messageId = crc32((messageIdStr+"1").getBytes());

                String subject = jMessage.get("tytul").getAsString();

                String body = "[META:"+messageIdStr+";-1]";
                body += jMessage.get("tresc").getAsString().replaceAll("\n", "<br>");

                long sentDate = Date.fromIso(jMessage.get("dataWyslania").getAsString());

                Message message = new Message(
                        profileId,
                        messageId,
                        subject,
                        body,
                        TYPE_SENT,
                        -1,
                        -1
                );

                for (JsonElement recipientEl: jMessage.getAsJsonArray("odbiorcy")) {
                    JsonObject recipient = recipientEl.getAsJsonObject();
                    String firstName = recipient.get("imie").getAsString();
                    String lastName = recipient.get("nazwisko").getAsString();
                    if (firstName.isEmpty() || lastName.isEmpty()) {
                        firstName = "usunięty";
                        lastName = "użytkownik";
                    }
                    Teacher rTeacher = searchTeacher(firstName, lastName);
                    rTeacher.loginId = recipient.get("id").getAsString()+":"+recipient.get("usr").getAsString();

                    MessageRecipient messageRecipient = new MessageRecipient(
                            profileId,
                            rTeacher.id,
                            -1,
                            -1,
                            /*messageId*/ messageId
                    );
                    messageRecipientIgnoreList.add(messageRecipient);
                }

                messageList.add(message);
                metadataList.add(new Metadata(profileId, Metadata.TYPE_MESSAGE, message.id, true, true, sentDate));
            }

            r("finish", "MessagesOutbox");
        });
    }

    @Override
    public Map<String, Endpoint> getConfigurableEndpoints(Profile profile) {
        return null;
    }

    @Override
    public boolean isEndpointEnabled(Profile profile, boolean defaultActive, String name) {
        return defaultActive;
    }

    @Override
    public void syncMessages(@NonNull Context activityContext, @NonNull SyncCallback errorCallback, @NonNull ProfileFull profile)
    {
    }

    /*    __  __
         |  \/  |
         | \  / | ___  ___ ___  __ _  __ _  ___  ___
         | |\/| |/ _ \/ __/ __|/ _` |/ _` |/ _ \/ __|
         | |  | |  __/\__ \__ \ (_| | (_| |  __/\__ \
         |_|  |_|\___||___/___/\__,_|\__, |\___||___/
                                      __/ |
                                     |__*/
    @Override
    public void getMessage(@NonNull Context activityContext, @NonNull SyncCallback errorCallback, @NonNull ProfileFull profile, @NonNull MessageFull message, @NonNull MessageGetCallback messageCallback) {
        if (message.body == null)
            return;
        String messageIdStr = null;
        long messageIdBefore = -1;
        Matcher matcher = Pattern.compile("\\[META:([A-z0-9]+);([0-9-]+)]").matcher(message.body);
        if (matcher.find()) {
            messageIdStr = matcher.group(1);
            messageIdBefore = Long.parseLong(matcher.group(2));
        }
        if (messageIdBefore != -1) {
            boolean readByAll = true;
            // load this message's recipient(s) data
            message.recipients = app.db.messageRecipientDao().getAllByMessageId(profile.getId(), message.id);
            for (MessageRecipientFull recipient: message.recipients) {
                if (recipient.id == -1)
                    recipient.fullName = profile.getStudentNameLong();
                if (message.type == TYPE_SENT && recipient.readDate < 1)
                    readByAll = false;
            }
            if (!message.seen) {
                app.db.metadataDao().setSeen(profile.getId(), message, true);
            }
            if (readByAll) {
                // if a sent msg is not read by everyone, download it again to check the read status
                new Handler(activityContext.getMainLooper()).post(() -> messageCallback.onSuccess(message));
                return;
            }
        }

        if (!prepare(activityContext, errorCallback, profile.getId(), profile, LoginStore.fromProfileFull(profile)))
            return;

        String finalMessageIdStr = messageIdStr;
        login(() -> apiRequest(Request.builder()
                .url(IDZIENNIK_URL +"/mod_komunikator/WS_wiadomosci.asmx/PobierzWiadomosc")
                .userAgent(userAgent)
                .addParameter("idWiadomosci", finalMessageIdStr)
                .addParameter("typWiadomosci", message.type == TYPE_SENT ? 1 : 0)
                .postJson(), (result, response) -> {
            JsonObject data = result.getAsJsonObject("d");
            if (data == null) {
                finishWithError(new AppError(TAG, 1418, CODE_MAINTENANCE, response, result));
                return;
            }
            JsonObject jMessage = data.getAsJsonObject("Wiadomosc");
            if (jMessage == null) {
                finishWithError(new AppError(TAG, 1423, CODE_MAINTENANCE, response, result));
                return;
            }

            List<MessageRecipientFull> messageRecipientList = new ArrayList<>();

            long messageId = jMessage.get("_recordId").getAsLong();

            message.body = message.body.replaceAll("\\[META:[A-z0-9]+;[0-9-]+]", "[META:"+ finalMessageIdStr +";"+ messageId +"]");

            message.clearAttachments();
            for (JsonElement jAttachmentEl: jMessage.getAsJsonArray("ListaZal")) {
                JsonObject jAttachment = jAttachmentEl.getAsJsonObject();
                message.addAttachment(jAttachment.get("Id").getAsLong(), jAttachment.get("Nazwa").getAsString(), -1);
            }

            if (message.type == TYPE_RECEIVED) {
                MessageRecipientFull recipient = new MessageRecipientFull(profileId, -1, message.id);

                String readDateStr = jMessage.get("DataOdczytania").getAsString();
                recipient.readDate = readDateStr.isEmpty() ? System.currentTimeMillis() : Date.fromIso(readDateStr);

                recipient.fullName = profile.getStudentNameLong();
                messageRecipientList.add(recipient);
            }
            else if (message.type == TYPE_SENT) {
                teacherList = app.db.teacherDao().getAllNow(profileId);
                for (JsonElement jReceiverEl: jMessage.getAsJsonArray("ListaOdbiorcow")) {
                    JsonObject jReceiver = jReceiverEl.getAsJsonObject();
                    String receiverLastFirstName = jReceiver.get("NazwaOdbiorcy").getAsString();

                    Teacher teacher = searchTeacherByLastFirst(receiverLastFirstName);

                    MessageRecipientFull recipient = new MessageRecipientFull(profileId, teacher.id, message.id);

                    recipient.readDate = jReceiver.get("Status").getAsInt();

                    recipient.fullName = teacher.getFullName();
                    messageRecipientList.add(recipient);
                }
            }

            if (!message.seen) {
                app.db.metadataDao().setSeen(profileId, message, true);
            }
            app.db.messageDao().add(message);
            app.db.messageRecipientDao().addAll((List<MessageRecipient>)(List<?>) messageRecipientList); // not addAllIgnore

            message.recipients = messageRecipientList;

            new Handler(activityContext.getMainLooper()).post(() -> messageCallback.onSuccess(message));
        }));
    }

    @Override
    public void getAttachment(@NonNull Context activityContext, @NonNull SyncCallback errorCallback, @NonNull ProfileFull profile, @NonNull MessageFull message, long attachmentId, @NonNull AttachmentGetCallback attachmentCallback) {
        if (message.body == null)
            return;
        if (!prepare(activityContext, errorCallback, profile.getId(), profile, LoginStore.fromProfileFull(profile)))
            return;

        login(() -> {
            String fileName = message.attachmentNames.get(message.attachmentIds.indexOf(attachmentId));

            long messageId = -1;
            Matcher matcher = Pattern.compile("\\[META:([A-z0-9]+);([0-9-]+)]").matcher(message.body);
            if (matcher.find()) {
                messageId = Long.parseLong(matcher.group(2));
            }

            Request.Builder builder = Request.builder()
                    .url("https://iuczniowie.progman.pl/idziennik/mod_komunikator/Download.ashx")
                    .post()
                    .contentType(MediaTypeUtils.APPLICATION_FORM)
                    .addParameter("id", messageId)
                    .addParameter("fileName", fileName);

            new Handler(activityContext.getMainLooper()).post(() -> attachmentCallback.onSuccess(builder));
        });
    }

    @Override
    public void getRecipientList(@NonNull Context activityContext, @NonNull SyncCallback errorCallback, @NonNull ProfileFull profile, @NonNull RecipientListGetCallback recipientListGetCallback) {
        if (!prepare(activityContext, errorCallback, profile.getId(), profile, LoginStore.fromProfileFull(profile)))
            return;

        if (System.currentTimeMillis() - profile.getLastReceiversSync() < 24 * 60 * 60 * 1000) {
            AsyncTask.execute(() -> {
                List<Teacher> teacherList = app.db.teacherDao().getAllNow(profileId);
                new Handler(activityContext.getMainLooper()).post(() -> recipientListGetCallback.onSuccess(teacherList));
            });
            return;
        }

        login(() -> {
            List<Cookie> cookieList = app.cookieJar.loadForRequest(HttpUrl.get(IDZIENNIK_URL));
            for (Cookie cookie: cookieList) {
                if (cookie.name().equalsIgnoreCase("Bearer")) {
                    loginBearerToken = cookie.value();
                }
            }
            loginStudentId = profile.getStudentData("studentId", null);// TODO: 2019-06-12 temporary duplicated token & ID extraction

            apiRequestArray(Request.builder()
                    .url(IDZIENNIK_URL + "/api/Wiadomosci/Odbiorcy?idUcznia="+loginStudentId)
                    .header("Authorization", "Bearer " + loginBearerToken)
                    .userAgent(userAgent), (result, response) -> {
                teacherList = app.db.teacherDao().getAllNow(profileId);
                for (JsonElement recipientEl: result) {
                    JsonObject recipient = recipientEl.getAsJsonObject();
                    String name = recipient.get("nazwaKontaktu").getAsString();
                    String loginId = recipient.get("idUzytkownika").getAsString();
                    JsonArray typesArray = recipient.getAsJsonArray("typOsoby");
                    List<Integer> types = new ArrayList<>();
                    for (JsonElement typeEl: typesArray) {
                        types.add(typeEl.getAsInt());
                    }
                    String delimiter;
                    if (types.size() == 1 && types.get(0) >= 6) /* parent or student */
                        delimiter = " (";
                    else
                        delimiter = ": ";
                    String nameFirstLast = name.substring(0, name.indexOf(delimiter));
                    Teacher teacher = searchTeacherByFirstLast(nameFirstLast);
                    teacher.loginId = loginId;
                    teacher.type = 0;
                    for (int type: types) {
                        switch (type) {
                            case 0:
                                teacher.setType(Teacher.TYPE_SCHOOL_ADMIN);
                                break;
                            case 1:
                                teacher.setType(Teacher.TYPE_SECRETARIAT);
                                break;
                            case 2:
                                teacher.setType(Teacher.TYPE_TEACHER);
                                teacher.typeDescription = name.substring(name.indexOf(": ")+2);
                                break;
                            case 3:
                                teacher.setType(Teacher.TYPE_PRINCIPAL);
                                break;
                            case 4:
                                teacher.setType(Teacher.TYPE_EDUCATOR);
                                break;
                            case 5:
                                teacher.setType(Teacher.TYPE_PEDAGOGUE);
                                break;
                            case 6:
                                teacher.setType(Teacher.TYPE_PARENT);
                                teacher.typeDescription = name.substring(name.indexOf(" (")+2, name.lastIndexOf(" ("));
                                break;
                            case 7:
                                teacher.setType(Teacher.TYPE_STUDENT);
                                break;
                        }
                    }
                }
                app.db.teacherDao().addAll(teacherList);

                profile.setLastReceiversSync(System.currentTimeMillis());
                app.db.profileDao().add(profile);

                new Handler(activityContext.getMainLooper()).post(() -> recipientListGetCallback.onSuccess(new ArrayList<>(teacherList)));
            });
        });
    }

    @Override
    public MessagesComposeInfo getComposeInfo(@NonNull ProfileFull profile) {
        return new MessagesComposeInfo(0, 0, 180, 1983);
    }
}
