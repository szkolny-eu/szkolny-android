/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-8.
 */

package pl.szczodrzynski.edziennik.ui.modules.agenda.lessonchanges

import android.view.View
import com.github.tibolte.agendacalendarview.render.EventRenderer
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.AgendaWrappedLessonChangesBinding

class LessonChangesEventRenderer : EventRenderer<LessonChangesEvent>() {

    override fun render(view: View, event: LessonChangesEvent) {
        val b = AgendaWrappedLessonChangesBinding.bind(view).item

        b.lessonChangeCount.text = event.changeCount.toString()
    }

    override fun getEventLayout(): Int = R.layout.agenda_wrapped_lesson_changes
}
