/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-8.
 */

package pl.szczodrzynski.edziennik.ui.modules.agenda.event

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.isVisible
import com.github.tibolte.agendacalendarview.render.EventRenderer
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.AgendaWrappedEventBinding
import pl.szczodrzynski.edziennik.databinding.AgendaWrappedEventCompactBinding
import pl.szczodrzynski.edziennik.join
import pl.szczodrzynski.edziennik.resolveAttr
import pl.szczodrzynski.edziennik.setTintColor
import pl.szczodrzynski.edziennik.utils.Colors

class AgendaEventRenderer(
    val isCompact: Boolean
) : EventRenderer<AgendaEvent>() {

    @SuppressLint("SetTextI18n")
    override fun render(view: View, aEvent: AgendaEvent) {
        val event = aEvent.event

        val timeText = if (event.time == null)
            view.context.getString(R.string.agenda_event_all_day)
        else
            event.time!!.stringHM

        val eventTitle = "${event.typeName ?: "wydarzenie"} - ${event.topic}"

        val eventSubtitle = listOfNotNull(
            timeText,
            event.subjectLongName,
            event.teacherName,
            event.teamName
        ).join(", ")

        if (isCompact) {
            val b = AgendaWrappedEventCompactBinding.bind(view).item

            b.card.foreground.setTintColor(event.eventColor)
            b.card.background.setTintColor(event.eventColor)
            b.title.text = eventTitle
            b.title.setTextColor(Colors.legibleTextColor(event.eventColor))

            b.badgeBackground.isVisible = aEvent.showItemBadge
            b.badgeBackground.background.setTintColor(
                android.R.attr.colorBackground.resolveAttr(view.context)
            )
            b.badge.isVisible = aEvent.showItemBadge
        }
        else {
            val b = AgendaWrappedEventBinding.bind(view).item

            b.card.foreground.setTintColor(event.eventColor)
            b.card.background.setTintColor(event.eventColor)
            b.title.text = eventTitle
            b.title.setTextColor(Colors.legibleTextColor(event.eventColor))
            b.subtitle.text = eventSubtitle
            b.subtitle.setTextColor(Colors.legibleTextColor(event.eventColor))

            b.badgeBackground.isVisible = aEvent.showItemBadge
            b.badgeBackground.background.setTintColor(
                android.R.attr.colorBackground.resolveAttr(view.context)
            )
            b.badge.isVisible = aEvent.showItemBadge
        }
    }

    override fun getEventLayout() = if (isCompact)
        R.layout.agenda_wrapped_event_compact
    else
        R.layout.agenda_wrapped_event
}
