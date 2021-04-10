/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-19.
 */

package pl.szczodrzynski.edziennik.config

import pl.szczodrzynski.edziennik.config.utils.get
import pl.szczodrzynski.edziennik.config.utils.set
import pl.szczodrzynski.edziennik.data.db.entity.Profile.Companion.AGENDA_DEFAULT
import pl.szczodrzynski.edziennik.ui.modules.home.HomeCardModel

class ProfileConfigUI(private val config: ProfileConfig) {
    private var mAgendaViewType: Int? = null
    var agendaViewType: Int
        get() { mAgendaViewType = mAgendaViewType ?: config.values.get("agendaViewType", 0); return mAgendaViewType ?: AGENDA_DEFAULT }
        set(value) { config.set("agendaViewType", value); mAgendaViewType = value }

    private var mAgendaCompactMode: Boolean? = null
    var agendaCompactMode: Boolean
        get() { mAgendaCompactMode = mAgendaCompactMode ?: config.values.get("agendaCompactMode", false); return mAgendaCompactMode ?: false }
        set(value) { config.set("agendaCompactMode", value); mAgendaCompactMode = value }

    private var mAgendaGroupByType: Boolean? = null
    var agendaGroupByType: Boolean
        get() { mAgendaGroupByType = mAgendaGroupByType ?: config.values.get("agendaGroupByType", false); return mAgendaGroupByType ?: false }
        set(value) { config.set("agendaGroupByType", value); mAgendaGroupByType = value }

    private var mAgendaLessonChanges: Boolean? = null
    var agendaLessonChanges: Boolean
        get() { mAgendaLessonChanges = mAgendaLessonChanges ?: config.values.get("agendaLessonChanges", true); return mAgendaLessonChanges ?: true }
        set(value) { config.set("agendaLessonChanges", value); mAgendaLessonChanges = value }

    private var mAgendaTeacherAbsence: Boolean? = null
    var agendaTeacherAbsence: Boolean
        get() { mAgendaTeacherAbsence = mAgendaTeacherAbsence ?: config.values.get("agendaTeacherAbsence", true); return mAgendaTeacherAbsence ?: true }
        set(value) { config.set("agendaTeacherAbsence", value); mAgendaTeacherAbsence = value }

    private var mAgendaElearningMark: Boolean? = null
    var agendaElearningMark: Boolean
        get() { mAgendaElearningMark = mAgendaElearningMark ?: config.values.get("agendaElearningMark", false); return mAgendaElearningMark ?: false }
        set(value) { config.set("agendaElearningMark", value); mAgendaElearningMark = value }

    private var mAgendaElearningGroup: Boolean? = null
    var agendaElearningGroup: Boolean
        get() { mAgendaElearningGroup = mAgendaElearningGroup ?: config.values.get("agendaElearningGroup", true); return mAgendaElearningGroup ?: true }
        set(value) { config.set("agendaElearningGroup", value); mAgendaElearningGroup = value }

    private var mHomeCards: List<HomeCardModel>? = null
    var homeCards: List<HomeCardModel>
        get() { mHomeCards = mHomeCards ?: config.values.get("homeCards", listOf(), HomeCardModel::class.java); return mHomeCards ?: listOf() }
        set(value) { config.set("homeCards", value); mHomeCards = value }
}
