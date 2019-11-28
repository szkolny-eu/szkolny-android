/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik

import android.util.Log
import androidx.work.Configuration

class Szkolny : /*MultiDexApplication(),*/ Configuration.Provider {

    /*val db by lazy { AppDb.getDatabase(this) }
    val networkUtils by lazy { NetworkUtils(this) }
    val notifier by lazy { Notifier(this) }
    val permissionChecker by lazy { PermissionChecker(this) }

    val cookieJar by lazy { PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(this)) }

    val deviceId: String by lazy { Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "" }*/

    override fun getWorkManagerConfiguration() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.VERBOSE)
            .build()

    /*override fun onCreate() {
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
                //.eventListener(new YourCustomEventListener())
                .apply()
        Iconics.init(applicationContext)
        Iconics.registerFont(SzkolnyFont)
    }*/
}