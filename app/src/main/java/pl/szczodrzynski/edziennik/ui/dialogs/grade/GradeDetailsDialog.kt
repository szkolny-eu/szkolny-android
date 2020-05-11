package pl.szczodrzynski.edziennik.ui.dialogs.grade

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.full.GradeFull
import pl.szczodrzynski.edziennik.databinding.DialogGradeDetailsBinding
import pl.szczodrzynski.edziennik.onClick
import pl.szczodrzynski.edziennik.setTintColor
import pl.szczodrzynski.edziennik.ui.dialogs.settings.GradesConfigDialog
import pl.szczodrzynski.edziennik.ui.modules.grades.GradesAdapter
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import kotlin.coroutines.CoroutineContext

class GradeDetailsDialog(
        val activity: AppCompatActivity,
        val grade: GradeFull,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "GradeDetailsDialog"
    }

    private lateinit var app: App
    private lateinit var b: DialogGradeDetailsBinding
    private lateinit var dialog: AlertDialog

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local variables go here

    init { run {
        if (activity.isFinishing)
            return@run
        onShowListener?.invoke(TAG)
        app = activity.applicationContext as App
        b = DialogGradeDetailsBinding.inflate(activity.layoutInflater)
        dialog = MaterialAlertDialogBuilder(activity)
                .setView(b.root)
                .setPositiveButton(R.string.close, null)
                .setOnDismissListener {
                    onDismissListener?.invoke(TAG)
                }
                .show()
        val manager = app.gradesManager

        val gradeColor = manager.getGradeColor(grade)
        b.grade = grade
        b.weightText = manager.getWeightString(app, grade)
        b.commentVisible = false
        b.devMode = App.debugMode
        b.gradeName.setTextColor(if (ColorUtils.calculateLuminance(gradeColor) > 0.3) 0xaa000000.toInt() else 0xccffffff.toInt())
        b.gradeName.background.setTintColor(gradeColor)

        b.gradeValue = if (grade.weight == 0f || grade.value < 0f) -1f else manager.getGradeValue(grade)

        b.customValueDivider.isVisible = manager.plusValue != null || manager.minusValue != null
        b.customValueLayout.isVisible = b.customValueDivider.isVisible
        b.customValueButton.onClick {
            GradesConfigDialog(activity, reloadOnDismiss = true)
        }

        launch {
            val historyList = withContext(Dispatchers.Default) {
                app.db.gradeDao().getByParentIdNow(App.profileId, grade.id)
            }
            if (historyList.isEmpty()) {
                b.historyVisible = false
                return@launch
            }
            b.historyVisible = true
            //b.gradeHistoryNest.isNestedScrollingEnabled = false
            b.gradeHistoryList.adapter = GradesAdapter(activity, {
                GradeDetailsDialog(activity, it)
            }).also { it.items = historyList.toMutableList() }
            b.gradeHistoryList.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(SimpleDividerItemDecoration(context))
            }
        }
    }}
}
