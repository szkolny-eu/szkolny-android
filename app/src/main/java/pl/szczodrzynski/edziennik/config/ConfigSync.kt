/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik.config

import pl.szczodrzynski.edziennik.config.utils.get
import pl.szczodrzynski.edziennik.config.utils.set

class ConfigSync(val config: Config) {
    private var mSyncEnabled: Boolean? = null
    var enabled: Boolean
        get() { mSyncEnabled = mSyncEnabled ?: config.values.get("syncEnabled", true); return mSyncEnabled ?: true }
        set(value) { config.set("syncEnabled", value); mSyncEnabled = value }

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
}