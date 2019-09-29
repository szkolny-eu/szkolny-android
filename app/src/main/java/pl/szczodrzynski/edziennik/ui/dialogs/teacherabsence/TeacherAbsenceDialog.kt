package pl.szczodrzynski.edziennik.ui.dialogs.teacherabsence

import android.content.Context
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.DialogTeacherAbsenceListBinding
import pl.szczodrzynski.edziennik.utils.models.Date

class TeacherAbsenceDialog(val context: Context) {

    val profileId: Int = App.profileId
    private lateinit var b: DialogTeacherAbsenceListBinding

    fun show(app: App, date: Date) {
        val dialog = MaterialDialog.Builder(context)
                .title(date.formattedString)
                .customView(R.layout.dialog_teacher_absence_list, false)
                .positiveText(R.string.close)
                .autoDismiss(false)
                .onPositive { dialog, _ -> dialog.dismiss()}
                .show()

        val customView: View = dialog.customView ?: return
        b = DataBindingUtil.bind(customView) ?: return

        b.teacherAbsenceView.setHasFixedSize(true)
        b.teacherAbsenceView.layoutManager = LinearLayoutManager(context)

        app.db.teacherAbsenceDao().getAllByDateFull(profileId, date).observe(context as LifecycleOwner, Observer { absenceList ->
            val adapter = TeacherAbsenceAdapter(context, date, absenceList)
            b.teacherAbsenceView.adapter = adapter
            b.teacherAbsenceView.visibility = View.VISIBLE
        })
    }

}
