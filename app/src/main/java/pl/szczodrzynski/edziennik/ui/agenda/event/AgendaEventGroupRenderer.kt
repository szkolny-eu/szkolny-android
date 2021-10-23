/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-10.
 */

package pl.szczodrzynski.edziennik.ui.agenda.event

import android.view.View
import androidx.core.view.isVisible
import com.github.tibolte.agendacalendarview.render.EventRenderer
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.AgendaWrappedGroupBinding
import pl.szczodrzynski.edziennik.ext.resolveAttr
import pl.szczodrzynski.edziennik.ext.setTintColor
import pl.szczodrzynski.edziennik.utils.Colors

class AgendaEventGroupRenderer : EventRenderer<AgendaEventGroup>() {

    override fun render(view: View, event: AgendaEventGroup) {
        val b = AgendaWrappedGroupBinding.bind(view).item

        b.card.foreground.setTintColor(event.color)
        b.card.background.setTintColor(event.color)
        b.name.text = event.typeName
        b.name.setTextColor(Colors.legibleTextColor(event.color))
        b.count.text = event.count.toString()
        b.count.background.setTintColor(android.R.attr.colorBackground.resolveAttr(view.context))

        b.badge.isVisible = event.showItemBadge
    }

    override fun getEventLayout(): Int = R.layout.agenda_wrapped_group
}

