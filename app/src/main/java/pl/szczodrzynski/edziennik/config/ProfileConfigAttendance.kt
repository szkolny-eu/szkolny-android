/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-29. 
 */

package pl.szczodrzynski.edziennik.config

import pl.szczodrzynski.edziennik.utils.managers.AttendanceManager.Companion.SORTED_BY_ALPHABET

@Suppress("RemoveExplicitTypeArguments")
class ProfileConfigAttendance(base: ProfileConfig) {

    var attendancePageSelection by base.config<Int>(1)
    var groupConsecutiveDays by base.config<Boolean>(true)
    var showPresenceInMonth by base.config<Boolean>(false)
    var useSymbols by base.config<Boolean>(false)
    var showDifference by base.config<Boolean>(false)
    var orderBy by base.config<Int>(SORTED_BY_ALPHABET)
}
