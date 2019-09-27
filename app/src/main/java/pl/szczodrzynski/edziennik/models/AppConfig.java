package pl.szczodrzynski.edziennik.models;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.BuildConfig;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.widgets.WidgetConfig;

import static pl.szczodrzynski.edziennik.datamodels.LoginStore.LOGIN_TYPE_IUCZNIOWIE;
import static pl.szczodrzynski.edziennik.datamodels.LoginStore.LOGIN_TYPE_LIBRUS;
import static pl.szczodrzynski.edziennik.datamodels.LoginStore.LOGIN_TYPE_MOBIDZIENNIK;
import static pl.szczodrzynski.edziennik.datamodels.LoginStore.LOGIN_TYPE_VULCAN;

public class AppConfig {
    private static final String TAG = "AppConfig";

    public int appTheme = 1;

    public List<Notification> notifications;

    public long lastDeleteUnused = System.currentTimeMillis();

    public Map<Long, Boolean> teacherImages = null;

    public boolean dontCountZeroToAverage = false;

    public AppConfig(App _app) {
        notifications = new ArrayList<>();
        miniDrawerButtonIds = new ArrayList<>();
        miniDrawerButtonIds.add(MainActivity.DRAWER_ITEM_HOME);
        miniDrawerButtonIds.add(MainActivity.DRAWER_ITEM_TIMETABLE);
        miniDrawerButtonIds.add(MainActivity.DRAWER_ITEM_AGENDA);
        miniDrawerButtonIds.add(MainActivity.DRAWER_ITEM_GRADES);
        miniDrawerButtonIds.add(MainActivity.DRAWER_ITEM_MESSAGES);
        miniDrawerButtonIds.add(MainActivity.DRAWER_ITEM_HOMEWORK);
        miniDrawerButtonIds.add(MainActivity.DRAWER_ITEM_SETTINGS);
        fcmToken = "";
        fcmTokens = new TreeMap<>();
        fcmTokens.put(LOGIN_TYPE_MOBIDZIENNIK, new Pair<>("", new ArrayList<>()));
        fcmTokens.put(LOGIN_TYPE_LIBRUS, new Pair<>("", new ArrayList<>()));
        fcmTokens.put(LOGIN_TYPE_IUCZNIOWIE, new Pair<>("", new ArrayList<>()));
        fcmTokens.put(LOGIN_TYPE_VULCAN, new Pair<>("", new ArrayList<>()));
    }

    public Map<Integer, WidgetConfig> widgetTimetableConfigs = new TreeMap<>();

    public static final int ORDER_BY_DATE_DESC = 0;
    public static final int ORDER_BY_SUBJECT_ASC = 1;
    public static final int ORDER_BY_DATE_ASC = 2;
    public static final int ORDER_BY_SUBJECT_DESC = 3;
    public int gradesOrderBy = ORDER_BY_DATE_DESC;

    public String headerBackground = null;
    public String appBackground = null;

    public int lastAppVersion = BuildConfig.VERSION_CODE;

    public boolean registerSyncEnabled = true;
    public boolean registerSyncOnlyWifi = false;
    public int registerSyncInterval = 60 * 60; // seconds

    public int timetableDisplayDaysForward = 7;
    public int timetableDisplayDaysBackward = 2;

    //public boolean syncNotificationsEnabled = true;

    public String fcmToken;
    public Map<Integer, Pair<String, List<Integer>>> fcmTokens;

    public boolean miniDrawerVisible = false;

    //public boolean autoRegistrationAllowed = false;

    public boolean loginFinished = false;

    public Time bellSyncDiff = null;
    public int bellSyncMultiplier = 0;

    public boolean savePending = false;


    public List<Integer> miniDrawerButtonIds;

    public boolean notifyAboutUpdates = true;
    public String updateVersion = "";
    public String updateUrl = "";
    public String updateFilename = "";
    public boolean updateMandatory = false;

    public boolean webPushEnabled = false;

    public boolean tapTargetSetAsRead = false;
    public boolean tapTargetSwitchProfile = false;

    public boolean countInSeconds = false;

    public long quietHoursStart = 0;
    public long quietHoursEnd = 0;
    public boolean quietDuringLessons = true;

    public String devModePassword = null;

    public long appInstalledTime = 0;
    public long appRateSnackbarTime = 0;

    public int mobidziennikOldMessages = -1;
}
