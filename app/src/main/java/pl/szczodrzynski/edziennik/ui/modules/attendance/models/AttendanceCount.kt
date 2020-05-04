/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-30.
 */

package pl.szczodrzynski.edziennik.ui.modules.attendance.models

class AttendanceCount {
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
