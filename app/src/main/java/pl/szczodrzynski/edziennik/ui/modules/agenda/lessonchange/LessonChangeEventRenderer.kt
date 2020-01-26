package pl.szczodrzynski.edziennik.ui.modules.agenda.lessonchange

import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.github.tibolte.agendacalendarview.render.EventRenderer
import pl.szczodrzynski.edziennik.R

class LessonChangeEventRenderer : EventRenderer<LessonChangeEvent>() {
    override fun render(view: View?, event: LessonChangeEvent) {
        val card = view?.findViewById<CardView>(R.id.lesson_change_card)
        val changeText = view?.findViewById<TextView>(R.id.lesson_change_text)
        val changeCount = view?.findViewById<TextView>(R.id.lessonChangeCount)
        card?.setCardBackgroundColor(event.color)
        changeText?.setTextColor(event.textColor)
        changeCount?.setTextColor(event.textColor)
        changeCount?.text = event.lessonChangeCount.toString()
    }

    override fun getEventLayout(): Int = R.layout.agenda_event_lesson_change
}
