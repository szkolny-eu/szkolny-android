/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-8.
 */

package pl.szczodrzynski.edziennik.ui.agenda.lessonchanges

import android.view.View
import androidx.core.view.isVisible
import com.github.tibolte.agendacalendarview.render.EventRenderer
import com.google.android.material.color.MaterialColors
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.AgendaCounterItemBinding
import pl.szczodrzynski.edziennik.databinding.AgendaWrappedCounterBinding
import pl.szczodrzynski.edziennik.ext.resolveAttr
import pl.szczodrzynski.edziennik.ext.setTintColor
import pl.szczodrzynski.edziennik.utils.Colors

class LessonChangesEventRenderer : EventRenderer<LessonChangesEvent>() {

    override fun render(view: View, event: LessonChangesEvent) {
        val b = AgendaWrappedCounterBinding.bind(view).item
        val harmonizedColor = MaterialColors.harmonizeWithPrimary(view.context, event.color)
        val textColor = Colors.legibleTextColor(harmonizedColor)

        b.card.foreground.setTintColor(harmonizedColor)
        b.card.background.setTintColor(harmonizedColor)
        b.name.setText(R.string.agenda_lesson_changes)
        b.name.setTextColor(textColor)
        b.count.text = event.count.toString()
        b.count.setTextColor(textColor)

        b.badgeBackground.isVisible = event.showItemBadge
        b.badgeBackground.background.setTintColor(
            android.R.attr.colorBackground.resolveAttr(view.context)
        )
        b.badge.isVisible = event.showItemBadge
    }

    fun render(b: AgendaCounterItemBinding, event: LessonChangesEvent) {
        val harmonizedColor = MaterialColors.harmonizeWithPrimary(b.root.context, event.color)
        val textColor = Colors.legibleTextColor(harmonizedColor)
        b.card.foreground.setTintColor(harmonizedColor)
        b.card.background.setTintColor(harmonizedColor)
        b.name.setText(R.string.agenda_lesson_changes)
        b.name.setTextColor(textColor)
        b.count.text = event.count.toString()
        b.count.setTextColor(textColor)

        b.badgeBackground.isVisible = event.showItemBadge
        b.badgeBackground.background.setTintColor(
            android.R.attr.colorBackground.resolveAttr(b.root.context)
        )
        b.badge.isVisible = event.showItemBadge
    }

    override fun getEventLayout(): Int = R.layout.agenda_wrapped_counter
}
