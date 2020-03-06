/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-27.
 */

package pl.szczodrzynski.edziennik.config

import pl.szczodrzynski.edziennik.config.utils.get
import pl.szczodrzynski.edziennik.config.utils.getFloat
import pl.szczodrzynski.edziennik.config.utils.set
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.COLOR_MODE_WEIGHTED
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.YEAR_ALL_GRADES

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

    private var mPlusValue: Float? = null
    var plusValue: Float?
        get() { mPlusValue = mPlusValue ?: config.values.getFloat("plusValue"); return mPlusValue }
        set(value) { config.set("plusValue", value); mPlusValue = value }
    private var mMinusValue: Float? = null
    var minusValue: Float?
        get() { mMinusValue = mMinusValue ?: config.values.getFloat("minusValue"); return mMinusValue }
        set(value) { config.set("minusValue", value); mMinusValue = value }

    private var mDontCountGrades: List<String>? = null
    var dontCountGrades: List<String>
        get() { mDontCountGrades = mDontCountGrades ?: config.values.get("dontCountGrades", listOf()); return mDontCountGrades ?: listOf() }
        set(value) { config.set("dontCountGrades", value); mDontCountGrades = value }
}
