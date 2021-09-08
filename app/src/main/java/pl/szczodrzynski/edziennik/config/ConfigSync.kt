/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik.config

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.config.utils.get
import pl.szczodrzynski.edziennik.config.utils.getIntList
import pl.szczodrzynski.edziennik.config.utils.set
import pl.szczodrzynski.edziennik.config.utils.setMap
import pl.szczodrzynski.edziennik.data.api.szkolny.response.RegisterAvailabilityStatus
import pl.szczodrzynski.edziennik.utils.models.Time

class ConfigSync(private val config: Config) {
    private val gson = Gson()

    private var mDontShowAppManagerDialog: Boolean? = null
    var dontShowAppManagerDialog: Boolean
        get() { mDontShowAppManagerDialog = mDontShowAppManagerDialog ?: config.values.get("dontShowAppManagerDialog", false); return mDontShowAppManagerDialog ?: false }
        set(value) { config.set("dontShowAppManagerDialog", value); mDontShowAppManagerDialog = value }

    private var mSyncEnabled: Boolean? = null
    var enabled: Boolean
        get() { mSyncEnabled = mSyncEnabled ?: config.values.get("syncEnabled", true); return mSyncEnabled ?: true }
        set(value) { config.set("syncEnabled", value); mSyncEnabled = value }

    private var mWebPushEnabled: Boolean? = null
    var webPushEnabled: Boolean
        get() { mWebPushEnabled = mWebPushEnabled ?: config.values.get("webPushEnabled", true); return mWebPushEnabled ?: true }
        set(value) { config.set("webPushEnabled", value); mWebPushEnabled = value }

    private var mSyncOnlyWifi: Boolean? = null
    var onlyWifi: Boolean
        get() { mSyncOnlyWifi = mSyncOnlyWifi ?: config.values.get("syncOnlyWifi", false); return mSyncOnlyWifi ?: notifyAboutUpdates }
        set(value) { config.set("syncOnlyWifi", value); mSyncOnlyWifi = value }

    private var mSyncInterval: Int? = null
    var interval: Int
        get() { mSyncInterval = mSyncInterval ?: config.values.get("syncInterval", 60*60); return mSyncInterval ?: 60*60 }
        set(value) { config.set("syncInterval", value); mSyncInterval = value }

    private var mNotifyAboutUpdates: Boolean? = null
    var notifyAboutUpdates: Boolean
        get() { mNotifyAboutUpdates = mNotifyAboutUpdates ?: config.values.get("notifyAboutUpdates", true); return mNotifyAboutUpdates ?: true }
        set(value) { config.set("notifyAboutUpdates", value); mNotifyAboutUpdates = value }

    private var mLastAppSync: Long? = null
    var lastAppSync: Long
        get() { mLastAppSync = mLastAppSync ?: config.values.get("lastAppSync", 0L); return mLastAppSync ?: 0L }
        set(value) { config.set("lastAppSync", value); mLastAppSync = value }

    /*     ____        _      _     _
          / __ \      (_)    | |   | |
         | |  | |_   _ _  ___| |_  | |__   ___  _   _ _ __ ___
         | |  | | | | | |/ _ \ __| | '_ \ / _ \| | | | '__/ __|
         | |__| | |_| | |  __/ |_  | | | | (_) | |_| | |  \__ \
          \___\_\\__,_|_|\___|\__| |_| |_|\___/ \__,_|_|  |__*/
    private var mQuietHoursEnabled: Boolean? = null
    var quietHoursEnabled: Boolean
        get() { mQuietHoursEnabled = mQuietHoursEnabled ?: config.values.get("quietHoursEnabled", false); return mQuietHoursEnabled ?: false }
        set(value) { config.set("quietHoursEnabled", value); mQuietHoursEnabled = value }

    private var mQuietHoursStart: Time? = null
    var quietHoursStart: Time?
        get() { mQuietHoursStart = mQuietHoursStart ?: config.values.get("quietHoursStart", null as Time?); return mQuietHoursStart }
        set(value) { config.set("quietHoursStart", value); mQuietHoursStart = value }

    private var mQuietHoursEnd: Time? = null
    var quietHoursEnd: Time?
        get() { mQuietHoursEnd = mQuietHoursEnd ?: config.values.get("quietHoursEnd", null as Time?); return mQuietHoursEnd }
        set(value) { config.set("quietHoursEnd", value); mQuietHoursEnd = value }

    private var mQuietDuringLessons: Boolean? = null
    var quietDuringLessons: Boolean
        get() { mQuietDuringLessons = mQuietDuringLessons ?: config.values.get("quietDuringLessons", false); return mQuietDuringLessons ?: false }
        set(value) { config.set("quietDuringLessons", value); mQuietDuringLessons = value }

    /*    ______ _____ __  __   _______    _
         |  ____/ ____|  \/  | |__   __|  | |
         | |__ | |    | \  / |    | | ___ | | _____ _ __  ___
         |  __|| |    | |\/| |    | |/ _ \| |/ / _ \ '_ \/ __|
         | |   | |____| |  | |    | | (_) |   <  __/ | | \__ \
         |_|    \_____|_|  |_|    |_|\___/|_|\_\___|_| |_|__*/
    private var mTokenApp: String? = null
    var tokenApp: String?
        get() { mTokenApp = mTokenApp ?: config.values.get("tokenApp", null as String?); return mTokenApp }
        set(value) { config.set("tokenApp", value); mTokenApp = value }
    private var mTokenMobidziennik: String? = null
    var tokenMobidziennik: String?
        get() { mTokenMobidziennik = mTokenMobidziennik ?: config.values.get("tokenMobidziennik", null as String?); return mTokenMobidziennik }
        set(value) { config.set("tokenMobidziennik", value); mTokenMobidziennik = value }
    private var mTokenLibrus: String? = null
    var tokenLibrus: String?
        get() { mTokenLibrus = mTokenLibrus ?: config.values.get("tokenLibrus", null as String?); return mTokenLibrus }
        set(value) { config.set("tokenLibrus", value); mTokenLibrus = value }
    private var mTokenVulcan: String? = null
    var tokenVulcan: String?
        get() { mTokenVulcan = mTokenVulcan ?: config.values.get("tokenVulcan", null as String?); return mTokenVulcan }
        set(value) { config.set("tokenVulcan", value); mTokenVulcan = value }
    private var mTokenVulcanHebe: String? = null
    var tokenVulcanHebe: String?
        get() { mTokenVulcanHebe = mTokenVulcanHebe ?: config.values.get("tokenVulcanHebe", null as String?); return mTokenVulcanHebe }
        set(value) { config.set("tokenVulcanHebe", value); mTokenVulcanHebe = value }

    private var mTokenMobidziennikList: List<Int>? = null
    var tokenMobidziennikList: List<Int>
        get() { mTokenMobidziennikList = mTokenMobidziennikList ?: config.values.getIntList("tokenMobidziennikList", listOf()); return mTokenMobidziennikList ?: listOf() }
        set(value) { config.set("tokenMobidziennikList", value); mTokenMobidziennikList = value }
    private var mTokenLibrusList: List<Int>? = null
    var tokenLibrusList: List<Int>
        get() { mTokenLibrusList = mTokenLibrusList ?: config.values.getIntList("tokenLibrusList", listOf()); return mTokenLibrusList ?: listOf() }
        set(value) { config.set("tokenLibrusList", value); mTokenLibrusList = value }
    private var mTokenVulcanList: List<Int>? = null
    var tokenVulcanList: List<Int>
        get() { mTokenVulcanList = mTokenVulcanList ?: config.values.getIntList("tokenVulcanList", listOf()); return mTokenVulcanList ?: listOf() }
        set(value) { config.set("tokenVulcanList", value); mTokenVulcanList = value }
    private var mTokenVulcanHebeList: List<Int>? = null
    var tokenVulcanHebeList: List<Int>
        get() { mTokenVulcanHebeList = mTokenVulcanHebeList ?: config.values.getIntList("tokenVulcanHebeList", listOf()); return mTokenVulcanHebeList ?: listOf() }
        set(value) { config.set("tokenVulcanHebeList", value); mTokenVulcanHebeList = value }

    private var mRegisterAvailability: Map<String, RegisterAvailabilityStatus>? = null
    var registerAvailability: Map<String, RegisterAvailabilityStatus>
        get() {
            val flavor = config.values.get("registerAvailabilityFlavor", null as String?)
            if (BuildConfig.FLAVOR != flavor)
                return mapOf()

            mRegisterAvailability = mRegisterAvailability ?: config.values.get("registerAvailability", null as String?)?.let { it ->
                gson.fromJson(it, object: TypeToken<Map<String, RegisterAvailabilityStatus>>(){}.type)
            }
            return mRegisterAvailability ?: mapOf()
        }
        set(value) {
            config.setMap("registerAvailability", value)
            config.set("registerAvailabilityFlavor", BuildConfig.FLAVOR)
            mRegisterAvailability = value
        }
}
