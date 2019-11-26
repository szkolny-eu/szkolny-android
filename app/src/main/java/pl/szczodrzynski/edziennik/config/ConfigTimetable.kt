/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik.config

import pl.szczodrzynski.edziennik.utils.models.Time

class ConfigTimetable(val config: Config) {
    private var mBellSyncMultiplier: Int? = null
    var bellSyncMultiplier: Int
        get() { mBellSyncMultiplier = mBellSyncMultiplier ?: config.values.get("bellSyncMultiplier", 0); return mBellSyncMultiplier ?: 0 }
        set(value) { config.set(-1, "bellSyncMultiplier", value); mBellSyncMultiplier = value }

    private var mBellSyncDiff: Time? = null
    var bellSyncDiff: Time?
        get() { mBellSyncDiff = mBellSyncDiff ?: config.values.get("bellSyncDiff", null as Time?); return mBellSyncDiff }
        set(value) { config.set(-1, "bellSyncDiff", value); mBellSyncDiff = value }
}