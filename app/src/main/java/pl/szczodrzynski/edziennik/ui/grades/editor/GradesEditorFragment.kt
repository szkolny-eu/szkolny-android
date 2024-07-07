package pl.szczodrzynski.edziennik.ui.grades.editor

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.core.manager.GradesManager.Companion.YEAR_1_AVG_2_AVG
import pl.szczodrzynski.edziennik.core.manager.GradesManager.Companion.YEAR_1_AVG_2_SEM
import pl.szczodrzynski.edziennik.core.manager.GradesManager.Companion.YEAR_1_SEM_2_AVG
import pl.szczodrzynski.edziennik.core.manager.GradesManager.Companion.YEAR_ALL_GRADES
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.databinding.FragmentGradesEditorBinding
import pl.szczodrzynski.edziennik.ext.getFloat
import pl.szczodrzynski.edziennik.ext.getInt
import pl.szczodrzynski.edziennik.ext.getLong
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.utils.Colors
import timber.log.Timber
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.floor

class GradesEditorFragment : BaseFragment<FragmentGradesEditorBinding, MainActivity>(
    inflater = FragmentGradesEditorBinding::inflate,
) {

    private val config by lazy { app.profile.config.grades }

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

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        subjectId = arguments.getLong("subjectId", -1)
        semester = arguments.getInt("semester", 1)

        if (subjectId == -1L) {
            SimpleDialog<Unit>(activity) {
                title(R.string.error_occured)
                message(R.string.error_no_subject_id)
                positive(R.string.ok) {
                    activity.navigateUp()
                }
            }.show()
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
        b.listView.adapter = GradesEditorAdapter(requireContext(), editorGrades, this, object : GradesEditorAdapter.OnGradeActionListener {
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
                    if (config.dontCountEnabled && config.dontCountGrades.contains(editorGrade.name.lowercase().trim())) {
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
            if (config.dontCountEnabled && config.dontCountGrades.contains(editorGrade.name.lowercase().trim())) {
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
                SimpleDialog<Unit>(activity) {
                    title(R.string.error_occured)
                    message(R.string.error_no_subject_id)
                    positive(R.string.ok) {
                        activity.navigateUp()
                    }
                }.show()
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
                        if (config.dontCountEnabled && config.dontCountGrades.contains(grade.name.lowercase().trim())) {
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
                SimpleDialog<Unit>(activity) {
                    title(R.string.grades_editor_add_grade_title)
                    message(R.string.grades_editor_add_grade_weight)
                    input(InputType.TYPE_NUMBER_FLAG_SIGNED)
                    positive(R.string.ok) {
                        try {
                            editorGrade.weight = getInput()?.text?.toString()?.toFloat() ?: 0.0f
                            callback()
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                    negative(R.string.cancel)
                }.show()
            } else {
                editorGrade.weight = item.itemId.toFloat()
                callback()
            }
            true
        }

        popup.show()
    }
}
