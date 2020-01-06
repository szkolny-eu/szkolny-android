/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-27.
 */

package pl.szczodrzynski.edziennik.config

import pl.szczodrzynski.edziennik.config.utils.get
import pl.szczodrzynski.edziennik.config.utils.set
import pl.szczodrzynski.edziennik.data.db.entity.Profile.Companion.COLOR_MODE_WEIGHTED
import pl.szczodrzynski.edziennik.data.db.entity.Profile.Companion.YEAR_ALL_GRADES

class ProfileConfigGrades(private val config: ProfileConfig) {
    private var mColorMode: Int? = null
    var colorMode: Int
        get() { mColorMode = mColorMode ?: config.values.get("gradesColorMode", COLOR_MODE_WEIGHTED); return mColorMode ?: COLOR_MODE_WEIGHTED }
        set(value) { config.set("gradesColorMode", value); mColorMode = value }

    private var mYearAverageMode: Int? = null
    var yearAverageMode: Int
        get() { mYearAverageMode = mYearAverageMode ?: config.values.get("yearAverageMode", YEAR_ALL_GRADES); return mYearAverageMode ?: YEAR_ALL_GRADES }
        set(value) { config.set("yearAverageMode", value); mYearAverageMode = value }

    private var mCountZeroToAvg: Boolean? = null
    var countZeroToAvg: Boolean
        get() { mCountZeroToAvg = mCountZeroToAvg ?: config.values.get("countZeroToAvg", true); return mCountZeroToAvg ?: true }
        set(value) { config.set("countZeroToAvg", value); mCountZeroToAvg = value }
}
