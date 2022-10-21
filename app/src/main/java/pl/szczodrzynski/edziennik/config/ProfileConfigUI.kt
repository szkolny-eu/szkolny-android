/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-19.
 */

package pl.szczodrzynski.edziennik.config

import pl.szczodrzynski.edziennik.ui.home.HomeCardModel

@Suppress("RemoveExplicitTypeArguments")
class ProfileConfigUI(base: ProfileConfig) {

    var agendaViewType by base.config<Int>(0)
    var agendaCompactMode by base.config<Boolean>(false)
    var agendaGroupByType by base.config<Boolean>(false)
    var agendaLessonChanges by base.config<Boolean>(true)
    var agendaTeacherAbsence by base.config<Boolean>(true)
    var agendaElearningMark by base.config<Boolean>(false)
    var agendaElearningGroup by base.config<Boolean>(true)

    var homeCards by base.config<List<HomeCardModel>> { listOf() }

    var messagesGreetingOnCompose by base.config<Boolean>(true)
    var messagesGreetingOnReply by base.config<Boolean>(true)
    var messagesGreetingOnForward by base.config<Boolean>(false)
    var messagesGreetingText by base.config<String?>(null)

    var timetableShowAttendance by base.config<Boolean>(true)
    var timetableShowEvents by base.config<Boolean>(true)
    var timetableTrimHourRange by base.config<Boolean>(false)
    var timetableColorSubjectName by base.config<Boolean>(false)
}
