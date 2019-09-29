package pl.szczodrzynski.edziennik.ui.modules.agenda.teacherabsence

import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.github.tibolte.agendacalendarview.render.EventRenderer
import pl.szczodrzynski.edziennik.R

class TeacherAbsenceEventRenderer : EventRenderer<TeacherAbsenceEvent>() {
    override fun render(view: View?, event: TeacherAbsenceEvent) {
        val card = view?.findViewById<CardView>(R.id.teacherAbsenceCard)
        val changeText = view?.findViewById<TextView>(R.id.teacherAbsenceText)
        val changeCount = view?.findViewById<TextView>(R.id.teacherAbsenceCount)
        card?.setCardBackgroundColor(event.color)
        changeText?.setTextColor(event.textColor)
        changeCount?.setTextColor(event.textColor)
        changeCount?.text = event.teacherAbsenceCount.toString()
    }

    override fun getEventLayout(): Int { return R.layout.agenda_event_teacher_absence }
}
