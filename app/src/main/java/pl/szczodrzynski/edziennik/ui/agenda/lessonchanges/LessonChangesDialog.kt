package pl.szczodrzynski.edziennik.ui.agenda.lessonchanges

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.DialogLessonChangeListBinding
import pl.szczodrzynski.edziennik.ui.base.dialog.BindingDialog
import pl.szczodrzynski.edziennik.ui.timetable.LessonDetailsDialog
import pl.szczodrzynski.edziennik.utils.models.Date

class LessonChangesDialog(
    activity: AppCompatActivity,
    private val profileId: Int,
    private val defaultDate: Date,
) : BindingDialog<DialogLessonChangeListBinding>(activity) {

    override fun getTitle(): String = defaultDate.formattedString
    override fun inflate(layoutInflater: LayoutInflater) =
        DialogLessonChangeListBinding.inflate(layoutInflater)

    override fun getPositiveButtonText() = R.string.close

    override suspend fun onShow() {
        val lessonChanges = withContext(Dispatchers.Default) {
            app.db.timetableDao().getChangesForDateNow(profileId, defaultDate)
        }

        val adapter = LessonChangesAdapter(
            activity,
            onLessonClick = {
                LessonDetailsDialog(
                    activity,
                    it,
                ).show()
            }
        ).apply {
            items = lessonChanges
        }

        b.lessonChangeView.adapter = adapter
        b.lessonChangeView.layoutManager = LinearLayoutManager(activity)
    }
}
