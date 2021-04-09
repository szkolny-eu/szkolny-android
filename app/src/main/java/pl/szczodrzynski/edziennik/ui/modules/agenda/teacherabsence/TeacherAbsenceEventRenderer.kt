/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-8.
 */

package pl.szczodrzynski.edziennik.ui.modules.agenda.teacherabsence

import android.view.View
import com.github.tibolte.agendacalendarview.render.EventRenderer
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.AgendaWrappedTeacherAbsenceBinding

class TeacherAbsenceEventRenderer : EventRenderer<TeacherAbsenceEvent>() {

    override fun render(view: View, event: TeacherAbsenceEvent) {
        val b = AgendaWrappedTeacherAbsenceBinding.bind(view).item

        b.teacherAbsenceCount.text = event.absenceCount.toString()
    }

    override fun getEventLayout(): Int = R.layout.agenda_wrapped_teacher_absence
}
