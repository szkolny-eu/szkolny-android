/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-1.
 */

package pl.szczodrzynski.edziennik.ui.modules.grades

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue.COMPLEX_UNIT_SP
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.ColorUtils
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_SEMESTER1_FINAL
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_SEMESTER1_PROPOSED
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_SEMESTER2_FINAL
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_SEMESTER2_PROPOSED
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_YEAR_FINAL
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_YEAR_PROPOSED
import pl.szczodrzynski.edziennik.dp
import pl.szczodrzynski.edziennik.resolveAttr
import pl.szczodrzynski.edziennik.setTintColor
import pl.szczodrzynski.edziennik.utils.managers.GradesManager

class GradeView : AppCompatTextView {

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr)

    constructor(context: Context, grade: Grade, manager: GradesManager, periodGradesTextual: Boolean = false) : this(context, null) {
        setGrade(grade, manager, false, periodGradesTextual)
    }

    @SuppressLint("RestrictedApi")
    fun setGrade(grade: Grade?, manager: GradesManager, bigView: Boolean = false, periodGradesTextual: Boolean = false) {
        if (grade == null) {
            visibility = View.GONE
            return
        }
        visibility = View.VISIBLE

        val gradeName = grade.name

        val gradeColor = manager.getGradeColor(grade)

        text = when {
            periodGradesTextual -> when (grade.type) {
                TYPE_SEMESTER1_PROPOSED, TYPE_SEMESTER2_PROPOSED -> context.getString(
                        R.string.grade_semester_proposed_format,
                        gradeName
                )
                TYPE_SEMESTER1_FINAL, TYPE_SEMESTER2_FINAL -> context.getString(
                        R.string.grade_semester_final_format,
                        gradeName
                )
                TYPE_YEAR_PROPOSED -> context.getString(
                        R.string.grade_year_proposed_format,
                        gradeName
                )
                TYPE_YEAR_FINAL -> context.getString(
                        R.string.grade_year_final_format,
                        gradeName
                )
                else -> gradeName
            }
            gradeName.isBlank() -> "  "
            else -> gradeName
        }

        setTextColor(when (grade.type) {
            TYPE_SEMESTER1_PROPOSED,
            TYPE_SEMESTER2_PROPOSED,
            TYPE_YEAR_PROPOSED -> android.R.attr.textColorPrimary.resolveAttr(context)
            else -> if (ColorUtils.calculateLuminance(gradeColor) > 0.3)
                0xaa000000.toInt()
            else
                0xccffffff.toInt()
        })

        //typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        setBackgroundResource(when (grade.type) {
            TYPE_SEMESTER1_PROPOSED,
            TYPE_SEMESTER2_PROPOSED,
            TYPE_YEAR_PROPOSED -> if (bigView) R.drawable.bg_rounded_8dp_outline else R.drawable.bg_rounded_4dp_outline
            else -> if (bigView) R.drawable.bg_rounded_8dp else R.drawable.bg_rounded_4dp
        })
        background.setTintColor(gradeColor)
        gravity = Gravity.CENTER

        if (bigView) {
            setTextSize(COMPLEX_UNIT_SP, 24f)
            setAutoSizeTextTypeUniformWithConfiguration(
                14,
                32,
                1,
                COMPLEX_UNIT_SP
            )
            setPadding(2.dp, 2.dp, 2.dp, 2.dp)
        }
        else {
            setTextSize(COMPLEX_UNIT_SP, 14f)
            setPadding(5.dp, 0, 5.dp, 0)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                setMargins(0, 0, 5.dp, 0)
            }
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            measure(WRAP_CONTENT, WRAP_CONTENT)
        }
    }

}
