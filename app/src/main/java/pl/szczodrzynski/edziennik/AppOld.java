/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-19.
 */

package pl.szczodrzynski.edziennik;

/*public class AppOld extends androidx.multidex.MultiDexApplication implements Configuration.Provider {
    private static final String TAG = "App";
    public static int profileId = -1;
    private Context mContext;

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(Log.VERBOSE)
                .build();
    }


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

    public SharedPreferences appSharedPrefs; // sharedPreferences for APPCONFIG + JOBS STORE
    public AppConfig appConfig; // APPCONFIG: common for all profiles
    //public AppProfile profile; // current profile
    public SharedPreferences registerStore; // sharedPreferences for REGISTER
    //public Register register; // REGISTER for current profile, read from registerStore

    public Profile profile;
    public Config config;
    private static Config mConfig;
    public static Config getConfig() {
        return mConfig;
    }

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

        config = new Config(db);
        config.migrate(this);
        mConfig = config;

        Iconics.init(getApplicationContext());
        Iconics.registerFont(SzkolnyFont.INSTANCE);

        notifier = new Notifier(this);
        permissionChecker = new PermissionChecker(mContext);

        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        cookieJar = new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(this));

        appSharedPrefs = getSharedPreferences(getString(R.string.preference_file_global), Context.MODE_PRIVATE);

        loadConfig();

        Signing.INSTANCE.getCert(this);

        Themes.INSTANCE.setThemeInt(config.getUi().getTheme());

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature: packageInfo.signatures) {
                byte[] signatureBytes = signature.toByteArray();
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signatureBytes);
                this.signature = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
                //Log.d(TAG, "Signature is "+this.signature);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if ("f054761fbdb6a238".equals(deviceId) || BuildConfig.DEBUG) {
            devMode = true;
        }
        else if (config.getDevModePassword() != null) {
            checkDevModePassword();
        }

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

        if (App.devMode || BuildConfig.DEBUG) {
            HyperLog.initialize(this);
            HyperLog.setLogLevel(Log.VERBOSE);
            HyperLog.setLogFormat(new DebugLogFormat(this));

            ChuckerCollector chuckerCollector = new ChuckerCollector(this, true, RetentionManager.Period.ONE_HOUR);
            ChuckerInterceptor chuckerInterceptor = new ChuckerInterceptor(this, chuckerCollector);
            httpBuilder.addInterceptor(chuckerInterceptor);
        }

        http = httpBuilder.build();
        httpLazy = http.newBuilder().followRedirects(false).followSslRedirects(false).build();

        MHttp.instance()
                .customOkHttpClient(http);

        //register = new Register(mContext);

        //profileLoadById(appSharedPrefs.getInt("current_profile_id", 1));

        if (config.getSync().getEnabled()) {
            SyncWorker.Companion.scheduleNext(this, false);
        }
        else {
            SyncWorker.Companion.cancelNext(this);
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

            if (config.getAppInstalledTime() == 0) {
                try {
                    config.setAppInstalledTime(getPackageManager().getPackageInfo(getPackageName(), 0).firstInstallTime);
                    config.setAppRateSnackbarTime(config.getAppInstalledTime() + 7 * 24 * 60 * 60 * 1000);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }



            FirebaseApp pushMobidziennikApp = FirebaseApp.initializeApp(
                    this,
                    new FirebaseOptions.Builder()
                            .setApiKey("AIzaSyCi5LmsZ5BBCQnGtrdvWnp1bWLCNP8OWQE")
                            .setApplicationId("1:747285019373:android:f6341bf7b158621d")
                            .build(),
                    "Mobidziennik2"
            );

            FirebaseApp pushLibrusApp = FirebaseApp.initializeApp(
                    this,
                    new FirebaseOptions.Builder()
                            .setApiKey("AIzaSyDfTuEoYPKdv4aceEws1CO3n0-HvTndz-o")
                            .setApplicationId("1:513056078587:android:1e29083b760af544")
                            .build(),
                    "Librus"
            );

            FirebaseApp pushVulcanApp = FirebaseApp.initializeApp(
                    this,
                    new FirebaseOptions.Builder()
                            .setApiKey("AIzaSyDW8MUtanHy64_I0oCpY6cOxB3jrvJd_iA")
                            .setApplicationId("1:987828170337:android:ac97431a0a4578c3")
                            .build(),
                    "Vulcan"
            );

            if (config.getRunSync()) {
                config.setRunSync(false);
                EdziennikTask.Companion.sync().enqueue(this);
            }

            try {
                final long startTime = System.currentTimeMillis();
                FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(instanceIdResult -> {
                    if (!instanceIdResult.getToken().equals(config.getSync().getTokenApp())) {
                        Log.d(TAG, "Token for App is " + instanceIdResult.getToken());
                        config.getSync().setTokenApp(instanceIdResult.getToken());
                    }
                });
                FirebaseInstanceId.getInstance(pushMobidziennikApp).getInstanceId().addOnSuccessListener(instanceIdResult -> {
                    if (!instanceIdResult.getToken().equals(config.getSync().getTokenMobidziennik())) {
                        Log.d(TAG, "Token for Mobidziennik2 is " + instanceIdResult.getToken());
                        config.getSync().setTokenMobidziennik(instanceIdResult.getToken());
                        config.getSync().setTokenMobidziennikList(new ArrayList<>());
                    }
                });
                FirebaseInstanceId.getInstance(pushLibrusApp).getInstanceId().addOnSuccessListener(instanceIdResult -> {
                    if (!instanceIdResult.getToken().equals(config.getSync().getTokenLibrus())) {
                        Log.d(TAG, "Token for Librus is " + instanceIdResult.getToken());
                        config.getSync().setTokenLibrus(instanceIdResult.getToken());
                        config.getSync().setTokenLibrusList(new ArrayList<>());
                    }
                });
                FirebaseInstanceId.getInstance(pushVulcanApp).getInstanceId().addOnSuccessListener(instanceIdResult -> {
                    if (!instanceIdResult.getToken().equals(config.getSync().getTokenVulcan())) {
                        Log.d(TAG, "Token for Vulcan is " + instanceIdResult.getToken());
                        config.getSync().setTokenVulcan(instanceIdResult.getToken());
                        config.getSync().setTokenVulcanList(new ArrayList<>());
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
                    Log.w(TAG, "Should remove app.appConfig."+fieldName);
                    //appSharedPrefs.edit().remove("app.appConfig."+fieldName).apply(); TODO migration
                }
            }
        }

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

    public void profileSave() {
        AsyncTask.execute(() -> {
            db.profileDao().add(profile);
        });
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

    public void profileLoadById(int id) {
        profileLoadById(id, false);
    }
    public void profileLoadById(int id, boolean loadedLast) {
        //Log.d(TAG, "Loading ID "+id);

        if (profile == null || profile.getId() != id) {
            profile = db.profileDao().getByIdNow(id);

            if (profile != null) {
                MainActivity.Companion.setUseOldMessages(profile.getLoginStoreType() == LOGIN_TYPE_MOBIDZIENNIK && appConfig.mobidziennikOldMessages == 1);
                profileId = profile.getId();
                appSharedPrefs.edit().putInt("current_profile_id", profile.getId()).apply();
                config.setProfile(profileId);
            }
            else if (!loadedLast) {
                profileLoadById(profileLastId(), true);
            }
            else {
                profileId = -1;
            }
        }
    }

    public int profileFirstId() {
        Integer id = db.profileDao().getFirstId();
        return id == null ? 1 : id;
    }

    public int profileLastId() {
        Integer id = db.profileDao().getLastId();
        return id == null ? 1 : id;
    }


    public Context getContext()
    {
        return mContext;
    }

    public void checkDevModePassword() {
        try {
            devMode = Utils.AESCrypt.decrypt("nWFVxY65Pa8/aRrT7EylNAencmOD+IxUY2Gg/beiIWY=", config.getDevModePassword()).equals("ok here you go it's enabled now")
                    || BuildConfig.DEBUG;
        } catch (Exception e) {
            e.printStackTrace();
            devMode = false;
        }
    }

}*/
