/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-27.
 */

package pl.szczodrzynski.edziennik.config

import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.UNIVERSITY_AVERAGE_MODE_ECTS
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.COLOR_MODE_WEIGHTED
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.YEAR_ALL_GRADES

@Suppress("RemoveExplicitTypeArguments")
class ProfileConfigGrades(base: ProfileConfig) {

    var averageWithoutWeight by base.config<Boolean>(true)
    var colorMode by base.config<Int>(COLOR_MODE_WEIGHTED)
    var dontCountEnabled by base.config<Boolean>(false)
    var dontCountGrades by base.config<List<String>> { listOf() }
    var hideImproved by base.config<Boolean>(false)
    var hideNoGrade by base.config<Boolean>(false)
    var hideSticksFromOld by base.config<Boolean>(false)
    var minusValue by base.config<Float?>(null)
    var plusValue by base.config<Float?>(null)
    var yearAverageMode by base.config<Int>(YEAR_ALL_GRADES)
    var universityAverageMode by base.config<Int>(UNIVERSITY_AVERAGE_MODE_ECTS)
    var countEctsInProgress by base.config<Boolean>(false)
}
