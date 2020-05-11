package pl.szczodrzynski.edziennik.ui.dialogs.teacherabsence

import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.DialogTeacherAbsenceListBinding
import pl.szczodrzynski.edziennik.utils.models.Date

class TeacherAbsenceDialog(
        val activity: AppCompatActivity,
        val profileId: Int,
        val date: Date,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) {
    companion object {
        private const val TAG = "TeacherAbsenceDialog"
    }

    private val app by lazy { activity.application as App }

    private lateinit var b: DialogTeacherAbsenceListBinding
    private lateinit var dialog: AlertDialog

    init { run {
        if (activity.isFinishing)
            return@run

        b = DialogTeacherAbsenceListBinding.inflate(activity.layoutInflater)

        dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(date.formattedString)
                .setView(b.root)
                .setPositiveButton(R.string.close) { dialog, _ -> dialog.dismiss() }
                .setOnDismissListener {
                    onDismissListener?.invoke(TAG)
                }
                .create()

        b.teacherAbsenceView.setHasFixedSize(true)
        b.teacherAbsenceView.layoutManager = LinearLayoutManager(activity)

        app.db.teacherAbsenceDao().getAllByDate(profileId, date).observe(activity as LifecycleOwner, Observer { absenceList ->
            val adapter = TeacherAbsenceAdapter(activity, date, absenceList)
            b.teacherAbsenceView.adapter = adapter
            b.teacherAbsenceView.visibility = View.VISIBLE
        })

        dialog.show()
    }}
}
