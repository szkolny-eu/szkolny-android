package pl.szczodrzynski.edziennik;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import com.evernote.android.job.JobManager;
import com.google.android.gms.security.ProviderInstaller;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mikepenz.iconics.Iconics;
import com.mikepenz.iconics.IconicsColor;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSize;
import com.mikepenz.iconics.typeface.IIcon;
import com.mikepenz.iconics.typeface.library.szkolny.font.SzkolnyFont;

import java.lang.reflect.Field;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import cat.ereza.customactivityoncrash.config.CaocConfig;
import im.wangchao.mhttp.MHttp;
import im.wangchao.mhttp.internal.cookie.PersistentCookieJar;
import im.wangchao.mhttp.internal.cookie.cache.SetCookieCache;
import im.wangchao.mhttp.internal.cookie.persistence.SharedPrefsCookiePersistor;
import me.leolin.shortcutbadger.ShortcutBadger;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;
import pl.szczodrzynski.edziennik.ui.modules.base.CrashActivity;
import pl.szczodrzynski.edziennik.api.Edziennik;
import pl.szczodrzynski.edziennik.api.Iuczniowie;
import pl.szczodrzynski.edziennik.api.Librus;
import pl.szczodrzynski.edziennik.api.Mobidziennik;
import pl.szczodrzynski.edziennik.api.Vulcan;
import pl.szczodrzynski.edziennik.datamodels.AppDb;
import pl.szczodrzynski.edziennik.datamodels.DebugLog;
import pl.szczodrzynski.edziennik.datamodels.LoginStore;
import pl.szczodrzynski.edziennik.datamodels.Profile;
import pl.szczodrzynski.edziennik.datamodels.ProfileFull;
import pl.szczodrzynski.edziennik.utils.models.AppConfig;
import pl.szczodrzynski.edziennik.network.NetworkUtils;
import pl.szczodrzynski.edziennik.network.TLSSocketFactory;
import pl.szczodrzynski.edziennik.receivers.JobsCreator;
import pl.szczodrzynski.edziennik.sync.SyncJob;
import pl.szczodrzynski.edziennik.utils.PermissionChecker;
import pl.szczodrzynski.edziennik.utils.Themes;
import pl.szczodrzynski.edziennik.utils.Utils;

import static pl.szczodrzynski.edziennik.datamodels.LoginStore.LOGIN_TYPE_MOBIDZIENNIK;
import static pl.szczodrzynski.edziennik.datamodels.LoginStore.LOGIN_TYPE_VULCAN;

public class App extends androidx.multidex.MultiDexApplication {
    private static final String TAG = "App";
    public static int profileId = -1;
    private Context mContext;

    public static final int REQUEST_TIMEOUT = 10 * 1000;

    // notifications
    //public NotificationManager mNotificationManager;
    //public final String NOTIFICATION_CHANNEL_ID_UPDATES = "4566";
    //public String NOTIFICATION_CHANNEL_NAME_UPDATES;
    public Notifier notifier;

    public static final String APP_URL = "://edziennik.szczodrzynski.pl/app/";

    public ShortcutManager shortcutManager;

    public PermissionChecker permissionChecker;

    public String signature = "";
    public String deviceId = "";

    public AppDb db;
    public void debugLog(String text) {
        if (!devMode)
            return;
        db.debugLogDao().add(new DebugLog(Utils.getCurrentTimeUsingCalendar()+": "+text));
    }
    public void debugLogAsync(String text) {
        if (!devMode)
            return;
        AsyncTask.execute(() -> {
            db.debugLogDao().add(new DebugLog(Utils.getCurrentTimeUsingCalendar()+": "+text));
        });
    }

    // network & APIs
    public NetworkUtils networkUtils;
    public PersistentCookieJar cookieJar;
    public OkHttpClient http;
    public OkHttpClient httpLazy;
    //public Jakdojade apiJakdojade;
    public Edziennik apiEdziennik;
    public Mobidziennik apiMobidziennik;
    public Iuczniowie apiIuczniowie;
    public Librus apiLibrus;
    public Vulcan apiVulcan;

    public SharedPreferences appSharedPrefs; // sharedPreferences for APPCONFIG + JOBS STORE
    public AppConfig appConfig; // APPCONFIG: common for all profiles
    //public AppProfile profile; // current profile
    public JsonObject loginStore = null;
    public SharedPreferences registerStore; // sharedPreferences for REGISTER
    //public Register register; // REGISTER for current profile, read from registerStore

    public ProfileFull profile;

    // other stuff
    public Gson gson;
    public String requestScheme = "https";
    public boolean unreadBadgesAvailable = true;

    public static boolean devMode = false;

    public static final boolean UPDATES_ON_PLAY_STORE = true;

    @RequiresApi(api = Build.VERSION_CODES.M)
    public Icon getDesktopIconFromIconics(IIcon icon) {
        final IconicsDrawable drawable = new IconicsDrawable(mContext, icon)
                .color(IconicsColor.colorInt(Color.WHITE))
                .size(IconicsSize.dp(48))
                .padding(IconicsSize.dp(8))
                .backgroundColor(IconicsColor.colorRes(R.color.colorPrimaryDark))
                .roundedCorners(IconicsSize.dp(10));
        //drawable.setStyle(Paint.Style.FILL);
        final Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return Icon.createWithBitmap(bitmap);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        CaocConfig.Builder.create()
                .backgroundMode(CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM) //default: CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM
                .enabled(true) //default: true
                .showErrorDetails(true) //default: true
                .showRestartButton(true) //default: true
                .logErrorOnRestart(true) //default: true
                .trackActivities(true) //default: false
                .minTimeBetweenCrashesMs(2000) //default: 3000
                .errorDrawable(R.drawable.ic_rip) //default: bug image
                .restartActivity(MainActivity.class) //default: null (your app's launch activity)
                .errorActivity(CrashActivity.class) //default: null (default error activity)
                //.eventListener(new YourCustomEventListener()) //default: null
                .apply();
        mContext = this;
        db = AppDb.getDatabase(this);
        gson = new Gson();
        networkUtils = new NetworkUtils(this);
        apiEdziennik = new Edziennik(this);
        //apiJakdojade = new Jakdojade(this);
        apiMobidziennik = new Mobidziennik(this);
        apiIuczniowie = new Iuczniowie(this);
        apiLibrus = new Librus(this);
        apiVulcan = new Vulcan(this);

        Iconics.init(getApplicationContext());
        Iconics.registerFont(SzkolnyFont.INSTANCE);

        notifier = new Notifier(this);
        permissionChecker = new PermissionChecker(mContext);

        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        cookieJar = new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(this));

        OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder()
                .cache(null)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .cookieJar(cookieJar)
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                try {
                    ProviderInstaller.installIfNeeded(this);
                } catch (Exception e) {
                    Log.e("OkHttpTLSCompat", "Play Services not found or outdated");
                    X509TrustManager x509TrustManager = null;
                    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    trustManagerFactory.init((KeyStore) null);
                    for (TrustManager trustManager: trustManagerFactory.getTrustManagers()) {
                        if (trustManager instanceof X509TrustManager)
                            x509TrustManager = (X509TrustManager) trustManager;
                    }

                    SSLContext sc = SSLContext.getInstance("TLSv1.2");
                    sc.init(null, null, null);
                    httpBuilder.sslSocketFactory(new TLSSocketFactory(sc.getSocketFactory()), x509TrustManager);

                    ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                            .tlsVersions(TlsVersion.TLS_1_0)
                            .tlsVersions(TlsVersion.TLS_1_1)
                            .tlsVersions(TlsVersion.TLS_1_2)
                            .build();

                    List<ConnectionSpec> specs = new ArrayList<>();
                    specs.add(cs);
                    specs.add(ConnectionSpec.COMPATIBLE_TLS);
                    specs.add(ConnectionSpec.CLEARTEXT);

                    httpBuilder.connectionSpecs(specs);
                }


            } catch (Exception exc) {
                Log.e("OkHttpTLSCompat", "Error while setting TLS 1.2", exc);
            }
        }

        http = httpBuilder.build();
        httpLazy = http.newBuilder().followRedirects(false).followSslRedirects(false).build();

        MHttp.instance()
                .customOkHttpClient(http);

        //register = new Register(mContext);

        appSharedPrefs = getSharedPreferences(getString(R.string.preference_file_global), Context.MODE_PRIVATE);

        loadConfig();

        Themes.INSTANCE.setThemeInt(appConfig.appTheme);

        //profileLoadById(appSharedPrefs.getInt("current_profile_id", 1));

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature: packageInfo.signatures) {
                byte[] signatureBytes = signature.toByteArray();
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signatureBytes);
                this.signature = Base64.encodeToString(md.digest(), Base64.DEFAULT);
                //Log.d(TAG, "Signature is "+this.signature);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if ("f054761fbdb6a238".equals(deviceId)) {
            devMode = true;
        }
        else if (appConfig.devModePassword != null) {
            checkDevModePassword();
        }

        JobManager.create(this).addJobCreator(new JobsCreator());
        if (appConfig.registerSyncEnabled) {
            SyncJob.schedule(this);
        }
        else {
            SyncJob.clear();
        }

        db.metadataDao().countUnseen().observeForever(count -> {
            Log.d("MainActivity", "Overall unseen count changed");
            assert count != null;
            if (unreadBadgesAvailable) {
                unreadBadgesAvailable = ShortcutBadger.applyCount(this, count);
            }
        });

        //new IonCookieManager(mContext);

        new Handler().post(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                shortcutManager = getSystemService(ShortcutManager.class);

                ShortcutInfo shortcutTimetable = new ShortcutInfo.Builder(mContext, "item_timetable")
                        .setShortLabel(getString(R.string.shortcut_timetable)).setLongLabel(getString(R.string.shortcut_timetable))
                        .setIcon(Icon.createWithResource(this, R.mipmap.ic_shortcut_timetable))
                        //.setIcon(getDesktopIconFromIconics(CommunityMaterial.Icon2.cmd_timetable))
                        .setIntent(new Intent(Intent.ACTION_MAIN, null, this, MainActivity.class)
                                .putExtra("fragmentId", MainActivity.DRAWER_ITEM_TIMETABLE))
                        .build();

                ShortcutInfo shortcutAgenda = new ShortcutInfo.Builder(mContext, "item_agenda")
                        .setShortLabel(getString(R.string.shortcut_agenda)).setLongLabel(getString(R.string.shortcut_agenda))
                        .setIcon(Icon.createWithResource(this, R.mipmap.ic_shortcut_agenda))
                        //.setIcon(getDesktopIconFromIconics(CommunityMaterial.Icon.cmd_calendar))
                        .setIntent(new Intent(Intent.ACTION_MAIN, null, this, MainActivity.class)
                                .putExtra("fragmentId", MainActivity.DRAWER_ITEM_AGENDA))
                        .build();

                ShortcutInfo shortcutGrades = new ShortcutInfo.Builder(mContext, "item_grades")
                        .setShortLabel(getString(R.string.shortcut_grades)).setLongLabel(getString(R.string.shortcut_grades))
                        .setIcon(Icon.createWithResource(this, R.mipmap.ic_shortcut_grades))
                        //.setIcon(getDesktopIconFromIconics(CommunityMaterial.Icon2.cmd_numeric_5_box))
                        .setIntent(new Intent(Intent.ACTION_MAIN, null, this, MainActivity.class)
                                .putExtra("fragmentId", MainActivity.DRAWER_ITEM_GRADES))
                        .build();

                ShortcutInfo shortcutHomework = new ShortcutInfo.Builder(mContext, "item_homeworks")
                        .setShortLabel(getString(R.string.shortcut_homework)).setLongLabel(getString(R.string.shortcut_homework))
                        .setIcon(Icon.createWithResource(this, R.mipmap.ic_shortcut_homework))
                        //.setIcon(getDesktopIconFromIconics(SzkolnyFont.Icon.szf_file_document_edit))
                        .setIntent(new Intent(Intent.ACTION_MAIN, null, this, MainActivity.class)
                                .putExtra("fragmentId", MainActivity.DRAWER_ITEM_HOMEWORK))
                        .build();

                ShortcutInfo shortcutMessages = new ShortcutInfo.Builder(mContext, "item_messages")
                        .setShortLabel(getString(R.string.shortcut_messages)).setLongLabel(getString(R.string.shortcut_messages))
                        .setIcon(Icon.createWithResource(this, R.mipmap.ic_shortcut_messages))
                        //.setIcon(getDesktopIconFromIconics(CommunityMaterial.Icon.cmd_email))
                        .setIntent(new Intent(Intent.ACTION_MAIN, null, this, MainActivity.class)
                                .putExtra("fragmentId", MainActivity.DRAWER_ITEM_MESSAGES ))
                        .build();

                shortcutManager.setDynamicShortcuts(Arrays.asList(shortcutTimetable, shortcutAgenda, shortcutGrades, shortcutHomework, shortcutMessages));
            }

            if (appConfig.appInstalledTime == 0) {
                try {
                    appConfig.appInstalledTime = getPackageManager().getPackageInfo(getPackageName(), 0).firstInstallTime;
                    appConfig.appRateSnackbarTime = appConfig.appInstalledTime + 7 * 24 * 60 * 60 * 1000;
                    saveConfig("appInstalledTime", "appRateSnackbarTime");
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }

            /*Task<CapabilityInfo> capabilityInfoTask =
                    Wearable.getCapabilityClient(this)
                            .getCapability("edziennik_wear_app", CapabilityClient.FILTER_REACHABLE);
            capabilityInfoTask.addOnCompleteListener((task) -> {
                if (task.isSuccessful()) {
                    CapabilityInfo capabilityInfo = task.getResult();
                    assert capabilityInfo != null;
                    Set<Node> nodes;
                    nodes = capabilityInfo.getNodes();
                    Log.d(TAG, "Nodes "+nodes);

                    if (nodes.size() > 0) {
                        Wearable.getMessageClient(this).sendMessage(
                                nodes.toArray(new Node[]{})[0].getId(), "/ping", "Hello world".getBytes());
                    }
                } else {
                    Log.d(TAG, "Capability request failed to return any results.");
                }
            });

            Wearable.getDataClient(this).addListener(dataEventBuffer -> {
                Log.d(TAG, "onDataChanged(): " + dataEventBuffer);

                for (DataEvent event : dataEventBuffer) {
                    if (event.getType() == DataEvent.TYPE_CHANGED) {
                        String path = event.getDataItem().getUri().getPath();
                        Log.d(TAG, "Data "+path+ " :: "+Arrays.toString(event.getDataItem().getData()));
                    }
                }
            });*/

            FirebaseApp pushMobidziennikApp = FirebaseApp.initializeApp(
                    this,
                    new FirebaseOptions.Builder()
                            .setApplicationId("1:1029629079999:android:58bb378dab031f42")
                            .setGcmSenderId("1029629079999")
                            .build(),
                    "Mobidziennik"
            );

            /*FirebaseApp pushLibrusApp = FirebaseApp.initializeApp(
                    this,
                    new FirebaseOptions.Builder()
                            .setApiKey("AIzaSyDfTuEoYPKdv4aceEws1CO3n0-HvTndz-o")
                            .setApplicationId("1:513056078587:android:1e29083b760af544")
                            .build(),
                    "Librus"
            );*/

            FirebaseApp pushVulcanApp = FirebaseApp.initializeApp(
                    this,
                    new FirebaseOptions.Builder()
                            .setApiKey("AIzaSyDW8MUtanHy64_I0oCpY6cOxB3jrvJd_iA")
                            .setApplicationId("1:987828170337:android:ac97431a0a4578c3")
                            .build(),
                    "Vulcan"
            );

            try {
                final long startTime = System.currentTimeMillis();
                FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(instanceIdResult -> {
                    Log.d(TAG, "Token for App is " + instanceIdResult.getToken() + ", ID is " + instanceIdResult.getId()+". Time is "+(System.currentTimeMillis() - startTime));
                    appConfig.fcmToken = instanceIdResult.getToken();
                });
                FirebaseInstanceId.getInstance(pushMobidziennikApp).getInstanceId().addOnSuccessListener(instanceIdResult -> {
                    Log.d(TAG, "Token for Mobidziennik is " + instanceIdResult.getToken() + ", ID is " + instanceIdResult.getId());
                    appConfig.fcmTokens.put(LOGIN_TYPE_MOBIDZIENNIK, new Pair<>(instanceIdResult.getToken(), new ArrayList<>()));
                });
                /*FirebaseInstanceId.getInstance(pushLibrusApp).getInstanceId().addOnSuccessListener(instanceIdResult -> {
                    Log.d(TAG, "Token for Librus is " + instanceIdResult.getToken() + ", ID is " + instanceIdResult.getId());
                    appConfig.fcmTokens.put(LOGIN_TYPE_LIBRUS, new Pair<>(instanceIdResult.getToken(), new ArrayList<>()));
                });*/
                FirebaseInstanceId.getInstance(pushVulcanApp).getInstanceId().addOnSuccessListener(instanceIdResult -> {
                    Log.d(TAG, "Token for Vulcan is " + instanceIdResult.getToken() + ", ID is " + instanceIdResult.getId());
                    Pair<String, List<Integer>> pair = appConfig.fcmTokens.get(LOGIN_TYPE_VULCAN);
                    if (pair == null || pair.first == null || !pair.first.equals(instanceIdResult.getToken())) {
                        appConfig.fcmTokens.put(LOGIN_TYPE_VULCAN, new Pair<>(instanceIdResult.getToken(), new ArrayList<>()));
                    }
                });


                FirebaseMessaging.getInstance().subscribeToTopic(getPackageName());
            }
            catch (IllegalStateException e) {
                e.printStackTrace();
            }
        });

    }


    public void loadConfig()
    {
        appConfig = new AppConfig(this);


        if (appSharedPrefs.contains("config")) {
            // remove old-format config, save the new one and empty the incorrectly-nulled config
            appConfig = gson.fromJson(appSharedPrefs.getString("config", ""), AppConfig.class);
            appSharedPrefs.edit().remove("config").apply();
            saveConfig();
            appConfig = new AppConfig(this);
        }

        if (appSharedPrefs.contains("profiles")) {
            SharedPreferences.Editor appSharedPrefsEditor = appSharedPrefs.edit();
            /*List<Integer> appProfileIds = gson.fromJson(appSharedPrefs.getString("profiles", ""), new TypeToken<List<Integer>>(){}.getType());
            for (int id: appProfileIds) {
                AppProfile appProfile = gson.fromJson(appSharedPrefs.getString("profile"+id, ""), AppProfile.class);
                if (appProfile != null) {
                    appConfig.profiles.add(appProfile);
                }
                appSharedPrefsEditor.remove("profile"+id);
            }*/
            appSharedPrefsEditor.remove("profiles");
            appSharedPrefsEditor.apply();
            //profilesSave();
        }


        Map<String,?> keys = appSharedPrefs.getAll();
        for (Map.Entry<String,?> entry : keys.entrySet()) {
            if (entry.getKey().startsWith("app.appConfig.")) {
                String fieldName = entry.getKey().replace("app.appConfig.", "");

                try {
                    Field field = AppConfig.class.getField(fieldName);
                    Object object;
                    try {
                        object = gson.fromJson(entry.getValue().toString(), field.getGenericType());
                    } catch (JsonSyntaxException e) {
                        Log.d(TAG, "For field "+fieldName);
                        e.printStackTrace();
                        object = entry.getValue().toString();
                    }
                    if (object != null)
                        field.set(appConfig, object);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                    appSharedPrefs.edit().remove("app.appConfig."+fieldName).apply();
                }
            }
        }

        /*if (appConfig.lastAppVersion > BuildConfig.VERSION_CODE) {
            BootReceiver br = new BootReceiver();
            Intent i = new Intent();
            //i.putExtra("UserChecked", true);
            br.onReceive(getContext(), i);
            Toast.makeText(mContext, R.string.warning_older_version_running, Toast.LENGTH_LONG).show();
            //Toast.makeText(mContext, "Zaktualizuj aplikację.", Toast.LENGTH_LONG).show();
            //System.exit(0);
        }*/

        if (appConfig == null) {
            appConfig = new AppConfig(this);
        }
    }
    public void saveConfig()
    {
        try {
            appConfig.savePending = false;

            SharedPreferences.Editor appSharedPrefsEditor = appSharedPrefs.edit();

            JsonObject appConfigJson = gson.toJsonTree(appConfig).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : appConfigJson.entrySet()) {
                String jsonObj;
                jsonObj = entry.getValue().toString();
            /*if (entry.getValue().isJsonObject()) {
                jsonObj = entry.getValue().getAsJsonObject().toString();
            }
            else if (entry.getValue().isJsonArray()) {
                jsonObj = entry.getValue().getAsJsonArray().toString();
            }
            else {
                jsonObj = entry.getValue().toString();
            }*/
                appSharedPrefsEditor.putString("app.appConfig." + entry.getKey(), jsonObj);
            }

            appSharedPrefsEditor.apply();
        }
        catch (ConcurrentModificationException e) {
            e.printStackTrace();
        }
        //appSharedPrefs.edit().putString("config", gson.toJson(appConfig)).apply();
    }
    public void saveConfig(String ... fieldNames)
    {
        appConfig.savePending = false;

        SharedPreferences.Editor appSharedPrefsEditor = appSharedPrefs.edit();

        for (String fieldName: fieldNames) {
            try {
                Object object = AppConfig.class.getField(fieldName).get(appConfig);
                appSharedPrefsEditor.putString("app.appConfig."+fieldName, gson.toJson(object));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (ConcurrentModificationException e) {
                e.printStackTrace();
            }
        }

        appSharedPrefsEditor.apply();
        //appSharedPrefs.edit().putString("config", gson.toJson(appConfig)).apply();
    }



    public void profileSaveAsync() {
        AsyncTask.execute(() -> {
            db.profileDao().add(profile);
        });
    }
    public void profileSaveAsync(Profile profile) {
        AsyncTask.execute(() -> {
            db.profileDao().add(profile);
        });
    }
    public void profileSaveFullAsync(ProfileFull profile) {
        AsyncTask.execute(() -> {
            profileSaveFull(profile);
        });
    }
    public void profileSaveFull(ProfileFull profileFull) {
        db.profileDao().add(profileFull);
        db.loginStoreDao().add(profileFull);
    }
    public void profileSaveFull(Profile profile, LoginStore loginStore) {
        db.profileDao().add(profile);
        db.loginStoreDao().add(loginStore);
    }

    public ProfileFull profileGetOrNull(int id) {
        return db.profileDao().getByIdNow(id);
    }

    public void profileLoadById(int id) {
        profileLoadById(id, false);
    }
    public void profileLoadById(int id, boolean loadedLast) {
        //Log.d(TAG, "Loading ID "+id);
        /*if (profile == null) {
            profile = profileNew();
            AppDb.profileId = profile.id;
            appSharedPrefs.edit().putInt("current_profile_id", profile.id).apply();
            return;
        }*/
        if (profile == null || profile.getId() != id) {
            profile = db.profileDao().getByIdNow(id);
            /*if (profile == null) {
                profileLoadById(id);
                return;
            }*/
            if (profile != null) {
                MainActivity.Companion.setUseOldMessages(profile.getLoginStoreType() == LOGIN_TYPE_MOBIDZIENNIK && appConfig.mobidziennikOldMessages == 1);
                profileId = profile.getId();
                appSharedPrefs.edit().putInt("current_profile_id", profile.getId()).apply();
            }
            else if (!loadedLast) {
                profileLoadById(profileLastId(), true);
            }
            else {
                profileId = -1;
            }
        }
    }

    public void profileLoad(ProfileFull profile) {
        MainActivity.Companion.setUseOldMessages(profile.getLoginStoreType() == LOGIN_TYPE_MOBIDZIENNIK && appConfig.mobidziennikOldMessages == 1);
        this.profile = profile;
        profileId = profile.getId();
    }

    /*public void profileRemove(int id)
    {
        Profile profile = db.profileDao().getByIdNow(id);

        if (profile.id == profile.loginStoreId) {
            // this profile is the owner of the login store
            // we need to check if any other profile is using it
            List<Integer> transferProfileIds = db.profileDao().getIdsByLoginStoreIdNow(profile.loginStoreId);
            if (transferProfileIds.size() == 1) {
                // this login store is free of users, remove it along with the profile
                db.loginStoreDao().remove(profile.loginStoreId);
                // the current store is removed, we are ready to remove the profile
            }
            else if (transferProfileIds.size() > 1) {
                transferProfileIds.remove(transferProfileIds.indexOf(profile.id));
                // someone is using the store
                // we need to transfer it to the firstProfileId
                db.loginStoreDao().changeId(profile.loginStoreId, transferProfileIds.get(0));
                db.profileDao().changeStoreId(profile.loginStoreId, transferProfileIds.get(0));
                // the current store is removed, we are ready to remove the profile
            }
        }
        // else, the profile uses a store that it doesn't own
        // leave the store and go on with removing

        Log.d(TAG, "Before removal: "+db.profileDao().getAllNow().toString());
        db.profileDao().remove(profile.id);
        Log.d(TAG, "After removal: "+db.profileDao().getAllNow().toString());

        *//*int newId = 1;
        if (appConfig.profiles.size() > 0) {
            newId = appConfig.profiles.get(appConfig.profiles.size() - 1).id;
        }
        Log.d(TAG, "New ID: "+newId);
        //Toast.makeText(mContext, "selected new id "+newId, Toast.LENGTH_SHORT).show();
        profileLoadById(newId);*//*
    }*/

    public int profileFirstId() {
        return db.profileDao().getFirstId();
    }

    public int profileLastId() {
        return db.profileDao().getLastId();
    }


    public Context getContext()
    {
        return mContext;
    }

    public void checkDevModePassword() {
        try {
            if (Utils.AESCrypt.decrypt("nWFVxY65Pa8/aRrT7EylNAencmOD+IxUY2Gg/beiIWY=", appConfig.devModePassword).equals("ok here you go it's enabled now")) {
                devMode = true;
            }
            else {
                devMode = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            devMode = false;
        }
    }

}
