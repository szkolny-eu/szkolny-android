package pl.szczodrzynski.edziennik.ui.dialogs.teacherabsence

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.full.TeacherAbsenceFull
import pl.szczodrzynski.edziennik.utils.models.Date

class TeacherAbsenceAdapter(
        private val context: Context,
        private val date: Date,
        private val teacherAbsenceList: List<TeacherAbsenceFull>
) : RecyclerView.Adapter<TeacherAbsenceAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(context)
        val view: View = inflater.inflate(R.layout.row_dialog_teacher_absence_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return teacherAbsenceList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val teacherAbsence: TeacherAbsenceFull = teacherAbsenceList[position]

        holder.teacherAbsenceTeacher.text = teacherAbsence.teacherName

        val time = when (teacherAbsence.timeFrom != null && teacherAbsence.timeTo != null) {
            true -> when (teacherAbsence.dateFrom.compareTo(teacherAbsence.dateTo)) {
                0 -> teacherAbsence.dateFrom.formattedStringShort + " " +
                        teacherAbsence.timeFrom.stringHM + " - " + teacherAbsence.timeTo.stringHM

                else -> teacherAbsence.dateFrom.formattedStringShort + " " + teacherAbsence.timeTo.stringHM +
                        " - " + teacherAbsence.dateTo.formattedStringShort + " " + teacherAbsence.timeTo.stringHM
            }

            false -> when (teacherAbsence.dateFrom.compareTo(teacherAbsence.dateTo)) {
                0 -> teacherAbsence.dateFrom.formattedStringShort
                else -> teacherAbsence.dateFrom.formattedStringShort + " - " + teacherAbsence.dateTo.formattedStringShort
            }
        }

        holder.teacherAbsenceTime.text = time

        if (teacherAbsence.name != null) {
            holder.teacherAbsenceName.visibility = View.VISIBLE
            holder.teacherAbsenceName.text = teacherAbsence.name
        } else {
            holder.teacherAbsenceName.visibility = View.GONE
        }

    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var teacherAbsenceTeacher: TextView = itemView.findViewById(R.id.teacherAbsenceTeacher)
        var teacherAbsenceTime: TextView = itemView.findViewById(R.id.teacherAbsenceTime)
        var teacherAbsenceName: TextView = itemView.findViewById(R.id.teacherAbsenceName)
    }
}
