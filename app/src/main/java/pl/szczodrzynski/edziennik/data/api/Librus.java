package pl.szczodrzynski.edziennik.data.api;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Base64;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import im.wangchao.mhttp.Request;
import im.wangchao.mhttp.Response;
import im.wangchao.mhttp.body.MediaTypeUtils;
import im.wangchao.mhttp.callback.JsonCallbackHandler;
import im.wangchao.mhttp.callback.TextCallbackHandler;
import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.BuildConfig;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.api.interfaces.AttachmentGetCallback;
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface;
import pl.szczodrzynski.edziennik.data.api.interfaces.LoginCallback;
import pl.szczodrzynski.edziennik.data.api.interfaces.MessageGetCallback;
import pl.szczodrzynski.edziennik.data.api.interfaces.RecipientListGetCallback;
import pl.szczodrzynski.edziennik.data.api.interfaces.SyncCallback;
import pl.szczodrzynski.edziennik.data.api.v2.models.DataStore;
import pl.szczodrzynski.edziennik.data.db.modules.announcements.Announcement;
import pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance;
import pl.szczodrzynski.edziennik.data.db.modules.events.Event;
import pl.szczodrzynski.edziennik.data.db.modules.events.EventType;
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade;
import pl.szczodrzynski.edziennik.data.db.modules.grades.GradeCategory;
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
import pl.szczodrzynski.edziennik.utils.Utils;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_GONE;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_INVALID_LOGIN;
import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_LIBRUS_DISCONNECTED;
import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_LIBRUS_NOT_ACTIVATED;
import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_MAINTENANCE;
import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_OTHER;
import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_PROFILE_NOT_FOUND;
import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_SYNERGIA_NOT_ACTIVATED;
import static pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance.TYPE_ABSENT;
import static pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance.TYPE_ABSENT_EXCUSED;
import static pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance.TYPE_BELATED;
import static pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance.TYPE_PRESENT;
import static pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance.TYPE_RELEASED;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_PT_MEETING;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_TEACHER_ABSENCE;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_NORMAL;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_SEMESTER1_FINAL;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_SEMESTER1_PROPOSED;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_SEMESTER2_FINAL;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_SEMESTER2_PROPOSED;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_YEAR_FINAL;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_YEAR_PROPOSED;
import static pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange.TYPE_CANCELLED;
import static pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange.TYPE_CHANGE;
import static pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_RECEIVED;
import static pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_SENT;
import static pl.szczodrzynski.edziennik.data.db.modules.notices.Notice.TYPE_NEGATIVE;
import static pl.szczodrzynski.edziennik.data.db.modules.notices.Notice.TYPE_NEUTRAL;
import static pl.szczodrzynski.edziennik.data.db.modules.notices.Notice.TYPE_POSITIVE;
import static pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher.TYPE_EDUCATOR;
import static pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher.TYPE_LIBRARIAN;
import static pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher.TYPE_OTHER;
import static pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher.TYPE_PARENTS_COUNCIL;
import static pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher.TYPE_PEDAGOGUE;
import static pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher.TYPE_SCHOOL_ADMIN;
import static pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher.TYPE_SCHOOL_PARENTS_COUNCIL;
import static pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher.TYPE_SECRETARIAT;
import static pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher.TYPE_SUPER_ADMIN;
import static pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher.TYPE_TEACHER;
import static pl.szczodrzynski.edziennik.utils.Utils.bs;
import static pl.szczodrzynski.edziennik.utils.Utils.c;
import static pl.szczodrzynski.edziennik.utils.Utils.contains;
import static pl.szczodrzynski.edziennik.utils.Utils.crc16;
import static pl.szczodrzynski.edziennik.utils.Utils.d;
import static pl.szczodrzynski.edziennik.utils.Utils.getGradeValue;
import static pl.szczodrzynski.edziennik.utils.Utils.strToInt;

public class Librus implements EdziennikInterface {
    public Librus(App app) {
        this.app = app;
    }

    private static final String TAG = "api.Librus";
    private static final String CLIENT_ID = "wmSyUMo8llDAs4y9tJVYY92oyZ6h4lAt7KCuy0Gv";
    private static final String REDIRECT_URL = "http://localhost/bar";
    private static final String AUTHORIZE_URL = "https://portal.librus.pl/oauth2/authorize?client_id="+CLIENT_ID+"&redirect_uri="+REDIRECT_URL+"&response_type=code";
    private static final String LOGIN_URL = "https://portal.librus.pl/rodzina/login/action";
    private static final String TOKEN_URL = "https://portal.librus.pl/oauth2/access_token";
    private static final String ACCOUNTS_URL = "https://portal.librus.pl/api/v2/SynergiaAccounts";
    private static final String ACCOUNT_URL = "https://portal.librus.pl/api/v2/SynergiaAccounts/fresh/"; // + login
    private static final String API_URL = "https://api.librus.pl/2.0/";
    private static final String SYNERGIA_URL = "https://wiadomosci.librus.pl/module/";
    private static final String SYNERGIA_SANDBOX_URL = "https://sandbox.librus.pl/index.php?action=";
    private static final String userAgent = "Dalvik/2.1.0 Android LibrusMobileApp";
    private static final String synergiaUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Gecko/20100101 Firefox/62.0";

    private App app;
    private Context activityContext = null;
    private SyncCallback callback = null;
    private int profileId = -1;
    private Profile profile = null;
    private LoginStore loginStore = null;
    private boolean fullSync = true;
    private Date today;
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
    private List<GradeCategory> gradeCategoryList;
    private List<Grade> gradeList;
    private List<Event> eventList;
    private List<EventType> eventTypeList;
    private List<Notice> noticeList;
    private List<Attendance> attendanceList;
    private List<Announcement> announcementList;
    private List<Message> messageList;
    private List<MessageRecipient> messageRecipientList;
    private List<MessageRecipient> messageRecipientIgnoreList;
    private List<Metadata> metadataList;
    private List<Metadata> messageMetadataList;

    private static boolean fakeLogin = false;
    private String librusEmail = null;
    private String librusPassword = null;
    private String synergiaLogin = null;
    private String synergiaPassword = null;
    private String synergiaLastLogin = null;
    private long synergiaLastLoginTime = -1;
    private boolean premium = false;
    private boolean enableStandardGrades = true;
    private boolean enablePointGrades = false;
    private boolean enableDescriptiveGrades = false;
    private boolean enableTextGrades = false;
    private boolean enableBehaviourGrades = true;
    private long unitId = -1;
    private int startPointsSemester1 = 0;
    private int startPointsSemester2 = 0;

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

        DataStore ds = new DataStore(app.db, profileId);

        this.librusEmail = loginStore.getLoginData("email", "");
        this.librusPassword = loginStore.getLoginData("password", "");
        if (profile == null) {
            this.synergiaLogin = null;
            this.synergiaPassword = null;
        }
        else {
            this.synergiaLogin = profile.getStudentData("accountLogin", null);
            this.synergiaPassword = profile.getStudentData("accountPassword", null);
        }
        if (librusEmail.equals("") || librusPassword.equals("")) {
            finishWithError(new AppError(TAG, 214, AppError.CODE_INVALID_LOGIN, "Login field is empty"));
            return false;
        }
        this.premium = profile != null && profile.getStudentData("isPremium", false);
        this.failed = 0;
        fakeLogin = BuildConfig.DEBUG && librusEmail.toLowerCase().startsWith("fake");
        this.synergiaLastLogin = null;
        this.synergiaLastLoginTime = -1;

        this.refreshTokenFailed = false;

        teamList = profileId == -1 ? new ArrayList<>() : app.db.teamDao().getAllNow(profileId);
        teacherList = profileId == -1 ? new ArrayList<>() : app.db.teacherDao().getAllNow(profileId);
        subjectList = new ArrayList<>();
        lessonList = new ArrayList<>();
        lessonChangeList = new ArrayList<>();
        gradeCategoryList = new ArrayList<>();
        gradeList = new ArrayList<>();
        eventList = new ArrayList<>();
        eventTypeList = new ArrayList<>();
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
            targetEndpoints.add("Me");
            targetEndpoints.add("Schools");
            targetEndpoints.add("Classes");
            targetEndpoints.add("VirtualClasses");
            targetEndpoints.add("Units");
            targetEndpoints.add("Users");
            targetEndpoints.add("Subjects");
            targetEndpoints.add("Classrooms");
            targetEndpoints.add("Timetables");
            targetEndpoints.add("Substitutions");
            targetEndpoints.add("Colors");

            targetEndpoints.add("SavedGradeCategories");
            targetEndpoints.add("GradesCategories");
            targetEndpoints.add("PointGradesCategories");
            targetEndpoints.add("DescriptiveGradesCategories");
            //targetEndpoints.add("TextGradesCategories");
            targetEndpoints.add("BehaviourGradesCategories"); // TODO: 2019-04-30
            targetEndpoints.add("SaveGradeCategories");

            targetEndpoints.add("Grades");
            targetEndpoints.add("PointGrades");
            targetEndpoints.add("DescriptiveGrades");
            targetEndpoints.add("TextGrades");
            targetEndpoints.add("BehaviourGrades");
            targetEndpoints.add("GradesComments");

            targetEndpoints.add("Events");
            targetEndpoints.add("CustomTypes");
            targetEndpoints.add("Homework");
            targetEndpoints.add("LuckyNumbers");
            targetEndpoints.add("Notices");
            targetEndpoints.add("AttendanceTypes");
            targetEndpoints.add("Attendance");
            targetEndpoints.add("Announcements");
            targetEndpoints.add("PtMeetings");

            /*if (isEndpointEnabled(profile, true, "SchoolFreeDays"))
                targetEndpoints.add("SchoolFreeDays");
            if (isEndpointEnabled(profile, true, "ClassFreeDays"))
                targetEndpoints.add("ClassFreeDays");*/
            targetEndpoints.add("MessagesLogin");
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
            targetEndpoints.add("Me");
            targetEndpoints.add("Schools");
            targetEndpoints.add("Classes");
            targetEndpoints.add("VirtualClasses");
            targetEndpoints.add("Units");
            targetEndpoints.add("Users");
            targetEndpoints.add("Subjects");
            targetEndpoints.add("Colors");
            boolean hasMessagesLogin = false;
            for (int feature: featureList) {
                switch (feature) {
                    case FEATURE_TIMETABLE:
                        targetEndpoints.add("Classrooms");
                        targetEndpoints.add("Timetables");
                        targetEndpoints.add("Substitutions");
                        break;
                    case FEATURE_AGENDA:
                        targetEndpoints.add("Events");
                        targetEndpoints.add("CustomTypes");
                        targetEndpoints.add("PtMeetings");
                        targetEndpoints.add("SchoolFreeDays");
                        break;
                    case FEATURE_GRADES:
                        targetEndpoints.add("SavedGradeCategories");
                        targetEndpoints.add("GradesCategories");
                        targetEndpoints.add("PointGradesCategories");
                        targetEndpoints.add("DescriptiveGradesCategories");
                        //targetEndpoints.add("TextGradesCategories");
                        targetEndpoints.add("BehaviourGradesCategories"); // TODO: 2019-04-30
                        targetEndpoints.add("SaveGradeCategories");

                        targetEndpoints.add("Grades");
                        targetEndpoints.add("PointGrades");
                        targetEndpoints.add("DescriptiveGrades");
                        targetEndpoints.add("TextGrades");
                        targetEndpoints.add("BehaviourGrades");

                        targetEndpoints.add("GradesComments");
                        break;
                    case FEATURE_HOMEWORK:
                        targetEndpoints.add("Homework");
                        break;
                    case FEATURE_NOTICES:
                        targetEndpoints.add("Notices");
                        break;
                    case FEATURE_ATTENDANCE:
                        targetEndpoints.add("AttendanceTypes");
                        targetEndpoints.add("Attendance");
                        break;
                    case FEATURE_MESSAGES_INBOX:
                        if (!hasMessagesLogin) {
                            hasMessagesLogin = true;
                            targetEndpoints.add("MessagesLogin");
                        }
                        targetEndpoints.add("MessagesInbox");
                        break;
                    case FEATURE_MESSAGES_OUTBOX:
                        if (!hasMessagesLogin) {
                            hasMessagesLogin = true;
                            targetEndpoints.add("MessagesLogin");
                        }
                        targetEndpoints.add("MessagesOutbox");
                        break;
                    case FEATURE_ANNOUNCEMENTS:
                        targetEndpoints.add("Announcements");
                        break;
                }
            }
            targetEndpoints.add("LuckyNumbers");

            /*if (isEndpointEnabled(profile, true, "SchoolFreeDays"))
                targetEndpoints.add("SchoolFreeDays");
            if (isEndpointEnabled(profile, true, "ClassFreeDays"))
                targetEndpoints.add("ClassFreeDays");*/
            targetEndpoints.add("Finish");
            PROGRESS_COUNT = targetEndpoints.size()-1;
            PROGRESS_STEP = (90/PROGRESS_COUNT);
            begin();
        });
    }

    private void begin() {
        if (profile == null) {
            finishWithError(new AppError(TAG, 214, AppError.CODE_PROFILE_NOT_FOUND, "Profile == null WTF???"));
            return;
        }
        String accountToken = profile.getStudentData("accountToken", null);
        d(TAG, "Beginning account "+ profile.getStudentNameLong() +" sync with token "+accountToken+". Full sync enabled "+fullSync);
        synergiaAccessToken = accountToken;

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
        if (index > targetEndpoints.size()) {
            finish();
            return;
        }
        d(TAG, "Called r("+type+", "+endpoint+"). Getting "+targetEndpoints.get(index));
        switch (targetEndpoints.get(index)) {
            case "Me":
                getMe();
                break;
            case "Schools":
                getSchools();
                break;
            case "Classes":
                getClasses();
                break;
            case "VirtualClasses":
                getVirtualClasses();
                break;
            case "Units":
                getUnits();
                break;
            case "Users":
                getUsers();
                break;
            case "Subjects":
                getSubjects();
                break;
            case "Classrooms":
                getClassrooms();
                break;
            case "Timetables":
                getTimetables();
                break;
            case "Substitutions":
                getSubstitutions();
                break;
            case "Colors":
                getColors();
                break;
            case "SavedGradeCategories":
                getSavedGradeCategories();
                break;
            case "GradesCategories":
                getGradesCategories();
                break;
            case "PointGradesCategories":
                getPointGradesCategories();
                break;
            case "DescriptiveGradesCategories":
                getDescriptiveGradesSkills();
                break;
            case "TextGradesCategories":
                getTextGradesCategories();
                break;
            case "BehaviourGradesCategories":
                getBehaviourGradesCategories();
                break;
            case "SaveGradeCategories":
                saveGradeCategories();
                break;
            case "Grades":
                getGrades();
                break;
            case "PointGrades":
                getPointGrades();
                break;
            case "DescriptiveGrades":
                getDescriptiveGrades();
                break;
            case "TextGrades":
                getTextGrades();
                break;
            case "GradesComments":
                getGradesComments();
                break;
            case "BehaviourGrades":
                getBehaviourGrades();
                break;
            case "Events":
                getEvents();
                break;
            case "CustomTypes":
                getCustomTypes();
                break;
            case "Homework":
                getHomework();
                break;
            case "LuckyNumbers":
                getLuckyNumbers();
                break;
            case "Notices":
                getNotices();
                break;
            case "AttendanceTypes":
                getAttendanceTypes();
                break;
            case "Attendance":
                getAttendance();
                break;
            case "Announcements":
                getAnnouncements();
                break;
            case "PtMeetings":
                getPtMeetings();
                break;
            case "TeacherFreeDaysTypes":
                getTeacherFreeDaysTypes();
                break;
            case "TeacherFreeDays":
                getTeacherFreeDays();
                break;
            case "SchoolFreeDays":
                getSchoolFreeDays();
                break;
            case "MessagesLogin":
                getMessagesLogin();
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
        if (teacherList.size() > 0 && teacherListChanged)
            app.db.teacherDao().addAllIgnore(teacherList);
        if (subjectList.size() > 0)
            app.db.subjectDao().addAll(subjectList);
        if (lessonList.size() > 0) {
            app.db.lessonDao().clear(profileId);
            app.db.lessonDao().addAll(lessonList);
        }
        if (lessonChangeList.size() > 0)
            app.db.lessonChangeDao().addAll(lessonChangeList);
        if (gradeCategoryList.size() > 0 && gradeCategoryListChanged)
            app.db.gradeCategoryDao().addAll(gradeCategoryList);
        if (gradeList.size() > 0) {
            app.db.gradeDao().clear(profileId);
            app.db.gradeDao().addAll(gradeList);
        }
        if (eventList.size() > 0) {
            app.db.eventDao().removeFuture(profileId, Date.getToday());
            app.db.eventDao().addAll(eventList);
        }
        if (eventTypeList.size() > 0)
            app.db.eventTypeDao().addAll(eventTypeList);
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
            finishWithError(new AppError(TAG, 480, CODE_OTHER, app.getString(R.string.sync_error_saving_data), null, null, e, null));
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
    public void login(@NonNull LoginCallback loginCallback)
    {

        authorizeCallback = new AuthorizeCallback() {
            @Override
            public void onCsrfToken(String csrfToken) {
                d(TAG, "Found CSRF token: "+csrfToken);
                login(csrfToken, librusEmail, librusPassword, librusLoginCallback);
            }

            @Override
            public void onAuthorizationCode(String code) {
                d(TAG, "Found auth code: "+code);
                accessToken(code, null, accessTokenCallback);
            }
        };

        librusLoginCallback = redirectUrl -> {
            fakeAuthorize = "authorize";
            authorize(AUTHORIZE_URL, authorizeCallback);
        };

        accessTokenCallback = new AccessTokenCallback() {
            @Override
            public void onSuccess(String tokenType, String accessToken, String refreshToken, int expiresIn) {
                d(TAG, "Got tokens: "+tokenType+" "+accessToken);
                d(TAG, "Got tokens: "+refreshToken);
                loginStore.putLoginData("tokenType", tokenType);
                loginStore.putLoginData("accessToken", accessToken);
                loginStore.putLoginData("refreshToken", refreshToken);
                loginStore.putLoginData("tokenExpiryTime", System.currentTimeMillis()/1000 + expiresIn);
                getSynergiaToken(tokenType, accessToken, refreshToken, System.currentTimeMillis()/1000 + expiresIn);
            }

            @Override
            public void onError() {
                d(TAG, "Beginning login (authorize)");
                authorize(AUTHORIZE_URL, authorizeCallback);
            }
        };

        synergiaAccountsCallback = data -> {
            d(TAG, "Accounts: "+data.toString());
            JsonArray accounts = data.getAsJsonArray("accounts");
            if (accounts.size() == 0) {
                finishWithError(new AppError(TAG, 1237, CODE_OTHER, app.getString(R.string.sync_error_register_no_students), data));
                return;
            }
            long accountDataTime;
            List<Integer> accountIds = new ArrayList<>();
            List<String> accountLogins = new ArrayList<>();
            List<String> accountTokens = new ArrayList<>();
            List<String> accountNamesLong = new ArrayList<>();
            List<String> accountNamesShort = new ArrayList<>();
            accountIds.clear();
            accountLogins.clear();
            accountTokens.clear();
            accountNamesLong.clear();
            accountNamesShort.clear();
            accountDataTime = data.get("lastModification").getAsLong();
            for (JsonElement accountEl: accounts) {
                JsonObject account = accountEl.getAsJsonObject();

                JsonElement state = account.get("state");
                if (state != null && !(state instanceof JsonNull)) {
                    if (state.getAsString().equals("requiring_an_action")) {
                        finishWithError(new AppError(TAG, 694, CODE_LIBRUS_DISCONNECTED, data));
                        return;
                    }
                    if (state.getAsString().equals("need-activation")) {
                        finishWithError(new AppError(TAG, 701, CODE_SYNERGIA_NOT_ACTIVATED, data));
                        return;
                    }
                }

                accountIds.add(account.get("id").getAsInt());
                accountLogins.add(account.get("login").getAsString());
                accountTokens.add(account.get("accessToken").getAsString());
                accountNamesLong.add(account.get("studentName").getAsString());
                String[] nameParts = account.get("studentName").getAsString().split(" ");
                accountNamesShort.add(nameParts[0]+" "+nameParts[1].charAt(0)+".");
            }

            List<Profile> profileList = new ArrayList<>();
            for (int index = 0; index < accountIds.size(); index++) {
                Profile newProfile = new Profile();
                newProfile.setStudentNameLong(accountNamesLong.get(index));
                newProfile.setStudentNameShort(accountNamesShort.get(index));
                newProfile.setName(newProfile.getStudentNameLong());
                newProfile.setSubname(librusEmail);
                newProfile.setEmpty(true);
                newProfile.setLoggedIn(true);
                newProfile.putStudentData("accountId", accountIds.get(index));
                newProfile.putStudentData("accountLogin", accountLogins.get(index));
                newProfile.putStudentData("accountToken", accountTokens.get(index));
                newProfile.putStudentData("accountTokenTime", accountDataTime);
                profileList.add(newProfile);
            }

            callback.onLoginFirst(profileList, loginStore);
        };

        synergiaAccountCallback = data -> {
            if (data == null) {
                // 410 Gone
                app.cookieJar.clearForDomain("portal.librus.pl");
                authorize(AUTHORIZE_URL, authorizeCallback);
                return;
            }
            if (profile == null) {
                // this cannot be run on a fresh login
                finishWithError(new AppError(TAG, 1290, CODE_PROFILE_NOT_FOUND, "Profile == null", data));
                return;
            }
            d(TAG, "Account: "+data.toString());
            // synergiaAccount is executed when a synergia token needs a refresh
            JsonElement id = data.get("id");
            JsonElement login = data.get("login");
            JsonElement accessToken = data.get("accessToken");
            if (id == null || login == null || accessToken == null) {
                finishWithError(new AppError(TAG, 1284, CODE_OTHER, data));
                return;
            }
            profile.putStudentData("accountId", id.getAsInt());
            profile.putStudentData("accountLogin", login.getAsString());
            profile.putStudentData("accountToken", accessToken.getAsString());
            profile.putStudentData("accountTokenTime", System.currentTimeMillis() / 1000);
            profile.setStudentNameLong(data.get("studentName").getAsString());
            String[] nameParts = data.get("studentName").getAsString().split(" ");
            profile.setStudentNameShort(nameParts[0] + " " + nameParts[1].charAt(0) + ".");
            loginCallback.onSuccess();
        };

        String tokenType = loginStore.getLoginData("tokenType", "Bearer");
        String accessToken = loginStore.getLoginData("accessToken", null);
        String refreshToken = loginStore.getLoginData("refreshToken", null);
        long tokenExpiryTime = loginStore.getLoginData("tokenExpiryTime", (long)0);
        String accountToken;

        if (profile != null
                && (accountToken = profile.getStudentData("accountToken", null)) != null
                && !accountToken.equals("")
                && (System.currentTimeMillis() / 1000) - profile.getStudentData("accountTokenTime", (long)0) < 3 * 60 * 60) {
            c(TAG, "synergia token should be valid");
            loginCallback.onSuccess();
        }
        else {
            getSynergiaToken(tokenType, accessToken, refreshToken, tokenExpiryTime);
        }
    }
    private String synergiaAccessToken = "";
    private void getSynergiaToken(String tokenType, String accessToken, String refreshToken, long tokenExpiryTime) {
        c(TAG, "we have no synergia token or it expired");
        if (!tokenType.equals("")
                && refreshToken != null
                && accessToken != null
                && tokenExpiryTime-30 > System.currentTimeMillis() / 1000) {
            c(TAG, "we have a valid librus token, so we can use the API");
            // we have to decide whether we can already proceed getting the synergiaToken
            // or a list of students
            if (profile != null) {
                app.cookieJar.clearForDomain("portal.librus.pl");
                c(TAG, "user is logged in, refreshing synergia token");
                d(TAG, "Librus token: "+accessToken);
                synergiaAccount(tokenType, accessToken, profile.getStudentData("accountLogin", null), synergiaAccountCallback);
            }
            else {
                // this *should* be executed only once. ever.
                c(TAG, "user is not logged in, getting all the accounts");
                synergiaAccounts(tokenType, accessToken, synergiaAccountsCallback);
            }
        } else if (refreshToken != null) {
            c(TAG, "we don't have a valid token or it expired");
            c(TAG, "but we have a refresh token");
            d(TAG, "Token expired at " + tokenExpiryTime + ", " + (System.currentTimeMillis() / 1000 - tokenExpiryTime) + " seconds ago");
            app.cookieJar.clearForDomain("portal.librus.pl");
            accessToken(null, refreshToken, accessTokenCallback);
        } else {
            c(TAG, "we don't have any of the needed librus tokens");
            c(TAG, "we need to log in and generate");
            app.cookieJar.clearForDomain("portal.librus.pl");
            authorize(AUTHORIZE_URL, authorizeCallback);
        }
    }
    public boolean loginSynergia(@NonNull LoginCallback loginCallback)
    {
        if (profile == null) {
            return false;
        }
        if (synergiaLogin == null || synergiaPassword == null || synergiaLogin.equals("") || synergiaPassword.equals("")) {
            finishWithError(new AppError(TAG, 1152, CODE_INVALID_LOGIN, "Login field is empty"));
            return false;
        }
        if (System.currentTimeMillis() - synergiaLastLoginTime < 10 * 60 * 1000 && synergiaLogin.equals(synergiaLastLogin)) {// 10 minutes
            loginCallback.onSuccess();
            return true;
        }

        String escapedPassword = synergiaPassword.replace("&", "&amp;");// TODO: 2019-05-07 check other chars to escape

        String body =
                "<service>\n" +
                        "  <header/>\n" +
                        "  <data>\n" +
                        "    <login>"+synergiaLogin+"</login>\n" +
                        "    <password>"+escapedPassword+"</password>\n" +
                        "    <KeyStroke>\n" +
                        "      <Keys>\n" +
                        "        <Up/>\n" +
                        "        <Down/>\n" +
                        "      </Keys>\n" +
                        "    </KeyStroke>\n" +
                        "  </data>\n" +
                        "</service>";
        synergiaRequest("Login", body, data -> {
            if (data == null) {
                finishWithError(new AppError(TAG, 1176, AppError.CODE_MAINTENANCE, "data == null (975)"));
                return;
            }
            String error = data.select("response Login status").text();
            if (error.equals("ok")) {
                synergiaLastLoginTime = System.currentTimeMillis();
                synergiaLastLogin = synergiaLogin;
                loginCallback.onSuccess();
            }
            else {
                finishWithError(new AppError(TAG, 1186, CODE_INVALID_LOGIN, (Response) null, data.outerHtml()));
            }
        });
        return true;
    }

    /*    _    _      _                                          _ _ _                _
         | |  | |    | |                       ___              | | | |              | |
         | |__| | ___| |_ __   ___ _ __ ___   ( _ )     ___ __ _| | | |__   __ _  ___| | _____
         |  __  |/ _ \ | '_ \ / _ \ '__/ __|  / _ \/\  / __/ _` | | | '_ \ / _` |/ __| |/ / __|
         | |  | |  __/ | |_) |  __/ |  \__ \ | (_>  < | (_| (_| | | | |_) | (_| | (__|   <\__ \
         |_|  |_|\___|_| .__/ \___|_|  |___/  \___/\/  \___\__,_|_|_|_.__/ \__,_|\___|_|\_\___/
                       | |
                       |*/
    private String fakeAuthorize = "authorize";
    private void authorize(String url, AuthorizeCallback authorizeCallback) {
        callback.onActionStarted(R.string.sync_action_authorizing);
        Request.builder()
                .url(fakeLogin ? "http://szkolny.eu/librus/"+fakeAuthorize+".php" : url)
                .userAgent(userAgent)
                .withClient(app.httpLazy)
                .callback(new TextCallbackHandler() {
                    @Override
                    public void onSuccess(String data, Response response) {
                        //d("headers "+response.headers().toString());
                        String location = response.headers().get("Location");
                        if (location != null) {
                            Matcher authMatcher = Pattern.compile(REDIRECT_URL+"\\?code=([A-z0-9]+?)$", Pattern.DOTALL | Pattern.MULTILINE).matcher(location);
                            if (authMatcher.find()) {
                                authorizeCallback.onAuthorizationCode(authMatcher.group(1));
                            }
                            else {
                                //callback.onError(activityContext, Edziennik.CODE_OTHER, "Auth code not found: "+location);
                                authorize(location, authorizeCallback);
                            }
                        }
                        else {
                            Matcher csrfMatcher = Pattern.compile("name=\"csrf-token\" content=\"([A-z0-9=+/\\-_]+?)\"", Pattern.DOTALL).matcher(data);
                            if (csrfMatcher.find()) {
                                authorizeCallback.onCsrfToken(csrfMatcher.group(1));
                            }
                            else {
                                finishWithError(new AppError(TAG, 463, CODE_OTHER, "CSRF token not found.", response, data));
                            }
                        }
                    }

                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        finishWithError(new AppError(TAG, 207, CODE_OTHER, response, throwable));
                    }
                })
                .build()
                .enqueue();
    }

    private void login(String csrfToken, String email, String password, LibrusLoginCallback librusLoginCallback) {
        callback.onActionStarted(R.string.sync_action_logging_in);
        Request.builder()
                .url(fakeLogin ? "http://szkolny.eu/librus/login_action.php" : LOGIN_URL)
                .userAgent(userAgent)
                .addParameter("email", email)
                .addParameter("password", password)
                .addHeader("X-CSRF-TOKEN", csrfToken)
                .contentType(MediaTypeUtils.APPLICATION_JSON)
                .post()
                .callback(new JsonCallbackHandler() {
                    @Override
                    public void onSuccess(JsonObject data, Response response) {
                        if (data == null) {
                            if (response.parserErrorBody != null && response.parserErrorBody.contains("link aktywacyjny")) {
                                finishWithError(new AppError(TAG, 487, CODE_LIBRUS_NOT_ACTIVATED, response));
                                return;
                            }
                            finishWithError(new AppError(TAG, 489, CODE_MAINTENANCE, response));
                            return;
                        }
                        if (data.get("errors") != null) {
                            finishWithError(new AppError(TAG, 490, CODE_OTHER, data.get("errors").getAsJsonArray().get(0).getAsString(), response, data));
                            return;
                        }
                        librusLoginCallback.onLogin(data.get("redirect") != null ? data.get("redirect").getAsString() : "");
                    }

                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        if (response.code() == 403
                                || response.code() == 401) {
                            finishWithError(new AppError(TAG, 248, AppError.CODE_INVALID_LOGIN, response, throwable));
                            return;
                        }
                        finishWithError(new AppError(TAG, 251, CODE_OTHER, response, throwable));
                    }
                })
                .build()
                .enqueue();
    }

    private boolean refreshTokenFailed = false;
    private void accessToken(String code, String refreshToken, AccessTokenCallback accessTokenCallback) {
        callback.onActionStarted(R.string.sync_action_getting_token);
        List<Pair<String, Object>> params = new ArrayList<>();
        params.add(new Pair<>("client_id", CLIENT_ID));
        if (code != null) {
            params.add(new Pair<>("grant_type", "authorization_code"));
            params.add(new Pair<>("code", code));
            params.add(new Pair<>("redirect_uri", REDIRECT_URL));
        }
        else if (refreshToken != null) {
            params.add(new Pair<>("grant_type", "refresh_token"));
            params.add(new Pair<>("refresh_token", refreshToken));
        }
        Request.builder()
                .url(fakeLogin ? "http://szkolny.eu/librus/access_token.php" : TOKEN_URL)
                .userAgent(userAgent)
                .addParams(params)
                .allowErrorCode(HTTP_UNAUTHORIZED)
                .post()
                .callback(new JsonCallbackHandler() {
                    @Override
                    public void onSuccess(JsonObject data, Response response) {
                        if (data == null) {
                            finishWithError(new AppError(TAG, 539, CODE_MAINTENANCE, response));
                            return;
                        }
                        if (data.get("error") != null) {
                            JsonElement message = data.get("message");
                            JsonElement hint = data.get("hint");
                            if (!refreshTokenFailed && refreshToken != null && hint != null && (hint.getAsString().equals("Token has been revoked") || hint.getAsString().equals("Token has expired"))) {
                                c(TAG, "refreshing the token failed. Trying to log in again.");
                                refreshTokenFailed = true;
                                accessTokenCallback.onError();
                                return;
                            }
                            String errorText = data.get("error").getAsString()+" "+(message == null ? "" : message.getAsString())+" "+(hint == null ? "" : hint.getAsString());
                            finishWithError(new AppError(TAG, 552, CODE_OTHER, errorText, response, data));
                            return;
                        }
                        try {
                            accessTokenCallback.onSuccess(
                                    data.get("token_type").getAsString(),
                                    data.get("access_token").getAsString(),
                                    data.get("refresh_token").getAsString(),
                                    data.get("expires_in").getAsInt());
                        }
                        catch (NullPointerException e) {
                            finishWithError(new AppError(TAG, 311, CODE_OTHER, response, e, data));
                        }
                    }

                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        finishWithError(new AppError(TAG, 317, CODE_OTHER, response, throwable));
                    }
                })
                .build()
                .enqueue();
    }

    private void synergiaAccounts(String tokenType, String accessToken, SynergiaAccountsCallback synergiaAccountsCallback) {
        callback.onActionStarted(R.string.sync_action_getting_accounts);
        Request.builder()
                .url(fakeLogin ? "http://szkolny.eu/librus/synergia_accounts.php" : ACCOUNTS_URL)
                .userAgent(userAgent)
                .addHeader("Authorization", tokenType+" "+accessToken)
                .get()
                .allowErrorCode(HTTP_FORBIDDEN)
                .allowErrorCode(HTTP_UNAUTHORIZED)
                .allowErrorCode(HTTP_BAD_REQUEST)
                .callback(new JsonCallbackHandler() {
                    @Override
                    public void onSuccess(JsonObject data, Response response) {
                        if (data == null) {
                            finishWithError(new AppError(TAG, 590, CODE_MAINTENANCE, response));
                            return;
                        }
                        if (data.get("error") != null) {
                            JsonElement message = data.get("message");
                            JsonElement hint = data.get("hint");
                            String errorText = data.get("error").getAsString()+" "+(message == null ? "" : message.getAsString())+" "+(hint == null ? "" : hint.getAsString());
                            finishWithError(new AppError(TAG, 597, CODE_OTHER, errorText, response, data));
                            return;
                        }
                        try {
                            synergiaAccountsCallback.onSuccess(data);
                        }
                        catch (NullPointerException e) {
                            e.printStackTrace();
                            finishWithError(new AppError(TAG, 358, CODE_OTHER, response, e, data));
                        }
                    }

                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        finishWithError(new AppError(TAG, 364, CODE_OTHER, response, throwable));
                    }
                })
                .build()
                .enqueue();
    }

    private boolean error410 = false;

    private void synergiaAccount(String tokenType, String accessToken, String accountLogin, SynergiaAccountCallback synergiaAccountCallback) {
        callback.onActionStarted(R.string.sync_action_getting_account);
        d(TAG, "Requesting "+(fakeLogin ? "http://szkolny.eu/librus/synergia_accounts_fresh.php?login="+accountLogin : ACCOUNT_URL+accountLogin));
        if (accountLogin == null) { // just for safety
            synergiaAccounts(tokenType, accessToken, synergiaAccountsCallback);
            return;
        }
        Request.builder()
                .url(fakeLogin ? "http://szkolny.eu/librus/synergia_accounts_fresh.php?login="+accountLogin : ACCOUNT_URL+accountLogin)
                .userAgent(userAgent)
                .addHeader("Authorization", tokenType+" "+accessToken)
                .get()
                .allowErrorCode(HTTP_NOT_FOUND)
                .allowErrorCode(HTTP_FORBIDDEN)
                .allowErrorCode(HTTP_UNAUTHORIZED)
                .allowErrorCode(HTTP_BAD_REQUEST)
                .allowErrorCode(HTTP_GONE)
                .callback(new JsonCallbackHandler() {
                    @Override
                    public void onSuccess(JsonObject data, Response response) {
                        if (data == null) {
                            finishWithError(new AppError(TAG, 641, CODE_MAINTENANCE, response));
                            return;
                        }
                        if (response.code() == 410 && !error410) {
                            JsonElement reason = data.get("reason");
                            if (reason != null && !(reason instanceof JsonNull) && reason.getAsString().equals("requires_an_action")) {
                                finishWithError(new AppError(TAG, 1078, CODE_LIBRUS_DISCONNECTED, response, data));
                                return;
                            }
                            error410 = true;
                            synergiaAccountCallback.onSuccess(null);
                            return;
                        }
                        if (data.get("message") != null) {
                            String message = data.get("message").getAsString();
                            if (message.equals("Account not found")) {
                                finishWithError(new AppError(TAG, 651, CODE_OTHER, app.getString(R.string.sync_error_register_student_not_associated_format, profile.getStudentNameLong(), accountLogin), response, data));
                                return;
                            }
                            finishWithError(new AppError(TAG, 654, CODE_OTHER, message+"\n\n"+accountLogin, response, data));
                            return;
                        }
                        if (response.code() == HTTP_OK) {
                            try {
                                synergiaAccountCallback.onSuccess(data);
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                                finishWithError(new AppError(TAG, 662, CODE_OTHER, response, e, data));
                            }
                        }
                        else {
                            finishWithError(new AppError(TAG, 425, CODE_OTHER, response, data));
                        }
                    }

                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        finishWithError(new AppError(TAG, 432, CODE_OTHER, response, throwable));
                    }
                })
                .build()
                .enqueue();
    }

    private int failed = 0;

    private void apiRequest(String endpoint, ApiRequestCallback apiRequestCallback) {
        d(TAG, "Requesting "+API_URL+endpoint);
        Request.builder()
                .url(fakeLogin ? "http://szkolny.eu/librus/api/"+endpoint : API_URL+endpoint)
                .userAgent(userAgent)
                .addHeader("Authorization", "Bearer "+synergiaAccessToken)
                .get()
                .allowErrorCode(HTTP_FORBIDDEN)
                .allowErrorCode(HTTP_UNAUTHORIZED)
                .allowErrorCode(HTTP_BAD_REQUEST)
                .callback(new JsonCallbackHandler() {
                    @Override
                    public void onSuccess(JsonObject data, Response response) {
                        if (data == null) {
                            if (response.parserErrorBody != null && response.parserErrorBody.equals("Nieprawidłowy węzeł.")) {
                                apiRequestCallback.onSuccess(null);
                                return;
                            }
                            finishWithError(new AppError(TAG, 453, CODE_MAINTENANCE, response));
                            return;
                        }
                        if (data.get("Status") != null) {
                            JsonElement message = data.get("Message");
                            JsonElement code = data.get("Code");
                            d(TAG, "apiRequest Error "+data.get("Status").getAsString()+" "+(message == null ? "" : message.getAsString())+" "+(code == null ? "" : code.getAsString())+"\n\n"+response.request().url().toString());
                            if (message != null && !(message instanceof JsonNull) && message.getAsString().equals("Student timetable is not public")) {
                                try {
                                    apiRequestCallback.onSuccess(null);
                                }
                                catch (NullPointerException e) {
                                    e.printStackTrace();
                                    d(TAG, "apiRequest exception "+e.getMessage());
                                    finishWithError(new AppError(TAG, 503, CODE_OTHER, response, e, data));
                                }
                                return;
                            }
                            if (code != null
                                    && !(code instanceof JsonNull)
                                    && (code.getAsString().equals("LuckyNumberIsNotActive")
                                    || code.getAsString().equals("NotesIsNotActive")
                                    || code.getAsString().equals("AccessDeny"))
                            ) {
                                try {
                                    apiRequestCallback.onSuccess(null);
                                }
                                catch (NullPointerException e) {
                                    e.printStackTrace();
                                    d(TAG, "apiRequest exception "+e.getMessage());
                                    finishWithError(new AppError(TAG, 504, CODE_OTHER, response, e, data));
                                }
                                return;
                            }
                            String errorText = data.get("Status").getAsString()+" "+(message == null ? "" : message.getAsString())+" "+(code == null ? "" : code.getAsString());
                            if (code != null && !(code instanceof JsonNull) && code.getAsString().equals("TokenIsExpired")) {
                                failed++;
                                d(TAG, "Trying to refresh synergia token, api request failed "+failed+" times now");
                                if (failed > 1) {
                                    d(TAG, "Giving up, failed "+failed+" times");
                                    finishWithError(new AppError(TAG, 485, CODE_OTHER, errorText, response, data));
                                    return;
                                }
                                String tokenType = loginStore.getLoginData("tokenType", "Bearer");
                                String accessToken = loginStore.getLoginData("accessToken", null);
                                String refreshToken = loginStore.getLoginData("refreshToken", null);
                                long tokenExpiryTime = loginStore.getLoginData("tokenExpiryTime", (long)0);
                                getSynergiaToken(tokenType, accessToken, refreshToken, tokenExpiryTime);
                                return;
                            }
                            finishWithError(new AppError(TAG, 497, CODE_OTHER, errorText, response, data));
                            return;
                        }
                        try {
                            apiRequestCallback.onSuccess(data);
                        }
                        catch (NullPointerException e) {
                            e.printStackTrace();
                            d(TAG, "apiRequest exception "+e.getMessage());
                            finishWithError(new AppError(TAG, 505, CODE_OTHER, response, e, data));
                        }
                    }

                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        if (response.code() == 405) {
                            // method not allowed
                            finishWithError(new AppError(TAG, 511, CODE_OTHER, response, throwable));
                            return;
                        }
                        if (response.code() == 500) {
                            // TODO: 2019-09-10 dirty hotfix
                            if ("Classrooms".equals(endpoint)) {
                                apiRequestCallback.onSuccess(null);
                                return;
                            }
                            finishWithError(new AppError(TAG, 516, CODE_MAINTENANCE, response, throwable));
                            return;
                        }
                        finishWithError(new AppError(TAG, 520, CODE_OTHER, response, throwable));
                    }
                })
                .build()
                .enqueue();
    }

    private void synergiaRequest(String endpoint, String body, SynergiaRequestCallback synergiaRequestCallback) {
        d(TAG, "Requesting "+SYNERGIA_URL+endpoint);
        Request.builder()
                .url(SYNERGIA_URL+endpoint)
                .userAgent(synergiaUserAgent)
                .setTextBody(body, MediaTypeUtils.APPLICATION_XML)
                .callback(new TextCallbackHandler() {
                    @Override
                    public void onSuccess(String data, Response response) {
                        if ((data.contains("<status>error</status>") || data.contains("<error>")) && !data.contains("Niepoprawny")) {
                            finishWithError(new AppError(TAG, 541, AppError.CODE_MAINTENANCE, response, data));
                            return;
                        }
                        synergiaRequestCallback.onSuccess(Jsoup.parse(data, "", Parser.xmlParser()));
                    }

                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        finishWithError(new AppError(TAG, 556, CODE_OTHER, response, throwable));
                    }
                })
                .build()
                .enqueue();
    }

    // CALLBACKS & INTERFACES
    private interface AuthorizeCallback {
        void onCsrfToken(String csrfToken);
        void onAuthorizationCode(String code);
    }
    private interface LibrusLoginCallback {
        void onLogin(String redirectUrl);
    }
    private interface AccessTokenCallback {
        void onSuccess(String tokenType, String accessToken, String refreshToken, int expiresIn);
        void onError();
    }
    private interface SynergiaAccountsCallback {
        void onSuccess(JsonObject data);
    }
    private interface SynergiaAccountCallback {
        void onSuccess(JsonObject data);
    }
    private interface ApiRequestCallback {
        void onSuccess(JsonObject data);
    }
    private interface SynergiaRequestCallback {
        void onSuccess(Document data);
    }

    private AuthorizeCallback authorizeCallback;
    private LibrusLoginCallback librusLoginCallback;
    private AccessTokenCallback accessTokenCallback;
    private SynergiaAccountsCallback synergiaAccountsCallback;
    private SynergiaAccountCallback synergiaAccountCallback;

    /*    _____        _          _____                            _
         |  __ \      | |        |  __ \                          | |
         | |  | | __ _| |_ __ _  | |__) |___  __ _ _   _  ___  ___| |_ ___
         | |  | |/ _` | __/ _` | |  _  // _ \/ _` | | | |/ _ \/ __| __/ __|
         | |__| | (_| | || (_| | | | \ \  __/ (_| | |_| |  __/\__ \ |_\__ \
         |_____/ \__,_|\__\__,_| |_|  \_\___|\__, |\__,_|\___||___/\__|___/
                                                | |
                                                |*/
    private void getMe() {
        if (!fullSync) {
            r("finish", "Me");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_account_info);
        apiRequest("Me", data -> {
            JsonObject me = data.get("Me").getAsJsonObject();
            me = me.get("Account").getAsJsonObject();
            //d("Got School: "+school.toString());
            try {
                boolean premium = me.get("IsPremium").getAsBoolean();
                boolean premiumDemo = me.get("IsPremiumDemo").getAsBoolean();
                this.premium = premium || premiumDemo;
                profile.putStudentData("isPremium", premium || premiumDemo);
                r("finish", "Me");
            }
            catch (Exception e) {
                finishWithError(new AppError(TAG, 1316, CODE_OTHER, e, data));
            }
        });
    }

    private Map<Integer, Pair<Time, Time>> lessonRanges = new HashMap<>();
    private String schoolName = "";
    private void getSchools() {
        if (!fullSync) {
            try {
                lessonRanges = app.gson.fromJson(profile.getStudentData("lessonRanges", "{}"), new TypeToken<Map<Integer, Pair<Time, Time>>>() {
                }.getType());
                if (lessonRanges != null && lessonRanges.size() > 0) {
                    r("finish", "Schools");
                    return;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        lessonRanges = new HashMap<>();
        callback.onActionStarted(R.string.sync_action_syncing_school_info);
        apiRequest("Schools", data -> {
            //d("Got School: "+school.toString());
            try {
                JsonObject school = data.get("School").getAsJsonObject();
                int schoolId = school.get("Id").getAsInt();
                String schoolNameLong = school.get("Name").getAsString();
                StringBuilder schoolNameShort = new StringBuilder();
                for (String schoolNamePart: schoolNameLong.split(" ")) {
                    if (schoolNamePart.isEmpty())
                        continue;
                    schoolNameShort.append(Character.toLowerCase(schoolNamePart.charAt(0)));
                }
                String schoolTown = school.get("Town").getAsString();
                schoolName = schoolId+schoolNameShort.toString()+"_"+schoolTown.toLowerCase();
                profile.putStudentData("schoolName", schoolName);

                lessonRanges.clear();
                int index = 0;
                for (JsonElement lessonRangeEl: school.get("LessonsRange").getAsJsonArray()) {
                    JsonObject lr = lessonRangeEl.getAsJsonObject();
                    JsonElement from = lr.get("From");
                    JsonElement to = lr.get("To");
                    if (from != null && to != null && !(from instanceof JsonNull) && !(to instanceof JsonNull)) {
                        lessonRanges.put(index, new Pair<>(Time.fromH_m(from.getAsString()), Time.fromH_m(to.getAsString())));
                    }
                    index++;
                }
                profile.putStudentData("lessonRanges", app.gson.toJson(lessonRanges));
                r("finish", "Schools");
            }
            catch (Exception e) {
                finishWithError(new AppError(TAG, 1364, CODE_OTHER, e, data));
            }
        });
    }

    private long teamClassId = -1;
    private void getClasses() {
        if (!fullSync) {
            r("finish", "Classes");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_class);
        apiRequest("Classes", data -> {
            //d("Got Class: "+myClass.toString());
            try {
                JsonObject myClass = data.get("Class").getAsJsonObject();
                String teamName = myClass.get("Number").getAsString()
                        + myClass.get("Symbol").getAsString();
                teamClassId = myClass.get("Id").getAsLong();
                teamList.add(new Team(
                        profileId,
                        teamClassId,
                        teamName,
                        1,
                        schoolName+":"+teamName,
                        myClass.get("ClassTutor").getAsJsonObject().get("Id").getAsLong()));
                JsonElement semester1Begin = myClass.get("BeginSchoolYear");
                JsonElement semester2Begin = myClass.get("EndFirstSemester");
                JsonElement yearEnd = myClass.get("EndSchoolYear");
                if (semester1Begin != null
                        && semester2Begin != null
                        && yearEnd != null
                        && !(semester1Begin instanceof JsonNull)
                        && !(semester2Begin instanceof JsonNull)
                        && !(yearEnd instanceof JsonNull)) {
                    profile.setDateSemester1Start(Date.fromY_m_d(semester1Begin.getAsString()));
                    profile.setDateSemester2Start(Date.fromY_m_d(semester2Begin.getAsString()));
                    profile.setDateYearEnd(Date.fromY_m_d(yearEnd.getAsString()));
                }
                JsonElement unit = myClass.get("Unit");
                if (unit != null && !(unit instanceof JsonNull)) {
                    unitId = unit.getAsJsonObject().get("Id").getAsLong();
                    profile.putStudentData("unitId", unitId);
                }
                r("finish", "Classes");
            }
            catch (Exception e) {
                finishWithError(new AppError(TAG, 1411, CODE_OTHER, e, data));
            }
        });
    }

    private void getVirtualClasses() {
        if (!fullSync) {
            r("finish", "VirtualClasses");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_teams);
        apiRequest("VirtualClasses", data -> {
            if (data == null) {
                r("finish", "VirtualClasses");
                return;
            }
            try {
                JsonArray classes = data.get("VirtualClasses").getAsJsonArray();
                for (JsonElement myClassEl: classes) {
                    JsonObject myClass = myClassEl.getAsJsonObject();
                    String teamName = myClass.get("Name").getAsString();
                    long teamId = myClass.get("Id").getAsLong();
                    long teacherId = -1;
                    JsonElement el;
                    if ((el = myClass.get("Teacher")) != null) {
                        teacherId = el.getAsJsonObject().get("Id").getAsLong();
                    }
                    teamList.add(new Team(
                            profileId,
                            teamId,
                            teamName,
                            2,
                            schoolName + ":" + teamName,
                            teacherId));
                }
                r("finish", "VirtualClasses");
            } catch (Exception e) {
                finishWithError(new AppError(TAG, 1449, CODE_OTHER, e, data));
            }
        });
    }

    private void getUnits() {
        if (!fullSync) {
            enableStandardGrades = profile.getStudentData("enableStandardGrades", true);
            enablePointGrades = profile.getStudentData("enablePointGrades", false);
            enableDescriptiveGrades = profile.getStudentData("enableDescriptiveGrades", false);
            enableTextGrades = profile.getStudentData("enableTextGrades", false);
            enableBehaviourGrades = profile.getStudentData("enableBehaviourGrades", true);
            startPointsSemester1 = profile.getStudentData("startPointsSemester1", 0);
            startPointsSemester2 = profile.getStudentData("startPointsSemester2", 0);
            r("finish", "Units");
            return;
        }
        d(TAG, "Grades settings: "+enableStandardGrades+", "+enablePointGrades+", "+enableDescriptiveGrades);
        callback.onActionStarted(R.string.sync_action_syncing_school_info);
        apiRequest("Units", data -> {
            if (data == null) {
                r("finish", "Units");
                return;
            }
            JsonArray units = data.getAsJsonArray("Units");
            try {
                long unitId = profile.getStudentData("unitId", (long)-1);
                enableStandardGrades = true; // once a week or two (during a full sync) force getting the standard grade list. If there aren't any, disable it again later.
                enableBehaviourGrades = true;
                enableTextGrades = true; // TODO: 2019-05-13 if "DescriptiveGradesEnabled" are also TextGrades
                profile.putStudentData("enableStandardGrades", true);
                profile.putStudentData("enableBehaviourGrades", true);
                profile.putStudentData("enableTextGrades", true);
                for (JsonElement unitEl: units) {
                    JsonObject unit = unitEl.getAsJsonObject();

                    if (unit.get("Id").getAsLong() == unitId) {
                        JsonObject gradesSettings = unit.getAsJsonObject("GradesSettings");
                        enablePointGrades = gradesSettings.get("PointGradesEnabled").getAsBoolean();
                        enableDescriptiveGrades = gradesSettings.get("DescriptiveGradesEnabled").getAsBoolean();

                        JsonObject behaviourGradesSettings = unit.getAsJsonObject("BehaviourGradesSettings");
                        JsonObject startPoints = behaviourGradesSettings.getAsJsonObject("StartPoints");

                        startPointsSemester1 = startPoints.get("Semester1").getAsInt();
                        JsonElement startPointsSemester2El;
                        if ((startPointsSemester2El = startPoints.get("Semester2")) != null) {
                            startPointsSemester2 = startPointsSemester2El.getAsInt();
                        }
                        else {
                            startPointsSemester2 = startPointsSemester1;
                        }

                        profile.putStudentData("enablePointGrades", enablePointGrades);
                        profile.putStudentData("enableDescriptiveGrades", enableDescriptiveGrades);
                        profile.putStudentData("startPointsSemester1", startPointsSemester1);
                        profile.putStudentData("startPointsSemester2", startPointsSemester2);
                        break;
                    }
                }
                d(TAG, "Grades settings: "+enableStandardGrades+", "+enablePointGrades+", "+enableDescriptiveGrades);
                r("finish", "Units");
            }
            catch (Exception e) {
                finishWithError(new AppError(TAG, 1506, CODE_OTHER, e, data));
            }
        });
    }

    private boolean teacherListChanged = false;
    private void getUsers() {
        if (!fullSync) {
            r("finish", "Users");
            teacherList = app.db.teacherDao().getAllNow(profileId);
            teacherListChanged = false;
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_users);
        apiRequest("Users", data -> {
            if (data == null) {
                r("finish", "Users");
                return;
            }
            JsonArray users = data.get("Users").getAsJsonArray();
            //d("Got Users: "+users.toString());
            try {
                teacherListChanged = true;
                for (JsonElement userEl : users) {
                    JsonObject user = userEl.getAsJsonObject();
                    JsonElement firstName = user.get("FirstName");
                    JsonElement lastName = user.get("LastName");
                    teacherList.add(new Teacher(
                            profileId,
                            user.get("Id").getAsLong(),
                            firstName instanceof JsonNull ? "" : firstName.getAsString(),
                            lastName instanceof JsonNull ? "" : lastName.getAsString()
                    ));
                }
                r("finish", "Users");
            }
            catch (Exception e) {
                finishWithError(new AppError(TAG, 1544, CODE_OTHER, e, data));
            }
        });
    }

    private void getSubjects() {
        if (!fullSync) {
            r("finish", "Subjects");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_subjects);
        apiRequest("Subjects", data -> {
            if (data == null) {
                r("finish", "Subjects");
                return;
            }
            JsonArray subjects = data.get("Subjects").getAsJsonArray();
            //d("Got Subjects: "+subjects.toString());
            try {
                for (JsonElement subjectEl : subjects) {
                    JsonObject subject = subjectEl.getAsJsonObject();
                    JsonElement longName = subject.get("Name");
                    JsonElement shortName = subject.get("Short");
                    subjectList.add(new Subject(
                            profileId,
                            subject.get("Id").getAsLong(),
                            longName instanceof JsonNull ? "" : longName.getAsString(),
                            shortName instanceof JsonNull ? "" : shortName.getAsString()
                    ));
                }
                subjectList.add(new Subject(
                        profileId,
                        1,
                        "Zachowanie",
                        "zach"
                ));
                r("finish", "Subjects");
            }
            catch (Exception e) {
                finishWithError(new AppError(TAG, 1588, CODE_OTHER, e, data));
            }
        });
    }

    private SparseArray<String> classrooms = new SparseArray<>();
    private void getClassrooms() {
        //if (!fullSync)
        //    r("finish", "Classrooms");
        callback.onActionStarted(R.string.sync_action_syncing_classrooms);
        apiRequest("Classrooms", data -> {
            if (data == null) {
                r("finish", "Classrooms");
                return;
            }
            if (data.get("Classrooms") == null) {
                r("finish", "Classrooms");
                return;
            }
            JsonArray jClassrooms = data.get("Classrooms").getAsJsonArray();
            //d("Got Classrooms: "+jClassrooms.toString());
            classrooms.clear();
            try {
                for (JsonElement classroomEl : jClassrooms) {
                    JsonObject classroom = classroomEl.getAsJsonObject();
                    classrooms.put(classroom.get("Id").getAsInt(), classroom.get("Name").getAsString());
                }
                r("finish", "Classrooms");
            }
            catch (Exception e) {
                finishWithError(new AppError(TAG, 1617, CODE_OTHER, e, data));
            }
        });
    }

    private void getTimetables() {
        callback.onActionStarted(R.string.sync_action_syncing_timetable);
        Date weekStart = Week.getWeekStart();
        if (Date.getToday().getWeekDay() > 4) {
            weekStart.stepForward(0, 0, 7);
        }
        apiRequest("Timetables?weekStart="+weekStart.getStringY_m_d(), data -> {
            if (data == null) {
                r("finish", "Timetables");
                return;
            }
            JsonObject timetables = data.get("Timetable").getAsJsonObject();
            try {
                for (Map.Entry<String, JsonElement> dayEl: timetables.entrySet()) {
                    JsonArray day = dayEl.getValue().getAsJsonArray();
                    for (JsonElement lessonGroupEl: day) {
                        if ((lessonGroupEl instanceof JsonArray && ((JsonArray) lessonGroupEl).size() == 0) || lessonGroupEl instanceof JsonNull || lessonGroupEl == null) {
                            continue;
                        }
                        JsonArray lessonGroup = lessonGroupEl.getAsJsonArray();
                        for (JsonElement lessonEl: lessonGroup) {
                            if ((lessonEl instanceof JsonArray && ((JsonArray) lessonEl).size() == 0) || lessonEl instanceof JsonNull || lessonEl == null) {
                                continue;
                            }
                            JsonObject lesson = lessonEl.getAsJsonObject();

                            boolean substitution = false;
                            boolean cancelled = false;
                            JsonElement isSubstitutionClass;
                            if ((isSubstitutionClass = lesson.get("IsSubstitutionClass")) != null) {
                                substitution = isSubstitutionClass.getAsBoolean();
                            }
                            JsonElement isCanceled;
                            if ((isCanceled = lesson.get("IsCanceled")) != null) {
                                cancelled = isCanceled.getAsBoolean();
                            }

                            if (substitution && cancelled) {
                                // the lesson is probably shifted. Skip this one
                                continue;
                            }

                            Time startTime = null;
                            Time endTime = null;
                            try {
                                startTime = Time.fromH_m(lesson.get(substitution && !cancelled ? "OrgHourFrom" : "HourFrom").getAsString());
                                endTime = Time.fromH_m(lesson.get(substitution && !cancelled ? "OrgHourTo" : "HourTo").getAsString());
                            }
                            catch (Exception ignore) {
                                try {
                                    JsonElement lessonNo;
                                    if (!((lessonNo = lesson.get("LessonNo")) instanceof JsonNull)) {
                                        Pair<Time, Time> timePair = lessonRanges.get(strToInt(lessonNo.getAsString()));
                                        if (timePair != null) {
                                            startTime = timePair.first;
                                            endTime = timePair.second;
                                        }
                                    }
                                }
                                catch (Exception ignore2) { }
                            }


                            Lesson lessonObject = new Lesson(
                                    profileId,
                                    lesson.get("DayNo").getAsInt() - 1,
                                    startTime,
                                    endTime
                            );

                            JsonElement subject;
                            if ((subject = lesson.get(substitution && !cancelled ? "OrgSubject" : "Subject")) != null) {
                                lessonObject.subjectId = subject.getAsJsonObject().get("Id").getAsLong();
                            }

                            JsonElement teacher;
                            if ((teacher = lesson.get(substitution && !cancelled ? "OrgTeacher" : "Teacher")) != null) {
                                lessonObject.teacherId = teacher.getAsJsonObject().get("Id").getAsLong();
                            }

                            JsonElement myClass;
                            if ((myClass = lesson.get("Class")) != null) {
                                lessonObject.teamId = myClass.getAsJsonObject().get("Id").getAsLong();
                            }
                            if (myClass == null && (myClass = lesson.get("VirtualClass")) != null) {
                                lessonObject.teamId = myClass.getAsJsonObject().get("Id").getAsLong();
                            }

                            JsonElement classroom;
                            if ((classroom = lesson.get(substitution && !cancelled ? "OrgClassroom" : "Classroom")) != null) {
                                lessonObject.classroomName = classrooms.get(classroom.getAsJsonObject().get("Id").getAsInt());
                            }

                            lessonList.add(lessonObject);
                        }
                    }
                }
                r("finish", "Timetables");
            }
            catch (Exception e) {
                finishWithError(new AppError(TAG, 1704, CODE_OTHER, e, data));
            }
        });
    }

    private void getSubstitutions() {
        callback.onActionStarted(R.string.sync_action_syncing_timetable_changes);
        apiRequest("Calendars/Substitutions", data -> {
            if (data == null) {
                r("finish", "Substitutions");
                return;
            }

            JsonArray substitutions = data.get("Substitutions").getAsJsonArray();
            try {
                List<Long> ignoreList = new ArrayList<>();
                for (JsonElement substitutionEl : substitutions) {
                    JsonObject substitution = substitutionEl.getAsJsonObject();

                    String str_date = substitution.get("OrgDate").getAsString();
                    Date lessonDate = Date.fromY_m_d(str_date);

                    Time startTime = Time.getNow();
                    JsonElement lessonNo;
                    if (!((lessonNo = substitution.get("OrgLessonNo")) instanceof JsonNull)) {
                        Pair<Time, Time> timePair = lessonRanges.get(lessonNo.getAsInt());
                        if (timePair != null)
                            startTime = timePair.first;
                    }

                    JsonElement isShifted;
                    JsonElement isCancelled;
                    if ((isShifted = substitution.get("IsShifted")) != null && isShifted.getAsBoolean()) {
                        // a lesson is shifted
                        // add a TYPE_CANCELLED for the source lesson and a TYPE_CHANGE for the destination lesson

                        // source lesson: cancel
                        LessonChange lessonCancelled = new LessonChange(profileId, lessonDate, startTime, startTime.clone().stepForward(0, 45, 0));
                        lessonCancelled.type = TYPE_CANCELLED;
                        lessonChangeList.add(lessonCancelled);
                        metadataList.add(new Metadata(profileId, Metadata.TYPE_LESSON_CHANGE, lessonCancelled.id, profile.getEmpty(), profile.getEmpty(), System.currentTimeMillis()));

                        // target lesson: change
                        startTime = Time.getNow();
                        if (!((lessonNo = substitution.get("LessonNo")) instanceof JsonNull)) {
                            Pair<Time, Time> timePair = lessonRanges.get(lessonNo.getAsInt());
                            if (timePair != null)
                                startTime = timePair.first;
                        }

                        LessonChange lessonChanged = new LessonChange(profileId, lessonDate, startTime, startTime.clone().stepForward(0, 45, 0));
                        lessonChanged.type = TYPE_CHANGE;
                        JsonElement subject;
                        if ((subject = substitution.get("Subject")) != null) {
                            lessonChanged.subjectId = subject.getAsJsonObject().get("Id").getAsLong();
                        }
                        JsonElement teacher;
                        if ((teacher = substitution.get("Teacher")) != null) {
                            lessonChanged.teacherId = teacher.getAsJsonObject().get("Id").getAsLong();
                        }
                        JsonElement myClass;
                        if ((myClass = substitution.get("Class")) != null) {
                            lessonChanged.teamId = myClass.getAsJsonObject().get("Id").getAsLong();
                        }
                        if (myClass == null && (myClass = substitution.get("VirtualClass")) != null) {
                            lessonChanged.teamId = myClass.getAsJsonObject().get("Id").getAsLong();
                        }
                        lessonChangeList.add(lessonChanged);
                        metadataList.add(new Metadata(profileId, Metadata.TYPE_LESSON_CHANGE, lessonChanged.id, profile.getEmpty(), profile.getEmpty(), System.currentTimeMillis()));

                        // ignore the target lesson in further array elements - it's already changed
                        ignoreList.add(lessonChanged.lessonDate.combineWith(lessonChanged.startTime));
                    }
                    else if ((isCancelled = substitution.get("IsCancelled")) != null && isCancelled.getAsBoolean()) {
                        LessonChange lessonChange = new LessonChange(profileId, lessonDate, startTime, startTime.clone().stepForward(0, 45, 0));
                        // if it's actually a lesson shift - ignore the target lesson cancellation
                        if (ignoreList.size() > 0 && ignoreList.contains(lessonChange.lessonDate.combineWith(lessonChange.startTime)))
                            continue;
                        lessonChange.type = TYPE_CANCELLED;
                        lessonChangeList.add(lessonChange);
                        metadataList.add(new Metadata(profileId, Metadata.TYPE_LESSON_CHANGE, lessonChange.id, profile.getEmpty(), profile.getEmpty(), System.currentTimeMillis()));
                    }
                    else {
                        LessonChange lessonChange = new LessonChange(profileId, lessonDate, startTime, startTime.clone().stepForward(0, 45, 0));

                        lessonChange.type = TYPE_CHANGE;

                        JsonElement subject;
                        if ((subject = substitution.get("Subject")) != null) {
                            lessonChange.subjectId = subject.getAsJsonObject().get("Id").getAsLong();
                        }
                        JsonElement teacher;
                        if ((teacher = substitution.get("Teacher")) != null) {
                            lessonChange.teacherId = teacher.getAsJsonObject().get("Id").getAsLong();
                        }

                        JsonElement myClass;
                        if ((myClass = substitution.get("Class")) != null) {
                            lessonChange.teamId = myClass.getAsJsonObject().get("Id").getAsLong();
                        }
                        if (myClass == null && (myClass = substitution.get("VirtualClass")) != null) {
                            lessonChange.teamId = myClass.getAsJsonObject().get("Id").getAsLong();
                        }

                        lessonChangeList.add(lessonChange);
                        metadataList.add(new Metadata(profileId, Metadata.TYPE_LESSON_CHANGE, lessonChange.id, profile.getEmpty(), profile.getEmpty(), System.currentTimeMillis()));
                    }

                }
                r("finish", "Substitutions");
            }
            catch (Exception e) {
                finishWithError(new AppError(TAG, 1822, CODE_OTHER, e, data));
            }
        });
    }

    private SparseIntArray colors = new SparseIntArray();
    private void getColors() {
        colors.put( 1, 0xFFF0E68C);
        colors.put( 2, 0xFF87CEFA);
        colors.put( 3, 0xFFB0C4DE);
        colors.put( 4, 0xFFF0F8FF);
        colors.put( 5, 0xFFF0FFFF);
        colors.put( 6, 0xFFF5F5DC);
        colors.put( 7, 0xFFFFEBCD);
        colors.put( 8, 0xFFFFF8DC);
        colors.put( 9, 0xFFA9A9A9);
        colors.put(10, 0xFFBDB76B);
        colors.put(11, 0xFF8FBC8F);
        colors.put(12, 0xFFDCDCDC);
        colors.put(13, 0xFFDAA520);
        colors.put(14, 0xFFE6E6FA);
        colors.put(15, 0xFFFFA07A);
        colors.put(16, 0xFF32CD32);
        colors.put(17, 0xFF66CDAA);
        colors.put(18, 0xFF66CDAA);
        colors.put(19, 0xFFC0C0C0);
        colors.put(20, 0xFFD2B48C);
        colors.put(21, 0xFF3333FF);
        colors.put(22, 0xFF7B68EE);
        colors.put(23, 0xFFBA55D3);
        colors.put(24, 0xFFFFB6C1);
        colors.put(25, 0xFFFF1493);
        colors.put(26, 0xFFDC143C);
        colors.put(27, 0xFFFF0000);
        colors.put(28, 0xFFFF8C00);
        colors.put(29, 0xFFFFD700);
        colors.put(30, 0xFFADFF2F);
        colors.put(31, 0xFF7CFC00);
        r("finish", "Colors");
        /*
        apiRequest("Colors", data -> {
            JsonArray jColors = data.get("Colors").getAsJsonArray();
            d("Got Colors: "+jColors.toString());
            colors.clear();
            try {
                for (JsonElement colorEl : jColors) {
                    JsonObject color = colorEl.getAsJsonObject();
                    colors.put(color.get("Id").getAsInt(), Color.parseColor("#"+color.get("RGB").getAsString()));
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            getGrades();
        });*/
    }

    private boolean gradeCategoryListChanged = false;
    private void getSavedGradeCategories() {
        gradeCategoryList = app.db.gradeCategoryDao().getAllNow(profileId);
        gradeCategoryListChanged = false;
        r("finish", "SavedGradeCategories");
    }

    private void saveGradeCategories() {
        r("finish", "SaveGradeCategories");
    }

    private void getGradesCategories() {
        if (!fullSync && false) {
            // cancel every not-full sync; no need to download categories again
            // every full sync it'll be enabled to make sure there are no grades - by getUnits
            r("finish", "GradesCategories");
            return;
        }
        // not a full sync. Will get all grade categories. Clear the current list.
        gradeCategoryList.clear();

        callback.onActionStarted(R.string.sync_action_syncing_grade_categories);
        apiRequest("Grades/Categories", data -> {
            if (data == null) {
                r("finish", "GradesCategories");
                return;
            }
            JsonArray categories = data.get("Categories").getAsJsonArray();
            enableStandardGrades = categories.size() > 0;
            profile.putStudentData("enableStandardGrades", enableStandardGrades);
            if (!enableStandardGrades) {
                r("finish", "GradesCategories");
                return;
            }
            gradeCategoryListChanged = true;
            //d("Got Grades/Categories: "+categories.toString());
            try {
                for (JsonElement categoryEl : categories) {
                    JsonObject category = categoryEl.getAsJsonObject();
                    JsonElement name = category.get("Name");
                    JsonElement weight = category.get("Weight");
                    JsonElement color = category.get("Color");
                    JsonElement countToTheAverage = category.get("CountToTheAverage");
                    int colorInt = Color.BLUE;
                    if (!(color instanceof JsonNull) && color != null) {
                        colorInt = colors.get(color.getAsJsonObject().get("Id").getAsInt());
                    }
                    boolean countToTheAverageBool = !(countToTheAverage instanceof JsonNull) && countToTheAverage != null && countToTheAverage.getAsBoolean();
                    int weightInt = weight instanceof JsonNull || weight == null || !countToTheAverageBool ? 0 : weight.getAsInt();
                    int categoryId = category.get("Id").getAsInt();
                    gradeCategoryList.add(new GradeCategory(
                            profileId,
                            categoryId,
                            weightInt,
                            colorInt,
                            name instanceof JsonNull || name == null ? "" : name.getAsString()
                    ));
                }
                r("finish", "GradesCategories");
            }
            catch (Exception e) {
                finishWithError(new AppError(TAG, 1954, CODE_OTHER, e, data));
            }
        });
    }

    private void getGradesComments() {
        callback.onActionStarted(R.string.sync_action_syncing_grade_comments);
        apiRequest("Grades/Comments", data -> {
            if (data == null) {
                r("finish", "GradesComments");
                return;
            }

            JsonArray comments = data.get("Comments").getAsJsonArray();
            for (JsonElement commentEl : comments) {
                JsonObject comment = commentEl.getAsJsonObject();
                long gradeId = comment.get("Grade").getAsJsonObject().get("Id").getAsLong();
                String text = comment.get("Text").getAsString();

                for (Grade grade : gradeList) {
                    if (grade.id == gradeId) {
                        grade.description = text;
                        break;
                    }
                }
            }

            r("finish", "GradesComments");
        });
    }

    private void getPointGradesCategories() {
        if (!fullSync || !enablePointGrades) {
            // cancel every not-full sync; no need to download categories again
            // or
            // if it's a full sync, point grades may have already been disabled in getUnits
            r("finish", "PointGradesCategories");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_point_grade_categories);
        apiRequest("PointGrades/Categories", data -> {
            if (data == null) {
                r("finish", "PointGradesCategories");
                return;
            }
            JsonArray categories = data.get("Categories").getAsJsonArray();
            enablePointGrades = categories.size() > 0;
            profile.putStudentData("enablePointGrades", enablePointGrades);
            if (!enablePointGrades) {
                r("finish", "PointGradesCategories");
                return;
            }
            gradeCategoryListChanged = true;
            //d("Got Grades/Categories: "+categories.toString());
            for (JsonElement categoryEl : categories) {
                JsonObject category = categoryEl.getAsJsonObject();
                JsonElement name = category.get("Name");
                JsonElement weight = category.get("Weight");
                JsonElement color = category.get("Color");
                JsonElement countToTheAverage = category.get("CountToTheAverage");
                JsonElement valueFrom = category.get("ValueFrom");
                JsonElement valueTo = category.get("ValueTo");
                int colorInt = Color.BLUE;
                if (!(color instanceof JsonNull) && color != null) {
                    colorInt = colors.get(color.getAsJsonObject().get("Id").getAsInt());
                }
                boolean countToTheAverageBool = !(countToTheAverage instanceof JsonNull) && countToTheAverage != null && countToTheAverage.getAsBoolean();
                int weightInt = weight instanceof JsonNull || weight == null || !countToTheAverageBool ? 0 : weight.getAsInt();
                int categoryId = category.get("Id").getAsInt();
                float valueFromFloat = valueFrom.getAsFloat();
                float valueToFloat = valueTo.getAsFloat();
                gradeCategoryList.add(
                        new GradeCategory(
                                profileId,
                                categoryId,
                                weightInt,
                                colorInt,
                                name instanceof JsonNull || name == null ? "" : name.getAsString()
                        ).setValueRange(valueFromFloat, valueToFloat)
                );
            }
            r("finish", "PointGradesCategories");
        });
    }

    private void getDescriptiveGradesSkills() {
        if (!fullSync || !enableDescriptiveGrades) {
            // cancel every not-full sync; no need to download categories again
            // or
            // if it's a full sync, descriptive grades may have already been disabled in getUnits
            r("finish", "DescriptiveGradesCategories");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_descriptive_grade_categories);
        apiRequest("DescriptiveTextGrades/Skills", data -> {
            if (data == null) {
                r("finish", "DescriptiveGradesCategories");
                return;
            }
            JsonArray categories = data.get("Skills").getAsJsonArray();
            enableDescriptiveGrades = categories.size() > 0;
            profile.putStudentData("enableDescriptiveGrades", enableDescriptiveGrades);
            if (!enableDescriptiveGrades) {
                r("finish", "DescriptiveGradesCategories");
                return;
            }
            gradeCategoryListChanged = true;
            //d("Got Grades/Categories: "+categories.toString());
            for (JsonElement categoryEl : categories) {
                JsonObject category = categoryEl.getAsJsonObject();
                JsonElement name = category.get("Name");
                JsonElement color = category.get("Color");
                int colorInt = Color.BLUE;
                if (!(color instanceof JsonNull) && color != null) {
                    colorInt = colors.get(color.getAsJsonObject().get("Id").getAsInt());
                }
                int weightInt = -1;
                int categoryId = category.get("Id").getAsInt();
                gradeCategoryList.add(new GradeCategory(
                        profileId,
                        categoryId,
                        weightInt,
                        colorInt,
                        name instanceof JsonNull || name == null ? "" : name.getAsString()
                ));
            }
            r("finish", "DescriptiveGradesCategories");
        });
    }

    private void getTextGradesCategories() {
        if (!fullSync || !enableTextGrades) {
            // cancel every not-full sync; no need to download categories again
            // or
            // if it's a full sync, text grades may have already been disabled in getUnits
            r("finish", "TextGradesCategories");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_descriptive_grade_categories);
        apiRequest("TextGrades/Categories", data -> {
            if (data == null) {
                r("finish", "TextGradesCategories");
                return;
            }
            JsonArray categories = data.get("Categories").getAsJsonArray();
            enableTextGrades = categories.size() > 0;
            profile.putStudentData("enableTextGrades", enableTextGrades);
            if (!enableTextGrades) {
                r("finish", "TextGradesCategories");
                return;
            }
            gradeCategoryListChanged = true;
            //d("Got Grades/Categories: "+categories.toString());
            for (JsonElement categoryEl : categories) {
                JsonObject category = categoryEl.getAsJsonObject();
                JsonElement name = category.get("Name");
                JsonElement color = category.get("Color");
                int colorInt = Color.BLUE;
                if (!(color instanceof JsonNull) && color != null) {
                    colorInt = colors.get(color.getAsJsonObject().get("Id").getAsInt());
                }
                int weightInt = -1;
                int categoryId = category.get("Id").getAsInt();
                gradeCategoryList.add(new GradeCategory(
                        profileId,
                        categoryId,
                        weightInt,
                        colorInt,
                        name instanceof JsonNull || name == null ? "" : name.getAsString()
                ));
            }
            r("finish", "TextGradesCategories");
        });
    }

    private void getBehaviourGradesCategories() {
        if (!fullSync || !enableBehaviourGrades) {
            // cancel every not-full sync; no need to download categories again
            // or
            // if it's a full sync, descriptive grades may have already been disabled in getUnits
            r("finish", "BehaviourGradesCategories");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_behaviour_grade_categories);
        apiRequest("BehaviourGrades/Points/Categories", data -> {
            if (data == null) {
                r("finish", "BehaviourGradesCategories");
                return;
            }
            JsonArray categories = data.get("Categories").getAsJsonArray();
            enableBehaviourGrades = categories.size() > 0;
            profile.putStudentData("enableBehaviourGrades", enableBehaviourGrades);
            if (!enableBehaviourGrades) {
                r("finish", "BehaviourGradesCategories");
                return;
            }
            gradeCategoryListChanged = true;
            //d("Got Grades/Categories: "+categories.toString());
            for (JsonElement categoryEl : categories) {
                JsonObject category = categoryEl.getAsJsonObject();
                JsonElement name = category.get("Name");
                JsonElement valueFrom = category.get("ValueFrom");
                JsonElement valueTo = category.get("ValueTo");
                int colorInt = Color.BLUE;
                int categoryId = category.get("Id").getAsInt();
                float valueFromFloat = valueFrom.getAsFloat();
                float valueToFloat = valueTo.getAsFloat();
                gradeCategoryList.add(
                        new GradeCategory(
                                profileId,
                                categoryId,
                                -1,
                                colorInt,
                                name instanceof JsonNull || name == null ? "" : name.getAsString()
                        ).setValueRange(valueFromFloat, valueToFloat)
                );
            }
            r("finish", "BehaviourGradesCategories");
        });
    }

    private void getGrades() {
        d(TAG, "Grades settings: "+enableStandardGrades+", "+enablePointGrades+", "+enableDescriptiveGrades);
        if (!enableStandardGrades && false) {
            // cancel only if grades have been disabled before
            // TODO do not cancel. this does not show any grades until a full sync happens. wtf
            // in KOTLIN api, maybe this will be forced when user synchronises this feature exclusively
            r("finish", "Grades");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_grades);
        apiRequest("Grades", data -> {
            if (data == null) {
                r("finish", "Grades");
                return;
            }
            JsonArray grades = data.get("Grades").getAsJsonArray();
            enableStandardGrades = grades.size() > 0;
            profile.putStudentData("enableStandardGrades", enableStandardGrades);
            if (!enableStandardGrades) {
                r("finish", "Grades");
                return;
            }
            //d("Got Grades: "+grades.toString());
            for (JsonElement gradeEl : grades) {
                JsonObject grade = gradeEl.getAsJsonObject();
                long id = grade.get("Id").getAsLong();
                long teacherId = grade.get("AddedBy").getAsJsonObject().get("Id").getAsLong();
                int semester = grade.get("Semester").getAsInt();
                long categoryId = grade.get("Category").getAsJsonObject().get("Id").getAsLong();
                long subjectId = grade.get("Subject").getAsJsonObject().get("Id").getAsLong();
                String name = grade.get("Grade").getAsString();
                float value = getGradeValue(name);

                String str_date = grade.get("AddDate").getAsString();
                long addedDate = Date.fromIso(str_date);

                float weight = 0.0f;
                String category = "";
                int color = -1;
                GradeCategory gradeCategory = GradeCategory.search(gradeCategoryList, categoryId);
                if (gradeCategory != null) {
                    weight = gradeCategory.weight;
                    category = gradeCategory.text;
                    color = gradeCategory.color;
                }

                if (name.equals("-") || name.equals("+") || name.equalsIgnoreCase("np") || name.equalsIgnoreCase("bz")) {
                    // fix for + and - grades that lower the average
                    weight = 0;
                }

                Grade gradeObject = new Grade(
                        profileId,
                        id,
                        category,
                        color,
                        "",
                        name,
                        value,
                        weight,
                        semester,
                        teacherId,
                        subjectId
                );

                if (grade.get("IsConstituent").getAsBoolean()) {
                    // normal grade
                    gradeObject.type = TYPE_NORMAL;
                }
                if (grade.get("IsSemester").getAsBoolean()) {
                    // semester final
                    gradeObject.type = (gradeObject.semester == 1 ? TYPE_SEMESTER1_FINAL : TYPE_SEMESTER2_FINAL);
                }
                else if (grade.get("IsSemesterProposition").getAsBoolean()) {
                    // semester proposed
                    gradeObject.type = (gradeObject.semester == 1 ? TYPE_SEMESTER1_PROPOSED : TYPE_SEMESTER2_PROPOSED);
                }
                else if (grade.get("IsFinal").getAsBoolean()) {
                    // year final
                    gradeObject.type = TYPE_YEAR_FINAL;
                }
                else if (grade.get("IsFinalProposition").getAsBoolean()) {
                    // year final
                    gradeObject.type = TYPE_YEAR_PROPOSED;
                }

                JsonElement historyEl = grade.get("Improvement");
                if (historyEl != null) {
                    JsonObject history = historyEl.getAsJsonObject();
                    long historicalId = history.get("Id").getAsLong();
                    for (Grade historicalGrade: gradeList) {
                        if (historicalGrade.id != historicalId)
                            continue;
                        // historicalGrade
                        historicalGrade.parentId = gradeObject.id;
                        if (historicalGrade.name.equals("nb")) {
                            historicalGrade.weight = 0;
                        }
                        break;
                    }
                    gradeObject.isImprovement = true;
                }

                /*if (RegisterGradeCategory.getById(app.register, registerGrade.categoryId, -1) == null) {
                    getGradesCategories = true;
                }*/

                gradeList.add(gradeObject);
                metadataList.add(new Metadata(profileId, Metadata.TYPE_GRADE, gradeObject.id, profile.getEmpty(), profile.getEmpty(), addedDate));
            }
            r("finish", "Grades");
        });
    }

    private void getPointGrades() {
        d(TAG, "Grades settings: "+enableStandardGrades+", "+enablePointGrades+", "+enableDescriptiveGrades);
        if (!enablePointGrades) {
            // cancel only if grades have been disabled before
            r("finish", "PointGrades");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_point_grades);
        apiRequest("PointGrades", data -> {
            if (data == null) {
                r("finish", "PointGrades");
                return;
            }
            JsonArray grades = data.get("Grades").getAsJsonArray();
            enablePointGrades = grades.size() > 0;
            profile.putStudentData("enablePointGrades", enablePointGrades);
            if (!enablePointGrades) {
                r("finish", "PointGrades");
                return;
            }
            //d("Got Grades: "+grades.toString());
            for (JsonElement gradeEl : grades) {
                JsonObject grade = gradeEl.getAsJsonObject();
                long id = grade.get("Id").getAsLong();
                long teacherId = grade.get("AddedBy").getAsJsonObject().get("Id").getAsLong();
                int semester = grade.get("Semester").getAsInt();
                long categoryId = grade.get("Category").getAsJsonObject().get("Id").getAsLong();
                long subjectId = grade.get("Subject").getAsJsonObject().get("Id").getAsLong();
                String name = grade.get("Grade").getAsString();
                float originalValue = grade.get("GradeValue").getAsFloat();

                String str_date = grade.get("AddDate").getAsString();
                long addedDate = Date.fromIso(str_date);

                float weight = 0.0f;
                String category = "";
                int color = -1;
                float value = 0.0f;
                float maxPoints = 0.0f;
                GradeCategory gradeCategory = GradeCategory.search(gradeCategoryList, categoryId);
                if (gradeCategory != null) {
                    weight = gradeCategory.weight;
                    category = gradeCategory.text;
                    color = gradeCategory.color;
                    maxPoints = gradeCategory.valueTo;
                    value = originalValue;
                }

                Grade gradeObject = new Grade(
                        profileId,
                        id,
                        category,
                        color,
                        "",
                        name,
                        value,
                        weight,
                        semester,
                        teacherId,
                        subjectId
                );
                gradeObject.type = Grade.TYPE_POINT;
                gradeObject.valueMax = maxPoints;

                gradeList.add(gradeObject);
                metadataList.add(new Metadata(profileId, Metadata.TYPE_GRADE, gradeObject.id, profile.getEmpty(), profile.getEmpty(), addedDate));
            }
            r("finish", "PointGrades");
        });
    }

    private void getDescriptiveGrades() {
        d(TAG, "Grades settings: "+enableStandardGrades+", "+enablePointGrades+", "+enableDescriptiveGrades);
        if (!enableDescriptiveGrades && !enableTextGrades) {
            // cancel only if grades have been disabled before
            r("finish", "DescriptiveGrades");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_descriptive_grades);
        apiRequest("BaseTextGrades", data -> {
            if (data == null) {
                r("finish", "DescriptiveGrades");
                return;
            }
            JsonArray grades = data.get("Grades").getAsJsonArray();
            int descriptiveGradesCount = 0;
            int textGradesCount = 0;
            //d("Got Grades: "+grades.toString());
            for (JsonElement gradeEl : grades) {
                JsonObject grade = gradeEl.getAsJsonObject();
                long id = grade.get("Id").getAsLong();
                long teacherId = grade.get("AddedBy").getAsJsonObject().get("Id").getAsLong();
                int semester = grade.get("Semester").getAsInt();
                long subjectId = grade.get("Subject").getAsJsonObject().get("Id").getAsLong();
                String description = grade.get("Grade").getAsString();

                long categoryId = -1;
                JsonElement categoryEl = grade.get("Category");
                JsonElement skillEl = grade.get("Skill");
                if (categoryEl != null) {
                    categoryId = categoryEl.getAsJsonObject().get("Id").getAsLong();
                    textGradesCount++;
                }
                if (skillEl != null) {
                    categoryId = skillEl.getAsJsonObject().get("Id").getAsLong();
                    descriptiveGradesCount++;
                }

                String str_date = grade.get("AddDate").getAsString();
                long addedDate = Date.fromIso(str_date);

                String category = "";
                int color = -1;
                GradeCategory gradeCategory = GradeCategory.search(gradeCategoryList, categoryId);
                if (gradeCategory != null) {
                    category = gradeCategory.text;
                    color = gradeCategory.color;
                }

                Grade gradeObject = new Grade(
                        profileId,
                        id,
                        category,
                        color,
                        description,
                        " ",
                        0.0f,
                        0,
                        semester,
                        teacherId,
                        subjectId
                );
                gradeObject.type = Grade.TYPE_DESCRIPTIVE;
                if (categoryEl != null) {
                    gradeObject.type = Grade.TYPE_TEXT;
                }
                if (skillEl != null) {
                    gradeObject.type = Grade.TYPE_DESCRIPTIVE;
                }

                gradeList.add(gradeObject);
                metadataList.add(new Metadata(profileId, Metadata.TYPE_GRADE, gradeObject.id, profile.getEmpty(), profile.getEmpty(), addedDate));
            }
            enableDescriptiveGrades = descriptiveGradesCount > 0;
            enableTextGrades = textGradesCount > 0;
            profile.putStudentData("enableDescriptiveGrades", enableDescriptiveGrades);
            profile.putStudentData("enableTextGrades", enableTextGrades);
            r("finish", "DescriptiveGrades");
        });
    }

    private void getTextGrades() {
        callback.onActionStarted(R.string.sync_action_syncing_descriptive_grades);
        apiRequest("DescriptiveGrades", data -> {
            if (data == null) {
                r("finish", "TextGrades");
                return;
            }
            JsonArray grades = data.get("Grades").getAsJsonArray();
            //d("Got Grades: "+grades.toString());
            for (JsonElement gradeEl : grades) {
                JsonObject grade = gradeEl.getAsJsonObject();
                long id = grade.get("Id").getAsLong();
                long teacherId = grade.get("AddedBy").getAsJsonObject().get("Id").getAsLong();
                int semester = grade.get("Semester").getAsInt();
                long subjectId = grade.get("Subject").getAsJsonObject().get("Id").getAsLong();
                JsonElement map = grade.get("Map");
                JsonElement realGrade = grade.get("RealGradeValue");
                String description = "";
                if (map != null) {
                    description = map.getAsString();
                }
                else if (realGrade != null) {
                    description = realGrade.getAsString();
                }

                long categoryId = -1;
                JsonElement skillEl = grade.get("Skill");
                if (skillEl != null) {
                    categoryId = skillEl.getAsJsonObject().get("Id").getAsLong();
                }

                String str_date = grade.get("AddDate").getAsString();
                long addedDate = Date.fromIso(str_date);

                String category = "";
                int color = -1;
                GradeCategory gradeCategory = GradeCategory.search(gradeCategoryList, categoryId);
                if (gradeCategory != null) {
                    category = gradeCategory.text;
                    color = gradeCategory.color;
                }

                Grade gradeObject = new Grade(
                        profileId,
                        id,
                        category,
                        color,
                        "",
                        description,
                        0.0f,
                        0,
                        semester,
                        teacherId,
                        subjectId
                );
                gradeObject.type = Grade.TYPE_DESCRIPTIVE;

                gradeList.add(gradeObject);
                metadataList.add(new Metadata(profileId, Metadata.TYPE_GRADE, gradeObject.id, profile.getEmpty(), profile.getEmpty(), addedDate));
            }
            r("finish", "TextGrades");
        });
    }

    private void getBehaviourGrades() {
        d(TAG, "Grades settings: "+enableStandardGrades+", "+enablePointGrades+", "+enableDescriptiveGrades);
        if (!enableBehaviourGrades) {
            // cancel only if grades have been disabled before
            r("finish", "BehaviourGrades");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_behaviour_grades);
        apiRequest("BehaviourGrades/Points", data -> {
            if (data == null) {
                r("finish", "BehaviourGrades");
                return;
            }
            JsonArray grades = data.get("Grades").getAsJsonArray();
            enableBehaviourGrades = grades.size() > 0;
            profile.putStudentData("enableBehaviourGrades", enableBehaviourGrades);
            if (!enableBehaviourGrades) {
                r("finish", "BehaviourGrades");
                return;
            }
            //d("Got Grades: "+grades.toString());
            DecimalFormat nameFormat = new DecimalFormat("#.##");

            Grade gradeStartSemester1 = new Grade(
                    profileId,
                    -1,
                    app.getString(R.string.grade_start_points),
                    0xffbdbdbd,
                    app.getString(R.string.grade_start_points_format, 1),
                    nameFormat.format(startPointsSemester1),
                    startPointsSemester1,
                    -1,
                    1,
                    -1,
                    1
            );
            gradeStartSemester1.type = Grade.TYPE_BEHAVIOUR;
            Grade gradeStartSemester2 = new Grade(
                    profileId,
                    -2,
                    app.getString(R.string.grade_start_points),
                    0xffbdbdbd,
                    app.getString(R.string.grade_start_points_format, 2),
                    nameFormat.format(startPointsSemester2),
                    startPointsSemester2,
                    -1,
                    2,
                    -1,
                    1
            );
            gradeStartSemester2.type = Grade.TYPE_BEHAVIOUR;

            gradeList.add(gradeStartSemester1);
            gradeList.add(gradeStartSemester2);
            metadataList.add(new Metadata(profileId, Metadata.TYPE_GRADE, -1, true, true, profile.getSemesterStart(1).getInMillis()));
            metadataList.add(new Metadata(profileId, Metadata.TYPE_GRADE, -2, true, true, profile.getSemesterStart(2).getInMillis()));

            for (JsonElement gradeEl : grades) {
                JsonObject grade = gradeEl.getAsJsonObject();
                long id = grade.get("Id").getAsLong();
                long teacherId = grade.get("AddedBy").getAsJsonObject().get("Id").getAsLong();
                int semester = grade.get("Semester").getAsInt();
                long categoryId = grade.get("Category").getAsJsonObject().get("Id").getAsLong();
                long subjectId = 1;

                float value = 0.0f;
                String name = "?";
                JsonElement nameValue;
                if ((nameValue = grade.get("Value")) != null) {
                    value = nameValue.getAsFloat();
                    name = value < 0 ? nameFormat.format(value) : "+"+nameFormat.format(value);
                }
                else if ((nameValue = grade.get("ShortName")) != null) {
                    name = nameValue.getAsString();
                }

                String str_date = grade.get("AddDate").getAsString();
                long addedDate = Date.fromIso(str_date);

                int color = value > 0 ? 16 : value < 0 ? 26 : 12;
                color = colors.get(color);

                String category = "";
                float maxPoints = 0.0f;
                GradeCategory gradeCategory = GradeCategory.search(gradeCategoryList, categoryId);
                if (gradeCategory != null) {
                    category = gradeCategory.text;
                    maxPoints = gradeCategory.valueTo;
                }

                Grade gradeObject = new Grade(
                        profileId,
                        id,
                        category,
                        color,
                        "",
                        name,
                        value,
                        -1,
                        semester,
                        teacherId,
                        subjectId
                );
                gradeObject.type = Grade.TYPE_BEHAVIOUR;
                gradeObject.valueMax = maxPoints;

                gradeList.add(gradeObject);
                metadataList.add(new Metadata(profileId, Metadata.TYPE_GRADE, gradeObject.id, profile.getEmpty(), profile.getEmpty(), addedDate));
            }
            r("finish", "BehaviourGrades");
        });
    }

    //private boolean eventTypeListChanged = false;
    private void getEvents() {
        //eventTypeList = app.db.eventTypeDao().getAllNow(profileId);
       // eventTypeListChanged = false;
        callback.onActionStarted(R.string.sync_action_syncing_events);
        apiRequest("HomeWorks", data -> {
            if (data == null) {
                r("finish", "Events");
                return;
            }
            JsonArray events = data.get("HomeWorks").getAsJsonArray();
            //d("Got Grades: "+events.toString());
            boolean getCustomTypes = false;
            try {
                for (JsonElement eventEl : events) {
                    JsonObject event = eventEl.getAsJsonObject();

                    JsonElement el;
                    JsonObject obj;

                    long id = event.get("Id").getAsLong();
                    long teacherId = -1;
                    long subjectId = -1;
                    int type = -1;

                    if ((el = event.get("CreatedBy")) != null
                            && (obj = el.getAsJsonObject()) != null
                            && (el = obj.get("Id")) != null) {
                        teacherId = el.getAsLong();
                    }
                    if ((el = event.get("Subject")) != null
                            && (obj = el.getAsJsonObject()) != null
                            && (el = obj.get("Id")) != null) {
                        subjectId = el.getAsLong();
                    }
                    String topic = event.get("Content").getAsString();

                    if ((el = event.get("Category")) != null
                            && (obj = el.getAsJsonObject()) != null
                            && (el = obj.get("Id")) != null) {
                        type = el.getAsInt();
                    }
                    /*EventType typeObject = app.db.eventTypeDao().getByIdNow(profileId, type);
                    if (typeObject == null) {
                        getCustomTypes = true;
                    }*/

                    JsonElement myClass = event.get("Class");
                    long teamId = myClass == null ? -1 : myClass.getAsJsonObject().get("Id").getAsLong();

                    String str_date = event.get("AddDate").getAsString();
                    long addedDate = Date.fromIso(str_date);

                    str_date = event.get("Date").getAsString();
                    Date eventDate = Date.fromY_m_d(str_date);


                    Time startTime = null;
                    JsonElement lessonNo;
                    JsonElement timeFrom;
                    if (!((lessonNo = event.get("LessonNo")) instanceof JsonNull)) {
                        Pair<Time, Time> timePair = lessonRanges.get(lessonNo.getAsInt());
                        if(timePair != null)
                            startTime = timePair.first;
                    }
                    if (startTime == null && !((timeFrom = event.get("TimeFrom")) instanceof JsonNull)) {
                        startTime = Time.fromH_m(timeFrom.getAsString());
                    }

                    Event eventObject = new Event(
                            profileId,
                            id,
                            eventDate,
                            startTime,
                            topic,
                            -1,
                            type,
                            false,
                            teacherId,
                            subjectId,
                            teamId
                    );

                    eventList.add(eventObject);
                    metadataList.add(new Metadata(profileId, Metadata.TYPE_EVENT, eventObject.id, profile.getEmpty(), profile.getEmpty(), addedDate));
                }
                r("finish", "Events");
            }
            catch (Exception e) {
                finishWithError(new AppError(TAG, 2541, CODE_OTHER, e, data));
            }
        });
    }

    private void getCustomTypes() {
        if (!fullSync) {
            r("finish", "CustomTypes");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_event_categories);
        apiRequest("HomeWorks/Categories", data -> {
            if (data == null) {
                r("finish", "CustomTypes");
                return;
            }
            JsonArray jCategories = data.get("Categories").getAsJsonArray();
            //d("Got Classrooms: "+jClassrooms.toString());
            try {
                for (JsonElement categoryEl : jCategories) {
                    JsonObject category = categoryEl.getAsJsonObject();
                    eventTypeList.add(new EventType(profileId, category.get("Id").getAsInt(), category.get("Name").getAsString(), colors.get(category.get("Color").getAsJsonObject().get("Id").getAsInt())));
                }
                r("finish", "CustomTypes");
            }
            catch (Exception e) {
                finishWithError(new AppError(TAG, 2573, CODE_OTHER, e, data));
            }
        });
    }

    private void getHomework() {
        if (!premium) {
            r("finish", "Homework");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_homework);
        apiRequest("HomeWorkAssignments", data -> {
            if (data == null) {
                r("finish", "Homework");
                return;
            }
            JsonArray homeworkList = data.get("HomeWorkAssignments").getAsJsonArray();
            //d("Got Grades: "+events.toString());
            try {
                for (JsonElement homeworkEl : homeworkList) {
                    JsonObject homework = homeworkEl.getAsJsonObject();

                    JsonElement el;
                    JsonObject obj;

                    long id = homework.get("Id").getAsLong();
                    long teacherId = -1;
                    long subjectId = -1;

                    if ((el = homework.get("Teacher")) != null
                            && (obj = el.getAsJsonObject()) != null
                            && (el = obj.get("Id")) != null) {
                        teacherId = el.getAsLong();
                    }

                    String topic = "";
                    try {
                        topic = homework.get("Topic").getAsString() + "\n";
                        topic += homework.get("Text").getAsString();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    String str_date = homework.get("Date").getAsString();
                    Date addedDate = Date.fromY_m_d(str_date);

                    str_date = homework.get("DueDate").getAsString();
                    Date eventDate = Date.fromY_m_d(str_date);


                    Time startTime = null;

                    Event eventObject = new Event(
                            profileId,
                            id,
                            eventDate,
                            startTime,
                            topic,
                            -1,
                            -1,
                            false,
                            teacherId,
                            subjectId,
                            -1
                    );

                    eventList.add(eventObject);
                    metadataList.add(new Metadata(profileId, Metadata.TYPE_EVENT, eventObject.id, profile.getEmpty(), profile.getEmpty(), addedDate.getInMillis()));
                }
                r("finish", "Homework");
            }
            catch (Exception e) {
                finishWithError(new AppError(TAG, 2648, CODE_OTHER, e, data));
            }
        });
    }

    private void getLuckyNumbers() {
        if (!profile.getLuckyNumberEnabled() || (profile.getLuckyNumberDate() != null && profile.getLuckyNumberDate().getValue() == Date.getToday().getValue())) {
            r("finish", "LuckyNumbers");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_lucky_number);
        apiRequest("LuckyNumbers", data -> {
            if (data == null) {
                profile.setLuckyNumberEnabled(false);
            }
            else {
                profile.setLuckyNumber(-1);
                profile.setLuckyNumberDate(Date.getToday());
                try {
                    JsonElement luckyNumberEl = data.get("LuckyNumber");
                    if (luckyNumberEl != null) {
                        JsonObject luckyNumber = luckyNumberEl.getAsJsonObject();
                        profile.setLuckyNumber(luckyNumber.get("LuckyNumber").getAsInt());
                        profile.setLuckyNumberDate(Date.fromY_m_d(luckyNumber.get("LuckyNumberDay").getAsString()));
                    }
                } catch (Exception e) {
                    finishWithError(new AppError(TAG, 2678, CODE_OTHER, e, data));
                }
                finally {
                    app.db.luckyNumberDao().add(new LuckyNumber(profileId, profile.getLuckyNumberDate(), profile.getLuckyNumber()));
                }
            }
            r("finish", "LuckyNumbers");
        });
    }

    private void getNotices() {
        callback.onActionStarted(R.string.sync_action_syncing_notices);
        apiRequest("Notes", data -> {
            if (data == null) {
                r("finish", "Notices");
                return;
            }
            try {
                JsonArray jNotices = data.get("Notes").getAsJsonArray();

                for (JsonElement noticeEl : jNotices) {
                    JsonObject notice = noticeEl.getAsJsonObject();

                    int type = notice.get("Positive").getAsInt();
                    switch (type) {
                        case 0:
                            type = TYPE_NEGATIVE;
                            break;
                        case 1:
                            type = TYPE_POSITIVE;
                            break;
                        case 2:
                            type = TYPE_NEUTRAL;
                            break;
                    }

                    long id = notice.get("Id").getAsLong();

                    Date addedDate = Date.fromY_m_d(notice.get("Date").getAsString());

                    int semester = profile.dateToSemester(addedDate);

                    JsonElement el;
                    JsonObject obj;
                    long teacherId = -1;
                    if ((el = notice.get("Teacher")) != null
                            && (obj = el.getAsJsonObject()) != null
                            && (el = obj.get("Id")) != null) {
                        teacherId = el.getAsLong();
                    }

                    Notice noticeObject = new Notice(
                            profileId,
                            id,
                            notice.get("Text").getAsString(),
                            semester,
                            type,
                            teacherId
                    );

                    noticeList.add(noticeObject);
                    metadataList.add(new Metadata(profileId, Metadata.TYPE_NOTICE, noticeObject.id, profile.getEmpty(), profile.getEmpty(), addedDate.getInMillis()));
                }
                r("finish", "Notices");
            }
            catch (Exception e) {
                finishWithError(new AppError(TAG, 2750, CODE_OTHER, e, data));
            }
        });
    }

    private SparseArray<Pair<Integer, String>> attendanceTypes = new SparseArray<>();
    private void getAttendanceTypes() {
        callback.onActionStarted(R.string.sync_action_syncing_attendance_types);
        apiRequest("Attendances/Types", data -> {
            if (data == null) {
                r("finish", "AttendanceTypes");
                return;
            }
            try {
                JsonArray jTypes = data.get("Types").getAsJsonArray();
                for (JsonElement typeEl : jTypes) {
                    JsonObject type = typeEl.getAsJsonObject();
                    int id = type.get("Id").getAsInt();
                    attendanceTypes.put(id,
                            new Pair<>(
                                    type.get("Standard").getAsBoolean() ? id : type.getAsJsonObject("StandardType").get("Id").getAsInt(),
                                    type.get("Name").getAsString()
                            )
                    );
                }
                r("finish", "AttendanceTypes");
            }
            catch (Exception e) {
                finishWithError(new AppError(TAG, 2782, CODE_OTHER, e, data));
            }
        });
    }

    private void getAttendance() {
        callback.onActionStarted(R.string.sync_action_syncing_attendance);
        apiRequest("Attendances"+(fullSync ? "" : "?dateFrom="+ Date.getToday().stepForward(0, -1, 0).getStringY_m_d()), data -> {
            if (data == null) {
                r("finish", "Attendance");
                return;
            }

            try {
                JsonArray jAttendance = data.get("Attendances").getAsJsonArray();

                for (JsonElement attendanceEl : jAttendance) {
                    JsonObject attendance = attendanceEl.getAsJsonObject();

                    int type = attendance.getAsJsonObject("Type").get("Id").getAsInt();
                    Pair<Integer, String> attendanceType;
                    if ((attendanceType = attendanceTypes.get(type)) != null) {
                        type = attendanceType.first;
                    }
                    switch (type) {
                        case 1:
                            type = TYPE_ABSENT;
                            break;
                        case 2:
                            type = TYPE_BELATED;
                            break;
                        case 3:
                            type = TYPE_ABSENT_EXCUSED;
                            break;
                        case 4:
                            type = TYPE_RELEASED;
                            break;
                        default:
                        case 100:
                            type = TYPE_PRESENT;
                            break;
                    }

                    String idStr = attendance.get("Id").getAsString();
                    int id = strToInt(idStr.replaceAll("[^\\d.]", ""));

                    long addedDate = Date.fromIso(attendance.get("AddDate").getAsString());

                    Time startTime = Time.getNow();
                    Pair<Time, Time> timePair = lessonRanges.get(attendance.get("LessonNo").getAsInt());
                    if (timePair != null)
                        startTime = timePair.first;
                    Date lessonDate = Date.fromY_m_d(attendance.get("Date").getAsString());
                    int lessonWeekDay = lessonDate.getWeekDay();
                    long subjectId = -1;
                    String topic = "";
                    if (attendanceType != null) {
                        topic = attendanceType.second;
                    }

                    for (Lesson lesson: lessonList) {
                        if (lesson.weekDay == lessonWeekDay && lesson.startTime.getValue() == startTime.getValue()) {
                            subjectId = lesson.subjectId;
                        }
                    }

                    Attendance attendanceObject = new Attendance(
                            profileId,
                            id,
                            attendance.getAsJsonObject("AddedBy").get("Id").getAsLong(),
                            subjectId,
                            attendance.get("Semester").getAsInt(),
                            topic,
                            lessonDate,
                            startTime,
                            type);

                    attendanceList.add(attendanceObject);
                    if (attendanceObject.type != TYPE_PRESENT) {
                        metadataList.add(new Metadata(profileId, Metadata.TYPE_ATTENDANCE, attendanceObject.id, profile.getEmpty(), profile.getEmpty(), addedDate));
                    }
                }
                r("finish", "Attendance");
            }
            catch (Exception e) {
                finishWithError(new AppError(TAG, 2872, CODE_OTHER, e, data));
            }
        });
    }

    private void getAnnouncements() {
        callback.onActionStarted(R.string.sync_action_syncing_announcements);
        apiRequest("SchoolNotices", data -> {
            if (data == null) {
                r("finish", "Announcements");
                return;
            }
            try {
                JsonArray jAnnouncements = data.get("SchoolNotices").getAsJsonArray();

                for (JsonElement announcementEl : jAnnouncements) {
                    JsonObject announcement = announcementEl.getAsJsonObject();

                    String idStr = announcement.get("Id").getAsString();
                    long id = crc16(idStr.getBytes());

                    long addedDate = Date.fromIso(announcement.get("CreationDate").getAsString());

                    boolean read = announcement.get("WasRead").getAsBoolean();

                    String subject = "";
                    String text = "";
                    Date startDate = null;
                    Date endDate = null;
                    try {
                        subject = announcement.get("Subject").getAsString();
                        text = announcement.get("Content").getAsString();
                        startDate = Date.fromY_m_d(announcement.get("StartDate").getAsString());
                        endDate = Date.fromY_m_d(announcement.get("EndDate").getAsString());
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    JsonElement el;
                    JsonObject obj;
                    long teacherId = -1;
                    if ((el = announcement.get("AddedBy")) != null
                            && (obj = el.getAsJsonObject()) != null
                            && (el = obj.get("Id")) != null) {
                        teacherId = el.getAsLong();
                    }

                    Announcement announcementObject = new Announcement(
                            profileId,
                            id,
                            subject,
                            text,
                            startDate,
                            endDate,
                            teacherId
                    );

                    announcementList.add(announcementObject);
                    metadataList.add(new Metadata(profileId, Metadata.TYPE_ANNOUNCEMENT, announcementObject.id, read, read, addedDate));
                }
                r("finish", "Announcements");
            }
            catch (Exception e) {
                finishWithError(new AppError(TAG, 2944, CODE_OTHER, e, data));
            }
        });
    }

    private void getPtMeetings() {
        if (!fullSync) {
            r("finish", "PtMeetings");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_pt_meetings);
        apiRequest("ParentTeacherConferences", data -> {
            if (data == null) {
                r("finish", "PtMeetings");
                return;
            }
            try {
                JsonArray jMeetings = data.get("ParentTeacherConferences").getAsJsonArray();
                for (JsonElement meetingEl: jMeetings) {
                    JsonObject meeting = meetingEl.getAsJsonObject();

                    long id = meeting.get("Id").getAsLong();
                    Event eventObject = new Event(
                            profileId,
                            id,
                            Date.fromY_m_d(meeting.get("Date").getAsString()),
                            Time.fromH_m(meeting.get("Time").getAsString()),
                            meeting.get("Topic").getAsString(),
                            -1,
                            TYPE_PT_MEETING,
                            false,
                            meeting.getAsJsonObject("Teacher").get("Id").getAsLong(),
                            -1,
                            teamClassId
                    );
                    eventList.add(eventObject);
                    metadataList.add(new Metadata(profileId, Metadata.TYPE_EVENT, eventObject.id, profile.getEmpty(), profile.getEmpty(), System.currentTimeMillis()));
                }
                r("finish", "PtMeetings");
            }
            catch (Exception e) {
                finishWithError(new AppError(TAG, 2996, CODE_OTHER, e, data));
            }
        });
    }

    private SparseArray<String> teacherFreeDaysTypes = new SparseArray<>();
    private void getTeacherFreeDaysTypes() {
        callback.onActionStarted(R.string.sync_action_syncing_teacher_free_days_types);
        apiRequest("TeacherFreeDays/Types", data -> {
            if (data == null) {
                r("finish", "TeacherFreeDays");
                return;
            }
            try {
                JsonArray jTypes = data.get("Types").getAsJsonArray();
                for (JsonElement typeEl : jTypes) {
                    JsonObject type = typeEl.getAsJsonObject();
                    int id = type.get("Id").getAsInt();
                    teacherFreeDaysTypes.put(id, type.get("Name").getAsString());
                }
                r("finish", "TeacherFreeDaysTypes");
            }
            catch (Exception e) {
                finishWithError(new AppError(TAG, 3019, CODE_OTHER, e, data));
            }
        });
    }

    private void getTeacherFreeDays() {
        callback.onActionStarted(R.string.sync_action_syncing_teacher_free_days);
        apiRequest("TeacherFreeDays", data -> {
            if (data == null) {
                r("finish", "TeacherFreeDays");
                return;
            }
            try {
                JsonArray jFreeDays = data.get("TeacherFreeDays").getAsJsonArray();
                for (JsonElement freeDayEl: jFreeDays) {
                    JsonObject freeDay = freeDayEl.getAsJsonObject();

                    long id = freeDay.get("Id").getAsLong();

                    Date dateFrom = Date.fromY_m_d(freeDay.get("DateFrom").getAsString());
                    Date dateTo = Date.fromY_m_d(freeDay.get("DateTo").getAsString());

                    int type = freeDay.getAsJsonObject("Type").get("Id").getAsInt();
                    String topic = teacherFreeDaysTypes.get(type)+"\n"+(dateFrom.getValue() != dateTo.getValue() ? dateFrom.getFormattedString()+" - "+dateTo.getFormattedString() : "");
                    Event eventObject = new Event(
                            profileId,
                            id,
                            dateFrom,
                            null,
                            topic,
                            -1,
                            TYPE_TEACHER_ABSENCE,
                            false,
                            freeDay.getAsJsonObject("Teacher").get("Id").getAsLong(),
                            -1,
                            -1
                    );
                    eventList.add(eventObject);
                    metadataList.add(new Metadata(profileId, Metadata.TYPE_EVENT, eventObject.id, profile.getEmpty(), profile.getEmpty(), System.currentTimeMillis()));
                }
                r("finish", "TeacherFreeDays");
            }
            catch (Exception e) {
                finishWithError(new AppError(TAG, 3069, CODE_OTHER, e, data));
            }
        });
    }

    private void getSchoolFreeDays() {
        callback.onActionStarted(R.string.sync_action_syncing_school_free_days);
        apiRequest("SchoolFreeDays" + (unitId != -1 ? "?unit=" + unitId : ""), data -> {
            if (data == null) {
                r("finish", "SchoolFreeDays");
                return;
            }
            try {
                JsonArray jFreeDays = data.get("SchoolFreeDays").getAsJsonArray();

                for (JsonElement freeDayEl: jFreeDays) {
                    continue;
                }
                r("finish", "SchoolFreeDays");
            } catch (Exception e) {
                finishWithError(new AppError(TAG, 3069, CODE_OTHER, e, data));
            }
        });
    }

    private void getMessagesLogin() {
        if (synergiaPassword == null) {
            // skip messages
            r("finish", "MessagesOutbox");
            return;
        }
        loginSynergia(() -> {
            r("finish", "MessagesLogin");
        });
    }

    private void getMessagesInbox() {
        String body =
                "<service>\n" +
                "  <header/>\n" +
                "  <data>\n" +
                "    <archive>0</archive>\n" +
                "  </data>\n" +
                "</service>";
        synergiaRequest("Inbox/action/GetList", body, data -> {
            try {
                long startTime = System.currentTimeMillis();

                for (Element e: data.select("response GetList data ArrayItem")) {

                    long id = Long.parseLong(e.select("messageId").text());

                    String subject = e.select("topic").text();

                    String senderFirstName = e.select("senderFirstName").text();
                    String senderLastName = e.select("senderLastName").text();

                    long senderId = -1;

                    for (Teacher teacher: teacherList) {
                        if (teacher.name.equalsIgnoreCase(senderFirstName) && teacher.surname.equalsIgnoreCase(senderLastName)) {
                            senderId = teacher.id;
                            break;
                        }
                    }
                    if (senderId == -1) {
                        Teacher teacher = new Teacher(profileId, -1 * Utils.crc16((senderFirstName+" "+senderLastName).getBytes()), senderFirstName, senderLastName);
                        senderId = teacher.id;
                        teacherList.add(teacher);
                        teacherListChanged = true;
                    }

                    long readDate = 0;
                    long sentDate;

                    String readDateStr = e.select("readDate").text();
                    String sentDateStr = e.select("sendDate").text();

                    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                    if (!readDateStr.isEmpty()) {
                        readDate = formatter.parse(readDateStr).getTime();
                    }
                    sentDate = formatter.parse(sentDateStr).getTime();

                    Message message = new Message(
                            profileId,
                            id,
                            subject,
                            null,
                            TYPE_RECEIVED,
                            senderId,
                            -1
                    );

                    MessageRecipient messageRecipient = new MessageRecipient(
                            profileId,
                            -1 /* me */,
                            -1,
                            readDate,
                            /*messageId*/ id
                    );

                    if (!e.select("isAnyFileAttached").text().equals("0"))
                        message.setHasAttachments();

                    messageList.add(message);
                    messageRecipientList.add(messageRecipient);
                    messageMetadataList.add(new Metadata(profileId, Metadata.TYPE_MESSAGE, message.id, readDate > 0, readDate > 0 || profile.getEmpty(), sentDate));
                }

            } catch (Exception e3) {
                finishWithError(new AppError(TAG, 3164, CODE_OTHER, e3, data.outerHtml()));
                return;
            }

            r("finish", "MessagesInbox");
        });
    }

    private void getMessagesOutbox() {
        if (!fullSync && onlyFeature != FEATURE_MESSAGES_OUTBOX && !profile.getEmpty()) {
            // a quick sync and the profile is already synced at least once
            r("finish", "MessagesOutbox");
            return;
        }
        String body =
                "<service>\n" +
                        "  <header/>\n" +
                        "  <data>\n" +
                        "    <archive>0</archive>\n" +
                        "  </data>\n" +
                        "</service>";
        synergiaRequest("Outbox/action/GetList", body, data -> {
            try {
                long startTime = System.currentTimeMillis();

                for (Element e: data.select("response GetList data ArrayItem")) {

                    long id = Long.parseLong(e.select("messageId").text());

                    String subject = e.select("topic").text();

                    String receiverFirstName = e.select("receiverFirstName").text();
                    String receiverLastName = e.select("receiverLastName").text();

                    long receiverId = -1;

                    for (Teacher teacher: teacherList) {
                        if (teacher.name.equalsIgnoreCase(receiverFirstName) && teacher.surname.equalsIgnoreCase(receiverLastName)) {
                            receiverId = teacher.id;
                            break;
                        }
                    }
                    if (receiverId == -1) {
                        Teacher teacher = new Teacher(profileId, -1 * Utils.crc16((receiverFirstName+" "+receiverLastName).getBytes()), receiverFirstName, receiverLastName);
                        receiverId = teacher.id;
                        teacherList.add(teacher);
                        teacherListChanged = true;
                    }

                    long sentDate;

                    String sentDateStr = e.select("sendDate").text();

                    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                    sentDate = formatter.parse(sentDateStr).getTime();

                    Message message = new Message(
                            profileId,
                            id,
                            subject,
                            null,
                            TYPE_SENT,
                            -1,
                            -1
                    );

                    MessageRecipient messageRecipient = new MessageRecipient(
                            profileId,
                            receiverId,
                            -1,
                            -1,
                            /*messageId*/ id
                    );

                    if (!e.select("isAnyFileAttached").text().equals("0"))
                        message.setHasAttachments();

                    messageList.add(message);
                    messageRecipientIgnoreList.add(messageRecipient);
                    metadataList.add(new Metadata(profileId, Metadata.TYPE_MESSAGE, message.id, true, true, sentDate));
                }

            } catch (Exception e3) {
                finishWithError(new AppError(TAG, 3270, CODE_OTHER, e3, data.outerHtml()));
                return;
            }

            r("finish", "MessagesOutbox");
        });
    }

    @Override
    public Map<String, Endpoint> getConfigurableEndpoints(Profile profile) {
        Map<String, Endpoint> configurableEndpoints = new LinkedHashMap<>();
        configurableEndpoints.put("Classrooms", new Endpoint("Classrooms",true, false, profile.getChangedEndpoints()));
        configurableEndpoints.put("Timetables", new Endpoint("Timetables",true, false, profile.getChangedEndpoints()));
        configurableEndpoints.put("Substitutions", new Endpoint("Substitutions",true, false, profile.getChangedEndpoints()));
        configurableEndpoints.put("Grades", new Endpoint("Grades",true, false, profile.getChangedEndpoints()));
        configurableEndpoints.put("PointGrades", new Endpoint("PointGrades",true, false, profile.getChangedEndpoints()));
        configurableEndpoints.put("Events", new Endpoint("Events",true, false, profile.getChangedEndpoints()));
        configurableEndpoints.put("Homework", new Endpoint("Homework",true, false, profile.getChangedEndpoints()));
        configurableEndpoints.put("LuckyNumbers", new Endpoint("LuckyNumbers",true, false, profile.getChangedEndpoints()));
        configurableEndpoints.put("Notices", new Endpoint("Notices",true, false, profile.getChangedEndpoints()));
        configurableEndpoints.put("Attendance", new Endpoint("Attendance",true, false, profile.getChangedEndpoints()));
        configurableEndpoints.put("Announcements", new Endpoint("Announcements",true, true, profile.getChangedEndpoints()));
        configurableEndpoints.put("PtMeetings", new Endpoint("PtMeetings",true, true, profile.getChangedEndpoints()));
        configurableEndpoints.put("TeacherFreeDays", new Endpoint("TeacherFreeDays",false, false, profile.getChangedEndpoints()));
        //configurableEndpoints.put("SchoolFreeDays", new Endpoint("SchoolFreeDays",true, true, profile.changedEndpoints));
        //configurableEndpoints.put("ClassFreeDays", new Endpoint("ClassFreeDays",true, true, profile.changedEndpoints));
        configurableEndpoints.put("MessagesInbox", new Endpoint("MessagesInbox", true, false, profile.getChangedEndpoints()));
        configurableEndpoints.put("MessagesOutbox", new Endpoint("MessagesOutbox", true, true, profile.getChangedEndpoints()));
        return configurableEndpoints;
    }

    @Override
    public boolean isEndpointEnabled(Profile profile, boolean defaultActive, String name) {
        return defaultActive ^ contains(profile.getChangedEndpoints(), name);
    }

    @Override
    public void syncMessages(@NonNull Context activityContext, @NonNull SyncCallback errorCallback, @NonNull ProfileFull profile)
    {
        if (!prepare(activityContext, errorCallback, profile.getId(), profile, LoginStore.fromProfileFull(profile)))
            return;

        login(() -> {
            Librus.this.profile.setEmpty(true);
            targetEndpoints = new ArrayList<>();
            targetEndpoints.add("Users");
            targetEndpoints.add("MessagesLogin");
            targetEndpoints.add("MessagesInbox");
            targetEndpoints.add("MessagesOutbox");
            targetEndpoints.add("Finish");
            PROGRESS_COUNT = targetEndpoints.size()-1;
            PROGRESS_STEP = (90/PROGRESS_COUNT);
            begin();
        });
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
    public void getMessage(@NonNull Context activityContext, @NonNull SyncCallback errorCallback, @NonNull ProfileFull profile, @NonNull MessageFull message, @NonNull MessageGetCallback messageCallback)
    {
        if (message.body != null) {
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
                new Handler(activityContext.getMainLooper()).post(() -> {
                    messageCallback.onSuccess(message);
                });
                return;
            }
        }

        if (!prepare(activityContext, errorCallback, profile.getId(), profile, LoginStore.fromProfileFull(profile)))
            return;

        loginSynergia(() -> {
            String requestBody =
                    "<service>\n" +
                            "  <header/>\n" +
                            "  <data>\n" +
                            "    <messageId>"+message.id+"</messageId>\n" +
                            "    <archive>0</archive>\n" +
                            "  </data>\n" +
                            "</service>";
            synergiaRequest("GetMessage", requestBody, data -> {

                List<MessageRecipientFull> messageRecipientList = new ArrayList<>();

                try {
                    Element e = data.select("response GetMessage data").first();

                    String body = e.select("Message").text();
                    body = new String(Base64.decode(body, Base64.DEFAULT));
                    body = body.replaceAll("\n", "<br>");
                    body = body.replaceAll("<!\\[CDATA\\[", "");
                    body = body.replaceAll("]]>", "");

                    message.clearAttachments();
                    Elements attachments = e.select("attachments ArrayItem");
                    if (attachments != null) {
                        for (Element attachment: attachments) {
                            message.addAttachment(Long.parseLong(attachment.select("id").text()), attachment.select("filename").text(), -1);
                        }
                    }

                    message.body = body;

                    if (message.type == TYPE_RECEIVED) {
                        app.db.teacherDao().updateLoginId(profileId, message.senderId, e.select("senderId").text());

                        MessageRecipientFull recipient = new MessageRecipientFull(profileId, -1, message.id);

                        long readDate = 0;
                        String readDateStr = e.select("readDate").text();
                        if (!readDateStr.isEmpty()) {
                            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                            readDate = formatter.parse(readDateStr).getTime();
                        }
                        recipient.readDate = readDate;

                        recipient.fullName = profile.getStudentNameLong();
                        messageRecipientList.add(recipient);

                    }
                    else if (message.type == TYPE_SENT) {
                        List<Teacher> teacherList = app.db.teacherDao().getAllNow(profileId);
                        for (Element receiver: e.select("receivers ArrayItem")) {
                            String receiverFirstName = e.select("firstName").text();
                            String receiverLastName = e.select("lastName").text();

                            long receiverId = -1;

                            for (Teacher teacher: teacherList) {
                                if (teacher.name.equalsIgnoreCase(receiverFirstName) && teacher.surname.equalsIgnoreCase(receiverLastName)) {
                                    receiverId = teacher.id;
                                    break;
                                }
                            }

                            app.db.teacherDao().updateLoginId(profileId, receiverId, receiver.select("receiverId").text());

                            MessageRecipientFull recipient = new MessageRecipientFull(profileId, receiverId, message.id);

                            long readDate = 0;
                            String readDateStr = e.select("readed").text();
                            if (!readDateStr.isEmpty()) {
                                DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                                readDate = formatter.parse(readDateStr).getTime();
                            }
                            recipient.readDate = readDate;

                            recipient.fullName = receiverFirstName+" "+receiverLastName;
                            messageRecipientList.add(recipient);
                        }
                    }

                }
                catch (Exception e) {
                    finishWithError(new AppError(TAG, 795, CODE_OTHER, e, data.outerHtml()));
                    return;
                }

                if (!message.seen) {
                    app.db.metadataDao().setSeen(profileId, message, true);
                }
                app.db.messageDao().add(message);
                app.db.messageRecipientDao().addAll((List<MessageRecipient>)(List<?>) messageRecipientList); // not addAllIgnore

                message.recipients = messageRecipientList;

                new Handler(activityContext.getMainLooper()).post(() -> {
                    messageCallback.onSuccess(message);
                });
            });
        });
    }

    @Override
    public void getAttachment(@NonNull Context activityContext, @NonNull SyncCallback errorCallback, @NonNull ProfileFull profile, @NonNull MessageFull message, long attachmentId, @NonNull AttachmentGetCallback attachmentCallback) {
        if (!prepare(activityContext, errorCallback, profile.getId(), profile, LoginStore.fromProfileFull(profile)))
            return;

        loginSynergia(() -> {
            String requestBody =
                    "<service>\n" +
                            "  <header/>\n" +
                            "  <data>\n" +
                            "    <fileId>"+attachmentId+"</fileId>\n" +
                            "    <msgId>"+message.id+"</msgId>\n" +
                            "    <archive>0</archive>\n" +
                            "  </data>\n" +
                            "</service>";
            synergiaRequest("GetFileDownloadLink", requestBody, data -> {
                String downloadLink = data.select("response GetFileDownloadLink downloadLink").text();
                Matcher keyMatcher = Pattern.compile("singleUseKey=([0-9A-f_]+)").matcher(downloadLink);
                if (keyMatcher.find()) {
                    getAttachmentCheckKeyTries = 0;
                    getAttachmentCheckKey(keyMatcher.group(1), attachmentCallback);
                }
                else {
                    finishWithError(new AppError(TAG, 629, CODE_OTHER, "Błąd pobierania tokenu. Skontaktuj się z twórcą aplikacji.", data.outerHtml()));
                }
            });
        });
    }
    private int getAttachmentCheckKeyTries = 0;
    private void getAttachmentCheckKey(String attachmentKey, AttachmentGetCallback attachmentCallback) {
        Request.builder()
                .url(SYNERGIA_SANDBOX_URL+"CSCheckKey")
                .userAgent(synergiaUserAgent)
                .addParameter("singleUseKey", attachmentKey)
                .post()
                .callback(new JsonCallbackHandler() {
                    @Override
                    public void onSuccess(JsonObject data, Response response) {
                        if (data == null) {
                            finishWithError(new AppError(TAG, 645, AppError.CODE_MAINTENANCE, response));
                            return;
                        }
                        try {
                            String status = data.get("status").getAsString();
                            if (status.equals("not_downloaded_yet")) {
                                if (getAttachmentCheckKeyTries++ > 5) {
                                    finishWithError(new AppError(TAG, 658, CODE_OTHER, "Załącznik niedostępny. Przekroczono czas oczekiwania.", response, data));
                                    return;
                                }
                                new Handler(activityContext.getMainLooper()).postDelayed(() -> {
                                    getAttachmentCheckKey(attachmentKey, attachmentCallback);
                                }, 2000);
                            }
                            else if (status.equals("ready")) {
                                Request.Builder builder = Request.builder()
                                        .url(SYNERGIA_SANDBOX_URL+"CSDownload&singleUseKey="+attachmentKey);
                                new Handler(activityContext.getMainLooper()).post(() -> {
                                    attachmentCallback.onSuccess(builder);
                                });
                            }
                            else {
                                finishWithError(new AppError(TAG, 667, AppError.CODE_ATTACHMENT_NOT_AVAILABLE, response, data));
                            }
                        }
                        catch (Exception e) {
                            finishWithError(new AppError(TAG, 671, AppError.CODE_OTHER, response, e, data));
                        }
                    }

                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        finishWithError(new AppError(TAG, 677, CODE_OTHER, response, throwable));
                    }
                })
                .build()
                .enqueue();
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

        loginSynergia(() -> {
            String requestBody =
                    "<service>\n" +
                            "  <header/>\n" +
                            "  <data>\n" +
                            "    <includeClass>1</includeClass>\n" +
                            "  </data>\n" +
                            "</service>";
            synergiaRequest("Receivers/action/GetTypes", requestBody, data -> {

                teacherList = app.db.teacherDao().getAllNow(profileId);

                for (Teacher teacher: teacherList) {
                    teacher.typeDescription = null; // TODO: 2019-06-13 it better
                }

                Elements categories = data.select("response GetTypes data list ArrayItem");
                for (Element category: categories) {
                    String categoryId = category.select("id").text();
                    String categoryName = category.select("name").text();
                    Elements categoryList = getRecipientCategory(categoryId);
                    if (categoryList == null)
                        return; // the error callback is already executed
                    for (Element item: categoryList) {
                        if (item.select("list").size() == 1) {
                            String className = item.select("label").text();
                            Elements list = item.select("list ArrayItem");
                            for (Element teacher: list) {
                                updateTeacher(categoryId, Long.parseLong(teacher.select("id").text()), teacher.select("label").text(), categoryName, className);
                            }
                        }
                        else {
                            updateTeacher(categoryId, Long.parseLong(item.select("id").text()), item.select("label").text(), categoryName, null);
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
    private Elements getRecipientCategory(String categoryId) {
        Response response = null;
        try {
            String endpoint = "Receivers/action/GetListForType";
            d(TAG, "Requesting "+SYNERGIA_URL+endpoint);
            String body =
                    "<service>\n" +
                            "  <header/>\n" +
                            "  <data>\n" +
                            "    <receiverType>"+categoryId+"</receiverType>\n" +
                            "  </data>\n" +
                            "</service>";
            response = Request.builder()
                    .url(SYNERGIA_URL+endpoint)
                    .userAgent(synergiaUserAgent)
                    .setTextBody(body, MediaTypeUtils.APPLICATION_XML)
                    .build().execute();
            if (response.code() != 200) {
                finishWithError(new AppError(TAG, 3569, CODE_OTHER, response));
                return null;
            }
            String data = new TextCallbackHandler().backgroundParser(response);
            if (data.contains("<status>error</status>") || data.contains("<error>")) {
                finishWithError(new AppError(TAG, 3556, AppError.CODE_MAINTENANCE, response, data));
                return null;
            }
            Document doc = Jsoup.parse(data, "", Parser.xmlParser());
            return doc.select("response GetListForType data ArrayItem");
        } catch (Exception e) {
            finishWithError(new AppError(TAG, 3562, CODE_OTHER, response, e));
            return null;
        }
    }
    private void updateTeacher(String category, long loginId, String nameLastFirst, String typeDescription, String className) {
        nameLastFirst = nameLastFirst.replaceAll("\\s+", " ");
        int type = TYPE_OTHER;
        String position;
        switch (category) {
            case "tutors":
                type = TYPE_EDUCATOR;
                break;
            case "teachers":
                type = TYPE_TEACHER;
                break;
            case "pedagogue":
                type = TYPE_PEDAGOGUE;
                break;
            case "librarian":
                type = TYPE_LIBRARIAN;
                break;
            case "admin":
                type = TYPE_SCHOOL_ADMIN;
                break;
            case "secretary":
                type = TYPE_SECRETARIAT;
                break;
            case "sadmin":
                type = TYPE_SUPER_ADMIN;
                break;
            case "parentsCouncil":
                type = TYPE_PARENTS_COUNCIL;
                int index = nameLastFirst.indexOf(" - ");
                position = index == -1 ? "" : nameLastFirst.substring(index+3);
                nameLastFirst = index == -1 ? nameLastFirst : nameLastFirst.substring(0, index);
                typeDescription = bs(className)+bs(": ", position);
                break;
            case "schoolParentsCouncil":
                type = TYPE_SCHOOL_PARENTS_COUNCIL;
                index = nameLastFirst.indexOf(" - ");
                position = index == -1 ? "" : nameLastFirst.substring(index+3);
                nameLastFirst = index == -1 ? nameLastFirst : nameLastFirst.substring(0, index);
                typeDescription = bs(position);
                break;
            case "contactsGroups":
                return;
        }
        Teacher teacher = Teacher.getByFullNameLastFirst(teacherList, nameLastFirst);
        if (teacher == null) {
            String[] nameParts = nameLastFirst.split(" ", Integer.MAX_VALUE);
            teacher = new Teacher(profileId, -1 * Utils.crc16((nameParts.length > 1 ? nameParts[1]+" "+nameParts[0] : nameParts[0]).getBytes()), nameParts.length > 1 ? nameParts[1] : "", nameParts[0]);
            teacherList.add(teacher);
        }
        teacher.loginId = String.valueOf(loginId);
        teacher.type = 0;
        teacher.setType(type);
        if (type == TYPE_OTHER) {
            teacher.typeDescription = typeDescription+bs(" ", teacher.typeDescription);
        }
        else {
            teacher.typeDescription = typeDescription;
        }
    }

    @Override
    public MessagesComposeInfo getComposeInfo(@NonNull ProfileFull profile) {
        return new MessagesComposeInfo(0, 0, 150, 20000);
    }
}
