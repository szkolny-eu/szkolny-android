/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik

import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import com.google.gson.Gson
import com.mikepenz.iconics.Iconics
import im.wangchao.mhttp.MHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.leolin.shortcutbadger.ShortcutBadger
import okhttp3.OkHttpClient
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.core.manager.AttendanceManager
import pl.szczodrzynski.edziennik.core.manager.AvailabilityManager
import pl.szczodrzynski.edziennik.core.manager.BuildManager
import pl.szczodrzynski.edziennik.core.manager.EventManager
import pl.szczodrzynski.edziennik.core.manager.FirebaseManager
import pl.szczodrzynski.edziennik.core.manager.GradesManager
import pl.szczodrzynski.edziennik.core.manager.LoggingManager
import pl.szczodrzynski.edziennik.core.manager.MessageManager
import pl.szczodrzynski.edziennik.core.manager.NoteManager
import pl.szczodrzynski.edziennik.core.manager.NotificationManager
import pl.szczodrzynski.edziennik.core.manager.PermissionManager
import pl.szczodrzynski.edziennik.core.manager.ShortcutManager
import pl.szczodrzynski.edziennik.core.manager.TextStylingManager
import pl.szczodrzynski.edziennik.core.manager.TimetableManager
import pl.szczodrzynski.edziennik.core.manager.UiManager
import pl.szczodrzynski.edziennik.core.manager.UpdateManager
import pl.szczodrzynski.edziennik.core.manager.UserActionManager
import pl.szczodrzynski.edziennik.core.network.DumbCookieJar
import pl.szczodrzynski.edziennik.core.work.SyncWorker
import pl.szczodrzynski.edziennik.core.work.UpdateWorker
import pl.szczodrzynski.edziennik.data.api.events.ProfileListEmptyEvent
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.data.api.szkolny.interceptor.Signing
import pl.szczodrzynski.edziennik.data.config.AppData
import pl.szczodrzynski.edziennik.data.config.Config
import pl.szczodrzynski.edziennik.data.db.AppDb
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.enums.LoginType
import pl.szczodrzynski.edziennik.network.SSLProviderInstaller
import pl.szczodrzynski.edziennik.ui.main.CrashActivity
import pl.szczodrzynski.edziennik.utils.PermissionChecker
import pl.szczodrzynski.edziennik.utils.Utils
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

class App : MultiDexApplication(), Configuration.Provider, CoroutineScope {
    companion object {
        @Volatile
        lateinit var db: AppDb
            private set
        lateinit var config: Config

        // private set // for LabFragment
        lateinit var profile: Profile
            private set
        lateinit var data: AppData
            private set
        val profileId
            get() = profile.id

        var enableChucker = false
        var devMode = false
    }

    val api by lazy { SzkolnyApi(this) }
    val attendanceManager by lazy { AttendanceManager(this) }
    val availabilityManager by lazy { AvailabilityManager(this) }
    val buildManager by lazy { BuildManager(this) }
    val eventManager by lazy { EventManager(this) }
    val firebaseManager by lazy { FirebaseManager(this) }
    val gradesManager by lazy { GradesManager(this) }
    val loggingManager by lazy { LoggingManager(this) }
    val messageManager by lazy { MessageManager(this) }
    val noteManager by lazy { NoteManager(this) }
    val notificationManager by lazy { NotificationManager(this) }
    val permissionManager by lazy { PermissionManager(this) }
    val shortcutManager by lazy { ShortcutManager(this) }
    val textStylingManager by lazy { TextStylingManager(this) }
    val timetableManager by lazy { TimetableManager(this) }
    val uiManager by lazy { UiManager(this) }
    val updateManager by lazy { UpdateManager(this) }
    val userActionManager by lazy { UserActionManager(this) }

    val db
        get() = App.db
    val config
        get() = App.config
    val profile
        get() = App.profile
    val profileId
        get() = App.profileId
    val data
        get() = App.data

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override val workManagerConfiguration: Configuration = Configuration.Builder()
        .setMinimumLoggingLevel(Log.VERBOSE)
        .build()

    val permissionChecker by lazy { PermissionChecker(this) }
    val gson by lazy { Gson() }

    /*    _    _ _______ _______ _____
         | |  | |__   __|__   __|  __ \
         | |__| |  | |     | |  | |__) |
         |  __  |  | |     | |  |  ___/
         | |  | |  | |     | |  | |
         |_|  |_|  |_|     |_|  |*/
    lateinit var http: OkHttpClient
    lateinit var httpLazy: OkHttpClient

    private fun buildHttp() {
        val builder = OkHttpClient.Builder()
            .cache(null)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .cookieJar(cookieJar)
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

        SSLProviderInstaller.enableSupportedTls(builder, enableCleartext = true)

        if (devMode) {
            if (enableChucker) {
                val chuckerCollector =
                    ChuckerCollector(this, true, RetentionManager.Period.ONE_HOUR)
                val chuckerInterceptor = ChuckerInterceptor(this, chuckerCollector)
                builder.addInterceptor(chuckerInterceptor)
            }
        }

        http = builder.build()

        httpLazy = http.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

        MHttp.instance().customOkHttpClient(http)
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
    val deviceId: String by lazy {
        Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: ""
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

        // initialize Timber to enable basic logging
        Timber.plant(loggingManager.logcatTree)
        Timber.i("Initializing Szkolny.eu app v${BuildConfig.VERSION_NAME}")
        // initialize core objects
        AppData.read(this)
        App.db = AppDb(this)
        // read and migrate global config
        App.config = Config(this)
        App.config.migrate()
        // add database logging to Timber
        Timber.plant(loggingManager.databaseTree)
        Timber.i("Initialized Szkolny.eu app v${BuildConfig.VERSION_NAME}")

        devMode = config.devMode ?: BuildConfig.DEBUG
        if (config.devModePassword != null)
            checkDevModePassword()
        enableChucker = config.enableChucker ?: devMode

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        CaocConfig.Builder.create()
            .backgroundMode(CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM)
            .enabled(true)
            .showErrorDetails(true)
            .showRestartButton(true)
            .logErrorOnRestart(true)
            .trackActivities(true)
            .minTimeBetweenCrashesMs(60 * 1000)
            .errorDrawable(R.drawable.ic_rip)
            .restartActivity(MainActivity::class.java)
            .errorActivity(CrashActivity::class.java)
            .apply()
        Iconics.init(applicationContext)
        Iconics.respectFontBoundsDefault = true
        Signing.getCert(this)
        Utils.initializeStorageDir(this)
        buildHttp()

        uiManager.applyNightMode()
        uiManager.applyLanguage(this)

        if (!profileLoadById(config.lastProfileId)) {
            val success = db.profileDao().firstId?.let { profileLoadById(it) }
            if (success != true)
                profileLoad(Profile(0, 0, LoginType.TEMPLATE, ""))
        }

        launch(Dispatchers.Default) {
            buildManager.fetchInstalledTime()
            firebaseManager.initializeApps()
            loggingManager.cleanupIfNeeded()
            loggingManager.cleanupHyperLogDatabase()
            notificationManager.registerAllChannels()
            shortcutManager.createShortcuts()

            SSLProviderInstaller.install(applicationContext, this@App::buildHttp)

            if (config.sync.enabled)
                SyncWorker.scheduleNext(this@App, false)
            else
                SyncWorker.cancelNext(this@App)

            if (config.sync.notifyAboutUpdates)
                UpdateWorker.scheduleNext(this@App, false)
            else
                UpdateWorker.cancelNext(this@App)
        }

        db.metadataDao().countUnseen().observeForever { count: Int ->
            if (unreadBadgesAvailable)
                unreadBadgesAvailable = ShortcutBadger.applyCount(this@App, count)
        }
    }

    fun profileLoad(profile: Profile) {
        App.profile = profile
        App.config.lastProfileId = profile.id
        try {
            App.data = AppData.get(profile.loginStoreType)
            Timber.d("Loaded AppData: ${App.data}")
            // apply newly-added config overrides, if not changed by the user yet
            for ((key, value) in App.data.configOverrides) {
                val config = App.profile.config
                if (key !in config)
                    config[key] = value
            }
        } catch (e: Exception) {
            Timber.e(e, "Cannot load AppData")
            Toast.makeText(this, R.string.app_cannot_load_data, Toast.LENGTH_LONG).show()
            exitProcess(0)
        }
    }

    private fun profileLoadById(profileId: Int): Boolean {
        db.profileDao().getByIdNow(profileId)?.also {
            profileLoad(it)
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
            } else {
                onSuccess(profile)
            }
        }
    }

    fun profileSave() = profileSave(profile)
    fun profileSave(profile: Profile) {
        if (profile.id == profileId)
            App.profile = profile
        launch(Dispatchers.Default) {
            App.db.profileDao().add(profile)
        }
    }

    fun checkDevModePassword() {
        devMode = devMode || try {
            Utils.AESCrypt.decrypt(
                "nWFVxY65Pa8/aRrT7EylNAencmOD+IxUY2Gg/beiIWY=",
                config.devModePassword
            ) == "ok here you go it's enabled now"
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }
}
