package pl.szczodrzynski.edziennik.ui.grades

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.full.GradeFull
import pl.szczodrzynski.edziennik.databinding.DialogGradeDetailsBinding
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.setTintColor
import pl.szczodrzynski.edziennik.ui.base.dialog.BindingDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.GradesConfigDialog
import pl.szczodrzynski.edziennik.ui.notes.setupNotesButton
import pl.szczodrzynski.edziennik.utils.BetterLink
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import pl.szczodrzynski.edziennik.core.manager.NoteManager

class GradeDetailsDialog(
    activity: AppCompatActivity,
    private val grade: GradeFull,
    private val showNotes: Boolean = true,
) : BindingDialog<DialogGradeDetailsBinding>(activity) {

    override fun getTitleRes(): Int? = null
    override fun inflate(layoutInflater: LayoutInflater) =
        DialogGradeDetailsBinding.inflate(layoutInflater)

    override fun getPositiveButtonText() = R.string.close

    override suspend fun onShow() {
        val manager = app.gradesManager

        val gradeColor = manager.getGradeColor(b.root.context, grade)
        b.grade = grade
        b.weightText = manager.getWeightString(app, grade)
        b.commentVisible = false
        b.devMode = App.devMode
        b.gradeName.setTextColor(
            if (ColorUtils.calculateLuminance(gradeColor) > 0.3)
                0xaa000000.toInt()
            else
                0xccffffff.toInt()
        )
        b.gradeName.background.setTintColor(gradeColor)

        b.gradeValue = if (grade.weight == 0f || grade.value < 0f)
            -1f
        else
            manager.getGradeValue(grade)

        b.customValueDivider.isVisible = manager.plusValue != null || manager.minusValue != null
        b.customValueLayout.isVisible = b.customValueDivider.isVisible
        b.customValueButton.onClick {
            GradesConfigDialog(activity, reloadOnDismiss = true).show()
        }

        grade.teacherName?.let { name ->
            BetterLink.attach(
                b.teacherName,
                teachers = mapOf(grade.teacherId to name),
                onActionSelected = ::dismiss
            )
        }

        val historyList = withContext(Dispatchers.Default) {
            app.db.gradeDao().getByParentIdNow(App.profileId, grade.id)
        }

        historyList.forEach {
            it.filterNotes()
        }

        b.historyVisible = historyList.isNotEmpty()
        if (historyList.isNotEmpty()) {
            b.gradeHistoryList.adapter = GradesAdapter(activity, onGradeClick = {
                GradeDetailsDialog(activity, it).show()
            }).also {
                it.items = historyList.toMutableList()
            }

            b.gradeHistoryList.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(SimpleDividerItemDecoration(context))
            }
        }

        b.notesButton.isVisible = showNotes
        b.notesButton.setupNotesButton(
            activity = activity,
            owner = grade,
        )
        b.legend.isVisible = showNotes
        if (showNotes)
            NoteManager.setLegendText(grade, b.legend)
    }
}
