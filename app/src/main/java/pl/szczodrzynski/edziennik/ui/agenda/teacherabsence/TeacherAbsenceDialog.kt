package pl.szczodrzynski.edziennik.ui.agenda.teacherabsence

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.DialogTeacherAbsenceListBinding
import pl.szczodrzynski.edziennik.ui.base.dialog.BindingDialog
import pl.szczodrzynski.edziennik.utils.models.Date

class TeacherAbsenceDialog(
    activity: AppCompatActivity,
    private val profileId: Int,
    private val date: Date,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BindingDialog<DialogTeacherAbsenceListBinding>(activity, onShowListener, onDismissListener) {

    override val TAG = "TeacherAbsenceDialog"

    override fun getTitle(): String = date.formattedString
    override fun inflate(layoutInflater: LayoutInflater) =
        DialogTeacherAbsenceListBinding.inflate(layoutInflater)

    override fun getPositiveButtonText() = R.string.close

    override suspend fun onShow() {
        b.teacherAbsenceView.setHasFixedSize(true)
        b.teacherAbsenceView.layoutManager = LinearLayoutManager(activity)

        app.db.teacherAbsenceDao().getAllByDate(profileId, date).observe(
            activity as LifecycleOwner
        ) { absenceList ->
            val adapter = TeacherAbsenceAdapter(activity, date, absenceList)
            b.teacherAbsenceView.adapter = adapter
            b.teacherAbsenceView.visibility = View.VISIBLE
        }
    }
}
