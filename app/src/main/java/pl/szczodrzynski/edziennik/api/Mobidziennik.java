package pl.szczodrzynski.edziennik.api;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.Html;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import im.wangchao.mhttp.Request;
import im.wangchao.mhttp.Response;
import im.wangchao.mhttp.callback.TextCallbackHandler;
import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.BuildConfig;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.api.interfaces.AttachmentGetCallback;
import pl.szczodrzynski.edziennik.api.interfaces.EdziennikInterface;
import pl.szczodrzynski.edziennik.api.interfaces.LoginCallback;
import pl.szczodrzynski.edziennik.api.interfaces.MessageGetCallback;
import pl.szczodrzynski.edziennik.api.interfaces.RecipientListGetCallback;
import pl.szczodrzynski.edziennik.api.interfaces.SyncCallback;
import pl.szczodrzynski.edziennik.datamodels.Attendance;
import pl.szczodrzynski.edziennik.datamodels.Event;
import pl.szczodrzynski.edziennik.datamodels.Grade;
import pl.szczodrzynski.edziennik.datamodels.GradeCategory;
import pl.szczodrzynski.edziennik.datamodels.Lesson;
import pl.szczodrzynski.edziennik.datamodels.LessonChange;
import pl.szczodrzynski.edziennik.datamodels.LoginStore;
import pl.szczodrzynski.edziennik.datamodels.LuckyNumber;
import pl.szczodrzynski.edziennik.datamodels.Message;
import pl.szczodrzynski.edziennik.datamodels.MessageFull;
import pl.szczodrzynski.edziennik.datamodels.MessageRecipient;
import pl.szczodrzynski.edziennik.datamodels.MessageRecipientFull;
import pl.szczodrzynski.edziennik.datamodels.Metadata;
import pl.szczodrzynski.edziennik.datamodels.Notice;
import pl.szczodrzynski.edziennik.datamodels.Profile;
import pl.szczodrzynski.edziennik.datamodels.ProfileFull;
import pl.szczodrzynski.edziennik.datamodels.Subject;
import pl.szczodrzynski.edziennik.datamodels.Teacher;
import pl.szczodrzynski.edziennik.datamodels.Team;
import pl.szczodrzynski.edziennik.messages.MessagesComposeInfo;
import pl.szczodrzynski.edziennik.models.Date;
import pl.szczodrzynski.edziennik.models.Endpoint;
import pl.szczodrzynski.edziennik.models.Time;
import pl.szczodrzynski.edziennik.models.Week;

import static pl.szczodrzynski.edziennik.api.AppError.CODE_INVALID_LOGIN;
import static pl.szczodrzynski.edziennik.api.AppError.CODE_MAINTENANCE;
import static pl.szczodrzynski.edziennik.api.AppError.CODE_OTHER;
import static pl.szczodrzynski.edziennik.datamodels.Attendance.TYPE_ABSENT;
import static pl.szczodrzynski.edziennik.datamodels.Attendance.TYPE_ABSENT_EXCUSED;
import static pl.szczodrzynski.edziennik.datamodels.Attendance.TYPE_BELATED;
import static pl.szczodrzynski.edziennik.datamodels.Attendance.TYPE_CUSTOM;
import static pl.szczodrzynski.edziennik.datamodels.Attendance.TYPE_PRESENT;
import static pl.szczodrzynski.edziennik.datamodels.Attendance.TYPE_RELEASED;
import static pl.szczodrzynski.edziennik.datamodels.Event.TYPE_DEFAULT;
import static pl.szczodrzynski.edziennik.datamodels.Event.TYPE_EXAM;
import static pl.szczodrzynski.edziennik.datamodels.Event.TYPE_SHORT_QUIZ;
import static pl.szczodrzynski.edziennik.datamodels.Grade.TYPE_SEMESTER1_FINAL;
import static pl.szczodrzynski.edziennik.datamodels.Grade.TYPE_SEMESTER1_PROPOSED;
import static pl.szczodrzynski.edziennik.datamodels.Grade.TYPE_SEMESTER2_FINAL;
import static pl.szczodrzynski.edziennik.datamodels.Grade.TYPE_SEMESTER2_PROPOSED;
import static pl.szczodrzynski.edziennik.datamodels.Grade.TYPE_YEAR_FINAL;
import static pl.szczodrzynski.edziennik.datamodels.Grade.TYPE_YEAR_PROPOSED;
import static pl.szczodrzynski.edziennik.datamodels.LoginStore.LOGIN_TYPE_MOBIDZIENNIK;
import static pl.szczodrzynski.edziennik.datamodels.Message.TYPE_DELETED;
import static pl.szczodrzynski.edziennik.datamodels.Message.TYPE_RECEIVED;
import static pl.szczodrzynski.edziennik.datamodels.Message.TYPE_SENT;
import static pl.szczodrzynski.edziennik.utils.Utils.bs;
import static pl.szczodrzynski.edziennik.utils.Utils.crc16;
import static pl.szczodrzynski.edziennik.utils.Utils.d;
import static pl.szczodrzynski.edziennik.utils.Utils.monthFromName;
import static pl.szczodrzynski.edziennik.utils.Utils.strToInt;

public class Mobidziennik implements EdziennikInterface {
    public Mobidziennik(App app) {
        this.app = app;
    }

    private static final String TAG = "api.Mobidziennik";

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
    private SparseArray<Long> gradeAddedDates;
    private SparseArray<Float> gradeAverages;
    private SparseIntArray gradeColors;
    private List<Grade> gradeList;
    private List<Event> eventList;
    private List<Notice> noticeList;
    private List<Attendance> attendanceList;
    private List<Message> messageList;
    private List<MessageRecipient> messageRecipientList;
    private List<MessageRecipient> messageRecipientIgnoreList;
    private List<Metadata> metadataList;
    private List<Metadata> messageMetadataList;

    private static boolean fakeLogin = false && !(BuildConfig.BUILD_TYPE.equals("release"));
    private String lastLogin = null;
    private long lastLoginTime = 0;
    private String lastResponse = null;
    private String loginServerName = null;
    private String loginUsername = null;
    private String loginPassword = null;
    private int studentId = -1;

    private long attendancesLastSync = 0;

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

        this.loginServerName = loginStore.getLoginData("serverName", "");
        this.loginUsername = loginStore.getLoginData("username", "");
        this.loginPassword = loginStore.getLoginData("password", "");
        if (loginServerName.equals("") || loginUsername.equals("") || loginPassword.equals("")) {
            finishWithError(new AppError(TAG, 157, AppError.CODE_INVALID_LOGIN, "Login field is empty"));
            return false;
        }
        this.studentId = profile == null ? -1 : profile.getStudentData("studentId", -1);
        this.attendancesLastSync = profile == null ? 0 : profile.getStudentData("attendancesLastSync", (long)0);
        fakeLogin = BuildConfig.DEBUG && loginUsername.toLowerCase().startsWith("fake");

        teamList = profileId == -1 ? new ArrayList<>() : app.db.teamDao().getAllNow(profileId);
        teacherList = profileId == -1 ? new ArrayList<>() : app.db.teacherDao().getAllNow(profileId);
        subjectList = new ArrayList<>();
        lessonList = new ArrayList<>();
        lessonChangeList = new ArrayList<>();
        gradeAddedDates = new SparseArray<>();
        gradeAverages = new SparseArray<>();
        gradeColors = new SparseIntArray();
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
            targetEndpoints.add("GetData");
            targetEndpoints.add("ProcessData");
            targetEndpoints.add("Attendances");
            targetEndpoints.add("ClassCalendar");
            targetEndpoints.add("GradeDetails");
            targetEndpoints.add("NoticeDetails");
            targetEndpoints.add("Messages");
            targetEndpoints.add("MessagesInbox");
            targetEndpoints.add("Finish");
            PROGRESS_COUNT = targetEndpoints.size()-1;
            PROGRESS_STEP = (90/PROGRESS_COUNT);
            begin();
        });
    }
    @Override
    public void syncFeature(@NonNull Context activityContext, @NonNull SyncCallback callback, @NonNull ProfileFull profile, @Nullable int ... featureList) {
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
            if (featureList.length == 1 && (featureList[0] == FEATURE_MESSAGES_INBOX || featureList[0] == FEATURE_MESSAGES_OUTBOX)) {
                teacherList = app.db.teacherDao().getAllNow(profileId);
                for (Teacher teacher: teacherList) {
                    teachersMap.put((int) teacher.id, teacher.getFullNameLastFirst());
                }
                if (featureList[0] == FEATURE_MESSAGES_INBOX) {
                    targetEndpoints.add("MessagesInbox");
                }
                else {
                    targetEndpoints.add("Messages");
                }
            }
            else {
                // this is needed for all features except messages
                targetEndpoints.add("GetData");
                targetEndpoints.add("ProcessData");
                for (int feature: featureList) {
                    switch (feature) {
                        case FEATURE_AGENDA:
                            targetEndpoints.add("ClassCalendar");
                            break;
                        case FEATURE_GRADES:
                            targetEndpoints.add("GradeDetails");
                            break;
                        case FEATURE_NOTICES:
                            targetEndpoints.add("NoticeDetails");
                            break;
                        case FEATURE_ATTENDANCES:
                            targetEndpoints.add("Attendances");
                            break;
                        case FEATURE_MESSAGES_INBOX:
                            targetEndpoints.add("MessagesInbox");
                            break;
                        case FEATURE_MESSAGES_OUTBOX:
                            targetEndpoints.add("Messages");
                            break;
                    }
                }
            }
            targetEndpoints.add("Finish");
            PROGRESS_COUNT = targetEndpoints.size()-1;
            PROGRESS_STEP = (90/PROGRESS_COUNT);
            begin();
        });
    }

    private void begin() {
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
            case "GetData":
                getData();
                break;
            case "ProcessData":
                processData();
                break;
            case "ClassCalendar":
                getClassCalendar();
                break;
            case "GradeDetails":
                getGradeDetails();
                break;
            case "NoticeDetails":
                getNoticeDetails();
                break;
            case "Attendances":
                getAttendances();
                break;
            case "Messages":
                getAllMessages();
                break;
            case "MessagesInbox":
                getMessagesInbox();
                break;
            case "Finish":
                finish();
                break;
        }
    }
    private void saveData() {
        if (teamList.size() > 0) {
            app.db.teamDao().clear(profileId);
            app.db.teamDao().addAll(teamList);
        }
        if (teacherList.size() > 0)
            app.db.teacherDao().addAllIgnore(teacherList);
        if (subjectList.size() > 0)
            app.db.subjectDao().addAll(subjectList);
        if (lessonList.size() > 0) {
            app.db.lessonDao().clear(profileId);
            app.db.lessonDao().addAll(lessonList);
        }
        if (lessonChangeList.size() > 0)
            app.db.lessonChangeDao().addAll(lessonChangeList);
        if (gradeList.size() > 0) {
            app.db.gradeDao().clearForSemester(profileId, profile.getCurrentSemester());
            app.db.gradeDao().addAll(gradeList);
            app.db.gradeDao().updateDetails(profileId, gradeAverages, gradeAddedDates, gradeColors);
        }
        if (eventList.size() > 0) {
            app.db.eventDao().removeFuture(profileId, today);
            app.db.eventDao().addAll(eventList);
        }
        if (noticeList.size() > 0) {
            app.db.noticeDao().clear(profileId);
            app.db.noticeDao().addAll(noticeList);
        }
        if (attendanceList.size() > 0) {
            // clear only last two weeks
            //app.db.attendanceDao().clearAfterDate(profileId, Date.getToday().stepForward(0, 0, -14));
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
        try {
            saveData();
        }
        catch (Exception e) {
            finishWithError(new AppError(TAG, 303, CODE_OTHER, app.getString(R.string.sync_error_saving_data), null, null, e, null));
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
        if (System.currentTimeMillis() - lastLoginTime < 10*60*1000
                && lastLogin.equals(loginServerName+":"+loginUsername)) {
            loginCallback.onSuccess();
            return;
        }
        app.cookieJar.clearForDomain(loginServerName+".mobidziennik.pl");

        Request.builder()
                .url(fakeLogin ? "https://szkolny.eu/mobimobi/mobi_mod_log.php" : "https://"+loginServerName+".mobidziennik.pl/api/")
                .userAgent(System.getProperty( "http.agent" ))
                .contentType("application/x-www-form-urlencoded; charset=UTF-8")
                .addParameter("wersja", "20")
                .addParameter("ip", app.deviceId)
                .addParameter("login", loginUsername)
                .addParameter("haslo", loginPassword)
                .addParameter("token", app.appConfig.fcmTokens.get(LOGIN_TYPE_MOBIDZIENNIK).first)
                .post()
                .callback(new TextCallbackHandler() {
                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        finishWithError(new AppError(TAG, 333, CODE_OTHER, response, throwable));
                    }

                    @Override
                    public void onSuccess(String data, Response response) {

                        //app.profile.loggedIn = (data != null && data.equals("ok"));
                        if (data == null || data.equals("")) { // for safety
                            finishWithError(new AppError(TAG, 339, CODE_MAINTENANCE, response));
                        }
                        else if (data.equals("Nie jestes zalogowany")) {
                            finishWithError(new AppError(TAG, 343, AppError.CODE_INVALID_LOGIN, response, data));
                        }
                        else if (data.equals("ifun")) {
                            finishWithError(new AppError(TAG, 346, AppError.CODE_INVALID_DEVICE, response, data));
                        }
                        else if (data.equals("stare haslo")) {
                            finishWithError(new AppError(TAG, 349, AppError.CODE_OLD_PASSWORD, response, data));
                        }
                        else if (data.equals("Archiwum")) {
                            finishWithError(new AppError(TAG, 352, AppError.CODE_ARCHIVED, response, data));
                        }
                        else if (data.equals("Trwają prace techniczne lub pojawił się jakiś problem")) {
                            finishWithError(new AppError(TAG, 355, CODE_MAINTENANCE, data, response, data));
                        }
                        else if (data.equals("ok"))
                        {
                            // a successful login
                            if (profile != null && studentId != -1) {
                                lastLogin = loginServerName+":"+loginUsername;
                                lastLoginTime = System.currentTimeMillis();
                                loginCallback.onSuccess();
                                return;
                            }
                            getData((data1 -> {
                                String[] tables = data1.split("T@B#LA");

                                List<Integer> studentIds = new ArrayList<>();
                                List<String> studentNamesLong = new ArrayList<>();
                                List<String> studentNamesShort = new ArrayList<>();
                                String[] student = tables[8].split("\n");
                                for (String aStudent : student) {
                                    if (aStudent.isEmpty()) {
                                        continue;
                                    }
                                    String[] student1 = aStudent.split("\\|", Integer.MAX_VALUE);
                                    if (student1.length == 2)
                                        continue;
                                    studentIds.add(strToInt(student1[0]));
                                    studentNamesLong.add(student1[2] + " " + student1[4]);
                                    studentNamesShort.add(student1[2] + " " + student1[4].charAt(0) + ".");
                                }

                                List<Profile> profileList = new ArrayList<>();

                                for (int index = 0; index < studentIds.size(); index++) {
                                    Profile profile = new Profile();
                                    profile.setStudentNameLong(studentNamesLong.get(index));
                                    profile.setStudentNameShort(studentNamesShort.get(index));
                                    profile.setName(profile.getStudentNameLong());
                                    profile.setSubname(loginUsername);
                                    profile.setEmpty(true);
                                    profile.setLoggedIn(true);
                                    profile.putStudentData("studentId", studentIds.get(index));
                                    profileList.add(profile);
                                }

                                callback.onLoginFirst(profileList, loginStore);
                            }));
                        }
                        else {
                            if (data.contains("Uuuups... nieprawidłowy adres")) {
                                finishWithError(new AppError(TAG, 404, AppError.CODE_INVALID_SERVER_ADDRESS, response, data));
                                return;
                            }
                            finishWithError(new AppError(TAG, 407, AppError.CODE_MAINTENANCE, response, data));
                        }
                    }
                })
                .build()
                .enqueue();
    }

    /*    _    _      _                                          _ _ _                _
         | |  | |    | |                       ___              | | | |              | |
         | |__| | ___| |_ __   ___ _ __ ___   ( _ )     ___ __ _| | | |__   __ _  ___| | _____
         |  __  |/ _ \ | '_ \ / _ \ '__/ __|  / _ \/\  / __/ _` | | | '_ \ / _` |/ __| |/ / __|
         | |  | |  __/ | |_) |  __/ |  \__ \ | (_>  < | (_| (_| | | | |_) | (_| | (__|   <\__ \
         |_|  |_|\___|_| .__/ \___|_|  |___/  \___/\/  \___\__,_|_|_|_.__/ \__,_|\___|_|\_\___/
                       | |
                       |*/
    private interface GetDataCallback {
        void onSuccess(String data);
    }
    private void getData(GetDataCallback getDataCallback) {
        callback.onActionStarted(R.string.sync_action_syncing);
        Request.builder()
                .url(fakeLogin ? "https://szkolny.eu/mobimobi/mobi_mod.php?"+System.currentTimeMillis() : "https://"+ loginServerName +".mobidziennik.pl/api/zrzutbazy")
                .userAgent(System.getProperty( "http.agent" ))
                .callback(new TextCallbackHandler() {
                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        finishWithError(new AppError(TAG, 427, CODE_OTHER, response, throwable));
                    }

                    @Override
                    public void onSuccess(String data, Response response) {
                        if (data == null || data.equals("")) {
                            finishWithError(new AppError(TAG, 432, CODE_MAINTENANCE, response));
                            return;
                        }
                        if (data.equals("Nie jestes zalogowany")) {
                            finishWithError(new AppError(TAG, 437, CODE_INVALID_LOGIN, response, data));
                            lastLoginTime = 0;
                            return;
                        }

                        lastResponse = data;

                        getDataCallback.onSuccess(data);
                    }
                })
                .build()
                .enqueue();
    }
    private void getData() {
        getData((data -> r("finish", "GetData")));
    }

    /*    _____        _          _____                            _
         |  __ \      | |        |  __ \                          | |
         | |  | | __ _| |_ __ _  | |__) |___  __ _ _   _  ___  ___| |_ ___
         | |  | |/ _` | __/ _` | |  _  // _ \/ _` | | | |/ _ \/ __| __/ __|
         | |__| | (_| | || (_| | | | \ \  __/ (_| | |_| |  __/\__ \ |_\__ \
         |_____/ \__,_|\__\__,_| |_|  \_\___|\__, |\__,_|\___||___/\__|___/
                                                | |
                                                |*/
    private void processData() {
        callback.onActionStarted(R.string.sync_action_processing_data);
        String[] tables = lastResponse.split("T@B#LA");

        if (studentId == -1) {
            return;
        }
        try {
            int i = 0;
            for (String table : tables) {
                if (i == 0) {
                    processUsers(table);
                }
                if (i == 3) {
                    processDates(table);
                }
                if (i == 4) {
                    processSubjects(table);
                }
                if (i == 7) {
                    processTeams(table, null);
                }
                if (i == 8) {
                    processStudent(table, loginUsername);
                }
                if (i == 9) {
                    processTeams(null, table);
                }
                if (i == 14) {
                    processGradeCategories(table);
                }
                if (i == 15) {
                    processLessons(table);
                }
                if (i == 16) {
                    processAttendances(table);
                }
                if (i == 17) {
                    processNotices(table);
                }
                if (i == 18) {
                    processGrades(table);
                }
                if (i == 21) {
                    processEvents(table);
                }
                if (i == 23) {
                    processHomeworks(table);
                }
                if (i == 24) {
                    processTimetable(table);
                }
                i++;
            }
            r("finish", "ProcessData");
        }
        catch (Exception e) {
            finishWithError(new AppError(TAG, 511, CODE_OTHER, e, lastResponse));
        }
    }

    private void getClassCalendar() {
        callback.onActionStarted(R.string.sync_action_syncing_calendar);
        Request.builder()
                .url(fakeLogin ? "https://szkolny.eu/mobimobi/mobi_mod_kalendarzklasowy.php" : "https://" + loginServerName + ".mobidziennik.pl/dziennik/kalendarzklasowy")
                .userAgent(System.getProperty("http.agent"))
                .callback(new TextCallbackHandler() {
                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        finishWithError(new AppError(TAG, 523, CODE_OTHER, response, throwable));
                    }

                    @Override
                    public void onSuccess(String data, Response response) {
                        // just skip any failures here
                        if (data == null || data.equals("")) {
                            r("finish", "ClassCalendar");
                            return;
                        }
                        if (data.contains("nie-pamietam-hasla")) {
                            r("finish", "ClassCalendar");
                            return;
                        }

                        if (profile.getLuckyNumberEnabled()
                                && (profile.getLuckyNumberDate() == null || profile.getLuckyNumberDate().getValue() != Date.getToday().getValue() || profile.getLuckyNumber() == -1)) {
                            // set the current date as we checked the lucky number today
                            profile.setLuckyNumber(-1);
                            profile.setLuckyNumberDate(Date.getToday());

                            Matcher matcher = Pattern.compile("class=\"szczesliwy_numerek\".*>0*([0-9]+)(?:/0*[0-9]+)*</a>", Pattern.DOTALL).matcher(data);
                            if (matcher.find()) {
                                try {
                                    profile.setLuckyNumber(Integer.parseInt(matcher.group(1)));
                                } catch (Exception e3) {
                                    e3.printStackTrace();
                                }
                            }
                            app.db.luckyNumberDao().add(new LuckyNumber(profileId, profile.getLuckyNumberDate(), profile.getLuckyNumber()));
                        }

                        Matcher matcher = Pattern.compile("events: (.+),$", Pattern.MULTILINE).matcher(data);
                        if (matcher.find()) {
                            try {
                                //d(TAG, matcher.group(1));
                                JsonArray events = new JsonParser().parse(matcher.group(1)).getAsJsonArray();
                                //d(TAG, "Events size "+events.size());
                                for (JsonElement eventEl: events) {
                                    JsonObject event = eventEl.getAsJsonObject();

                                    //d(TAG, "Event "+event.toString());

                                    JsonElement idEl = event.get("id");
                                    String idStr;
                                    if (idEl != null && (idStr = idEl.getAsString()).startsWith("kalendarz;")) {
                                        String[] idParts = idStr.split(";", Integer.MAX_VALUE);
                                        if (idParts.length > 2) {
                                            long id = idParts[2].isEmpty() ? -1 : strToInt(idParts[2]);
                                            long targetStudentId = strToInt(idParts[1]);
                                            if (targetStudentId != studentId)
                                                continue;

                                            // TODO null-safe getAs..()
                                            Date eventDate = Date.fromY_m_d(event.get("start").getAsString());
                                            //if (eventDate.getValue() < today.getValue())
                                            //    continue;

                                            String eventColor = event.get("color").getAsString();

                                            int eventType = Event.TYPE_INFORMATION;

                                            switch (eventColor) {
                                                case "#C54449":
                                                case "#c54449":
                                                    eventType = Event.TYPE_SHORT_QUIZ;
                                                    break;
                                                case "#AB0001":
                                                case "#ab0001":
                                                    eventType = Event.TYPE_EXAM;
                                                    break;
                                                case "#008928":
                                                    eventType = Event.TYPE_CLASS_EVENT;
                                                    break;
                                                case "#b66000":
                                                case "#B66000":
                                                    eventType = Event.TYPE_EXCURSION;
                                                    break;
                                            }

                                            String title = event.get("title").getAsString();
                                            String comment = event.get("comment").getAsString();

                                            String topic = title;
                                            if (!title.equals(comment)) {
                                                topic += "\n"+comment;
                                            }

                                            if (id == -1) {
                                                id = crc16(topic.getBytes());
                                            }

                                            Event eventObject = new Event(
                                                    profileId,
                                                    id,
                                                    eventDate,
                                                    null,
                                                    topic,
                                                    -1,
                                                    eventType,
                                                    false,
                                                    -1,
                                                    -1,
                                                    teamClass != null ? teamClass.id : -1
                                            );


                                            //d(TAG, "Event "+eventObject);
                                            eventList.add(eventObject);
                                            metadataList.add(new Metadata(profileId, Metadata.TYPE_EVENT, eventObject.id, profile.getEmpty(), profile.getEmpty(), System.currentTimeMillis() /* no addedDate here though */));
                                        }
                                    }
                                }
                                r("finish", "ClassCalendar");
                            } catch (Exception e3) {
                                finishWithError(new AppError(TAG, 638, CODE_OTHER, response, e3, data));
                            }
                        }
                        else {
                            r("finish", "ClassCalendar");
                        }
                    }
                })
                .build()
                .enqueue();
    }

    private void getGradeDetails() {
        callback.onActionStarted(R.string.sync_action_syncing_grade_details);
        Request.builder()
                .url(fakeLogin ? "https://szkolny.eu/mobimobi/mobi_mod_oceny.php" : "https://" + loginServerName + ".mobidziennik.pl/dziennik/oceny?semestr="+ profile.getCurrentSemester())
                .userAgent(System.getProperty("http.agent"))
                .callback(new TextCallbackHandler() {
                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        finishWithError(new AppError(TAG, 658, CODE_OTHER, response, throwable));
                    }

                    @Override
                    public void onSuccess(String data, Response response) {
                        // just skip any failures here
                        if (data == null || data.equals("")) {
                            r("finish", "GradeDetails");
                            return;
                        }
                        if (data.contains("nie-pamietam-hasla")) {
                            r("finish", "GradeDetails");
                            return;
                        }

                        try {
                            Document doc = Jsoup.parse(data);

                            Elements grades = doc.select("table.spis a, table.spis span, table.spis div");

                            String gradeCategory = "";
                            int gradeColor = -1;
                            String subjectName = "";

                            for (Element e: grades) {
                                switch (e.tagName()) {
                                    case "div": {
                                        //d(TAG, "Outer HTML "+e.outerHtml());
                                        Matcher matcher = Pattern.compile("<div.*?>\\n*\\s*(.+?)\\n*(?:<.*?)??</div>", Pattern.DOTALL).matcher(e.outerHtml());
                                        if (matcher.find()) {
                                            subjectName = matcher.group(1);
                                        }
                                        break;
                                    }
                                    case "span": {
                                        String css = e.attr("style");
                                        Matcher matcher = Pattern.compile("background-color:([#A-Fa-f0-9]+);", Pattern.DOTALL).matcher(css);
                                        if (matcher.find()) {
                                            gradeColor = Color.parseColor(matcher.group(1));
                                        }
                                        matcher = Pattern.compile(">&nbsp;(.+?):</span>", Pattern.DOTALL).matcher(e.outerHtml());
                                        if (matcher.find()) {
                                            gradeCategory = matcher.group(1);
                                        }
                                        break;
                                    }
                                    case "a": {
                                        int gradeId = strToInt(e.attr("rel"));
                                        float gradeClassAverage = -1;
                                        Date gradeAddedDate = null;
                                        long gradeAddedDateMillis = -1;

                                        String html = e.html();
                                        Matcher matcher = Pattern.compile("Średnia ocen:.*<strong>([0-9]*\\.?[0-9]*)</strong>", Pattern.DOTALL).matcher(html);
                                        if (matcher.find()) {
                                            gradeClassAverage = Float.parseFloat(matcher.group(1));
                                        }

                                        matcher = Pattern.compile("Wpisano:.*<strong>.+?,\\s([0-9]+)\\s(.+?)\\s([0-9]{4}),\\sgodzina\\s([0-9:]+)</strong>", Pattern.DOTALL).matcher(html);
                                        int month = 1;
                                        if (matcher.find()) {
                                            switch (matcher.group(2)) {
                                                case "stycznia":
                                                    month = 1;
                                                    break;
                                                case "lutego":
                                                    month = 2;
                                                    break;
                                                case "marca":
                                                    month = 3;
                                                    break;
                                                case "kwietnia":
                                                    month = 4;
                                                    break;
                                                case "maja":
                                                    month = 5;
                                                    break;
                                                case "czerwca":
                                                    month = 6;
                                                    break;
                                                case "lipca":
                                                    month = 7;
                                                    break;
                                                case "sierpnia":
                                                    month = 8;
                                                    break;
                                                case "września":
                                                    month = 9;
                                                    break;
                                                case "października":
                                                    month = 10;
                                                    break;
                                                case "listopada":
                                                    month = 11;
                                                    break;
                                                case "grudnia":
                                                    month = 12;
                                                    break;
                                            }
                                            gradeAddedDate = new Date(
                                                    strToInt(matcher.group(3)),
                                                    month,
                                                    strToInt(matcher.group(1))
                                            );
                                            Time time = Time.fromH_m_s(
                                                    matcher.group(4)
                                            );
                                            gradeAddedDateMillis = gradeAddedDate.combineWith(time);
                                        }

                                        matcher = Pattern.compile("Liczona do średniej:.*?<strong>nie<br/?></strong>", Pattern.DOTALL).matcher(html);
                                        if (matcher.find()) {
                                            matcher = Pattern.compile("<strong.*?>(.+?)</strong>.*?<sup>.+?</sup>.*?<small>\\((.+?)\\)</small>.*?<span>.*?Wartość oceny:.*?<strong>([0-9.]+)</strong>.*?Wpisał\\(a\\):.*?<strong>(.+?)</strong>", Pattern.DOTALL).matcher(html);
                                            if (matcher.find()) {
                                                String gradeName = matcher.group(1);
                                                String gradeDescription = matcher.group(2);
                                                float gradeValue = Float.parseFloat(matcher.group(3));
                                                String teacherName = matcher.group(4);

                                                long teacherId = -1;
                                                long subjectId = -1;

                                                //d(TAG, "Grade "+gradeName+" "+gradeCategory+" subject "+subjectName+" teacher "+teacherName);

                                                if (!subjectName.equals("")) {
                                                    int subjectIndex = -1;
                                                    for (int i = 0; i < subjectsMap.size(); i++) {
                                                        if (subjectsMap.valueAt(i).equals(subjectName)) {
                                                            subjectIndex = i;
                                                        }
                                                    }
                                                    //d(TAG, "subjectIndex "+subjectIndex);
                                                    if (subjectIndex >= 0 && subjectIndex < subjectsMap.size()) {
                                                        subjectId = subjectsMap.keyAt(subjectIndex);
                                                    }
                                                }

                                                if (!teacherName.equals("")) {
                                                    //d(TAG, "teacherName "+teacherName);
                                                    int teacherIndex = -1;
                                                    for (int i = 0; i < teachersMap.size(); i++) {
                                                        if (teachersMap.valueAt(i).equals(teacherName)) {
                                                            teacherIndex = i;
                                                        }
                                                    }
                                                    //d(TAG, "teacherIndex "+teacherIndex);
                                                    if (teacherIndex >= 0 && teacherIndex < teachersMap.size()) {
                                                        teacherId = teachersMap.keyAt(teacherIndex);
                                                    }
                                                }

                                                int semester = profile.dateToSemester(gradeAddedDate);

                                                Grade gradeObject = new Grade(
                                                        profileId,
                                                        gradeId,
                                                        gradeCategory,
                                                        gradeColor,
                                                        "NLDŚR, "+gradeDescription,
                                                        gradeName,
                                                        gradeValue,
                                                        0,
                                                        semester,
                                                        teacherId,
                                                        subjectId
                                                );

                                                gradeObject.classAverage = gradeClassAverage;

                                                gradeList.add(gradeObject);
                                                metadataList.add(new Metadata(profileId, Metadata.TYPE_GRADE, gradeObject.id, profile.getEmpty(), profile.getEmpty(), gradeAddedDateMillis));
                                            }
                                        } else {
                                            gradeAverages.put(gradeId, gradeClassAverage);
                                            gradeAddedDates.put(gradeId, gradeAddedDateMillis);
                                            gradeColors.put(gradeId, gradeColor);
                                        }
                                        break;
                                    }
                                }
                            }

                            r("finish", "GradeDetails");
                        }
                        catch (Exception e) {
                            finishWithError(new AppError(TAG, 852, CODE_OTHER, response, e, data));
                        }
                    }
                })
                .build()
                .enqueue();
    }

    // TODO: 2019-06-04 punkty z zachowania znikają po synchronizacji 
    private void getNoticeDetails() {
        if (noticeList.size() == 0) {
            r("finish", "NoticeDetails");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_notice_details);
        Request.builder()
                .url(fakeLogin ? "https://szkolny.eu/mobimobi/mobi_mod_zachowanie.php" : "https://" + loginServerName + ".mobidziennik.pl/mobile/zachowanie")
                .userAgent(System.getProperty("http.agent"))
                .callback(new TextCallbackHandler() {
                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        finishWithError(new AppError(TAG, 873, CODE_OTHER, response, throwable));
                    }

                    @Override
                    public void onSuccess(String data, Response response) {
                        // just skip any failures here
                        if (data == null || data.equals("")) {
                            r("finish", "NoticeDetails");
                            return;
                        }
                        if (data.contains("nie-pamietam-hasla")) {
                            r("finish", "NoticeDetails");
                            return;
                        }

                        Matcher matcher = Pattern.compile("(?:<span style=\"color:#[a-f0-9]+\">([0-9-.,]+)</span>.+?)?<div data-role=\"popup\" id=\"uwaga_tooltip([0-9]+)\">.+?Treść:.+?(?:Kategoria: <b>(.+?)</b>.+?)?Czas", Pattern.DOTALL).matcher(data);
                        while (matcher.find()) {
                            try {
                                String pointsStr = matcher.group(1);
                                String idStr = matcher.group(2);
                                String categoryStr = matcher.groupCount() == 3 ? matcher.group(3) : null;
                                float points = pointsStr == null ? 0 : Float.parseFloat(pointsStr);
                                long id = Long.parseLong(idStr);
                                for (Notice notice: noticeList) {
                                    if (notice.id != id)
                                        continue;
                                    notice.points = points;
                                    notice.category = categoryStr;
                                }
                            } catch (Exception e) {
                                Crashlytics.logException(e);
                                e.printStackTrace();
                                if (onlyFeature == FEATURE_NOTICES)
                                    finishWithError(new AppError(TAG, 880, CODE_OTHER, response, e, data));
                                else
                                    r("finish", "NoticeDetails");
                                return;
                            }
                        }
                        r("finish", "NoticeDetails");
                    }
                })
                .build()
                .enqueue();
    }

    public static class AttendanceLessonRange {
        int weekDay;
        int lessonNumber;
        Time startTime;
        Time endTime;
        List<AttendanceLesson> lessons;

        public AttendanceLessonRange(int section, int number, int lessonNumber, Time startTime, Time endTime) {
            this.weekDay = section == 1 ? number + 4 : number;
            this.lessonNumber = lessonNumber;
            this.startTime = startTime;
            this.endTime = endTime;
            this.lessons = new ArrayList<>();
        }
        public void addLesson(AttendanceLesson lesson) {
            lessons.add(lesson);
        }
    }
    public static class AttendanceLesson {
        String subjectName;
        String topic;
        String teamName;
        String teacherName;
        long lessonId;

        public AttendanceLesson(String subjectName, String topic, String teamName, String teacherName, long lessonId) {
            this.subjectName = subjectName;
            this.topic = topic;
            this.teamName = teamName;
            this.teacherName = teacherName;
            this.lessonId = lessonId;
        }
    }
    private Date attendancesCheckDate = Week.getWeekStart();
    private void getAttendances() {
        r("finish", "Attendances");
        // TODO: 2019-09-10 please download attendances from /dziennik/frekwencja. /mobile does not work above v13.0
        if (true) {
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_attendances);
        d(TAG, "Get attendances for week "+attendancesCheckDate.getStringY_m_d());
        Request.builder()
                .url(fakeLogin ? "https://szkolny.eu/mobimobi/mobi_mod_frekwencja.php" : "https://" + loginServerName + ".mobidziennik.pl/mobile/frekwencja")
                .userAgent(System.getProperty("http.agent"))
                .addParameter("uczen", studentId)
                .addParameter("data_poniedzialek", attendancesCheckDate.getStringY_m_d())
                .post()
                .callback(new TextCallbackHandler() {
                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        finishWithError(new AppError(TAG, 944, CODE_OTHER, response, throwable));
                    }

                    @Override
                    public void onSuccess(String data, Response response) {
                        // just skip any failures here
                        if (data == null || data.equals("")) {
                            r("finish", "Attendances");
                            return;
                        }
                        if (data.contains("nie-pamietam-hasla")) {
                            r("finish", "Attendances");
                            return;
                        }

                        List<AttendanceLessonRange> ranges = new ArrayList<>();

                        try {
                            Matcher matcher = Pattern.compile("<div data-role=\"popup\" id=\"popup([0-9])-([0-9])-([0-9]{1,3})\">.*?<p>([0-9]{2}):([0-9]{2}) - ([0-9]{2}):([0-9]{2}) (.+?)\\s*?<p>.*?</div>", Pattern.DOTALL).matcher(data);
                            while (matcher.find()) {
                                int section = strToInt(matcher.group(1));
                                int number = strToInt(matcher.group(2));
                                int lessonNumber = strToInt(matcher.group(3));
                                int startHour = strToInt(matcher.group(4));
                                int startMinute = strToInt(matcher.group(5));
                                int endHour = strToInt(matcher.group(6));
                                int endMinute = strToInt(matcher.group(7));
                                String lessonsString = matcher.group(8);
                                AttendanceLessonRange range = new AttendanceLessonRange(section, number, lessonNumber, new Time(startHour, startMinute, 0), new Time(endHour, endMinute, 0));
                                Matcher matcher2 = Pattern.compile("<strong>(.+?) - (.+?)??</strong>.+?<small>.+?\\(([0-9]{1,2}) (.+?),\\s+(.+?), id lekcji: ([0-9]+?)\\).+?</small>", Pattern.DOTALL).matcher(lessonsString);
                                while (matcher2.find()) {
                                    String topic = matcher2.group(2);
                                    topic = topic == null ? "" : Html.fromHtml(topic).toString();
                                    if (topic.startsWith("Lekcja odwołana:"))
                                        continue;
                                    String teamName = matcher2.group(3)+matcher2.group(4);
                                    boolean notFound = true;
                                    for (Team team: teamList) {
                                        if (teamName.equals(team.name)) {
                                            notFound = false;
                                            break;
                                        }
                                    }
                                    if (notFound)
                                        continue;
                                    range.addLesson(
                                            new AttendanceLesson(
                                                    matcher2.group(1),
                                                    topic,
                                                    teamName,
                                                    matcher2.group(5),
                                                    strToInt(matcher2.group(6))
                                            )
                                    );
                                }
                                ranges.add(range);
                            }
                            matcher = Pattern.compile("<td\\s+?style=\"border-top:1px.+?>([.|sz+]+)?</td>", Pattern.DOTALL).matcher(data);
                            int index = 0;
                            while (matcher.find()) {
                                int currentIndex = index++;
                                String markers = matcher.groupCount() == 1 ? matcher.group(1) : null;
                                if (markers == null)
                                    continue;
                                AttendanceLessonRange range = ranges.get(currentIndex);

                                Date date = attendancesCheckDate.clone().stepForward(0, 0, range.weekDay);
                                long addedDate = date.combineWith(range.startTime);

                                int markerIndex = 0;
                                for (char marker: markers.toCharArray()) {
                                    int type = TYPE_CUSTOM;
                                    switch (marker) {
                                        case '.':
                                            type = TYPE_PRESENT;
                                            break;
                                        case '|':
                                            type = TYPE_ABSENT;
                                            break;
                                        case 's':
                                            type = TYPE_BELATED;
                                            break;
                                        case 'z':
                                            type = TYPE_RELEASED;
                                            break;
                                        case '+':
                                            type = TYPE_ABSENT_EXCUSED;
                                    }
                                    AttendanceLesson lesson;
                                    if (markerIndex >= range.lessons.size()) {
                                        lesson = new AttendanceLesson("", "Nieznana lekcja", "???", "", addedDate);
                                    }
                                    else {
                                        lesson = range.lessons.get(markerIndex);
                                    }

                                    long teacherId = -1;
                                    long subjectId = -1;

                                    int teacherIndex = -1;
                                    for (int i = 0; i < teachersMap.size(); i++) {
                                        if (teachersMap.valueAt(i).equals(lesson.teacherName)) {
                                            teacherIndex = i;
                                        }
                                    }
                                    if (teacherIndex >= 0 && teacherIndex < teachersMap.size()) {
                                        teacherId = teachersMap.keyAt(teacherIndex);
                                    }

                                    int subjectIndex = -1;
                                    for (int i = 0; i < subjectsMap.size(); i++) {
                                        if (subjectsMap.valueAt(i).equals(lesson.subjectName)) {
                                            subjectIndex = i;
                                        }
                                    }
                                    if (subjectIndex >= 0 && subjectIndex < subjectsMap.size()) {
                                        subjectId = subjectsMap.keyAt(subjectIndex);
                                    }

                                    Attendance attendanceObject = new Attendance(
                                            profileId,
                                            lesson.lessonId,
                                            teacherId,
                                            subjectId,
                                            profile.dateToSemester(date),
                                            lesson.topic,
                                            date,
                                            range.startTime,
                                            type);
                                    markerIndex++;
                                    attendanceList.add(attendanceObject);
                                    if (attendanceObject.type != TYPE_PRESENT) {
                                        boolean markAsRead = onlyFeature == FEATURE_ATTENDANCES && attendancesLastSync == 0;
                                        metadataList.add(new Metadata(profileId, Metadata.TYPE_ATTENDANCE, attendanceObject.id, profile.getEmpty() || markAsRead, profile.getEmpty() || markAsRead, addedDate));
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Crashlytics.logException(e);
                            e.printStackTrace();
                            if (onlyFeature == FEATURE_ATTENDANCES)
                                finishWithError(new AppError(TAG, 955, CODE_OTHER, response, e, data));
                            else
                                r("finish", "Attendances");
                            return;
                        }

                        if (onlyFeature == FEATURE_ATTENDANCES) {
                            // syncing attendances exclusively
                            if (attendancesLastSync == 0) {
                                // first sync - get attendances until it's start of the school year
                                attendancesLastSync = profile.getSemesterStart(1).getInMillis();
                            }
                            Date lastSyncDate = Date.fromMillis(attendancesLastSync);
                            lastSyncDate.stepForward(0, 0, -7);
                            if (lastSyncDate.getValue() < attendancesCheckDate.getValue()) {
                                attendancesCheckDate.stepForward(0, 0, -7);
                                r("get", "Attendances");
                            }
                            else {
                                profile.putStudentData("attendancesLastSync", System.currentTimeMillis());
                                r("finish", "Attendances");
                            }
                        }
                        else {
                            if (attendancesLastSync != 0) {
                                // not a first sync
                                Date lastSyncDate = Date.fromMillis(attendancesLastSync);
                                lastSyncDate.stepForward(0, 0, 2);
                                if (lastSyncDate.getValue() >= attendancesCheckDate.getValue()) {
                                    profile.putStudentData("attendancesLastSync", System.currentTimeMillis());
                                }
                            }
                            r("finish", "Attendances");
                        }
                    }
                })
                .build()
                .enqueue();
    }

    private void getAllMessages() {
        if (!fullSync && onlyFeature != FEATURE_MESSAGES_OUTBOX) {
            r("finish", "Messages");
            return;
        }
        callback.onActionStarted(R.string.sync_action_syncing_messages);
        Request.builder()
                .url(fakeLogin ? "https://szkolny.eu/mobimobi/mobi_mod_wiadomosci_szukaj.php" : "https://" + loginServerName + ".mobidziennik.pl/dziennik/wyszukiwarkawiadomosci?q=+")
                .userAgent(System.getProperty("http.agent"))
                .callback(new TextCallbackHandler() {
                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        finishWithError(new AppError(TAG, 1012, CODE_OTHER, response, throwable));
                    }

                    @Override
                    public void onSuccess(String data, Response response) {
                        // just skip any failures here
                        if (data == null || data.equals("")) {
                            r("finish", "GradeDetails");
                            return;
                        }
                        if (data.contains("nie-pamietam-hasla")) {
                            r("finish", "GradeDetails");
                            return;
                        }

                        try {
                            long startTime = System.currentTimeMillis();
                            Document doc = Jsoup.parse(data);

                            Element listElement = doc.getElementsByClass("spis").first();
                            if (listElement == null) {
                                r("finish", "Messages");
                                return;
                            }
                            Elements list = listElement.getElementsByClass("podswietl");
                            for (Element item: list) {
                                long id = Long.parseLong(item.attr("rel").replaceAll("[^\\d]", "" ));

                                Element subjectEl = item.select("td:eq(0) div").first();
                                String subject = subjectEl.text();

                                Element addedDateEl = item.select("td:eq(1)").first();
                                String addedDateStr = addedDateEl.text();
                                long addedDate = Date.fromIsoHm(addedDateStr);

                                Element typeEl = item.select("td:eq(2) img").first();
                                int type = TYPE_RECEIVED;
                                if (typeEl.outerHtml().contains("mail_send.png"))
                                    type = TYPE_SENT;

                                Element senderEl = item.select("td:eq(3) div").first();
                                long senderId = -1;
                                if (type == TYPE_RECEIVED) {
                                    String senderName = senderEl.text();
                                    for (int i = 0; i < teachersMap.size(); i++) {
                                        if (senderName.equals(teachersMap.valueAt(i))) {
                                            senderId = teachersMap.keyAt(i);
                                            break;
                                        }
                                    }
                                    messageRecipientList.add(new MessageRecipient(profileId, -1, id));
                                }
                                else {
                                    // TYPE_SENT, so multiple recipients possible
                                    String[] recipientNames = senderEl.text().split(", ");
                                    for (String recipientName: recipientNames) {
                                        long recipientId = -1;
                                        for (int i = 0; i < teachersMap.size(); i++) {
                                            if (recipientName.equals(teachersMap.valueAt(i))) {
                                                recipientId = teachersMap.keyAt(i);
                                                break;
                                            }
                                        }
                                        messageRecipientIgnoreList.add(new MessageRecipient(profileId, recipientId, id));
                                    }
                                }

                                Message message = new Message(
                                        profileId,
                                        id,
                                        subject,
                                        null,
                                        type,
                                        senderId,
                                        -1
                                );

                                messageList.add(message);
                                metadataList.add(new Metadata(profileId, Metadata.TYPE_MESSAGE, message.id, true, true, addedDate));
                            }

                        } catch (Exception e3) {
                            finishWithError(new AppError(TAG, 949, CODE_OTHER, response, e3, data));
                            return;
                        }

                        r("finish", "Messages");
                    }
                })
                .build()
                .enqueue();
    }

    private void getMessagesInbox() {
        callback.onActionStarted(R.string.sync_action_syncing_messages);
        Request.builder()
                .url(fakeLogin ? "https://szkolny.eu/mobimobi/mobi_mod_wiadomosci.php" : "https://" + loginServerName + ".mobidziennik.pl/dziennik/wiadomosci")
                .userAgent(System.getProperty("http.agent"))
                .callback(new TextCallbackHandler() {
                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        finishWithError(new AppError(TAG, 972, CODE_OTHER, response, throwable));
                    }

                    @Override
                    public void onSuccess(String data, Response response) {
                        // just skip any failures here
                        if (data == null || data.equals("")) {
                            r("finish", "GradeDetails");
                            return;
                        }
                        if (data.contains("nie-pamietam-hasla")) {
                            r("finish", "GradeDetails");
                            return;
                        }

                        try {
                            if (data.contains("Brak wiadomości odebranych.")) {
                                r("finish", "MessagesInbox");
                                return;
                            }
                            Document doc = Jsoup.parse(data);

                            Elements list = doc.getElementsByClass("spis").first().getElementsByClass("podswietl");
                            for (Element item: list) {
                                long id = Long.parseLong(item.attr("rel"));

                                Element subjectEl = item.select("td:eq(0)").first();
                                //d(TAG, "subjectEl "+subjectEl.outerHtml());
                                boolean hasAttachments = false;
                                if (subjectEl.getElementsByTag("a").size() != 0) {
                                    hasAttachments = true;
                                }
                                String subject = subjectEl.ownText();

                                Element addedDateEl = item.select("td:eq(1) small").first();
                                //d(TAG, "addedDateEl "+addedDateEl.outerHtml());
                                String addedDateStr = addedDateEl.text();
                                long addedDate = Date.fromIsoHm(addedDateStr);

                                Element senderEl = item.select("td:eq(2)").first();
                                //d(TAG, "senderEl "+senderEl.outerHtml());
                                String senderName = senderEl.ownText();
                                long senderId = -1;
                                for (int i = 0; i < teachersMap.size(); i++) {
                                    if (senderName.equals(teachersMap.valueAt(i))) {
                                        senderId = teachersMap.keyAt(i);
                                        break;
                                    }
                                }
                                messageRecipientIgnoreList.add(new MessageRecipient(profileId, -1, id));

                                boolean isRead = item.select("td:eq(3) span").first().hasClass("wiadomosc_przeczytana");

                                Message message = new Message(
                                        profileId,
                                        id,
                                        subject,
                                        null,
                                        TYPE_RECEIVED,
                                        senderId,
                                        -1
                                );

                                if (hasAttachments)
                                    message.setHasAttachments();


                                messageList.add(message);
                                messageMetadataList.add(new Metadata(profileId, Metadata.TYPE_MESSAGE, message.id, isRead, isRead || profile.getEmpty(), addedDate));
                            }
                            r("finish", "MessagesInbox");
                        } catch (Exception e3) {
                            finishWithError(new AppError(TAG, 1044, CODE_OTHER, response, e3, data));
                        }
                    }
                })
                .build()
                .enqueue();
    }

    private SparseArray<String> teachersMap = new SparseArray<>();
    private void processUsers(String table)
    {
        String[] users = table.split("\n");

        //app.db.teacherDao().clear(profileId);
        for (String userStr: users)
        {
            if (userStr.isEmpty()) {
                continue;
            }
            String[] user = userStr.split("\\|", Integer.MAX_VALUE);

            teachersMap.put(strToInt(user[0]), user[5]+" "+user[4]);
            teacherList.add(new Teacher(profileId, strToInt(user[0]), user[4], user[5]));
        }
    }

    private void processDates(String table)
    {
        String[] dates = table.split("\n");
        for (String dateStr: dates)
        {
            if (dateStr.isEmpty()) {
                continue;
            }
            String[] date = dateStr.split("\\|", Integer.MAX_VALUE);
            switch (date[1]) {
                case "semestr1_poczatek":
                    profile.setDateSemester1Start(Date.fromYmd(date[3]));
                    break;
                case "semestr2_poczatek":
                    profile.setDateSemester2Start(Date.fromYmd(date[3]));
                    break;
                case "koniec_roku_szkolnego":
                    profile.setDateYearEnd(Date.fromYmd(date[3]));
                    break;
            }
        }
    }

    private SparseArray<String> subjectsMap = new SparseArray<>();
    private void processSubjects(String table)
    {
        String[] subjects = table.split("\n");

        // because a student may no longer have a subject
        // which he had before
        // therefore, this subject's grades do not get deleted
        // so we should keep the subject for correct naming
        //app.db.subjectDao().clear(profileId);
        for (String subjectStr: subjects)
        {
            if (subjectStr.isEmpty()) {
                continue;
            }
            String[] subject = subjectStr.split("\\|", Integer.MAX_VALUE);

            subjectsMap.put(strToInt(subject[0]), subject[1]);
            subjectList.add(new Subject(profileId, strToInt(subject[0]), subject[1], subject[2]));
        }
    }

    private Team teamClass = null;
    private SparseArray<String> teamsMap = new SparseArray<>();
    private List<Team> allTeams = new ArrayList<>();
    private void processTeams(String tableTeams, String tableRelations)
    {
        if (tableTeams != null)
        {
            allTeams.clear();
            String[] teams = tableTeams.split("\n");
            for (String teamStr: teams) {
                if (teamStr.isEmpty()) {
                    continue;
                }
                String[] team = teamStr.split("\\|", Integer.MAX_VALUE);
                Team teamObject = new Team(
                        profileId,
                        strToInt(team[0]),
                        team[1]+team[2],
                        strToInt(team[3]),
                        loginServerName+":"+team[1]+team[2],// TODO zrobiłem to na szybko
                        strToInt(team[4], -1));
                allTeams.add(teamObject);
            }
        }
        if (tableRelations != null)
        {
            app.db.teamDao().clear(profileId);
            String[] teams = tableRelations.split("\n");
            for (String teamStr: teams) {
                if (teamStr.isEmpty()) {
                    continue;
                }
                String[] team = teamStr.split("\\|", Integer.MAX_VALUE);
                if (strToInt(team[1]) != studentId)
                    continue;
                Team teamObject = Team.getById(allTeams, strToInt(team[2]));
                if (teamObject != null) {
                    if (teamObject.type == 1) {
                        profile.setStudentNumber(strToInt(team[4]));
                        teamClass = teamObject;
                    }
                    teamsMap.put((int)teamObject.id, teamObject.name);
                    teamList.add(teamObject);
                }
            }
        }
    }

    private SparseArray<Pair<String[], String[]>> students = new SparseArray<>();
    private boolean processStudent(String table, String loginUsername)
    {
        students.clear();
        String[] student = table.split("\n");
        for (int i = 0; i < student.length; i++) {
            if (student[i].isEmpty()) {
                continue;
            }
            String[] student1 = student[i].split("\\|", Integer.MAX_VALUE);
            String[] student2 = student[++i].split("\\|", Integer.MAX_VALUE);
            students.put(strToInt(student1[0]), new Pair<>(student1, student2));
        }
        Pair<String[], String[]> studentData = students.get(studentId);
        try {
            profile.setAttendancePercentage(Float.parseFloat(studentData.second[1]));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private SparseArray<GradeCategory> gradeCategoryList = new SparseArray<>();
    private void processGradeCategories(String table)
    {
        String[] gradeCategories = table.split("\n");

        for (String gradeCategoryStr: gradeCategories)
        {
            if (gradeCategoryStr.isEmpty()) {
                continue;
            }
            String[] gradeCategory = gradeCategoryStr.split("\\|", Integer.MAX_VALUE);
            List<String> columns = new ArrayList<>();
            Collections.addAll(columns, gradeCategory[7].split(";"));

            if (teamsMap.indexOfKey(strToInt(gradeCategory[1])) >= 0) {
                gradeCategoryList.put(
                        Integer.parseInt(gradeCategory[0]),
                        new GradeCategory(
                                profileId,
                                Integer.parseInt(gradeCategory[0]),
                                Float.parseFloat(gradeCategory[3]),
                                Color.parseColor("#"+gradeCategory[6]),
                                gradeCategory[4]
                        ).addColumns(columns)
                );
            }
        }
    }

    private class MobiLesson {
        long id;
        int subjectId;
        int teacherId;
        int teamId;
        String topic;
        Date date;
        Time startTime;
        Time endTime;
        int presentCount;
        int absentCount;
        int lessonNumber;
        String signed;

        MobiLesson(long id, int subjectId, int teacherId, int teamId, String topic, Date date, Time startTime, Time endTime, int presentCount, int absentCount, int lessonNumber, String signed) {
            this.id = id;
            this.subjectId = subjectId;
            this.teacherId = teacherId;
            this.teamId = teamId;
            this.topic = topic;
            this.date = date;
            this.startTime = startTime;
            this.endTime = endTime;
            this.presentCount = presentCount;
            this.absentCount = absentCount;
            this.lessonNumber = lessonNumber;
            this.signed = signed;
        }
    }

    private List<MobiLesson> mobiLessons = new ArrayList<>();

    private MobiLesson getLesson(long id) {
        for (MobiLesson mobiLesson : mobiLessons) {
            if (mobiLesson.id == id)
                return mobiLesson;
        }
        return null;
    }

    private void processLessons(String table)
    {
        mobiLessons.clear();

        String[] lessons2 = table.split("\n");
        for (String lessonStr: lessons2)
        {
            if (lessonStr.isEmpty()) {
                continue;
            }
            String[] lesson = lessonStr.split("\\|", Integer.MAX_VALUE);

            MobiLesson newMobiLesson = new MobiLesson(
                    Long.parseLong(lesson[0]),
                    strToInt(lesson[1]),
                    strToInt(lesson[2]),
                    strToInt(lesson[3]),
                    lesson[4],
                    Date.fromYmd(lesson[5]),
                    Time.fromYmdHm(lesson[6]),
                    Time.fromYmdHm(lesson[7]),
                    strToInt(lesson[8]),
                    strToInt(lesson[9]),
                    strToInt(lesson[10]),
                    lesson[11]);
            mobiLessons.add(newMobiLesson);
        }
    }

    private void processAttendances(String table)
    {
        if (true)
            return;
        String[] attendances = table.split("\n");
        for (String attendanceStr: attendances)
        {
            if (attendanceStr.isEmpty()) {
                continue;
            }
            String[] attendance = attendanceStr.split("\\|", Integer.MAX_VALUE);

            if (strToInt(attendance[2]) != studentId)
                continue;
            //RegisterAttendance oldAttendance = RegisterAttendance.getById(oldAttendances, Long.parseLong(attendance[0]));

            MobiLesson mobiLesson = getLesson(Long.parseLong(attendance[1]));
            if (mobiLesson != null) {
                int type = TYPE_PRESENT;
                switch (attendance[4]) {
                    case "2":
                        type = TYPE_ABSENT;
                        break;
                    case "5":
                        type = TYPE_ABSENT_EXCUSED;
                        break;
                    case "4":
                        type = TYPE_RELEASED;
                        break;
                }

                int semester = profile.dateToSemester(mobiLesson.date);

                Attendance attendanceObject = new Attendance(
                        profileId,
                        strToInt(attendance[0]),
                        mobiLesson.teacherId,
                        mobiLesson.subjectId,
                        semester,
                        mobiLesson.topic,
                        mobiLesson.date,
                        mobiLesson.startTime,
                        type);
                attendanceList.add(attendanceObject);
                metadataList.add(new Metadata(profileId, Metadata.TYPE_ATTENDANCE, attendanceObject.id, profile.getEmpty(), profile.getEmpty(), System.currentTimeMillis()));
            }
        }

    }

    private void processNotices(String table)
    {
        String[] notices = table.split("\n");

        for (String noticeStr: notices)
        {
            if (noticeStr.isEmpty()) {
                continue;
            }
            String[] notice = noticeStr.split("\\|", Integer.MAX_VALUE);

            if (strToInt(notice[2]) != studentId)
                continue;

            Notice noticeObject = new Notice(
                    profileId,
                    strToInt(notice[0]),
                    notice[4],
                    strToInt(notice[6]),
                    (notice[3] != null ? (notice[3].equals("1") ? Notice.TYPE_POSITIVE : Notice.TYPE_NEGATIVE) : Notice.TYPE_NEUTRAL),
                    strToInt(notice[5])
            );

            Metadata metadata = new Metadata(profileId, Metadata.TYPE_NOTICE, noticeObject.id, profile.getEmpty(), profile.getEmpty(), new Date().parseFromYmd(notice[7]).getInMillis());

            noticeList.add(noticeObject);
            metadataList.add(metadata);
        }
    }

    private void processGrades(String table)
    {
        app.db.gradeDao().getDetails(profileId, gradeAddedDates, gradeAverages, gradeColors);

        String[] grades = table.split("\n");

        long addedDate = Date.getNowInMillis();
        for (String gradeStr: grades)
        {
            if (gradeStr.isEmpty()) {
                continue;
            }
            String[] grade = gradeStr.split("\\|", Integer.MAX_VALUE);

            if (strToInt(grade[1]) != studentId)
                continue;

            if (grade[6].equals("")) {
                grade[6] = "-1";
            }
            if (grade[10].equals("")) {
                grade[10] = "1";
            }

            float weight = 0.0f;
            String category = "";
            String description = "";
            int color = -1;
            int categoryId = strToInt(grade[6]);
            GradeCategory gradeCategory = gradeCategoryList.get(categoryId);
            if (gradeCategory != null) {
                weight = gradeCategory.weight;
                category = gradeCategory.text;
                description = gradeCategory.columns.get(strToInt(grade[10]) - 1);
                color = gradeCategory.color;
            }

            Grade gradeObject = new Grade(
                    profileId,
                    strToInt(grade[0]),
                    category,
                    color,
                    description,
                    grade[7],
                    Float.parseFloat(grade[11]),
                    weight,
                    strToInt(grade[5]),
                    strToInt(grade[2]),
                    strToInt(grade[3])
            );


            // fix for "0" value grades, so they're not counted in the average
            if (gradeObject.value == 0.0f) {
                gradeObject.weight = 0;
            }

            switch (grade[8]) {
                case "3":
                    // semester proposed
                    gradeObject.type = (gradeObject.semester == 1 ? TYPE_SEMESTER1_PROPOSED : TYPE_SEMESTER2_PROPOSED);
                    break;
                case "1":
                    // semester final
                    gradeObject.type = (gradeObject.semester == 1 ? TYPE_SEMESTER1_FINAL : TYPE_SEMESTER2_FINAL);
                    break;
                case "4":
                    // year proposed
                    gradeObject.type = TYPE_YEAR_PROPOSED;
                    break;
                case "2":
                    // year final
                    gradeObject.type = TYPE_YEAR_FINAL;
                    break;
            }

            Metadata metadata = new Metadata(profileId, Metadata.TYPE_GRADE, gradeObject.id, profile.getEmpty(), profile.getEmpty(), addedDate);
            gradeList.add(gradeObject);
            metadataList.add(metadata);
            addedDate++; // increase the added date to sort grades as they are in the school profile
        }
    }

    private void processEvents(String table)
    {
        String[] events = table.split("\n");
        Date today = Date.getToday();
        for (String eventStr: events)
        {
            if (eventStr.isEmpty()) {
                continue;
            }
            String[] event = eventStr.split("\\|", Integer.MAX_VALUE);

            Date eventDate = new Date().parseFromYmd(event[4]);
            if (eventDate.getValue() < today.getValue())
                continue;

            String examType = "";
            Pattern pattern = Pattern.compile("\\(([0-9A-ząęóżźńśłć]*?)\\)$");
            Matcher matcher = pattern.matcher(event[5]);
            if (matcher.find()) {
                examType = matcher.group(1);
            }

            long addedDate = 0;

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            try {
                addedDate = simpleDateFormat.parse(event[7]).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            int eventType = examType.equals("sprawdzian") ? TYPE_EXAM : examType.equals("kartkówka") ? TYPE_SHORT_QUIZ : TYPE_DEFAULT;
            Event eventObject = new Event(
                    profileId,
                    strToInt(event[0]),
                    eventDate,
                    new Time().parseFromYmdHm(event[6]),
                    event[5].replace("("+examType+")", ""),
                    -1,
                    eventType,
                    false,
                    strToInt(event[1]),
                    strToInt(event[3]),
                    strToInt(event[2])
            );

            if (teamsMap.indexOfKey((int)eventObject.teamId) >= 0) {
                eventList.add(eventObject);
                metadataList.add(new Metadata(profileId, Metadata.TYPE_EVENT, eventObject.id, profile.getEmpty(), profile.getEmpty(), addedDate));
            }
        }
    }

    private void processHomeworks(String table)
    {
        String[] homeworks = table.split("\n");
        Date today = Date.getToday();
        for (String homeworkStr: homeworks)
        {
            if (homeworkStr.isEmpty()) {
                continue;
            }
            String[] homework = homeworkStr.split("\\|", Integer.MAX_VALUE);

            Date eventDate = new Date().parseFromYmd(homework[2]);
            if (eventDate.getValue() < today.getValue())
                continue;

            Event eventObject = new Event(
                    profileId,
                    strToInt(homework[0]),
                    new Date().parseFromYmd(homework[2]),
                    new Time().parseFromYmdHm(homework[3]),
                    homework[1],
                    -1,
                    Event.TYPE_HOMEWORK,
                    false,
                    strToInt(homework[7]),
                    strToInt(homework[6]),
                    strToInt(homework[5])
            );

            if (teamsMap.indexOfKey((int)eventObject.teamId) >= 0) {
                eventList.add(eventObject);
                metadataList.add(new Metadata(profileId, Metadata.TYPE_HOMEWORK, eventObject.id, profile.getEmpty(), profile.getEmpty(), System.currentTimeMillis()));
            }
        }
    }

    private void processTimetable(String table)
    {
        String[] lessons = table.split("\n");
        List<String> list = Arrays.asList(lessons);
        //Collections.reverse(list);

        // searching for all planned lessons
        for (String lessonStr: (String[])list.toArray())
        {
            //Log.d(TAG, lessonStr);
            if (!lessonStr.isEmpty())
            {
                String[] lesson = lessonStr.split("\\|", Integer.MAX_VALUE);

                if (strToInt(lesson[0]) != studentId)
                    continue;

                if (lesson[1].equals("plan_lekcji") || lesson[1].equals("lekcja"))
                {
                    Lesson lessonObject = new Lesson(profileId, lesson[2], lesson[3], lesson[4]);

                    for(int i = 0; i < subjectsMap.size(); i++) {
                        int key = subjectsMap.keyAt(i);
                        String str = subjectsMap.valueAt(i);
                        if (lesson[5].equalsIgnoreCase(str)) {
                            lessonObject.subjectId = key;
                        }
                    }
                    for(int i = 0; i < teachersMap.size(); i++) {
                        int key = teachersMap.keyAt(i);
                        String str = teachersMap.valueAt(i);
                        if ((lesson[7] + " " + lesson[6]).equalsIgnoreCase(str)) {
                            lessonObject.teacherId = key;
                        }
                    }
                    for(int i = 0; i < teamsMap.size(); i++) {
                        int key = teamsMap.keyAt(i);
                        String str = teamsMap.valueAt(i);
                        if ((lesson[8] + lesson[9]).equalsIgnoreCase(str)) {
                            lessonObject.teamId = key;
                        }
                    }
                    lessonObject.classroomName = lesson[11];
                    lessonList.add(lessonObject);
                }
            }
        }

        // searching for all changes
        for (String lessonStr: (String[])list.toArray())
        {
            if (!lessonStr.isEmpty())
            {
                String[] lesson = lessonStr.split("\\|", Integer.MAX_VALUE);

                if (strToInt(lesson[0]) != studentId)
                    continue;

                if (lesson[1].equals("zastepstwo") || lesson[1].equals("lekcja_odwolana"))
                {
                    LessonChange lessonChange = new LessonChange(profileId, lesson[2], lesson[3], lesson[4]);

                    //Log.d(TAG, "Lekcja "+lessonStr);

                    for(int i = 0; i < subjectsMap.size(); i++) {
                        int key = subjectsMap.keyAt(i);
                        String str = subjectsMap.valueAt(i);
                        if (lesson[5].equalsIgnoreCase(str)) {
                            lessonChange.subjectId = key;
                        }
                    }
                    for(int i = 0; i < teachersMap.size(); i++) {
                        int key = teachersMap.keyAt(i);
                        String str = teachersMap.valueAt(i);
                        if ((lesson[7] + " " + lesson[6]).equalsIgnoreCase(str)) {
                            lessonChange.teacherId = key;
                        }
                    }
                    for(int i = 0; i < teamsMap.size(); i++) {
                        int key = teamsMap.keyAt(i);
                        String str = teamsMap.valueAt(i);
                        if ((lesson[8] + lesson[9]).equalsIgnoreCase(str)) {
                            lessonChange.teamId = key;
                        }
                    }

                    if (lesson[1].equals("zastepstwo")) {
                        lessonChange.type = LessonChange.TYPE_CHANGE;
                    }
                    if (lesson[1].equals("lekcja_odwolana")) {
                        lessonChange.type = LessonChange.TYPE_CANCELLED;
                    }
                    if (lesson[1].equals("lekcja")) {
                        lessonChange.type = LessonChange.TYPE_ADDED;
                    }
                    lessonChange.classroomName = lesson[11];

                    Lesson originalLesson = lessonChange.getOriginalLesson(lessonList);

                    if (lessonChange.type == LessonChange.TYPE_ADDED) {
                        if (originalLesson == null) {
                            // original lesson doesn't exist, save a new addition
                            // TODO
                            /*if (!RegisterLessonChange.existsAddition(app.profile, registerLessonChange)) {
                                app.profile.timetable.addLessonAddition(registerLessonChange);
                            }*/
                        }
                        else {
                            // original lesson exists, so we need to compare them
                            if (!lessonChange.matches(originalLesson)) {
                                // the lessons are different, so it's probably a lesson change
                                // ahhh this damn API
                                lessonChange.type = LessonChange.TYPE_CHANGE;
                            }
                        }

                    }
                    if (lessonChange.type != LessonChange.TYPE_ADDED) {
                        // it's not a lesson addition
                        lessonChangeList.add(lessonChange);
                        metadataList.add(new Metadata(profileId, Metadata.TYPE_LESSON_CHANGE, lessonChange.id, profile.getEmpty(), profile.getEmpty(), System.currentTimeMillis()));
                        if (originalLesson == null) {
                            // there is no original lesson, so we have to add one in order to change it
                            lessonList.add(Lesson.fromLessonChange(lessonChange));
                        }
                    }
                }
            }
        }
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
    public void syncMessages(@NonNull Context activityContext, @NonNull SyncCallback errorCallback, @NonNull ProfileFull profile) {

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
            boolean readByAll = true;
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

        login(() -> Request.builder()
                .url("https://" + loginServerName + ".mobidziennik.pl/dziennik/"+(message.type == TYPE_RECEIVED || message.type == TYPE_DELETED ? "wiadodebrana" : "wiadwyslana")+"/?id="+message.id)
                .userAgent(System.getProperty("http.agent"))
                .callback(new TextCallbackHandler() {
                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        finishWithError(new AppError(TAG, 1720, CODE_OTHER, response, throwable));
                    }

                    @Override
                    public void onSuccess(String data, Response response) {
                        if (data == null || data.equals("")) {
                            finishWithError(new AppError(TAG, 1727, CODE_MAINTENANCE, response));
                            return;
                        }
                        if (data.contains("nie-pamietam-hasla")) {
                            lastLoginTime = 0;
                            finishWithError(new AppError(TAG, 1732, CODE_INVALID_LOGIN, response, data));
                            return;
                        }

                        List<MessageRecipientFull> messageRecipientList = new ArrayList<>();

                        try {
                            Document doc = Jsoup.parse(data);

                            Element content = doc.select("#content").first();

                            Element body = content.select(".wiadomosc_tresc").first();

                            if (message.type == TYPE_RECEIVED) {
                                MessageRecipientFull recipient = new MessageRecipientFull(profileId, -1, message.id);
                                long readDate = System.currentTimeMillis();
                                Matcher matcher = Pattern.compile("czas przeczytania:.+?,\\s([0-9]+)\\s(.+?)\\s([0-9]{4}),\\sgodzina\\s([0-9:]+)", Pattern.DOTALL).matcher(body.html());
                                if (matcher.find()) {
                                    Date date = new Date(
                                            strToInt(matcher.group(3)),
                                            monthFromName(matcher.group(2)),
                                            strToInt(matcher.group(1))
                                    );
                                    Time time = Time.fromH_m_s(
                                            matcher.group(4)
                                    );
                                    readDate = date.combineWith(time);
                                }
                                recipient.readDate = readDate;
                                recipient.fullName = profile.getStudentNameLong();
                                messageRecipientList.add(recipient);
                            }
                            else {
                                message.senderId = -1;
                                message.senderReplyId = -1;

                                List<Teacher> teacherList = app.db.teacherDao().getAllNow(profileId);

                                Elements recipients = content.select("table.spis tr:has(td)");
                                for (Element recipientEl: recipients) {

                                    Element senderEl = recipientEl.select("td:eq(0)").first();
                                    Teacher teacher = null;
                                    String senderName = senderEl.text();
                                    for (Teacher aTeacher: teacherList) {
                                        if (aTeacher.getFullNameLastFirst().equals(senderName)) {
                                            teacher = aTeacher;
                                            break;
                                        }
                                    }

                                    long readDate = 0;
                                    Element isReadEl = recipientEl.select("td:eq(2)").first();
                                    if (!isReadEl.ownText().equals("NIE")) {
                                        Element readDateEl = recipientEl.select("td:eq(3) small").first();
                                        Matcher matcher = Pattern.compile(".+?,\\s([0-9]+)\\s(.+?)\\s([0-9]{4}),\\sgodzina\\s([0-9:]+)", Pattern.DOTALL).matcher(readDateEl.ownText());
                                        if (matcher.find()) {
                                            Date date = new Date(
                                                    strToInt(matcher.group(3)),
                                                    monthFromName(matcher.group(2)),
                                                    strToInt(matcher.group(1))
                                            );
                                            Time time = Time.fromH_m_s(
                                                    matcher.group(4)
                                            );
                                            readDate = date.combineWith(time);
                                        }
                                    }

                                    MessageRecipientFull recipient = new MessageRecipientFull(profileId, teacher == null ? -1 : teacher.id, message.id);
                                    recipient.readDate = readDate;
                                    recipient.fullName = teacher == null ? "?" : teacher.getFullName();
                                    messageRecipientList.add(recipient);
                                }
                            }

                            body.select("div").remove();

                            message.body = body.html();

                            message.clearAttachments();
                            Elements attachments = content.select("ul li");
                            if (attachments != null) {
                                //d(TAG, "attachments "+attachments.outerHtml());
                                for (Element attachment: attachments) {
                                    attachment = attachment.select("a").first();
                                    //d(TAG, "attachment "+attachment.outerHtml());
                                    String attachmentName = attachment.ownText();
                                    long attachmentId = -1;
                                    Matcher matcher = Pattern.compile("href=\"https://.+?\\.mobidziennik.pl/.+?&(?:amp;)?zalacznik=([0-9]+)\"(?:.+?<small.+?\\(([0-9.]+)\\s(M|K|G|)B\\))*", Pattern.DOTALL).matcher(attachment.outerHtml());
                                    if (matcher.find()) {
                                        attachmentId = Long.parseLong(matcher.group(1));
                                        String size = matcher.group(2);
                                        float attachmentSizeFloat = Float.parseFloat(size == null ? "-1" : size);
                                        String sizeMultiplier = matcher.group(3);
                                        switch (sizeMultiplier == null ? "" : sizeMultiplier) {
                                            case "K":
                                                attachmentSizeFloat *= 1024;
                                                break;
                                            case "M":
                                                attachmentSizeFloat *= 1024*1024;
                                                break;
                                            case "G":
                                                attachmentSizeFloat *= 1024*1024*1024;
                                                break;
                                        }
                                        message.addAttachment(attachmentId, attachmentName, (long) attachmentSizeFloat);
                                    }
                                }
                            }
                        }
                        catch (Exception e) {
                            finishWithError(new AppError(TAG, 1841, CODE_OTHER, response, e, data));
                            return;
                        }

                        if (!message.seen) {
                            app.db.metadataDao().setSeen(profileId, message, true);
                        }
                        app.db.messageDao().add(message);
                        app.db.messageRecipientDao().addAll((List<MessageRecipient>)(List<?>) messageRecipientList);

                        message.recipients = messageRecipientList;

                        new Handler(activityContext.getMainLooper()).post(() -> {
                            messageCallback.onSuccess(message);
                        });
                    }
                })
                .build()
                .enqueue());
    }

    @Override
    public void getAttachment(@NonNull Context activityContext, @NonNull SyncCallback errorCallback, @NonNull ProfileFull profile, @NonNull MessageFull message, long attachmentId, @NonNull AttachmentGetCallback attachmentCallback) {
        if (!prepare(activityContext, errorCallback, profile.getId(), profile, LoginStore.fromProfileFull(profile)))
            return;

        login(() -> {
            Request.Builder builder = Request.builder()
                    .url("https://"+loginServerName+".mobidziennik.pl/dziennik/"+(message.type == TYPE_SENT ? "wiadwyslana" : "wiadodebrana")+"/?id="+message.id+"&zalacznik="+attachmentId);
            new Handler(activityContext.getMainLooper()).post(() -> {
                attachmentCallback.onSuccess(builder);
            });
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
            Request.builder()
                    .url("https://" + loginServerName + ".mobidziennik.pl/mobile/dodajwiadomosc")
                    .userAgent(System.getProperty("http.agent"))
                    .callback(new TextCallbackHandler() {
                        @Override
                        public void onFailure(Response response, Throwable throwable) {
                            finishWithError(new AppError(TAG, 2289, CODE_OTHER, response, throwable));
                        }

                        @Override
                        public void onSuccess(String data, Response response) {
                            if (data == null || data.equals("")) {
                                finishWithError(new AppError(TAG, 2295, CODE_MAINTENANCE, response));
                                return;
                            }
                            if (data.contains("nie-pamietam-hasla")) {
                                lastLoginTime = 0;
                                finishWithError(new AppError(TAG, 2300, CODE_INVALID_LOGIN, response, data));
                                return;
                            }

                            teacherList = app.db.teacherDao().getAllNow(profileId);

                            for (Teacher teacher: teacherList) {
                                teacher.typeDescription = null; // TODO: 2019-06-13 it better
                            }

                            Matcher categoryMatcher = Pattern.compile("<option value=\"(.+?)\"(?:\\sselected)?>(.+?)</option>").matcher(data);
                            while (categoryMatcher.find()) {
                                String categoryId = categoryMatcher.group(1);
                                String categoryName = categoryMatcher.group(2);
                                String categoryHtml = getRecipientCategory(categoryId);
                                if (categoryHtml == null)
                                    return; // the error callback is already executed
                                Matcher teacherMatcher = Pattern.compile("<optgroup label=\"(.+?)\">(.+?)</optgroup>|<option value=\"(.+?)\">\\s?(.+?)</option>").matcher(categoryHtml);
                                while (teacherMatcher.find()) {
                                    if (teacherMatcher.group(1) != null) {
                                        String className = teacherMatcher.group(1);
                                        String listHtml = teacherMatcher.group(2);
                                        Matcher listMatcher = Pattern.compile("<option value=\"(.+?)\">\\s?<span.+?>(.+?)</span>\\s?</option>").matcher(listHtml);
                                        while (listMatcher.find()) {
                                            updateTeacher(categoryId, Long.parseLong(listMatcher.group(1)), listMatcher.group(2), categoryName, className);
                                        }
                                    }
                                    else {
                                        updateTeacher(categoryId, Long.parseLong(teacherMatcher.group(3)), teacherMatcher.group(4), categoryName, null);
                                    }
                                }
                            }
                            app.db.teacherDao().addAll(teacherList);

                            profile.setLastReceiversSync(System.currentTimeMillis());
                            app.db.profileDao().add(profile);

                            new Handler(activityContext.getMainLooper()).post(() -> recipientListGetCallback.onSuccess(new ArrayList<>(teacherList)));
                        }
                    })
                    .build()
                    .enqueue();
        });
    }
    private String getRecipientCategory(String categoryId) {
        Response response = null;
        try {
            response = Request.builder()
                    .url("https://" + loginServerName + ".mobidziennik.pl/mobile/odbiorcyWiadomosci")
                    .userAgent(System.getProperty("http.agent"))
                    .addParameter("typ", categoryId)
                    .post()
                    .build().execute();
            if (response.code() != 200) {
                finishWithError(new AppError(TAG, 2349, CODE_OTHER, response));
                return null;
            }
            String data = new TextCallbackHandler().backgroundParser(response);
            if (data == null || data.equals("")) {
                return "";
            }
            if (data.contains("nie-pamietam-hasla")) {
                lastLoginTime = 0;
                finishWithError(new AppError(TAG, 2300, CODE_INVALID_LOGIN, response, data));
                return null;
            }
            return data;
        } catch (Exception e) {
            finishWithError(new AppError(TAG, 2355, CODE_OTHER, response, e));
            return null;
        }
    }
    private void updateTeacher(String category, long id, String nameLastFirst, String typeDescription, String className) {
        Teacher teacher = Teacher.getById(teacherList, id);
        if (teacher == null) {
            String[] nameParts = nameLastFirst.split(" ", Integer.MAX_VALUE);
            teacher = new Teacher(profileId, id, nameParts[1], nameParts[0]);
            teacherList.add(teacher);
        }
        teacher.loginId = String.valueOf(id);
        teacher.type = 0;
        if (className != null) {
            teacher.typeDescription = bs("", teacher.typeDescription, ", ")+className;
        }
        switch (category) {
            case "1":
                teacher.setType(Teacher.TYPE_PRINCIPAL);
                break;
            case "2":
                teacher.setType(Teacher.TYPE_TEACHER);
                break;
            case "8":
                teacher.setType(Teacher.TYPE_EDUCATOR);
                break;
            case "9":
                teacher.setType(Teacher.TYPE_PEDAGOGUE);
                break;
            case "10":
                teacher.setType(Teacher.TYPE_OTHER);
                teacher.typeDescription = "Specjaliści";
                break;
            case "Administracja WizjaNet":
                teacher.setType(Teacher.TYPE_SUPER_ADMIN);
                break;
            case "Administratorzy":
                teacher.setType(Teacher.TYPE_SCHOOL_ADMIN);
                break;
            case "Sekretarka":
                teacher.setType(Teacher.TYPE_SECRETARIAT);
                break;
            default:
                teacher.setType(Teacher.TYPE_OTHER);
                teacher.typeDescription = typeDescription+bs(" ", teacher.typeDescription);
                break;
        }
    }

    @Override
    public MessagesComposeInfo getComposeInfo(@NonNull ProfileFull profile) {
        return new MessagesComposeInfo(-1, profile.getStudentData("attachmentSizeLimit", 6291456L), 100, -1);
    }
}
