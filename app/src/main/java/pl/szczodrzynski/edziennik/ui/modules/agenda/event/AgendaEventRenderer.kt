/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-8.
 */

package pl.szczodrzynski.edziennik.ui.modules.agenda.event

import android.annotation.SuppressLint
import android.view.View
import com.github.tibolte.agendacalendarview.render.EventRenderer
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.AgendaWrappedEventBinding
import pl.szczodrzynski.edziennik.utils.Colors

class AgendaEventRenderer(
    private val isCompact: Boolean
) : EventRenderer<AgendaEvent>() {

    @SuppressLint("SetTextI18n")
    override fun render(view: View, aEvent: AgendaEvent) {
        val b = AgendaWrappedEventBinding.bind(view).item
        val event = aEvent.event

        b.isCompact = isCompact

        b.card.setCardBackgroundColor(event.eventColor)
        b.eventTitle.setTextColor(Colors.legibleTextColor(event.eventColor))
        b.eventSubtitle.setTextColor(Colors.legibleTextColor(event.eventColor))

        b.eventTitle.text = "${event.typeName ?: "wydarzenie"} - ${event.topic}"
        b.eventSubtitle.text =
            (if (event.time == null)
                view.context.getString(R.string.agenda_event_all_day)
            else
                event.time!!.stringHM) +
                    (event.subjectLongName?.let { ", $it" } ?: "") +
                    (event.teacherName?.let { ", $it" } ?: "") +
                    (event.teamName?.let { ", $it" } ?: "")
    }

    override fun getEventLayout(): Int = R.layout.agenda_wrapped_event
}
