/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-29. 
 */

package pl.szczodrzynski.edziennik.config

@Suppress("RemoveExplicitTypeArguments")
class ProfileConfigAttendance(base: ProfileConfig) {

    var attendancePageSelection by base.config<Int>(1)
    var groupConsecutiveDays by base.config<Boolean>(true)
    var showPresenceInMonth by base.config<Boolean>(false)
    var useSymbols by base.config<Boolean>(false)
    var showDifference by base.config<Boolean>(false)
    var sortedDescending by base.config<Boolean>(false)
}
