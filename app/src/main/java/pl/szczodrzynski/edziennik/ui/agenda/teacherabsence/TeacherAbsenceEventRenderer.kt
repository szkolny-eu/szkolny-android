/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-8.
 */

package pl.szczodrzynski.edziennik.ui.agenda.teacherabsence

import android.view.View
import androidx.core.view.isVisible
import com.github.tibolte.agendacalendarview.render.EventRenderer
import com.google.android.material.color.MaterialColors
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.AgendaCounterItemBinding
import pl.szczodrzynski.edziennik.databinding.AgendaWrappedCounterBinding
import pl.szczodrzynski.edziennik.ext.setTintColor
import pl.szczodrzynski.edziennik.utils.Colors

class TeacherAbsenceEventRenderer : EventRenderer<TeacherAbsenceEvent>() {

    override fun render(view: View, event: TeacherAbsenceEvent) {
        val b = AgendaWrappedCounterBinding.bind(view).item
        val harmonizedColor = MaterialColors.harmonizeWithPrimary(view.context, event.color)
        val textColor = Colors.legibleTextColor(harmonizedColor)

        b.card.foreground.setTintColor(harmonizedColor)
        b.card.background.setTintColor(harmonizedColor)
        b.name.setText(R.string.agenda_teacher_absence)
        b.name.setTextColor(textColor)
        b.count.text = event.count.toString()
        b.count.setTextColor(textColor)

        b.badgeBackground.isVisible = false
        b.badge.isVisible = false
    }

    fun render(b: AgendaCounterItemBinding, event: TeacherAbsenceEvent) {
        val harmonizedColor = MaterialColors.harmonizeWithPrimary(b.root.context, event.color)
        val textColor = Colors.legibleTextColor(harmonizedColor)

        b.card.foreground.setTintColor(harmonizedColor)
        b.card.background.setTintColor(harmonizedColor)
        b.name.setText(R.string.agenda_teacher_absence)
        b.name.setTextColor(textColor)
        b.count.text = event.count.toString()
        b.count.setTextColor(textColor)

        b.badgeBackground.isVisible = false
        b.badge.isVisible = false
    }

    override fun getEventLayout(): Int = R.layout.agenda_wrapped_counter
}
