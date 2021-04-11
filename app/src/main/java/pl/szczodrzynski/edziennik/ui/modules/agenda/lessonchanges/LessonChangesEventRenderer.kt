/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-8.
 */

package pl.szczodrzynski.edziennik.ui.modules.agenda.lessonchanges

import android.view.View
import androidx.core.view.isVisible
import com.github.tibolte.agendacalendarview.render.EventRenderer
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.AgendaWrappedCounterBinding
import pl.szczodrzynski.edziennik.resolveAttr
import pl.szczodrzynski.edziennik.setTintColor
import pl.szczodrzynski.edziennik.utils.Colors

class LessonChangesEventRenderer : EventRenderer<LessonChangesEvent>() {

    override fun render(view: View, event: LessonChangesEvent) {
        val b = AgendaWrappedCounterBinding.bind(view).item

        b.card.foreground.setTintColor(event.color)
        b.card.background.setTintColor(event.color)
        b.name.setText(R.string.agenda_lesson_changes)
        b.name.setTextColor(Colors.legibleTextColor(event.color))
        b.count.text = event.count.toString()
        b.count.setTextColor(b.name.currentTextColor)

        b.badgeBackground.isVisible = event.showItemBadge
        b.badgeBackground.background.setTintColor(
            android.R.attr.colorBackground.resolveAttr(view.context)
        )
        b.badge.isVisible = event.showItemBadge
    }

    override fun getEventLayout(): Int = R.layout.agenda_wrapped_counter
}
