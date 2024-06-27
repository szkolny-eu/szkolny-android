/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-27.
 */

package pl.szczodrzynski.edziennik.data.config

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.config.migration.ProfileConfigMigration2
import pl.szczodrzynski.edziennik.data.config.migration.ProfileConfigMigration3
import pl.szczodrzynski.edziennik.data.config.migration.ProfileConfigMigration4
import pl.szczodrzynski.edziennik.data.config.migration.ProfileConfigMigration5
import pl.szczodrzynski.edziennik.data.db.entity.ConfigEntry
import pl.szczodrzynski.edziennik.data.db.entity.Profile.Companion.AGENDA_DEFAULT
import pl.szczodrzynski.edziennik.data.enums.NotificationType
import pl.szczodrzynski.edziennik.ui.home.HomeCardModel
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.COLOR_MODE_WEIGHTED
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.YEAR_ALL_GRADES

class ProfileConfig(
    app: App,
    profileId: Int,
    entries: List<ConfigEntry>?,
) : BaseConfig<ProfileConfig>(app, profileId, entries) {

    override val dataVersion = 5
    override val migrations
        get() = mapOf(
            2 to ProfileConfigMigration2(),
            3 to ProfileConfigMigration3(),
            4 to ProfileConfigMigration4(),
            5 to ProfileConfigMigration5(),
        )

    val grades by lazy { Grades() }
    val ui by lazy { UI() }
    val sync by lazy { Sync() }
    val attendance by lazy { Attendance() }

    var shareByDefault by config<Boolean>(false)

    inner class Grades {
        var averageWithoutWeight by config<Boolean>(true)
        var colorMode by config<Int>(COLOR_MODE_WEIGHTED)
        var dontCountEnabled by config<Boolean>(false)
        var dontCountGrades by config<List<String>> { listOf() }
        var hideImproved by config<Boolean>(false)
        var hideSticksFromOld by config<Boolean>(false)
        var minusValue by config<Float?>(null)
        var plusValue by config<Float?>(null)
        var yearAverageMode by config<Int>(YEAR_ALL_GRADES)
    }

    inner class UI {
        var agendaViewType by config<Int>(AGENDA_DEFAULT)
        var agendaCompactMode by config<Boolean>(false)
        var agendaGroupByType by config<Boolean>(false)
        var agendaLessonChanges by config<Boolean>(true)
        var agendaTeacherAbsence by config<Boolean>(true)
        var agendaSubjectImportant by config<Boolean>(false)
        var agendaElearningMark by config<Boolean>(false)
        var agendaElearningGroup by config<Boolean>(true)

        var homeCards by config<List<HomeCardModel>> { listOf() }

        var messagesGreetingOnCompose by config<Boolean>(true)
        var messagesGreetingOnReply by config<Boolean>(true)
        var messagesGreetingOnForward by config<Boolean>(false)
        var messagesGreetingText by config<String?>(null)

        var timetableShowAttendance by config<Boolean>(true)
        var timetableShowEvents by config<Boolean>(true)
        var timetableTrimHourRange by config<Boolean>(false)
        var timetableColorSubjectName by config<Boolean>(false)
    }

    inner class Sync {
        var notificationFilter by config(NotificationType.Companion::getDefaultConfig)
    }

    inner class Attendance {
        var attendancePageSelection by config<Int>(1)
        var groupConsecutiveDays by config<Boolean>(true)
        var showPresenceInMonth by config<Boolean>(false)
        var useSymbols by config<Boolean>(false)
    }
}
