/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-8.
 */

package pl.szczodrzynski.edziennik.ui.agenda.teacherabsence

import android.view.View
import androidx.core.view.isVisible
import com.github.tibolte.agendacalendarview.render.EventRenderer
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.AgendaCounterItemBinding
import pl.szczodrzynski.edziennik.databinding.AgendaWrappedCounterBinding
import pl.szczodrzynski.edziennik.setTintColor
import pl.szczodrzynski.edziennik.utils.Colors

class TeacherAbsenceEventRenderer : EventRenderer<TeacherAbsenceEvent>() {

    override fun render(view: View, event: TeacherAbsenceEvent) {
        val b = AgendaWrappedCounterBinding.bind(view).item
        val textColor = Colors.legibleTextColor(event.color)

        b.card.foreground.setTintColor(event.color)
        b.card.background.setTintColor(event.color)
        b.name.setText(R.string.agenda_teacher_absence)
        b.name.setTextColor(textColor)
        b.count.text = event.count.toString()
        b.count.setTextColor(textColor)

        b.badgeBackground.isVisible = false
        b.badge.isVisible = false
    }

    fun render(b: AgendaCounterItemBinding, event: TeacherAbsenceEvent) {
        val textColor = Colors.legibleTextColor(event.color)

        b.card.foreground.setTintColor(event.color)
        b.card.background.setTintColor(event.color)
        b.name.setText(R.string.agenda_teacher_absence)
        b.name.setTextColor(textColor)
        b.count.text = event.count.toString()
        b.count.setTextColor(textColor)

        b.badgeBackground.isVisible = false
        b.badge.isVisible = false
    }

    override fun getEventLayout(): Int = R.layout.agenda_wrapped_counter
}
