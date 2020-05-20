/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.hypertrack.hyperlog.HyperLog
import com.mikepenz.iconics.Iconics
import com.mikepenz.iconics.typeface.library.szkolny.font.SzkolnyFont
import im.wangchao.mhttp.MHttp
import kotlinx.coroutines.*
import me.leolin.shortcutbadger.ShortcutBadger
import okhttp3.OkHttpClient
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.config.Config
import pl.szczodrzynski.edziennik.data.api.events.ProfileListEmptyEvent
import pl.szczodrzynski.edziennik.data.api.szkolny.interceptor.Signing
import pl.szczodrzynski.edziennik.data.db.AppDb
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.network.NetworkUtils
import pl.szczodrzynski.edziennik.network.cookie.DumbCookieJar
import pl.szczodrzynski.edziennik.sync.SyncWorker
import pl.szczodrzynski.edziennik.sync.UpdateWorker
import pl.szczodrzynski.edziennik.ui.modules.base.CrashActivity
import pl.szczodrzynski.edziennik.utils.*
import pl.szczodrzynski.edziennik.utils.Utils.d
import pl.szczodrzynski.edziennik.utils.managers.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class App : MultiDexApplication(), Configuration.Provider, CoroutineScope {
    companion object {
        @Volatile
        lateinit var db: AppDb
        val config: Config by lazy { Config(db) }
        var profile: Profile by mutableLazy { Profile(0, 0, 0, "") }
        val profileId
            get() = profile.id

        var devMode = false
        var debugMode = false
    }

    val notificationChannelsManager by lazy { NotificationChannelsManager(this) }
    val userActionManager by lazy { UserActionManager(this) }
    val gradesManager by lazy { GradesManager(this) }
    val timetableManager by lazy { TimetableManager(this) }
    val eventManager by lazy { EventManager(this) }
    val permissionManager by lazy { PermissionManager(this) }
    val attendanceManager by lazy { AttendanceManager(this) }

    val db
        get() = App.db
    val config
        get() = App.config
    val profile
        get() = App.profile
    val profileId
        get() = App.profileId

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main
    override fun getWorkManagerConfiguration() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.VERBOSE)
            .build()

    val permissionChecker by lazy { PermissionChecker(this) }
    val networkUtils by lazy { NetworkUtils(this) }
    val gson by lazy { Gson() }

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
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
        builder.installHttpsSupport(this)

        if (debugMode || BuildConfig.DEBUG) {
            HyperLog.initialize(this)
            HyperLog.setLogLevel(Log.VERBOSE)
            HyperLog.setLogFormat(DebugLogFormat(this))
            val chuckerCollector = ChuckerCollector(this, true, RetentionManager.Period.ONE_HOUR)
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
    val cookieJar by lazy { DumbCookieJar(this) }

    /*     _____ _                   _
          / ____(_)                 | |
         | (___  _  __ _ _ __   __ _| |_ _   _ _ __ ___
          \___ \| |/ _` | '_ \ / _` | __| | | | '__/ _ \
          ____) | | (_| | | | | (_| | |_| |_| | | |  __/
         |_____/|_|\__, |_| |_|\__,_|\__|\__,_|_|  \___|
                    __/ |
                   |__*/
    val deviceId: String by lazy { Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "" }
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
        App.db = AppDb(this)
        Themes.themeInt = config.ui.theme
        debugMode = config.debugMode
        MHttp.instance().customOkHttpClient(http)

        if (!profileLoadById(config.lastProfileId)) {
            db.profileDao().firstId?.let { profileLoadById(it) }
        }

        config.ui.language?.let {
            setLanguage(it)
        }

        devMode = BuildConfig.DEBUG
        if (BuildConfig.DEBUG)
            debugMode = true

        Signing.getCert(this)

        launch {
            withContext(Dispatchers.Default) {
                config.migrate(this@App)

                if (config.devModePassword != null)
                    checkDevModePassword()
                debugMode = devMode || config.debugMode

                if (config.sync.enabled)
                    SyncWorker.scheduleNext(this@App, false)
                else
                    SyncWorker.cancelNext(this@App)

                if (config.sync.notifyAboutUpdates)
                    UpdateWorker.scheduleNext(this@App, false)
                else
                    UpdateWorker.cancelNext(this@App)

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

                notificationChannelsManager.registerAllChannels()


                if (config.appInstalledTime == 0L)
                    try {
                        config.appInstalledTime = packageManager.getPackageInfo(packageName, 0).firstInstallTime
                        config.appRateSnackbarTime = config.appInstalledTime + 7 * DAY * MS
                    } catch (e: PackageManager.NameNotFoundException) {
                        e.printStackTrace()
                    }

                val pushMobidziennikApp = FirebaseApp.initializeApp(
                        this@App,
                        FirebaseOptions.Builder()
                                .setProjectId("mobidziennik")
                                .setStorageBucket("mobidziennik.appspot.com")
                                .setDatabaseUrl("https://mobidziennik.firebaseio.com")
                                .setGcmSenderId("747285019373")
                                .setApiKey("AIzaSyCi5LmsZ5BBCQnGtrdvWnp1bWLCNP8OWQE")
                                .setApplicationId("1:747285019373:android:f6341bf7b158621d")
                                .build(),
                        "Mobidziennik2"
                )

                val pushLibrusApp = FirebaseApp.initializeApp(
                        this@App,
                        FirebaseOptions.Builder()
                                .setProjectId("synergiadru")
                                .setStorageBucket("synergiadru.appspot.com")
                                .setDatabaseUrl("https://synergiadru.firebaseio.com")
                                .setGcmSenderId("513056078587")
                                .setApiKey("AIzaSyDfTuEoYPKdv4aceEws1CO3n0-HvTndz-o")
                                .setApplicationId("1:513056078587:android:1e29083b760af544")
                                .build(),
                        "Librus"
                )

                val pushVulcanApp = FirebaseApp.initializeApp(
                        this@App,
                        FirebaseOptions.Builder()
                                .setProjectId("dzienniczekplus")
                                .setStorageBucket("dzienniczekplus.appspot.com")
                                .setDatabaseUrl("https://dzienniczekplus.firebaseio.com")
                                .setGcmSenderId("987828170337")
                                .setApiKey("AIzaSyDW8MUtanHy64_I0oCpY6cOxB3jrvJd_iA")
                                .setApplicationId("1:987828170337:android:ac97431a0a4578c3")
                                .build(),
                        "Vulcan"
                )

                try {
                    FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { instanceIdResult ->
                        val token = instanceIdResult.token
                        d("Firebase", "Got App token: $token")
                        config.sync.tokenApp = token
                    }
                    FirebaseInstanceId.getInstance(pushMobidziennikApp).instanceId.addOnSuccessListener { instanceIdResult ->
                        val token = instanceIdResult.token
                        d("Firebase", "Got Mobidziennik2 token: $token")
                        if (token != config.sync.tokenMobidziennik) {
                            config.sync.tokenMobidziennik = token
                            config.sync.tokenMobidziennikList = listOf()
                        }
                    }
                    FirebaseInstanceId.getInstance(pushLibrusApp).instanceId.addOnSuccessListener { instanceIdResult ->
                        val token = instanceIdResult.token
                        d("Firebase", "Got Librus token: $token")
                        if (token != config.sync.tokenLibrus) {
                            config.sync.tokenLibrus = token
                            config.sync.tokenLibrusList = listOf()
                        }
                    }
                    FirebaseInstanceId.getInstance(pushVulcanApp).instanceId.addOnSuccessListener { instanceIdResult ->
                        val token = instanceIdResult.token
                        d("Firebase", "Got Vulcan token: $token")
                        if (token != config.sync.tokenVulcan) {
                            config.sync.tokenVulcan = token
                            config.sync.tokenVulcanList = listOf()
                        }
                    }
                    FirebaseMessaging.getInstance().subscribeToTopic(packageName)
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
            }

            db.metadataDao().countUnseen().observeForever { count: Int ->
                if (unreadBadgesAvailable)
                    unreadBadgesAvailable = ShortcutBadger.applyCount(this@App, count)
            }
        }
    }

    private fun profileLoadById(profileId: Int): Boolean {
        db.profileDao().getByIdNow(profileId)?.also {
            App.profile = it
            App.config.lastProfileId = it.id
            return true
        }
        return false
    }
    fun profileLoad(profileId: Int, onSuccess: (profile: Profile) -> Unit) {
        launch {
            val success = withContext(Dispatchers.Default) {
                profileLoadById(profileId)
            }
            if (success)
                onSuccess(profile)
            else
                profileLoadLast(onSuccess)
        }
    }
    fun profileLoadLast(onSuccess: (profile: Profile) -> Unit) {
        launch {
            val success = withContext(Dispatchers.Default) {
                profileLoadById(db.profileDao().lastId ?: return@withContext false)
            }
            if (!success) {
                EventBus.getDefault().post(ProfileListEmptyEvent())
            }
        }
    }
    fun profileSave() = profileSave(profile)
    fun profileSave(profile: Profile) {
        launch(Dispatchers.Default) {
            App.db.profileDao().add(profile)
        }
    }

    fun checkDevModePassword() {
        devMode = try {
            Utils.AESCrypt.decrypt("nWFVxY65Pa8/aRrT7EylNAencmOD+IxUY2Gg/beiIWY=", config.devModePassword) == "ok here you go it's enabled now" || BuildConfig.DEBUG
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
