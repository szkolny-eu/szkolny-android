package pl.szczodrzynski.edziennik.ui.grades.editor

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.databinding.FragmentGradesEditorBinding
import pl.szczodrzynski.edziennik.utils.Colors
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.YEAR_1_AVG_2_AVG
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.YEAR_1_AVG_2_SEM
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.YEAR_1_SEM_2_AVG
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.YEAR_ALL_GRADES
import java.text.DecimalFormat
import java.util.*
import kotlin.math.floor

class GradesEditorFragment : Fragment() {

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: FragmentGradesEditorBinding
/*
    private val navController: NavController by lazy { Navigation.findNavController(b.root) }
*/

    private val config by lazy { app.config.getFor(App.profileId).grades }

    private var subjectId: Long = -1
    private var semester: Int = 1

    private val editorGrades = ArrayList<EditorGrade>()
    private lateinit var addingGrade: EditorGrade

    private var gradeSumSemester = 0f
    private var gradeCountSemester = 0f
    private var averageSemester = 0f

    private var gradeSumOtherSemester = 0f
    private var gradeCountOtherSemester = 0f
    private var averageOtherSemester = 0f

    private var finalOtherSemester = 0.0f

    private var averageMode = YEAR_ALL_GRADES // this means the container should be gone
    private var yearAverageBefore = 0.0f

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        if (context == null)
            return null
        app = activity.application as App
        // activity, context and profile is valid
        b = FragmentGradesEditorBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded)
            return

        subjectId = arguments.getLong("subjectId", -1)
        semester = arguments.getInt("semester", 1)

        if (subjectId == -1L) {
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.error_occured)
                .setMessage(R.string.error_no_subject_id)
                .setPositiveButton(R.string.ok) { _, _ -> activity.navigateUp() }
                .show()
            return
        }

        averageMode = arguments.getInt("averageMode", YEAR_ALL_GRADES)
        yearAverageBefore = arguments.getFloat("yearAverageBefore", -1f)

        gradeSumOtherSemester = arguments.getFloat("gradeSumOtherSemester", -1f)
        gradeCountOtherSemester = arguments.getFloat("gradeCountOtherSemester", -1f)
        averageOtherSemester = arguments.getFloat("averageOtherSemester", -1f)
        finalOtherSemester = arguments.getFloat("finalOtherSemester", -1f)

        b.listView.setHasFixedSize(false)
        b.listView.isNestedScrollingEnabled = false
        b.listView.layoutManager = LinearLayoutManager(context)
        b.listView.adapter = GradesEditorAdapter(context!!, editorGrades, object : GradesEditorAdapter.OnGradeActionListener {
            override fun onClickRemove(gradeId: Long) {
                gradeSumSemester = 0f
                gradeCountSemester = 0f
                averageSemester = 0f
                var pos = 0
                var removePos = 0
                for (editorGrade in editorGrades) {
                    if (editorGrade.id == gradeId) {
                        removePos = pos
                        pos++
                        continue
                    }
                    var weight = editorGrade.weight
                    if (config.dontCountEnabled && config.dontCountGrades.contains(editorGrade.name.toLowerCase().trim())) {
                        weight = 0f
                    }
                    val value = editorGrade.value * weight
                    gradeSumSemester += value
                    gradeCountSemester += weight//(value > 0 ? weight : 0);
                    pos++
                }
                editorGrades.removeAt(removePos)
                averageSemester = gradeSumSemester / gradeCountSemester
                refreshYearAverageAfter()
                b.averageAfter.text = String.format(Locale.getDefault(), "%.02f", averageSemester)
                var gradeInt = floor(averageSemester.toDouble()).toInt()
                if (averageSemester % 1 >= 0.75)
                    gradeInt++
                b.averageAfter.background.colorFilter = PorterDuffColorFilter(Colors.gradeNameToColor(gradeInt.toString()), PorterDuff.Mode.MULTIPLY)
                b.listView.adapter!!.notifyItemRemoved(removePos)
            }

            override fun onClickEdit(gradeId: Long) {
                refreshDataset()
            }

            override fun onClickAdd() {

            }
        })
        b.listView.visibility = View.VISIBLE

        b.restoreGrades.setOnClickListener { refreshViews() }

        b.addGrade.setOnClickListener { v ->
            addingGrade = EditorGrade(System.currentTimeMillis(), "0", 0.0f, getString(R.string.grades_editor_new_grade), 0f)
            addGradeHandler(v, addingGrade) {
                editorGrades.add(0, addingGrade)
                refreshDataset()
            }
        }

        refreshViews()
    }

    private fun refreshYearAverageAfter() {
        val yearAverage = when (averageMode) {
            YEAR_ALL_GRADES -> (gradeSumOtherSemester + gradeSumSemester) / (gradeCountOtherSemester + gradeCountSemester)
            YEAR_1_AVG_2_AVG -> (averageOtherSemester + averageSemester) / 2
            YEAR_1_SEM_2_AVG, // the final grade is always the 'other'
            YEAR_1_AVG_2_SEM -> (finalOtherSemester + averageSemester) / 2
            else -> (gradeSumOtherSemester + gradeSumSemester) / (gradeCountOtherSemester + gradeCountSemester)
        }

        var gradeInt = floor(yearAverage.toDouble()).toInt()
        if (yearAverage % 1 >= 0.75)
            gradeInt++

        b.yearAverageAfter.text = String.format(Locale.getDefault(), "%.02f", yearAverage)
        b.yearAverageAfter.background.colorFilter = PorterDuffColorFilter(Colors.gradeNameToColor(gradeInt.toString()), PorterDuff.Mode.MULTIPLY)
    }

    private fun refreshDataset() {
        gradeSumSemester = 0f
        gradeCountSemester = 0f
        averageSemester = 0f
        for (editorGrade in editorGrades) {
            var weight = editorGrade.weight
            if (config.dontCountEnabled && config.dontCountGrades.contains(editorGrade.name.toLowerCase().trim())) {
                weight = 0f
            }
            val value = editorGrade.value * weight
            gradeSumSemester += value
            gradeCountSemester += weight
        }
        averageSemester = gradeSumSemester / gradeCountSemester
        refreshYearAverageAfter()
        b.averageAfter.text = String.format(Locale.getDefault(), "%.02f", averageSemester)
        var gradeInt = floor(averageSemester.toDouble()).toInt()
        if (averageSemester % 1 >= 0.75)
            gradeInt++
        b.averageAfter.background.colorFilter = PorterDuffColorFilter(Colors.gradeNameToColor(gradeInt.toString()), PorterDuff.Mode.MULTIPLY)
        b.listView.adapter!!.notifyDataSetChanged()
    }

    private fun refreshViews() {
        editorGrades.clear()

        app.db.subjectDao().getById(App.profileId, subjectId).observe(this, Observer { subject ->
            if (subject == null || subject.id == -1L) {
                MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.error_occured)
                    .setMessage(R.string.error_no_subject_id)
                    .setPositiveButton(R.string.ok) { _, _ -> activity.navigateUp() }
                    .show()
                return@Observer
            }

            gradeSumSemester = 0f
            gradeCountSemester = 0f
            averageSemester = 0f

            app.db.gradeDao().getAllBySubject(App.profileId, subject.id).observe(this, Observer { grades ->
                for (grade in grades) {
                    if (grade.type == Grade.TYPE_NORMAL) {
                        if (grade.weight < 0) {
                            // do not show *normal* grades with negative weight - these are historical grades - Iuczniowie
                            continue
                        }
                        var weight = grade.weight
                        if (config.dontCountEnabled && config.dontCountGrades.contains(grade.name.toLowerCase().trim())) {
                            weight = 0f
                        }
                        val value = grade.value * weight
                        if (grade.semester == semester) {
                            gradeSumSemester += value
                            gradeCountSemester += weight//(value > 0 ? weight : 0);
                            editorGrades.add(EditorGrade(grade.id, grade.name, grade.value, grade.description + " - " + grade.category, grade.weight))
                        }
                    }
                }

                averageSemester = gradeSumSemester / gradeCountSemester

                if (averageMode == -1) {
                    b.yearAverageContainer.visibility = View.GONE
                } else {
                    b.yearAverageContainer.visibility = View.VISIBLE
                    var gradeInt = floor(yearAverageBefore.toDouble()).toInt()
                    if (yearAverageBefore % 1 >= 0.75)
                        gradeInt++
                    b.yearAverageBefore.text = String.format(Locale.getDefault(), "%.02f", yearAverageBefore)
                    b.yearAverageBefore.background.colorFilter = PorterDuffColorFilter(Colors.gradeNameToColor(gradeInt.toString()), PorterDuff.Mode.MULTIPLY)

                    refreshYearAverageAfter()
                }

                b.subjectName.text = subject.longName
                b.semesterName.text = getString(R.string.grades_semester_header_format, semester)
                var gradeInt = floor(averageSemester.toDouble()).toInt()
                if (averageSemester % 1 >= 0.75)
                    gradeInt++
                b.averageBefore.text = String.format(Locale.getDefault(), "%.02f", averageSemester)
                b.averageBefore.background.colorFilter = PorterDuffColorFilter(Colors.gradeNameToColor(gradeInt.toString()), PorterDuff.Mode.MULTIPLY)
                b.averageAfter.text = String.format(Locale.getDefault(), "%.02f", averageSemester)
                b.averageAfter.background.colorFilter = PorterDuffColorFilter(Colors.gradeNameToColor(gradeInt.toString()), PorterDuff.Mode.MULTIPLY)
                b.listView.adapter!!.notifyDataSetChanged()
            })
        })
    }

    data class EditorGrade(
            var id: Long,
            var name: String,
            var value: Float,
            var category: String,
            var weight: Float
    )

    companion object {
        fun modifyGradeChooser(v: View, editorGrade: EditorGrade, callback: () -> Unit) {
            val popup = PopupMenu(v.context, v)
            popup.menu.add(0, 0, 0, R.string.grades_editor_change_grade)
            popup.menu.add(0, 1, 1, R.string.grades_editor_change_weight)

            popup.setOnMenuItemClickListener { item ->
                if (item.itemId == 0) {
                    modifyGradeName(v, editorGrade, callback)
                }
                if (item.itemId == 1) {
                    modifyGradeWeight(v, editorGrade, callback)
                }
                true
            }

            popup.show()
        }

        fun addGradeHandler(v: View, editorGrade: EditorGrade, callback: () -> Unit) {
            modifyGradeName(v, editorGrade) {
                modifyGradeWeight(v, editorGrade, callback)
            }
        }

        fun modifyGradeName(v: View, editorGrade: EditorGrade, callback: () -> Unit) {
            val popup = PopupMenu(v.context, v)
            popup.menu.add(0, 75, 0, "1-")
            popup.menu.add(0, 100, 1, "1")
            popup.menu.add(0, 150, 2, "1+")
            popup.menu.add(0, 175, 3, "2-")
            popup.menu.add(0, 200, 4, "2")
            popup.menu.add(0, 250, 5, "2+")
            popup.menu.add(0, 275, 6, "3-")
            popup.menu.add(0, 300, 7, "3")
            popup.menu.add(0, 350, 8, "3+")
            popup.menu.add(0, 375, 9, "4-")
            popup.menu.add(0, 400, 10, "4")
            popup.menu.add(0, 450, 11, "4+")
            popup.menu.add(0, 475, 12, "5-")
            popup.menu.add(0, 500, 13, "5")
            popup.menu.add(0, 550, 14, "5+")
            popup.menu.add(0, 575, 15, "6-")
            popup.menu.add(0, 600, 16, "6")
            popup.menu.add(0, 650, 17, "6+")
            popup.menu.add(0, 0, 18, "0")

            popup.setOnMenuItemClickListener { item ->
                editorGrade.name = item.title.toString()
                editorGrade.value = item.itemId.toFloat() / 100
                callback()
                true
            }

            popup.show()
        }

        fun modifyGradeWeight(v: View, editorGrade: EditorGrade, callback: () -> Unit) {
            val popup = PopupMenu(v.context, v)
            for (i in 0..6) {
                popup.menu.add(0, i, i, v.context.getString(R.string.grades_editor_weight_format, DecimalFormat("#.##").format(i.toLong())))
            }
            popup.menu.add(1, 100, 100, v.context.getString(R.string.grades_editor_weight_other))

            popup.setOnMenuItemClickListener { item ->
                if (item.itemId == 100) {
                    MaterialAlertDialogBuilder(v.context)
                        .setTitle(R.string.grades_editor_add_grade_title)
                        .input(
                            message = v.context.getString(R.string.grades_editor_add_grade_weight),
                            type = InputType.TYPE_NUMBER_FLAG_SIGNED,
                            positiveButton = R.string.ok,
                            positiveListener = { _, input ->
                                try {
                                    editorGrade.weight = input.toFloat()
                                    callback()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                true
                            }
                        )
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                } else {
                    editorGrade.weight = item.itemId.toFloat()
                    callback()
                }
                true
            }

            popup.show()
        }
    }
}
