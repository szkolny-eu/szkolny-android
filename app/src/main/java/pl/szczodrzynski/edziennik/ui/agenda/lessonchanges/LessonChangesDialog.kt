package pl.szczodrzynski.edziennik.ui.agenda.lessonchanges

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.DialogLessonChangeListBinding
import pl.szczodrzynski.edziennik.ui.timetable.LessonDetailsDialog
import pl.szczodrzynski.edziennik.utils.models.Date
import kotlin.coroutines.CoroutineContext

class LessonChangesDialog(
        val activity: AppCompatActivity,
        val profileId: Int,
        private val defaultDate: Date,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        const val TAG = "LessonChangeDialog"
    }

    private val app by lazy { activity.application as App }

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private lateinit var b: DialogLessonChangeListBinding
    private lateinit var dialog: AlertDialog

    init { run {
        if (activity.isFinishing)
            return@run
        job = Job()
        onShowListener?.invoke(TAG)
        b = DialogLessonChangeListBinding.inflate(activity.layoutInflater)
        dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(defaultDate.formattedString)
                .setView(b.root)
                .setPositiveButton(R.string.close) { dialog, _ -> dialog.dismiss() }
                .setOnDismissListener {
                    onDismissListener?.invoke(TAG)
                }
                .create()
        loadLessonChanges()
    }}

    private fun loadLessonChanges() { launch {
        val lessonChanges = withContext(Dispatchers.Default) {
            app.db.timetableDao().getChangesForDateNow(profileId, defaultDate)
        }

        val adapter = LessonChangesAdapter(
                activity,
                onItemClick = {
                    LessonDetailsDialog(
                            activity,
                            it,
                            onShowListener = onShowListener,
                            onDismissListener = onDismissListener
                    )
                }
        ).apply {
            items = lessonChanges
        }

        b.lessonChangeView.adapter = adapter
        b.lessonChangeView.layoutManager = LinearLayoutManager(activity)

        dialog.show()
    }}
}
