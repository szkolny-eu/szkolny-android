/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-1.
 */

package pl.szczodrzynski.edziennik.utils.managers

import android.content.Context
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_NORMAL
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_POINT_AVG
import pl.szczodrzynski.edziennik.ui.modules.grades.models.GradesAverages
import pl.szczodrzynski.edziennik.ui.modules.grades.models.GradesSemester
import pl.szczodrzynski.edziennik.utils.Colors
import java.text.DecimalFormat
import kotlin.math.floor

class GradesManager(val app: App) {
    companion object {
        const val ORDER_BY_DATE_DESC = 0
        const val ORDER_BY_SUBJECT_ASC = 1
        const val ORDER_BY_DATE_ASC = 2
        const val ORDER_BY_SUBJECT_DESC = 3
        const val YEAR_1_AVG_2_AVG = 0
        const val YEAR_1_SEM_2_AVG = 1
        const val YEAR_1_AVG_2_SEM = 2
        const val YEAR_1_SEM_2_SEM = 3
        const val YEAR_ALL_GRADES = 4
        const val COLOR_MODE_DEFAULT = 0
        const val COLOR_MODE_WEIGHTED = 1
    }

    private val gradeRegex by lazy { """([0-6])([+-])?""".toRegex() }
    private val format = DecimalFormat("#.##")

    val orderBy
        get() = app.config.grades.orderBy
    val yearAverageMode
        get() = app.config.forProfile().grades.yearAverageMode
    val colorMode
        get() = app.config.forProfile().grades.colorMode
    val plusValue
        get() = app.config.forProfile().grades.plusValue
    val minusValue
        get() = app.config.forProfile().grades.minusValue
    val dontCountGrades
        get() = app.config.forProfile().grades.dontCountGrades
    val hideImproved
        get() = app.config.forProfile().grades.hideImproved
    val averageWithoutWeight
        get() = app.config.forProfile().grades.averageWithoutWeight


    fun getOrderByString() = when (orderBy) {
        ORDER_BY_SUBJECT_ASC -> "subjectLongName ASC, gradeSemester DESC, addedDate DESC"
        ORDER_BY_DATE_ASC -> "subjectId ASC, gradeSemester DESC, addedDate ASC"
        ORDER_BY_SUBJECT_DESC -> "subjectLongName DESC, gradeSemester DESC, addedDate DESC"
        else -> "subjectId ASC, gradeSemester DESC, addedDate DESC"
    }

    fun getWeightString(context: Context, grade: Grade, showClassAverage: Boolean = false) = when (grade.type) {
        TYPE_NORMAL -> if (grade.weight == 0f)
            context.getString(R.string.grades_weight_not_counted)
        else
            if (showClassAverage && (grade.classAverage ?: 0f) != 0f)
                context.getString(R.string.grades_weight_format, format.format(grade.weight)+", "+format.format(grade.classAverage))
            else
                context.getString(R.string.grades_weight_format, format.format(grade.weight))
        TYPE_POINT_AVG -> context.getString(R.string.grades_max_points_format, format.format(grade.valueMax))
        else -> null
    }

    private fun gradeNameToColorStr(grade: String): String? {
        when (grade.toLowerCase()) {
            "+", "++", "+++" ->
                return "4caf50"
            "-", "-,", "-,-,", "np", "np.", "npnp", "np,", "np,np,", "bs", "nk" ->
                return "ff7043"
            "1-", "1", "f" ->
                return "ff0000"
            "1+", "ef" ->
                return "ff3d00"
            "2-", "2", "e" ->
                return "ff9100"
            "2+", "de" ->
                return "ffab00"
            "3-", "3", "d" ->
                return "ffff00"
            "3+", "cd" ->
                return "c6ff00"
            "4-", "4", "c" ->
                return "76ff03"
            "4+", "bc" ->
                return "64dd17"
            "5-", "5", "b" ->
                return "00c853"
            "5+", "ab" ->
                return "00bfa5"
            "6-", "6", "a" ->
                return "2196f3"
            "6+", "a+" ->
                return "0091ea"
        }
        return "bdbdbd"
    }

    fun getRoundedGrade(value: Float): Int {
        return floor(value.toDouble()).toInt() + if (value % 1.0f >= 0.75) 1 else 0
    }

    fun getGradeValue(grade: Grade): Float {
        gradeRegex.find(grade.name)?.let {
            var value = it[1].toFloatOrNull() ?: return grade.value
            if (it[2] == "+")
                value += plusValue ?: return grade.value
            if (it[2] == "-")
                value -= minusValue ?: return grade.value
            return value
        }
        return grade.value
    }

    fun getGradeWeight(dontCountGrades: List<String>, grade: Grade): Float {
        if (grade.name.toLowerCase() in dontCountGrades)
            return 0f
        return grade.weight
    }

    fun getColor(grade: Grade): Int {
        return Colors.gradeToColor(grade)
    }

    fun calculateAverages(averages: GradesAverages, semesters: List<GradesSemester>? = null) {
        if (averages.pointAvgMax != 0f)
            averages.pointAvgPercent = averages.pointAvgSum / averages.pointAvgMax * 100f

        if (averages.normalCount <= 0f) {
            // no grades to count
            return
        }

        if (semesters == null || yearAverageMode == YEAR_ALL_GRADES) {
            // counting average for one semester
            // or yearly, but just averaging every grade
            averages.normalAvg = when {
                averages.normalWeightedCount > 0f -> {
                    averages.normalWeightedSum / averages.normalWeightedCount
                }
                averageWithoutWeight && averages.normalSum > 0f && averages.normalCount > 0f -> {
                    averages.normalSum / averages.normalCount
                }
                else -> null
            }
            return
        }

        val yearAverageMode = yearAverageMode
        averages.normalAvg = when (yearAverageMode) {
            YEAR_1_AVG_2_AVG -> {
                semesters.mapNotNull { it.averages.normalAvg }.average().toFloat()
            }
            else -> {
                if (semesters.size >= 2) {
                    val sem1 = semesters.firstOrNull { it.number == 1 }
                    val sem2 = semesters.firstOrNull { it.number == 2 }
                    when (yearAverageMode) {
                        YEAR_1_SEM_2_AVG -> {
                            ifNotNull(sem1?.finalGrade?.value, sem2?.averages?.normalAvg) { s1, s2 ->
                                (s1 + s2) / 2f
                            }
                        }
                        YEAR_1_AVG_2_SEM -> {
                            ifNotNull(sem1?.averages?.normalAvg, sem2?.finalGrade?.value) { s1, s2 ->
                                (s1 + s2) / 2f
                            }
                        }
                        YEAR_1_SEM_2_SEM -> {
                            ifNotNull(sem1?.finalGrade?.value, sem2?.finalGrade?.value) { s1, s2 ->
                                (s1 + s2) / 2f
                            }
                        }
                        else -> null
                    }
                }
                else null
            }
        }
    }

    fun getAverageString(context: Context, averages: GradesAverages, nameSemester: Boolean = false, showSemester: Int? = null, noAverageRes: Int? = null): CharSequence? {
        val averageString = when {
            averages.normalAvg != null -> String.format("%.2f", averages.normalAvg)
            averages.pointSum != 0f -> context.getString(R.string.grades_average_value_sum_format, format.format(averages.pointSum))
            averages.pointAvgPercent != null -> context.getString(
                R.string.grades_average_value_point_format,
                format.format(averages.pointAvgSum),
                format.format(averages.pointAvgMax),
                format.format(averages.pointAvgPercent)
            )
            else -> return noAverageRes?.let { context.getString(it) }
        }.asColoredSpannable(android.R.attr.textColorSecondary.resolveAttr(context))

        return if (nameSemester) {
            context.getString(
                if (showSemester == null)
                    R.string.grades_average_year_format
                else
                    R.string.grades_average_semester_format,
                showSemester ?: 0,
                averageString
            )
        }
        else {
            when {
                averages.normalAvg != null -> context.getString(R.string.grades_average_normal_format, averageString)
                averages.pointSum != 0f -> context.getString(R.string.grades_average_sum_format, averageString)
                averages.pointAvgPercent != null -> context.getString(R.string.grades_average_point_format, averageString)
                else -> null
            }
        }
    }

    fun getYearSummaryString(context: Context, gradeCount: Int, averages: GradesAverages): CharSequence {
        val gradeCountString = context.plural(R.plurals.grades_format, gradeCount)
        return context.getString(
            R.string.grades_year_summary_format,
            gradeCountString,
            getAverageString(context, averages, noAverageRes = R.string.grades_average_no)
        )
    }
}
