/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-8.
 */

package pl.szczodrzynski.edziennik.ui.agenda.event

import android.annotation.SuppressLint
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.github.tibolte.agendacalendarview.render.EventRenderer
import com.mikepenz.iconics.view.IconicsTextView
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.AgendaWrappedEventBinding
import pl.szczodrzynski.edziennik.databinding.AgendaWrappedEventCompactBinding
import pl.szczodrzynski.edziennik.join
import pl.szczodrzynski.edziennik.resolveAttr
import pl.szczodrzynski.edziennik.setTintColor
import pl.szczodrzynski.edziennik.utils.Colors
import pl.szczodrzynski.edziennik.utils.managers.EventManager

class AgendaEventRenderer(
    val manager: EventManager,
    val isCompact: Boolean
) : EventRenderer<AgendaEvent>() {

    @SuppressLint("SetTextI18n")
    override fun render(view: View, aEvent: AgendaEvent) {
        if (isCompact) {
            val b = AgendaWrappedEventCompactBinding.bind(view).item
            bindView(aEvent, b.card, b.title, null, b.badgeBackground, b.badge)
        } else {
            val b = AgendaWrappedEventBinding.bind(view).item
            bindView(aEvent, b.card, b.title, b.subtitle, b.badgeBackground, b.badge)
        }
    }

    private fun bindView(
        aEvent: AgendaEvent,
        card: FrameLayout,
        title: IconicsTextView,
        subtitle: TextView?,
        badgeBackground: View,
        badge: View
    ) {
        val event = aEvent.event

        val textColor = Colors.legibleTextColor(event.eventColor)

        val timeText = if (event.time == null)
            card.context.getString(R.string.agenda_event_all_day)
        else
            event.time!!.stringHM

        val eventSubtitle = listOfNotNull(
            timeText,
            event.subjectLongName,
            event.teacherName,
            event.teamName
        ).join(", ")

        card.foreground.setTintColor(event.eventColor)
        card.background.setTintColor(event.eventColor)
        manager.setEventTopic(title, event, doneIconColor = textColor)
        title.setTextColor(textColor)
        subtitle?.text = eventSubtitle
        subtitle?.setTextColor(textColor)

        badgeBackground.isVisible = aEvent.showItemBadge
        badgeBackground.background.setTintColor(
            android.R.attr.colorBackground.resolveAttr(card.context)
        )
        badge.isVisible = aEvent.showItemBadge
    }

    override fun getEventLayout() = if (isCompact)
        R.layout.agenda_wrapped_event_compact
    else
        R.layout.agenda_wrapped_event
}
