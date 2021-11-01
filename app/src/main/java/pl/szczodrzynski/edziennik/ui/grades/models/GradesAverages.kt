/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-1.
 */

package pl.szczodrzynski.edziennik.ui.grades.models

class GradesAverages {
    var normalSum = 0f
    var normalCount = 0
    var normalWeightedSum = 0f
    var normalWeightedCount = 0f

    var pointSum = 0f

    var pointAvgSum = 0f
    var pointAvgMax = 0f

    var normalAvg: Float? = null
    var pointAvgPercent: Float? = null
}
