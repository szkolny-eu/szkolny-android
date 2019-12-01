/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik

import android.util.Log
import androidx.work.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

class Szkolny : /*MultiDexApplication(),*/ Configuration.Provider, CoroutineScope {
    companion object {
        var devMode = false
    }

    //lateinit var db: AppDb
    //val config by lazy { Config(db); // TODO migrate }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main
    override fun getWorkManagerConfiguration() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.VERBOSE)
            .build()

    /*val preferences by lazy { getSharedPreferences(getString(R.string.preference_file), Context.MODE_PRIVATE) }
    val notifier by lazy { Notifier(this) }
    val permissionChecker by lazy { PermissionChecker(this) }

    lateinit var profile: ProfileFull

    /*    _    _ _______ _______ _____
         | |  | |__   __|__   __|  __ \
         | |__| |  | |     | |  | |__) |
         |  __  |  | |     | |  |  ___/
         | |  | |  | |     | |  | |
         |_|  |_|  |_|     |_|  |*/
    val http: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
                .cache(null)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .cookieJar(cookieJar)
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
        builder.installHttpsSupport()

        if (devMode || BuildConfig.DEBUG) {
            HyperLog.initialize(this)
            HyperLog.setLogLevel(Log.VERBOSE)
            HyperLog.setLogFormat(DebugLogFormat(this))
            val chuckerCollector = ChuckerCollector(this, true, Period.ONE_HOUR)
            val chuckerInterceptor = ChuckerInterceptor(this, chuckerCollector)
            builder.addInterceptor(chuckerInterceptor)
        }

        builder.build()
    }
    val httpLazy: OkHttpClient by lazy {
        http.newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .build()
    }
    val cookieJar by lazy { PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(this)) }

    /*     _____ _                   _
          / ____(_)                 | |
         | (___  _  __ _ _ __   __ _| |_ _   _ _ __ ___
          \___ \| |/ _` | '_ \ / _` | __| | | | '__/ _ \
          ____) | | (_| | | | | (_| | |_| |_| | | |  __/
         |_____/|_|\__, |_| |_|\__,_|\__|\__,_|_|  \___|
                    __/ |
                   |__*/
    private val deviceId: String by lazy { Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "" }
    private val signature: String by lazy {
        var str = ""
        try {
            val packageInfo: PackageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in packageInfo.signatures) {
                val signatureBytes = signature.toByteArray()
                val md = MessageDigest.getInstance("SHA")
                md.update(signatureBytes)
                str = Base64.encodeToString(md.digest(), Base64.DEFAULT)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        str
    }
    private var unreadBadgesAvailable = true

    /*                 _____                _
                      / ____|              | |
           ___  _ __ | |     _ __ ___  __ _| |_ ___
          / _ \| '_ \| |    | '__/ _ \/ _` | __/ _ \
         | (_) | | | | |____| | |  __/ (_| | ||  __/
          \___/|_| |_|\_____|_|  \___|\__,_|\__\__*/
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        CaocConfig.Builder.create()
                .backgroundMode(CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM)
                .enabled(true)
                .showErrorDetails(true)
                .showRestartButton(true)
                .logErrorOnRestart(true)
                .trackActivities(true)
                .minTimeBetweenCrashesMs(60*1000)
                .errorDrawable(R.drawable.ic_rip)
                .restartActivity(MainActivity::class.java)
                .errorActivity(CrashActivity::class.java)
                .apply()
        Iconics.init(applicationContext)
        Iconics.registerFont(SzkolnyFont)
        db = AppDb.getDatabase(this)
        Themes.themeInt = config.ui.theme
        MHttp.instance().customOkHttpClient(http)

        devMode = "f054761fbdb6a238" == deviceId || BuildConfig.DEBUG
        if (config.devModePassword != null)
            checkDevModePassword()



        launch { async(Dispatchers.Default) {
            if (config.sync.enabled) {
                scheduleNext(this@App, false)
            } else {
                cancelNext(this@App)
            }

            db.metadataDao().countUnseen().observeForever { count: Int ->
                if (unreadBadgesAvailable)
                    unreadBadgesAvailable = ShortcutBadger.applyCount(this@App, count)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                val shortcutManager = getSystemService(ShortcutManager::class.java)

                val shortcutTimetable = ShortcutInfo.Builder(this@App, "item_timetable")
                        .setShortLabel(getString(R.string.shortcut_timetable)).setLongLabel(getString(R.string.shortcut_timetable))
                        .setIcon(Icon.createWithResource(this@App, R.mipmap.ic_shortcut_timetable))
                        .setIntent(Intent(Intent.ACTION_MAIN, null, this@App, MainActivity::class.java)
                                .putExtra("fragmentId", MainActivity.DRAWER_ITEM_TIMETABLE))
                        .build()

                val shortcutAgenda = ShortcutInfo.Builder(this@App, "item_agenda")
                        .setShortLabel(getString(R.string.shortcut_agenda)).setLongLabel(getString(R.string.shortcut_agenda))
                        .setIcon(Icon.createWithResource(this@App, R.mipmap.ic_shortcut_agenda))
                        .setIntent(Intent(Intent.ACTION_MAIN, null, this@App, MainActivity::class.java)
                                .putExtra("fragmentId", MainActivity.DRAWER_ITEM_AGENDA))
                        .build()

                val shortcutGrades = ShortcutInfo.Builder(this@App, "item_grades")
                        .setShortLabel(getString(R.string.shortcut_grades)).setLongLabel(getString(R.string.shortcut_grades))
                        .setIcon(Icon.createWithResource(this@App, R.mipmap.ic_shortcut_grades))
                        .setIntent(Intent(Intent.ACTION_MAIN, null, this@App, MainActivity::class.java)
                                .putExtra("fragmentId", MainActivity.DRAWER_ITEM_GRADES))
                        .build()

                val shortcutHomework = ShortcutInfo.Builder(this@App, "item_homeworks")
                        .setShortLabel(getString(R.string.shortcut_homework)).setLongLabel(getString(R.string.shortcut_homework))
                        .setIcon(Icon.createWithResource(this@App, R.mipmap.ic_shortcut_homework))
                        .setIntent(Intent(Intent.ACTION_MAIN, null, this@App, MainActivity::class.java)
                                .putExtra("fragmentId", MainActivity.DRAWER_ITEM_HOMEWORK))
                        .build()

                val shortcutMessages = ShortcutInfo.Builder(this@App, "item_messages")
                        .setShortLabel(getString(R.string.shortcut_messages)).setLongLabel(getString(R.string.shortcut_messages))
                        .setIcon(Icon.createWithResource(this@App, R.mipmap.ic_shortcut_messages))
                        .setIntent(Intent(Intent.ACTION_MAIN, null, this@App, MainActivity::class.java)
                                .putExtra("fragmentId", MainActivity.DRAWER_ITEM_MESSAGES))
                        .build()

                shortcutManager.dynamicShortcuts = listOf(
                        shortcutTimetable,
                        shortcutAgenda,
                        shortcutGrades,
                        shortcutHomework,
                        shortcutMessages
                )
            } // shortcuts - end

            if (config.appInstalledTime == 0L)
                try {
                    config.appInstalledTime = packageManager.getPackageInfo(packageName, 0).firstInstallTime
                    config.appRateSnackbarTime = config.appInstalledTime + 7*DAY*MS
                } catch (e: NameNotFoundException) {
                    e.printStackTrace()
                }

            val pushMobidziennikApp = FirebaseApp.initializeApp(
                    this@App,
                    FirebaseOptions.Builder()
                            .setApiKey("AIzaSyCi5LmsZ5BBCQnGtrdvWnp1bWLCNP8OWQE")
                            .setApplicationId("1:747285019373:android:f6341bf7b158621d")
                            .build(),
                    "Mobidziennik2"
            )

            val pushLibrusApp = FirebaseApp.initializeApp(
                    this@App,
                    FirebaseOptions.Builder()
                            .setApiKey("AIzaSyDfTuEoYPKdv4aceEws1CO3n0-HvTndz-o")
                            .setApplicationId("1:513056078587:android:1e29083b760af544")
                            .build(),
                    "Librus"
            )

            val pushVulcanApp = FirebaseApp.initializeApp(
                    this@App,
                    FirebaseOptions.Builder()
                            .setApiKey("AIzaSyDW8MUtanHy64_I0oCpY6cOxB3jrvJd_iA")
                            .setApplicationId("1:987828170337:android:ac97431a0a4578c3")
                            .build(),
                    "Vulcan"
            )

            try {
                FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { instanceIdResult ->
                    val token = instanceIdResult.token
                    config.sync.tokenApp = token
                }
                FirebaseInstanceId.getInstance(pushMobidziennikApp).instanceId.addOnSuccessListener { instanceIdResult ->
                    val token = instanceIdResult.token
                    if (token != config.sync.tokenMobidziennik) {
                        config.sync.tokenMobidziennik = token
                        config.sync.tokenMobidziennikList = listOf()
                    }
                }
                FirebaseInstanceId.getInstance(pushLibrusApp).instanceId.addOnSuccessListener { instanceIdResult ->
                    val token = instanceIdResult.token
                    if (token != config.sync.tokenLibrus) {
                        config.sync.tokenLibrus = token
                        config.sync.tokenLibrusList = listOf()
                    }
                }
                FirebaseInstanceId.getInstance(pushVulcanApp).instanceId.addOnSuccessListener { instanceIdResult ->
                    val token = instanceIdResult.token
                    if (token != config.sync.tokenVulcan) {
                        config.sync.tokenVulcan = token
                        config.sync.tokenVulcanList = listOf()
                    }
                }
                FirebaseMessaging.getInstance().subscribeToTopic(packageName)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }}
    }

    private fun profileLoad(profileId: Int) {
        db.profileDao().getFullByIdNow(profileId)?.also {
            profile = it
        } ?: run {
            if (!::profile.isInitialized) {
                profile = ProfileFull(-1, "", "", -1)
            }
        }
    }
    fun profileLoad(profileId: Int, onSuccess: (profile: ProfileFull) -> Unit) {
        launch {
            val deferred = async(Dispatchers.Default) {
                profileLoad(profileId)
            }
            deferred.await()
            onSuccess(profile)
        }
    }

    private fun OkHttpClient.Builder.installHttpsSupport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                try {
                    ProviderInstaller.installIfNeeded(this@App)
                } catch (e: Exception) {
                    Log.e("OkHttpTLSCompat", "Play Services not found or outdated")

                    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                    trustManagerFactory.init(null as KeyStore?)

                    val x509TrustManager = trustManagerFactory.trustManagers.singleOrNull { it is X509TrustManager } as X509TrustManager?
                            ?: return

                    val sc = SSLContext.getInstance("TLSv1.2")
                    sc.init(null, null, null)
                    sslSocketFactory(TLSSocketFactory(sc.socketFactory), x509TrustManager)
                    val cs: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                            .tlsVersions(TlsVersion.TLS_1_0)
                            .tlsVersions(TlsVersion.TLS_1_1)
                            .tlsVersions(TlsVersion.TLS_1_2)
                            .build()
                    val specs: MutableList<ConnectionSpec> = ArrayList()
                    specs.add(cs)
                    specs.add(ConnectionSpec.COMPATIBLE_TLS)
                    specs.add(ConnectionSpec.CLEARTEXT)
                    connectionSpecs(specs)
                }
            } catch (exc: Exception) {
                Log.e("OkHttpTLSCompat", "Error while setting TLS 1.2", exc)
            }
        }
    }

    fun checkDevModePassword() {
        devMode = try {
            Utils.AESCrypt.decrypt("nWFVxY65Pa8/aRrT7EylNAencmOD+IxUY2Gg/beiIWY=", config.devModePassword) == "ok here you go it's enabled now" || BuildConfig.DEBUG
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }*/
}