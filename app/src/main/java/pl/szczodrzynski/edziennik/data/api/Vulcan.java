package pl.szczodrzynski.edziennik.data.api;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Base64;
import android.util.Pair;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import im.wangchao.mhttp.Request;
import im.wangchao.mhttp.Response;
import im.wangchao.mhttp.callback.JsonCallbackHandler;
import okhttp3.OkHttpClient;
import okio.Buffer;
import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.api.interfaces.AttachmentGetCallback;
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikInterface;
import pl.szczodrzynski.edziennik.data.api.interfaces.LoginCallback;
import pl.szczodrzynski.edziennik.data.api.interfaces.MessageGetCallback;
import pl.szczodrzynski.edziennik.data.api.interfaces.RecipientListGetCallback;
import pl.szczodrzynski.edziennik.data.api.interfaces.SyncCallback;
import pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance;
import pl.szczodrzynski.edziennik.data.db.modules.events.Event;
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade;
import pl.szczodrzynski.edziennik.data.db.modules.grades.GradeCategory;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.Lesson;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange;
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore;
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

import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_OTHER;
import static pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance.TYPE_ABSENT;
import static pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance.TYPE_ABSENT_EXCUSED;
import static pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance.TYPE_BELATED;
import static pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance.TYPE_BELATED_EXCUSED;
import static pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance.TYPE_PRESENT;
import static pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance.TYPE_RELEASED;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_EXAM;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_SHORT_QUIZ;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_SEMESTER1_FINAL;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_SEMESTER1_PROPOSED;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_SEMESTER2_FINAL;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_SEMESTER2_PROPOSED;
import static pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange.TYPE_CANCELLED;
import static pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange.TYPE_CHANGE;
import static pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore.LOGIN_TYPE_VULCAN;
import static pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_RECEIVED;
import static pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_SENT;
import static pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata.TYPE_MESSAGE;
import static pl.szczodrzynski.edziennik.data.db.modules.notices.Notice.TYPE_NEUTRAL;
import static pl.szczodrzynski.edziennik.utils.Utils.c;
import static pl.szczodrzynski.edziennik.utils.Utils.crc16;
import static pl.szczodrzynski.edziennik.utils.Utils.d;
import static pl.szczodrzynski.edziennik.utils.Utils.getGradeValue;
import static pl.szczodrzynski.edziennik.utils.Utils.getVulcanGradeColor;
import static pl.szczodrzynski.edziennik.utils.Utils.intToStr;

public class Vulcan implements EdziennikInterface {
    public Vulcan(App app) {
        this.app = app;
    }

    private static final String TAG = "api.Vulcan";
    private static final String MOBILE_APP_NAME = "VULCAN-Android-ModulUcznia";
    private static final String MOBILE_APP_VERSION = "19.4.1.436";
    private static final String ENDPOINT_CERTIFICATE = "mobile-api/Uczen.v3.UczenStart/Certyfikat";
    private static final String ENDPOINT_STUDENT_LIST = "mobile-api/Uczen.v3.UczenStart/ListaUczniow";
    private static final String ENDPOINT_DICTIONARIES = "mobile-api/Uczen.v3.Uczen/Slowniki";
    private static final String ENDPOINT_TIMETABLE = "mobile-api/Uczen.v3.Uczen/PlanLekcjiZeZmianami";
    private static final String ENDPOINT_GRADES = "mobile-api/Uczen.v3.Uczen/Oceny";
    private static final String ENDPOINT_GRADES_PROPOSITIONS = "mobile-api/Uczen.v3.Uczen/OcenyPodsumowanie";
    private static final String ENDPOINT_EVENTS = "mobile-api/Uczen.v3.Uczen/Sprawdziany";
    private static final String ENDPOINT_HOMEWORK = "mobile-api/Uczen.v3.Uczen/ZadaniaDomowe";
    private static final String ENDPOINT_NOTICES = "mobile-api/Uczen.v3.Uczen/UwagiUcznia";
    private static final String ENDPOINT_ATTENDANCES = "mobile-api/Uczen.v3.Uczen/Frekwencje";
    private static final String ENDPOINT_MESSAGES_RECEIVED = "mobile-api/Uczen.v3.Uczen/WiadomosciOdebrane";
    private static final String ENDPOINT_MESSAGES_SENT = "mobile-api/Uczen.v3.Uczen/WiadomosciWyslane";
    private static final String ENDPOINT_MESSAGES_CHANGE_STATUS = "mobile-api/Uczen.v3.Uczen/ZmienStatusWiadomosci";
    private static final String ENDPOINT_PUSH = "mobile-api/Uczen.v3.Uczen/UstawPushToken";
    private static final String userAgent = "MobileUserAgent";

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
    private List<GradeCategory> gradeCategoryList;
    private List<Grade> gradeList;
    private List<Event> eventList;
    private List<Notice> noticeList;
    private List<Attendance> attendanceList;
    private List<Message> messageList;
    private List<MessageRecipient> messageRecipientList;
    private List<MessageRecipient> messageRecipientIgnoreList;
    private List<Metadata> metadataList;
    private List<Metadata> messageMetadataList;

    private String apiUrl = null;
    private String certificateKey = null;
    private String certificatePfx = null;
    private String deviceToken = null;
    private String deviceSymbol = null;
    private String devicePin = null;
    /**
     * deviceSymbol_JednostkaSprawozdawczaSymbol
     */private String schoolName = null;
    /**
     * JednostkaSprawozdawczaSymbol
     */private String schoolSymbol = null;
    /**
     * Id
     */private int studentId = -1;
    /**
     * UzytkownikLoginId
     */private int studentLoginId = -1;
    /**
     * IdOddzial
     */private int studentClassId = -1;
    /**
     * IdOkresKlasyfikacyjny
     */private int studentSemesterId = -1;
    /**
     * OkresNumer
     */private int studentSemesterNumber = -1;
    private boolean syncingSemester1 = false;
    private OkHttpClient signingHttp = null;
    private Date oneMonthBack = today.clone().stepForward(0, -1, 0);

    /*    _____
         |  __ \
         | |__) | __ ___ _ __   __ _ _ __ ___
         |  ___/ '__/ _ \ '_ \ / _` | '__/ _ \
         | |   | | |  __/ |_) | (_| | | |  __/
         |_|   |_|  \___| .__/ \__,_|_|  \___|
                        | |
                        |*/
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

        if (this.signingHttp == null) {
            this.signingHttp = this.app.http.newBuilder().addInterceptor(chain -> {
                okhttp3.Request request = chain.request();
                Buffer buffer = new Buffer();
                assert request.body() != null;
                request.body().writeTo(buffer);
                String signature = "";
                // cannot use Utils.exception, because we are not in the main thread here!
                // Utils.exception would show the dialog which has to be shown in the UI thread
                try {
                    signature = Utils.VulcanRequestEncryptionUtils.signContent(buffer.readByteArray(), new ByteArrayInputStream(Base64.decode(this.certificatePfx, Base64.DEFAULT)));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                }
                request = request.newBuilder().addHeader("RequestSignatureValue", signature).addHeader("RequestCertificateKey", this.certificateKey).build();
                return chain.proceed(request);
            }).build();
        }


        if (certificateKey == null || certificatePfx == null || apiUrl == null) {
            c(TAG, "first login, use TOKEN, SYMBOL, PIN");

        }

        teamList = profileId == -1 ? new ArrayList<>() : app.db.teamDao().getAllNow(profileId);
        teacherList = profileId == -1 ? new ArrayList<>() : app.db.teacherDao().getAllNow(profileId);
        subjectList = new ArrayList<>();
        lessonList = new ArrayList<>();
        lessonChangeList = new ArrayList<>();
        gradeCategoryList = new ArrayList<>();
        gradeList = new ArrayList<>();
        eventList = new ArrayList<>();
        noticeList = new ArrayList<>();
        attendanceList = new ArrayList<>();
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
            targetEndpoints.add("SetPushToken");
            targetEndpoints.add("Dictionaries");
            targetEndpoints.add("Timetable");
            targetEndpoints.add("Grades");
            targetEndpoints.add("ProposedGrades");
            targetEndpoints.add("Events");
            targetEndpoints.add("Homework");
            targetEndpoints.add("Notices");
            targetEndpoints.add("Attendances");
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
            targetEndpoints.add("SetPushToken");
            targetEndpoints.add("Dictionaries");
            for (int feature: featureList) {
                switch (feature) {
                    case FEATURE_TIMETABLE:
                        targetEndpoints.add("Timetable");
                        break;
                    case FEATURE_AGENDA:
                        targetEndpoints.add("Events");
                        break;
                    case FEATURE_GRADES:
                        targetEndpoints.add("Grades");
                        targetEndpoints.add("ProposedGrades");
                        break;
                    case FEATURE_HOMEWORK:
                        targetEndpoints.add("Homework");
                        break;
                    case FEATURE_NOTICES:
                        targetEndpoints.add("Notices");
                        break;
                    case FEATURE_ATTENDANCES:
                        targetEndpoints.add("Attendances");
                        break;
                    case FEATURE_MESSAGES_INBOX:
                        targetEndpoints.add("MessagesInbox");
                        break;
                    case FEATURE_MESSAGES_OUTBOX:
                        targetEndpoints.add("MessagesOutbox");
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
        schoolName = profile.getStudentData("schoolName", null);
        schoolSymbol = profile.getStudentData("schoolSymbol", null);
        studentId = profile.getStudentData("studentId", -1);
        studentLoginId = profile.getStudentData("studentLoginId", -1);
        studentClassId = profile.getStudentData("studentClassId", -1);
        studentSemesterId = profile.getStudentData("studentSemesterId", -1);
        studentSemesterNumber = profile.getStudentData("studentSemesterNumber", profile.getCurrentSemester());
        if (profile.getEmpty() && studentSemesterNumber == 2) {
            syncingSemester1 = true;
            studentSemesterId -= 1;
            studentSemesterNumber -= 1;
        }
        else {
            syncingSemester1 = false;
        }
        d(TAG, "Syncing student "+studentId+", class "+studentClassId+", semester "+studentSemesterId);

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
            case "SetPushToken":
                setPushToken();
                break;
            case "Dictionaries":
                getDictionaries();
                break;
            case "Timetable":
                getTimetable();
                break;
            case "Grades":
                getGrades();
                break;
            case "ProposedGrades":
                getProposedGrades();
                break;
            case "Events":
                getEvents();
                break;
            case "Homework":
                getHomework();
                break;
            case "Notices":
                getNotices();
                break;
            case "Attendances":
                getAttendances();
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
        if (gradeCategoryList.size() > 0)
            app.db.gradeCategoryDao().addAll(gradeCategoryList);
        if (gradeList.size() > 0) {
            app.db.gradeDao().clearForSemester(profileId, studentSemesterNumber);
            app.db.gradeDao().addAll(gradeList);
        }
        if (eventList.size() > 0) {
            app.db.eventDao().removeFuture(profileId, Date.getToday());
            app.db.eventDao().addAll(eventList);
        }
        if (noticeList.size() > 0) {
            app.db.noticeDao().clearForSemester(profileId, studentSemesterNumber);
            app.db.noticeDao().addAll(noticeList);
        }
        if (attendanceList.size() > 0) {
            app.db.attendanceDao().clearAfterDate(profileId, getCurrentSemesterStartDate());
            app.db.attendanceDao().addAll(attendanceList);
        }
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
        if (syncingSemester1) {
            syncingSemester1 = false;
            studentSemesterId += 1;
            studentSemesterNumber += 1;
            // no need to download dictionaries again
            r("get", null);// TODO: 2019-06-04 start with Timetables or first element (if partial sync)
            return;
        }
        try {
            saveData();
        }
        catch (Exception e) {
            finishWithError(new AppError(TAG, 425, CODE_OTHER, app.getString(R.string.sync_error_saving_data), null, null, e, null));
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
        if (profile == null) {
            // FIRST LOGIN
            this.deviceToken = loginStore.getLoginData("deviceToken", null);
            this.deviceSymbol = loginStore.getLoginData("deviceSymbol", null);
            this.devicePin = loginStore.getLoginData("devicePin", null);
            if (deviceToken == null || deviceSymbol == null || devicePin == null) {
                finishWithError(new AppError(TAG, 443, AppError.CODE_INVALID_LOGIN, "Login field is empty"));
                return;
            }

            setApiUrl(deviceToken, deviceSymbol);
            getCertificate(deviceToken, devicePin, ((certificateKey, certificatePfx, apiUrl, userLogin, userName) -> {
                loginStore.removeLoginData("deviceToken");
                loginStore.removeLoginData("devicePin");
                loginStore.putLoginData("certificateKey", certificateKey);
                loginStore.putLoginData("certificatePfx", certificatePfx);
                loginStore.putLoginData("apiUrl", apiUrl);
                loginStore.putLoginData("userLogin", userLogin);
                this.apiUrl = apiUrl;
                this.certificateKey = certificateKey;
                this.certificatePfx = certificatePfx;
                this.deviceToken = null;
                this.devicePin = null;

                c(TAG, "first login. get the list of students");
                getStudentList(usersMap -> {
                    List<Profile> profileList = new ArrayList<>();
                    for (int index = 0; index < usersMap.size(); index++) {
                        JsonObject account = new ArrayList<>(usersMap.values()).get(index);

                        Profile newProfile = new Profile();
                        newProfile.setEmpty(true);
                        newProfile.setLoggedIn(true);
                        saveStudentData(newProfile, account);

                        profileList.add(newProfile);
                    }
                    callback.onLoginFirst(profileList, loginStore);
                }, false);

            }));
        }
        else {
            this.apiUrl = loginStore.getLoginData("apiUrl", null);
            this.certificateKey = loginStore.getLoginData("certificateKey", null);
            this.certificatePfx = loginStore.getLoginData("certificatePfx", null);
            if (apiUrl == null || certificateKey == null || certificatePfx == null) {
                finishWithError(new AppError(TAG, 489, AppError.CODE_INVALID_LOGIN, "Login field is empty"));
                return;
            }
            if (profile.getStudentData("currentSemesterEndDate", System.currentTimeMillis()/*default always larger*/) < System.currentTimeMillis()/1000
                            || (studentLoginId = profile.getStudentData("studentLoginId", -1)) == -1) {
                // current semester is over, we need to get student data again
                callback.onActionStarted(R.string.sync_action_getting_account);
                getStudentList(usersMap -> {
                    studentId = profile.getStudentData("studentId", -1);
                    d(TAG, "Searching the student ID "+studentId);
                    JsonObject foundUser = null;
                    for (JsonObject user: usersMap.values()) {
                        if (user.get("Id").getAsInt() == studentId) {
                            foundUser = user;
                        }
                    }
                    if (foundUser == null || foundUser.get("IdOkresKlasyfikacyjny").getAsInt() == 0) {
                        d(TAG, "Not found");
                        finishWithError(new AppError(TAG, 507, AppError.CODE_OTHER, app.getString(R.string.error_register_student_no_term), usersMap.toString()));
                    }
                    else {
                        d(TAG, "Saving updated student data");
                        saveStudentData(profile, foundUser);
                        loginCallback.onSuccess();
                    }
                }, true);
            }
            else {
                loginCallback.onSuccess();
            }
        }
    }

    /*    _    _      _                                          _ _ _                _
         | |  | |    | |                       ___              | | | |              | |
         | |__| | ___| |_ __   ___ _ __ ___   ( _ )     ___ __ _| | | |__   __ _  ___| | _____
         |  __  |/ _ \ | '_ \ / _ \ '__/ __|  / _ \/\  / __/ _` | | | '_ \ / _` |/ __| |/ / __|
         | |  | |  __/ | |_) |  __/ |  \__ \ | (_>  < | (_| (_| | | | |_) | (_| | (__|   <\__ \
         |_|  |_|\___|_| .__/ \___|_|  |___/  \___/\/  \___\__,_|_|_|_.__/ \__,_|\___|_|\_\___/
                       | |
                       |*/
    private void getCertificate(String token, String pin, CertificateCallback certificateCallback) {
        callback.onActionStarted(R.string.sync_action_getting_certificate);
        d(TAG, "Post JSON "+apiUrl + ENDPOINT_CERTIFICATE);
        if (apiUrl == null) {
            finishWithError(new AppError(TAG, 1215, AppError.CODE_INVALID_TOKEN, "Empty apiUrl(1)"));
            return;
        }
        Request.builder()
                .url(apiUrl + ENDPOINT_CERTIFICATE)
                .userAgent(userAgent)
                .addParameter("PIN", pin)
                .addParameter("TokenKey", token)
                .addParameter("AppVersion", MOBILE_APP_VERSION)
                .addParameter("DeviceId", UUID.randomUUID().toString())
                .addParameter("DeviceName", "Szkolny.eu "+Build.MODEL)
                .addParameter("DeviceNameUser", "")
                .addParameter("DeviceDescription", "")
                .addParameter("DeviceSystemType", "Android")
                .addParameter("DeviceSystemVersion", Build.VERSION.RELEASE)
                .addParameter("RemoteMobileTimeKey", getUnixTime())
                .addParameter("TimeKey", getUnixTime() - 1)
                .addParameter("RequestId", UUID.randomUUID().toString())
                .addParameter("RemoteMobileAppVersion", MOBILE_APP_VERSION)
                .addParameter("RemoteMobileAppName", MOBILE_APP_NAME)
                .postJson()
                .addHeader("RequestMobileType", "RegisterDevice")
                .callback(new JsonCallbackHandler() {
                    @Override
                    public void onSuccess(JsonObject data, Response response) {
                        if (data == null) {
                            finishWithError(new AppError(TAG, 1241, AppError.CODE_MAINTENANCE, response));
                            return;
                        }
                        d(TAG, "Certificate data "+data.toString());
                        boolean isError = data.get("IsError").getAsBoolean();
                        if (isError) {
                            JsonElement message = data.get("Message");
                            JsonElement tokenStatus = data.get("TokenStatus");
                            String msg = null;
                            String tokenStatusStr = null;
                            if (message != null) {
                                msg = message.getAsString();
                            }
                            if (tokenStatus != null) {
                                tokenStatusStr = tokenStatus.getAsString();
                            }
                            if ("TokenNotFound".equals(msg)) {
                                finishWithError(new AppError(TAG, 1258, AppError.CODE_INVALID_TOKEN, response, data));
                                return;
                            }
                            if ("TokenDead".equals(msg)) {
                                finishWithError(new AppError(TAG, 1262, AppError.CODE_EXPIRED_TOKEN, response, data));
                                return;
                            }
                            if ("WrongPIN".equals(tokenStatusStr)) {
                                Matcher matcher = Pattern.compile("Liczba pozostałych prób: ([0-9])", Pattern.DOTALL).matcher(tokenStatusStr);
                                finishWithError(new AppError(TAG, 1267, AppError.CODE_INVALID_PIN, matcher.matches() ? matcher.group(1) : "?", response, data));
                                return;
                            }
                            if ("Broken".equals(tokenStatusStr)) {
                                finishWithError(new AppError(TAG, 1262, AppError.CODE_INVALID_PIN, "0", response, data));
                                return;
                            }
                            finishWithError(new AppError(TAG, 1274, AppError.CODE_OTHER, "Sprawdź wprowadzone dane.\n\nBłąd certyfikatu "+(message == null ? "" : message.getAsString()), response, data));
                            return;
                        }
                        JsonElement tokenCert = data.get("TokenCert");
                        if (tokenCert == null || tokenCert instanceof JsonNull) {
                            finishWithError(new AppError(TAG, 1279, AppError.CODE_OTHER, "Sprawdź wprowadzone dane.\n\nCertificate error. TokenCert null", response, data));
                            return;
                        }
                        data = tokenCert.getAsJsonObject();
                        try {
                            certificateCallback.onSuccess(
                                    data.get("CertyfikatKlucz").getAsString(),
                                    data.get("CertyfikatPfx").getAsString(),
                                    data.get("AdresBazowyRestApi").getAsString(),
                                    data.get("UzytkownikLogin").getAsString(),
                                    data.get("UzytkownikNazwa").getAsString()
                            );
                        }
                        catch (Exception e) {
                            finishWithError(new AppError(TAG, 1293, AppError.CODE_OTHER, response, e, data));
                        }
                    }

                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        if (response.code() == 400) {
                            finishWithError(new AppError(TAG, 1300, AppError.CODE_INVALID_SYMBOL, response, throwable));
                            return;
                        }
                        finishWithError(new AppError(TAG, 1303, AppError.CODE_OTHER, response, throwable));
                    }
                })
                .build()
                .enqueue();
    }

    private void apiRequest(String endpoint, JsonObject payload, ApiRequestCallback apiRequestCallback) {
        d(TAG, "API request "+apiUrl + endpoint);
        if (apiUrl == null) {
            finishWithError(new AppError(TAG, 1313, AppError.CODE_OTHER, app.getString(R.string.sync_error_no_api_url), "Empty apiUrl(2)"));
            return;
        }
        if (payload == null)
            payload = new JsonObject();
        payload.addProperty("RemoteMobileTimeKey", getUnixTime());
        payload.addProperty("TimeKey", getUnixTime() - 1);
        payload.addProperty("RequestId", UUID.randomUUID().toString());
        payload.addProperty("RemoteMobileAppVersion", MOBILE_APP_VERSION);
        payload.addProperty("RemoteMobileAppName", MOBILE_APP_NAME);
        Request.builder()
                .url(apiUrl + endpoint)
                .userAgent(userAgent)
                .withClient(signingHttp)
                .setJsonBody(payload)
                .callback(new JsonCallbackHandler() {
                    @Override
                    public void onSuccess(JsonObject data, Response response) {
                        if (data == null) {
                            finishWithError(new AppError(TAG, 1332, AppError.CODE_MAINTENANCE, response));
                            return;
                        }
                        try {
                            apiRequestCallback.onSuccess(data);
                        }
                        catch (Exception e) {
                            finishWithError(new AppError(TAG, 1339, AppError.CODE_OTHER, response, e, data));
                        }
                    }

                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        finishWithError(new AppError(TAG, 1345, AppError.CODE_OTHER, response, throwable));
                    }
                })
                .build()
                .enqueue();
    }

    private void getStudentList(StudentListCallback studentListCallback, boolean reportStudentsWithNoSemester) {
        apiRequest(ENDPOINT_STUDENT_LIST, null, data -> {
            d(TAG, "Got: " + data.toString());
            JsonElement listEl = data.get("Data");
            JsonArray accountList;
            if (listEl == null || listEl instanceof JsonNull || (accountList = listEl.getAsJsonArray()).size() == 0) {
                finishWithError(new AppError(TAG, 1369, AppError.CODE_OTHER, app.getString(R.string.sync_error_register_no_students), data));
                return;
            }
            LinkedHashMap<String, JsonObject> usersMap = new LinkedHashMap<>();
            for (JsonElement userEl : accountList) {
                JsonObject user = userEl.getAsJsonObject();
                if (reportStudentsWithNoSemester || (user != null && user.get("IdOkresKlasyfikacyjny").getAsInt() != 0)) {
                    usersMap.put(user.get("Imie").getAsString() + " " + user.get("Nazwisko").getAsString() + " " + (user.get("OddzialKod") instanceof JsonNull ? "" : user.get("OddzialKod").getAsString()), user);
                } else if (accountList.size() == 1) {
                    finishWithError(new AppError(TAG, 1377, AppError.CODE_OTHER, app.getString(R.string.error_register_student_no_term), data));
                    return;
                }
            }
            studentListCallback.onSuccess(usersMap);
        });
    }

    private void saveStudentData(Profile targetProfile, JsonObject account) {
        String firstName = account.get("Imie").getAsString();
        String lastName = account.get("Nazwisko").getAsString();
        schoolName = loginStore.getLoginData("deviceSymbol", getSymbolFromApiUrl())+"_"+account.get("JednostkaSprawozdawczaSymbol").getAsString();
        schoolSymbol = account.get("JednostkaSprawozdawczaSymbol").getAsString();
        studentId = account.get("Id").getAsInt();
        studentLoginId = account.get("UzytkownikLoginId").getAsInt();
        studentClassId = account.get("IdOddzial").getAsInt();
        studentSemesterId = account.get("IdOkresKlasyfikacyjny").getAsInt();
        String studentClassName = account.get("OkresPoziom").getAsInt()+account.get("OddzialSymbol").getAsString();
        targetProfile.putStudentData("userName", account.get("UzytkownikNazwa").getAsString());
        targetProfile.putStudentData("schoolName", schoolName);
        targetProfile.putStudentData("schoolSymbol", schoolSymbol);
        targetProfile.putStudentData("studentId", studentId);
        targetProfile.putStudentData("studentLoginId", studentLoginId);
        targetProfile.putStudentData("studentClassId", studentClassId);
        targetProfile.putStudentData("studentClassName", studentClassName);
        targetProfile.putStudentData("studentSemesterId", studentSemesterId);
        targetProfile.putStudentData("currentSemesterEndDate", account.get("OkresDataDo").getAsInt()+86400);
        targetProfile.putStudentData("studentSemesterNumber", account.get("OkresNumer").getAsInt());
        targetProfile.setCurrentSemester(account.get("OkresNumer").getAsInt());
        if (targetProfile.getCurrentSemester() == 1) {
            targetProfile.setDateSemester1Start(Date.fromMillis((long) account.get("OkresDataOd").getAsInt() * 1000));
            targetProfile.setDateSemester2Start(Date.fromMillis(((long) account.get("OkresDataDo").getAsInt() + 86400) * 1000));
        }
        else if (targetProfile.getCurrentSemester() == 2) {
            targetProfile.setDateSemester2Start(Date.fromMillis((long) account.get("OkresDataOd").getAsInt() * 1000));
            targetProfile.setDateYearEnd(Date.fromMillis(((long) account.get("OkresDataDo").getAsInt() + 86400) * 1000));
        }
        //db.teamDao().add(new Team(profileId, studentClassId, account.get("OddzialKod").getAsString(), 1, -1));
        targetProfile.setStudentNameLong(firstName + " " + lastName);
        targetProfile.setStudentNameShort(firstName + " " + lastName.charAt(0) + ".");
        if (targetProfile.getName() == null || targetProfile.getName().equals("")) {
            targetProfile.setName(targetProfile.getStudentNameLong());
        }
        targetProfile.setSubname(account.get("UzytkownikLogin").getAsString());
    }

    private Team searchTeam(String name, String code, long teacherId) {
        Team team;
        team = Team.getByName(teamList, name);

        if (team == null) {
            team = new Team(profileId, crc16(name.getBytes()), name, 2, code, teacherId);
            teamList.add(team);
        }
        else {
            team.teacherId = teacherId;
        }
        return team;
    }

    public interface CertificateCallback {
        void onSuccess(String certificateKey, String certificatePfx, String apiUrl, String userLogin, String userName);
    }
    private interface StudentListCallback {
        void onSuccess(LinkedHashMap<String, JsonObject> usersMap);
    }
    private interface ApiRequestCallback {
        void onSuccess(JsonObject data);
    }

    private String getSymbolFromApiUrl() {
        String[] parts = apiUrl.split("/");
        return parts[parts.length - 2];
    }

    private String getClassTeamName() {
        if (teamList.size() == 0)
            return "";
        if (teamList.get(0).type != 1)
            return "";
        return teamList.get(0).name;
    }

    private long getClassTeamId() {
        if (teamList.size() == 0)
            return -1;
        if (teamList.get(0).type != 1)
            return -1;
        return teamList.get(0).id;
    }

    private Date getCurrentSemesterStartDate() {
        return profile.getSemesterStart(studentSemesterNumber);
    }

    private Date getCurrentSemesterEndDate() {
        return profile.getSemesterEnd(studentSemesterNumber);
    }

    private long getUnixTime() {
        return System.currentTimeMillis() / 1000;
    }

    private void setApiUrl(String token, String symbol) {
        String rule = token.substring(0, 3);
        switch (rule) {
            case "3S1":
                if (!App.devMode)
                    apiUrl = "https://lekcjaplus.vulcan.net.pl/"+symbol+"/";
                else
                    apiUrl = "http://hack.szkolny.eu/"+symbol+"/";
                break;
            case "TA1":
                apiUrl = "https://uonetplus-komunikacja.umt.tarnow.pl/"+symbol+"/";
                break;
            case "OP1":
                apiUrl = "https://uonetplus-komunikacja.eszkola.opolskie.pl/"+symbol+"/";
                break;
            case "RZ1":
                apiUrl = "https://uonetplus-komunikacja.resman.pl/"+symbol+"/";
                break;
            case "GD1":
                apiUrl = "https://uonetplus-komunikacja.edu.gdansk.pl/"+symbol+"/";
                break;
            case "KA1":
                apiUrl = "https://uonetplus-komunikacja.mcuw.katowice.eu/"+symbol+"/";
                break;
            case "KA2":
                apiUrl = "https://uonetplus-komunikacja-test.mcuw.katowice.eu/"+symbol+"/";
                break;
            case "P03":
                apiUrl = "https://efeb-komunikacja-pro-efebmobile.pro.vulcan.pl/"+symbol+"/";
                break;
            case "P01":
                apiUrl = "http://efeb-komunikacja.pro-hudson.win.vulcan.pl/"+symbol+"/";
                break;
            case "P02":
                apiUrl = "http://efeb-komunikacja.pro-hudsonrc.win.vulcan.pl/"+symbol+"/";
                break;
            case "P90":
                apiUrl = "http://efeb-komunikacja-pro-mwujakowska.neo.win.vulcan.pl/"+symbol+"/";
                break;
            case "FK1":
                apiUrl = "http://api.fakelog.cf/"+symbol+"/";
                break;
            case "SZ9":
                //apiUrl = "http://vulcan.szkolny.eu/"+symbol+"/";
                break;
            default:
                apiUrl = null;
                break;
        }
    }

    /*    _____        _          _____                            _
         |  __ \      | |        |  __ \                          | |
         | |  | | __ _| |_ __ _  | |__) |___  __ _ _   _  ___  ___| |_ ___
         | |  | |/ _` | __/ _` | |  _  // _ \/ _` | | | |/ _ \/ __| __/ __|
         | |__| | (_| | || (_| | | | \ \  __/ (_| | |_| |  __/\__ \ |_\__ \
         |_____/ \__,_|\__\__,_| |_|  \_\___|\__, |\__,_|\___||___/\__|___/
                                                | |
                                                |*/
    private void setPushToken() {
        String token;
        Pair<String, List<Integer>> pair = app.appConfig.fcmTokens.get(LOGIN_TYPE_VULCAN);
        if (pair == null || (token = pair.first) == null || (pair.second != null && pair.second.contains(profileId))) {
            r("finish", "SetPushToken");
            return;
        }
        callback.onActionStarted(R.string.sync_action_setting_push_token);
        JsonObject json = new JsonObject();
        json.addProperty("IdUczen", studentId);
        json.addProperty("Token", token);
        json.addProperty("PushOcena", true);
        json.addProperty("PushFrekwencja", true);
        json.addProperty("PushUwaga", true);
        json.addProperty("PushWiadomosc", true);
        apiRequest(schoolSymbol+"/"+ENDPOINT_PUSH, json, result -> {
            if (pair.second == null) {
                List<Integer> list = new ArrayList<>();
                list.add(profileId);
                app.appConfig.fcmTokens.put(LOGIN_TYPE_VULCAN, new Pair<>(token, list));
            }
            else {
                pair.second.add(profileId);
                app.appConfig.fcmTokens.put(LOGIN_TYPE_VULCAN, new Pair<>(token, pair.second));
            }
            r("finish", "SetPushToken");
        });
    }

    private SparseArray<Pair<Time, Time>> lessonRanges = new SparseArray<>();
    private SparseArray<String> noticeCategories = new SparseArray<>();
    private SparseArray<Pair<Integer, String>> attendanceCategories = new SparseArray<>();
    private void getDictionaries() {
        callback.onActionStarted(R.string.sync_action_syncing_dictionaries);
        if (teamList.size() == 0 || teamList.get(0).type != 1) {
            String name = profile.getStudentData("studentClassName", null);
            if (name != null) {
                long id = crc16(name.getBytes());
                teamList.add(new Team(profileId, id, name, 1, schoolName + ":" + name, -1));
            }
        }
        apiRequest(schoolSymbol+"/"+ENDPOINT_DICTIONARIES, null, result -> {
            JsonObject data = result.getAsJsonObject("Data");
            JsonArray teachers = data.getAsJsonArray("Nauczyciele");
            JsonArray subjects = data.getAsJsonArray("Przedmioty");
            JsonArray lessonRanges = data.getAsJsonArray("PoryLekcji");
            JsonArray gradeCategories = data.getAsJsonArray("KategorieOcen");
            JsonArray noticeCategories = data.getAsJsonArray("KategorieUwag");
            JsonArray attendanceCategories = data.getAsJsonArray("KategorieFrekwencji");

            for (JsonElement teacherEl : teachers) {
                JsonObject teacher = teacherEl.getAsJsonObject();
                JsonElement loginId = teacher.get("LoginId");
                teacherList.add(
                        new Teacher(
                                profileId,
                                teacher.get("Id").getAsInt(),
                                teacher.get("Imie").getAsString(),
                                teacher.get("Nazwisko").getAsString(),
                                loginId == null || loginId instanceof JsonNull ? "-1" : loginId.getAsString()
                        )
                );
            }

            for (JsonElement subjectEl : subjects) {
                JsonObject subject = subjectEl.getAsJsonObject();
                subjectList.add(
                        new Subject(
                                profileId,
                                subject.get("Id").getAsInt(),
                                subject.get("Nazwa").getAsString(),
                                subject.get("Kod").getAsString()
                        )
                );
            }

            this.lessonRanges.clear();
            for (JsonElement lessonRangeEl : lessonRanges) {
                JsonObject lessonRange = lessonRangeEl.getAsJsonObject();
                this.lessonRanges.put(
                        lessonRange.get("Id").getAsInt(),
                        new Pair<>(
                                Time.fromH_m(lessonRange.get("PoczatekTekst").getAsString()),
                                Time.fromH_m(lessonRange.get("KoniecTekst").getAsString())
                        )
                );
            }

            for (JsonElement gradeCategoryEl : gradeCategories) {
                JsonObject gradeCategory = gradeCategoryEl.getAsJsonObject();
                gradeCategoryList.add(
                        new GradeCategory(
                                profileId,
                                gradeCategory.get("Id").getAsInt(),
                                -1,
                                -1,
                                gradeCategory.get("Nazwa").getAsString()
                        )
                );
            }

            this.noticeCategories.clear();
            for (JsonElement noticeCategoryEl : noticeCategories) {
                JsonObject noticeCategory = noticeCategoryEl.getAsJsonObject();
                this.noticeCategories.put(
                        noticeCategory.get("Id").getAsInt(),
                        noticeCategory.get("Nazwa").getAsString()
                );
            }

            this.attendanceCategories.clear();
            for (JsonElement attendanceCategoryEl : attendanceCategories) {
                JsonObject attendanceCategory = attendanceCategoryEl.getAsJsonObject();
                int type = -1;
                boolean absent = attendanceCategory.get("Nieobecnosc").getAsBoolean();
                boolean excused = attendanceCategory.get("Usprawiedliwione").getAsBoolean();
                if (absent) {
                    type = excused ? TYPE_ABSENT_EXCUSED : TYPE_ABSENT;
                }
                else {
                    if (attendanceCategory.get("Spoznienie").getAsBoolean()) {
                        type = excused ? TYPE_BELATED_EXCUSED : TYPE_BELATED;
                    }
                    else if (attendanceCategory.get("Zwolnienie").getAsBoolean()) {
                        type = TYPE_RELEASED;
                    }
                    else if (attendanceCategory.get("Obecnosc").getAsBoolean()) {
                        type = TYPE_PRESENT;
                    }
                }
                this.attendanceCategories.put(
                        attendanceCategory.get("Id").getAsInt(),
                        new Pair<>(
                                type,
                                attendanceCategory.get("Nazwa").getAsString()
                        )
                );
            }
            r("finish", "Dictionaries");
        });
    }

    private void getTimetable() {
        if (syncingSemester1) {
            // we are in semester 2. we're syncing semester 1 to show old data. skip the timetable.
            r("finish", "Timetable");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_timetable);
        JsonObject json = new JsonObject();
        json.addProperty("IdUczen", studentId);

        Date weekStart = Week.getWeekStart();
        if (Date.getToday().getWeekDay() > 4) {
            weekStart.stepForward(0, 0, 7);
        }
        Date weekEnd = weekStart.clone().stepForward(0, 0, 6);

        json.addProperty("DataPoczatkowa", weekStart.getStringY_m_d());
        json.addProperty("DataKoncowa", weekEnd.getStringY_m_d());
        json.addProperty("IdOddzial", studentClassId);
        json.addProperty("IdOkresKlasyfikacyjny", studentSemesterId);
        apiRequest(schoolSymbol+"/"+ENDPOINT_TIMETABLE, json, result -> {
            JsonArray lessons = result.getAsJsonArray("Data");

            for (JsonElement lessonEl: lessons) {
                JsonObject lesson = lessonEl.getAsJsonObject();

                if (!lesson.get("PlanUcznia").getAsBoolean()) {
                    continue;
                }

                Date lessonDate = Date.fromY_m_d(lesson.get("DzienTekst").getAsString());
                Pair<Time, Time> lessonRange = lessonRanges.get(lesson.get("IdPoraLekcji").getAsInt());
                Lesson lessonObject = new Lesson(
                        profileId,
                        lessonDate.getWeekDay(),
                        lessonRange.first,
                        lessonRange.second
                );

                JsonElement subject = lesson.get("IdPrzedmiot");
                if (!(subject instanceof JsonNull)) {
                    lessonObject.subjectId = subject.getAsInt();
                }
                JsonElement teacher = lesson.get("IdPracownik");
                if (!(teacher instanceof JsonNull)) {
                    lessonObject.teacherId = teacher.getAsInt();
                }

                lessonObject.teamId = getClassTeamId();
                JsonElement team = lesson.get("PodzialSkrot");
                if (team != null && !(team instanceof JsonNull)) {
                    String name = getClassTeamName()+" "+team.getAsString();
                    Team teamObject = searchTeam(name, schoolName+":"+name, lessonObject.teacherId);
                    lessonObject.teamId = teamObject.id;
                }
                JsonElement classroom = lesson.get("Sala");
                if (classroom != null && !(classroom instanceof JsonNull)) {
                    lessonObject.classroomName = classroom.getAsString();
                }

                JsonElement isCancelled = lesson.get("PrzekreslonaNazwa");
                JsonElement oldTeacherId = lesson.get("IdPracownikOld");
                if (isCancelled != null && isCancelled.getAsBoolean()) {
                    LessonChange lessonChangeObject = new LessonChange(
                            profileId,
                            lessonDate, lessonObject.startTime, lessonObject.endTime
                    );
                    lessonChangeObject.type = TYPE_CANCELLED;
                    lessonChangeObject.teacherId = lessonObject.teacherId;
                    lessonChangeObject.subjectId = lessonObject.subjectId;
                    lessonChangeObject.teamId = lessonObject.teamId;
                    lessonChangeObject.classroomName = lessonObject.classroomName;

                    lessonChangeList.add(lessonChangeObject);
                    metadataList.add(new Metadata(profileId,Metadata.TYPE_LESSON_CHANGE, lessonChangeObject.id, profile.getEmpty(), profile.getEmpty(), System.currentTimeMillis()));
                }
                else if (!(oldTeacherId instanceof JsonNull)) {
                    LessonChange lessonChangeObject = new LessonChange(
                            profileId,
                            lessonDate, lessonObject.startTime, lessonObject.endTime
                    );
                    lessonChangeObject.type = TYPE_CHANGE;
                    lessonChangeObject.teacherId = lessonObject.teacherId;
                    lessonChangeObject.subjectId = lessonObject.subjectId;
                    lessonChangeObject.teamId = lessonObject.teamId;
                    lessonChangeObject.classroomName = lessonObject.classroomName;
                    lessonObject.teacherId = oldTeacherId.getAsInt();

                    lessonChangeList.add(lessonChangeObject);
                    metadataList.add(new Metadata(profileId, Metadata.TYPE_LESSON_CHANGE, lessonChangeObject.id, profile.getEmpty(), profile.getEmpty(), System.currentTimeMillis()));
                }
                lessonList.add(lessonObject);
            }
            r("finish", "Timetable");
        });
    }

    private void getGrades() {
        callback.onActionStarted(R.string.sync_action_syncing_grades);
        JsonObject json = new JsonObject();
        json.addProperty("IdUczen", studentId);
        json.addProperty("IdOkresKlasyfikacyjny", studentSemesterId);
        apiRequest(schoolSymbol+"/"+ENDPOINT_GRADES, json, result -> {
            JsonArray grades = result.getAsJsonArray("Data");

            for (JsonElement gradeEl: grades) {
                JsonObject grade = gradeEl.getAsJsonObject();
                JsonElement name = grade.get("Wpis");
                JsonElement description = grade.get("Opis");
                JsonElement comment = grade.get("Komentarz");
                JsonElement value = grade.get("Wartosc");
                JsonElement modificatorValue = grade.get("WagaModyfikatora");
                JsonElement numerator = grade.get("Licznik");
                JsonElement denominator = grade.get("Mianownik");

                int id = grade.get("Id").getAsInt();
                int weight = grade.get("WagaOceny").getAsInt();
                int subjectId = grade.get("IdPrzedmiot").getAsInt();
                int teacherId = grade.get("IdPracownikD").getAsInt();
                int categoryId = grade.get("IdKategoria").getAsInt();
                long addedDate = Date.fromY_m_d(grade.get("DataModyfikacjiTekst").getAsString()).getInMillis();

                float finalValue = 0.0f;
                String finalName;
                String finalDescription = "";

                if (!(numerator instanceof JsonNull) && !(denominator instanceof JsonNull)) {
                    // calculate the grade's value and name: divide
                    float numeratorF = numerator.getAsFloat();
                    float denominatorF = denominator.getAsFloat();
                    finalValue = numeratorF / denominatorF;
                    weight = 0;
                    finalName = intToStr(Math.round(finalValue*100))+"%";
                    finalDescription += new DecimalFormat("#.##").format(numeratorF)+"/"+new DecimalFormat("#.##").format(denominatorF);
                }
                else {
                    // "normal" grade. Get the name and value if set. Otherwise zero the weight.
                    finalName = name.getAsString();
                    if (!(value instanceof JsonNull)) {
                        finalValue = value.getAsFloat();
                        if (!(modificatorValue instanceof JsonNull)) {
                            finalValue += modificatorValue.getAsFloat();
                        }
                    }
                    else {
                        weight = 0;
                    }
                }

                if (!(comment instanceof JsonNull)) {
                    if (finalName.equals("")) {
                        finalName = comment.getAsString();
                    }
                    else {
                        finalDescription += (finalDescription.equals("") ? "" : " ")+comment.getAsString();
                    }
                }
                if (!(description instanceof JsonNull)) {
                    finalDescription += (finalDescription.equals("") ? "" : " - ")+description.getAsString();
                }

                int semester = studentSemesterNumber;
                /*Date addedDateObj = Date.fromMillis(addedDate);
                if (app.register.dateSemester2Start != null && addedDateObj.compareTo(app.register.dateSemester2Start) > 0) {
                    semester = 2;
                }*/

                String category = "";
                for (GradeCategory gradeCategory: gradeCategoryList) {
                    if (gradeCategory.categoryId == categoryId) {
                        category = gradeCategory.text;
                    }
                }

                int color = 0xff3D5F9C;
                switch (finalName) {
                    case "1-":
                    case "1":
                    case "1+":
                        color = 0xffd65757;
                        break;
                    case "2-":
                    case "2":
                    case "2+":
                        color = 0xff9071b3;
                        break;
                    case "3-":
                    case "3":
                    case "3+":
                        color = 0xffd2ab24;
                        break;
                    case "4-":
                    case "4":
                    case "4+":
                        color = 0xff50b6d6;
                        break;
                    case "5-":
                    case "5":
                    case "5+":
                        color = 0xff2cbd92;
                        break;
                    case "6-":
                    case "6":
                    case "6+":
                        color = 0xff91b43c;
                        break;
                }

                Grade gradeObject = new Grade(
                        profileId,
                        id,
                        category,
                        color,
                        finalDescription,
                        finalName,
                        finalValue,
                        weight,
                        semester,
                        teacherId,
                        subjectId
                );

                gradeList.add(gradeObject);
                metadataList.add(new Metadata(profileId, Metadata.TYPE_GRADE, gradeObject.id, profile.getEmpty(), profile.getEmpty(), addedDate));
            }
            r("finish", "Grades");
        });
    }

    private void processGradeList(JsonArray jsonGrades, List<Grade> gradeList, List<Metadata> metadataList, boolean isFinal) {
        for (JsonElement gradeEl: jsonGrades) {
            JsonObject grade = gradeEl.getAsJsonObject();

            String name = grade.get("Wpis").getAsString();
            float value = getGradeValue(name);
            long subjectId = grade.get("IdPrzedmiot").getAsLong();

            long id = subjectId * -100 - studentSemesterNumber;

            int color = getVulcanGradeColor(name);

            Grade gradeObject = new Grade(
                    profileId,
                    id,
                    "",
                    color,
                    "",
                    name,
                    value,
                    0,
                    studentSemesterNumber,
                    -1,
                    subjectId
            );
            if (studentSemesterNumber == 1) {
                gradeObject.type = isFinal ? TYPE_SEMESTER1_FINAL : TYPE_SEMESTER1_PROPOSED;
            }
            else {
                gradeObject.type = isFinal ? TYPE_SEMESTER2_FINAL : TYPE_SEMESTER2_PROPOSED;
            }
            gradeList.add(gradeObject);
            metadataList.add(new Metadata(profileId, Metadata.TYPE_GRADE, gradeObject.id, profile.getEmpty(), profile.getEmpty(), System.currentTimeMillis()));
        }
    }
    private void getProposedGrades() {
        callback.onActionStarted(R.string.sync_action_syncing_proposition_grades);
        JsonObject json = new JsonObject();
        json.addProperty("IdUczen", studentId);
        json.addProperty("IdOkresKlasyfikacyjny", studentSemesterId);
        apiRequest(schoolSymbol+"/"+ENDPOINT_GRADES_PROPOSITIONS, json, result -> {
            JsonObject grades = result.getAsJsonObject("Data");
            JsonArray gradesProposed = grades.getAsJsonArray("OcenyPrzewidywane");
            JsonArray gradesFinal = grades.getAsJsonArray("OcenyKlasyfikacyjne");

            processGradeList(gradesProposed, gradeList, metadataList, false);
            processGradeList(gradesFinal, gradeList, metadataList, true);
            r("finish", "ProposedGrades");
        });
    }

    private void getEvents() {
        callback.onActionStarted(R.string.sync_action_syncing_exams);
        JsonObject json = new JsonObject();
        // from today to the end of the current semester
        // or if empty: from the beginning of the semester
        json.addProperty("DataPoczatkowa", profile.getEmpty() ? getCurrentSemesterStartDate().getStringY_m_d() : oneMonthBack.getStringY_m_d());
        json.addProperty("DataKoncowa", getCurrentSemesterEndDate().getStringY_m_d());
        json.addProperty("IdOddzial", studentClassId);
        json.addProperty("IdUczen", studentId);
        json.addProperty("IdOkresKlasyfikacyjny", studentSemesterId);
        apiRequest(schoolSymbol+"/"+ENDPOINT_EVENTS, json, result -> {
            JsonArray events = result.getAsJsonArray("Data");

            for (JsonElement eventEl: events) {
                JsonObject event = eventEl.getAsJsonObject();

                int id = event.get("Id").getAsInt();

                int eventType = event.get("Rodzaj").getAsBoolean() ? TYPE_EXAM : TYPE_SHORT_QUIZ;

                int subjectId = event.get("IdPrzedmiot").getAsInt();

                Date lessonDate = Date.fromY_m_d(event.get("DataTekst").getAsString());
                Time startTime = null;

                int weekDay = lessonDate.getWeekDay();
                for (Lesson lesson: lessonList) {
                    if (lesson.weekDay == weekDay && lesson.subjectId == subjectId) {
                        startTime = lesson.startTime;
                    }
                }

                long teacherId = event.get("IdPracownik").getAsInt();

                Event eventObject = new Event(
                        profileId,
                        id,
                        lessonDate,
                        startTime,
                        event.get("Opis").getAsString(),
                        -1,
                        eventType,
                        false,
                        teacherId,
                        subjectId,
                        getClassTeamId()
                );

                JsonElement team = event.get("PodzialSkrot");
                if (team != null && !(team instanceof JsonNull)) {
                    String name = getClassTeamName()+" "+team.getAsString();
                    Team teamObject = searchTeam(name, schoolName+":"+name, teacherId);
                    eventObject.teamId = teamObject.id;
                }

                eventList.add(eventObject);
                metadataList.add(new Metadata(profileId, Metadata.TYPE_EVENT, eventObject.id, profile.getEmpty(), profile.getEmpty(), System.currentTimeMillis()));
            }
            r("finish", "Events");
        });
    }

    private void getHomework() {
        callback.onActionStarted(R.string.sync_action_syncing_homework);
        JsonObject json = new JsonObject();
        json.addProperty("DataPoczatkowa", profile.getEmpty() ? getCurrentSemesterStartDate().getStringY_m_d() : oneMonthBack.getStringY_m_d());
        json.addProperty("DataKoncowa", getCurrentSemesterEndDate().getStringY_m_d());
        json.addProperty("IdOddzial", studentClassId);
        json.addProperty("IdUczen", studentId);
        json.addProperty("IdOkresKlasyfikacyjny", studentSemesterId);
        apiRequest(schoolSymbol+"/"+ ENDPOINT_HOMEWORK, json, result -> {
            JsonArray homeworkList = result.getAsJsonArray("Data");

            for (JsonElement homeworkEl: homeworkList) {
                JsonObject homework = homeworkEl.getAsJsonObject();

                int id = homework.get("Id").getAsInt();

                int subjectId = homework.get("IdPrzedmiot").getAsInt();

                Date lessonDate = Date.fromY_m_d(homework.get("DataTekst").getAsString());
                Time startTime = null;

                int weekDay = lessonDate.getWeekDay();
                for (Lesson lesson: lessonList) {
                    if (lesson.weekDay == weekDay && lesson.subjectId == subjectId) {
                        startTime = lesson.startTime;
                    }
                }

                Event eventObject = new Event(
                        profileId,
                        id,
                        lessonDate,
                        startTime,
                        homework.get("Opis").getAsString(),
                        -1,
                        Event.TYPE_HOMEWORK,
                        false,
                        homework.get("IdPracownik").getAsInt(),
                        subjectId,
                        getClassTeamId()
                );

                eventList.add(eventObject);
                metadataList.add(new Metadata(profileId, Metadata.TYPE_HOMEWORK, eventObject.id, profile.getEmpty(), profile.getEmpty(), System.currentTimeMillis()));
            }
            r("finish", "Homework");
        });
    }

    private void getNotices() {
        callback.onActionStarted(R.string.sync_action_syncing_notices);
        JsonObject json = new JsonObject();
        json.addProperty("IdUczen", studentId);
        json.addProperty("IdOkresKlasyfikacyjny", studentSemesterId);
        apiRequest(schoolSymbol+"/"+ENDPOINT_NOTICES, json, result -> {
            JsonArray notices = result.getAsJsonArray("Data");

            for (JsonElement noticeEl: notices) {
                JsonObject notice = noticeEl.getAsJsonObject();

                int id = notice.get("Id").getAsInt();
                long addedDate = Date.fromY_m_d(notice.get("DataWpisuTekst").getAsString()).getInMillis();

                int semester = studentSemesterNumber;
                /*Date addedDateObj = Date.fromMillis(addedDate);
                if (profile.dateSemester2Start != null && addedDateObj.compareTo(profile.dateSemester2Start) > 0) {
                    semester = 2;
                }*/

                Notice noticeObject = new Notice(
                        profileId,
                        id,
                        (notice.get("IdKategoriaUwag") instanceof JsonNull ? "" : noticeCategories.get(notice.get("IdKategoriaUwag").getAsInt())) +"\n"+notice.get("TrescUwagi").getAsString(),
                        semester,
                        TYPE_NEUTRAL,
                        notice.get("IdPracownik").getAsInt()
                );

                noticeList.add(noticeObject);
                metadataList.add(new Metadata(profileId, Metadata.TYPE_NOTICE, noticeObject.id, profile.getEmpty(), profile.getEmpty(), addedDate));
            }
            r("finish", "Notices");
        });
    }

    private void getAttendances() {
        callback.onActionStarted(R.string.sync_action_syncing_attendances);
        JsonObject json = new JsonObject();
        json.addProperty("DataPoczatkowa", true ? getCurrentSemesterStartDate().getStringY_m_d() : oneMonthBack.getStringY_m_d());
        json.addProperty("DataKoncowa", getCurrentSemesterEndDate().getStringY_m_d());
        json.addProperty("IdOddzial", studentClassId);
        json.addProperty("IdUczen", studentId);
        json.addProperty("IdOkresKlasyfikacyjny", studentSemesterId);
        apiRequest(schoolSymbol+"/"+ENDPOINT_ATTENDANCES, json, result -> {
            JsonArray attendances = result.getAsJsonObject("Data").getAsJsonArray("Frekwencje");

            for (JsonElement attendanceEl: attendances) {
                JsonObject attendance = attendanceEl.getAsJsonObject();

                Pair<Integer, String> attendanceCategory = attendanceCategories.get(attendance.get("IdKategoria").getAsInt());
                if (attendanceCategory == null)
                    continue;

                int type = attendanceCategory.first;

                int id = attendance.get("Dzien").getAsInt() + attendance.get("Numer").getAsInt();

                long lessonDateMillis = Date.fromY_m_d(attendance.get("DzienTekst").getAsString()).getInMillis();
                Date lessonDate = Date.fromMillis(lessonDateMillis);

                int lessonSemester = profile.dateToSemester(lessonDate);

                Attendance attendanceObject = new Attendance(
                        profileId,
                        id,
                        0,
                        attendance.get("IdPrzedmiot").getAsInt(),
                        lessonSemester,
                        attendance.get("PrzedmiotNazwa").getAsString()+" - "+attendanceCategory.second,
                        lessonDate,
                        lessonRanges.get(attendance.get("IdPoraLekcji").getAsInt()).first,
                        type);

                attendanceList.add(attendanceObject);
                if (attendanceObject.type != TYPE_PRESENT) {
                    metadataList.add(new Metadata(profileId, Metadata.TYPE_ATTENDANCE, attendanceObject.id, profile.getEmpty(), profile.getEmpty(), attendanceObject.lessonDate.combineWith(attendanceObject.startTime)));
                }
            }
            r("finish", "Attendances");
        });
    }

    private void getMessagesInbox() {
        callback.onActionStarted(R.string.sync_action_syncing_messages_inbox);
        JsonObject json = new JsonObject();
        json.addProperty("DataPoczatkowa", true ? getCurrentSemesterStartDate().getInUnix() : oneMonthBack.getInUnix());
        json.addProperty("DataKoncowa", getCurrentSemesterEndDate().getInUnix());
        json.addProperty("LoginId", studentLoginId);
        json.addProperty("IdUczen", studentId);
        apiRequest(schoolSymbol+"/"+ENDPOINT_MESSAGES_RECEIVED, json, result -> {
            JsonArray messages = result.getAsJsonArray("Data");

            for (JsonElement messageEl: messages) {
                JsonObject message = messageEl.getAsJsonObject();

                long id = message.get("WiadomoscId").getAsLong();

                long senderId = -1;
                int senderLoginId = message.get("NadawcaId").getAsInt();
                String senderLoginIdStr = String.valueOf(senderLoginId);

                for (Teacher teacher: teacherList) {
                    if (senderLoginIdStr.equals(teacher.loginId)) {
                        senderId = teacher.id;
                    }
                }

                String subject = message.get("Tytul").getAsString();
                String body = message.get("Tresc").getAsString();
                body = body.replaceAll("\n", "<br>");

                long addedDate = message.get("DataWyslaniaUnixEpoch").getAsLong() * 1000;
                long readDate = 0;
                JsonElement readDateUnix;
                if (!((readDateUnix = message.get("DataPrzeczytaniaUnixEpoch")) instanceof JsonNull)) {
                    readDate = readDateUnix.getAsLong() * 1000L;
                }

                Message messageObject = new Message(profileId, id, subject, body, TYPE_RECEIVED, senderId, -1);
                MessageRecipient messageRecipientObject = new MessageRecipient(profileId, -1, -1, readDate, id);

                messageList.add(messageObject);
                messageRecipientList.add(messageRecipientObject);
                messageMetadataList.add(new Metadata(profileId, TYPE_MESSAGE, id, readDate > 0, readDate > 0 || profile.getEmpty(), addedDate));
            }
            r("finish", "MessagesInbox");
        });
    }

    private void getMessagesOutbox() {
        if (!fullSync && onlyFeature != FEATURE_MESSAGES_OUTBOX) {
            r("finish", "MessagesOutbox");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_messages_outbox);
        JsonObject json = new JsonObject();
        json.addProperty("DataPoczatkowa", true ? getCurrentSemesterStartDate().getInUnix() : oneMonthBack.getInUnix());
        json.addProperty("DataKoncowa", getCurrentSemesterEndDate().getInUnix());
        json.addProperty("LoginId", studentLoginId);
        json.addProperty("IdUczen", studentId);
        apiRequest(schoolSymbol+"/"+ENDPOINT_MESSAGES_SENT, json, result -> {
            JsonArray messages = result.getAsJsonArray("Data");

            for (JsonElement jMessageEl: messages) {
                JsonObject jMessage = jMessageEl.getAsJsonObject();

                long messageId = jMessage.get("WiadomoscId").getAsLong();

                String subject = jMessage.get("Tytul").getAsString();

                String body = jMessage.get("Tresc").getAsString();
                body = body.replaceAll("\n", "<br>");

                long sentDate = jMessage.get("DataWyslaniaUnixEpoch").getAsLong() * 1000;

                Message message = new Message(
                        profileId,
                        messageId,
                        subject,
                        body,
                        TYPE_SENT,
                        -1,
                        -1
                );

                int readBy = jMessage.get("Przeczytane").getAsInt();
                int unreadBy = jMessage.get("Nieprzeczytane").getAsInt();
                for (JsonElement recipientEl: jMessage.getAsJsonArray("Adresaci")) {
                    JsonObject recipient = recipientEl.getAsJsonObject();
                    long recipientId = -1;
                    int recipientLoginId = recipient.get("LoginId").getAsInt();
                    String recipientLoginIdStr = String.valueOf(recipientLoginId);
                    for (Teacher teacher: teacherList) {
                        if (recipientLoginIdStr.equals(teacher.loginId)) {
                            recipientId = teacher.id;
                        }
                    }
                    MessageRecipient messageRecipient = new MessageRecipient(
                            profileId,
                            recipientId,
                            -1,
                            readBy == 0 ? 0 : unreadBy == 0 ? 1 : -1,
                            /*messageId*/ messageId
                    );
                    messageRecipientIgnoreList.add(messageRecipient);
                }

                messageList.add(message);
                metadataList.add(new Metadata(profileId, Metadata.TYPE_MESSAGE, messageId, true, true, sentDate));
            }
            r("finish", "MessagesOutbox");
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
    public void getMessage(@NonNull Context activityContext, @NonNull SyncCallback errorCallback, @NonNull ProfileFull profile, @NonNull MessageFull message, @NonNull MessageGetCallback messageCallback) {
        if (message.body != null) {
            message.recipients = app.db.messageRecipientDao().getAllByMessageId(profile.getId(), message.id);
            for (MessageRecipientFull recipient: message.recipients) {
                if (recipient.id == -1)
                    recipient.fullName = profile.getStudentNameLong();
            }
            if (!message.seen) {
                studentId = profile.getStudentData("studentId", -1);
                studentLoginId = profile.getStudentData("studentLoginId", -1);
                JsonObject json = new JsonObject();
                json.addProperty("WiadomoscId", message.id);
                json.addProperty("FolderWiadomosci", "Odebrane");
                json.addProperty("Status", "Widoczna");
                json.addProperty("LoginId", studentLoginId);
                json.addProperty("IdUczen", studentId);
                apiRequest(schoolSymbol+"/"+ENDPOINT_MESSAGES_CHANGE_STATUS, json, result -> { });
                app.db.metadataDao().setSeen(profile.getId(), message, true);
                if (message.type != TYPE_SENT) {
                    app.db.messageRecipientDao().add(new MessageRecipient(profile.getId(), -1, -1, System.currentTimeMillis(), message.id));
                }
            }
            new Handler(activityContext.getMainLooper()).post(() -> {
                messageCallback.onSuccess(message);
            });
            return;
        }
    }

    @Override
    public void getAttachment(@NonNull Context activityContext, @NonNull SyncCallback errorCallback, @NonNull ProfileFull profile, @NonNull MessageFull message, long attachmentId, @NonNull AttachmentGetCallback attachmentCallback) {

    }

    @Override
    public void getRecipientList(@NonNull Context activityContext, @NonNull SyncCallback errorCallback, @NonNull ProfileFull profile, @NonNull RecipientListGetCallback recipientListGetCallback) {

    }

    @Override
    public MessagesComposeInfo getComposeInfo(@NonNull ProfileFull profile) {
        return new MessagesComposeInfo(0, 0, -1, -1);
    }

    @Override
    public void syncMessages(@NonNull Context activityContext, @NonNull SyncCallback errorCallback, @NonNull ProfileFull profile) {

    }

    @Override
    public Map<String, Endpoint> getConfigurableEndpoints(Profile profile) {
        return null;
    }

    @Override
    public boolean isEndpointEnabled(Profile profile, boolean defaultActive, String name) {
        return defaultActive;
    }
}
