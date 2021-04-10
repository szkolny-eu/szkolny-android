/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-10.
 */

package pl.szczodrzynski.edziennik.ui.modules.agenda.event

import android.view.View
import com.github.tibolte.agendacalendarview.render.EventRenderer
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.AgendaWrappedGroupBinding
import pl.szczodrzynski.edziennik.resolveAttr
import pl.szczodrzynski.edziennik.setTintColor
import pl.szczodrzynski.edziennik.utils.Colors

class AgendaEventGroupRenderer : EventRenderer<AgendaEventGroup>() {

    override fun render(view: View, event: AgendaEventGroup) {
        val b = AgendaWrappedGroupBinding.bind(view).item

        b.foreground.foreground.setTintColor(event.typeColor)
        b.background.background.setTintColor(event.typeColor)
        b.name.background.setTintColor(event.typeColor)
        b.name.text = event.typeName
        b.name.setTextColor(Colors.legibleTextColor(event.typeColor))
        b.count.text = event.eventCount.toString()
        b.count.background.setTintColor(android.R.attr.colorBackground.resolveAttr(view.context))
    }

    override fun getEventLayout(): Int = R.layout.agenda_wrapped_group
}

