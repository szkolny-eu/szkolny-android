/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-29.
 */

package pl.szczodrzynski.edziennik.ui.grades.viewholder

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.GradesItemSubjectBinding
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.ui.grades.GradeView
import pl.szczodrzynski.edziennik.ui.grades.GradesAdapter
import pl.szczodrzynski.edziennik.ui.grades.GradesAdapter.Companion.STATE_CLOSED
import pl.szczodrzynski.edziennik.ui.grades.models.GradesSubject
import pl.szczodrzynski.edziennik.utils.Themes

class SubjectViewHolder(
    inflater: LayoutInflater,
    parent: ViewGroup,
    val b: GradesItemSubjectBinding = GradesItemSubjectBinding.inflate(inflater, parent, false)
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<GradesSubject, GradesAdapter> {
    companion object {
        private const val TAG = "SubjectViewHolder"
    }

    override fun onBind(activity: AppCompatActivity, app: App, item: GradesSubject, position: Int, adapter: GradesAdapter) {
        val manager = app.gradesManager
        val contextWrapper = ContextThemeWrapper(activity, Themes.appTheme)

        if (!item.isUnknown) {
            b.subjectName.text = item.subjectName
        }
        else {
            b.subjectName.text = R.string.grades_subject_unknown.resolveString(activity).asItalicSpannable()
        }
        b.dropdownIcon.rotation = when (item.state) {
            STATE_CLOSED -> 0f
            else -> 180f
        }

        b.unread.isVisible = item.hasUnseen

        b.previewContainer.visibility = if (item.state == STATE_CLOSED) View.VISIBLE else View.INVISIBLE
        b.yearSummary.visibility = if (item.state == STATE_CLOSED) View.INVISIBLE else View.VISIBLE

        val gradesContainer = b.previewContainer[0]
        b.previewContainer.removeAllViews()
        b.gradesContainer.removeAllViews()
        b.previewContainer.addView(gradesContainer)

        val firstSemester = item.semesters.firstOrNull() ?: return

        b.yearSummary.text = manager.getYearSummaryString(app, item.semesters.map { it.grades.size }.sum(), item.averages)

        if (firstSemester.number != item.semester) {
            b.gradesContainer.addView(TextView(contextWrapper).apply {
                setTextColor(android.R.attr.textColorSecondary.resolveAttr(context))
                setText(R.string.grades_preview_other_semester, firstSemester.number)
                setPadding(0, 0, 5.dp, 0)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })
        }

        /*if (firstSemester.grades.isEmpty()) {
            b.previewContainer.addView(TextView(app).apply {
                setText(R.string.grades_no_grades_in_semester, firstSemester.number)
            })
        }*/

        val hideImproved = manager.hideImproved
        for (grade in firstSemester.grades) {
            if (hideImproved && grade.isImproved)
                continue
            b.gradesContainer.addView(GradeView(
                contextWrapper,
                grade,
                manager,
                periodGradesTextual = false
            ))
        }

        b.previewContainer.addView(TextView(contextWrapper).apply {
            setTextColor(android.R.attr.textColorSecondary.resolveAttr(context))
            text = manager.getAverageString(app, firstSemester.averages, nameSemester = true, showSemester = firstSemester.number)
            //gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                setMargins(0, 0, 8.dp, 0)
            }
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        })

        // add the topmost semester's grades to preview container (collapsed)
        firstSemester.proposedGrade?.let {
            b.previewContainer.addView(GradeView(
                contextWrapper,
                it,
                manager
            ))
        }
        firstSemester.finalGrade?.let {
            b.previewContainer.addView(GradeView(
                contextWrapper,
                it,
                manager
            ))
        }

        // remove previously added grades from year preview
        if (b.yearContainer.childCount > 1)
            b.yearContainer.removeViews(1, b.yearContainer.childCount - 1)
        // add the yearly grades to summary container (expanded)
        item.proposedGrade?.let {
            b.yearContainer.addView(GradeView(
                    contextWrapper,
                    it,
                    manager
            ))
        }
        item.finalGrade?.let {
            b.yearContainer.addView(GradeView(
                    contextWrapper,
                    it,
                    manager
            ))
        }

        // if showing semester 2, add yearly grades to preview container (collapsed)
        if (firstSemester.number == item.semester) {
            b.previewContainer.addView(TextView(contextWrapper).apply {
                text = manager.getAverageString(app, item.averages, nameSemester = true)
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    setMargins(0, 0, 8.dp, 0)
                }
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })

            item.proposedGrade?.let {
                b.previewContainer.addView(GradeView(
                    contextWrapper,
                    it,
                    manager
                ))
            }
            item.finalGrade?.let {
                b.previewContainer.addView(GradeView(
                    contextWrapper,
                    it,
                    manager
                ))
            }
        }
    }
}
