/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-1.
 */

package pl.szczodrzynski.edziennik.utils.managers

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_NORMAL
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_NO_GRADE
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_POINT_AVG
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_POINT_SUM
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_YEAR_FINAL
import pl.szczodrzynski.edziennik.data.db.enums.SchoolType
import pl.szczodrzynski.edziennik.data.db.full.GradeFull
import pl.szczodrzynski.edziennik.ext.asColoredSpannable
import pl.szczodrzynski.edziennik.ext.get
import pl.szczodrzynski.edziennik.ext.ifNotNull
import pl.szczodrzynski.edziennik.ext.notEmptyOrNull
import pl.szczodrzynski.edziennik.ext.plural
import pl.szczodrzynski.edziennik.ext.resolveAttr
import pl.szczodrzynski.edziennik.ext.startCoroutineTimer
import pl.szczodrzynski.edziennik.ui.grades.models.GradesAverages
import pl.szczodrzynski.edziennik.ui.grades.models.GradesSemester
import java.text.DecimalFormat
import kotlin.coroutines.CoroutineContext
import kotlin.math.floor

class GradesManager(val app: App) : CoroutineScope {
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

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    private val gradeRegex by lazy { """([+-])?([0-6])([+-])?""".toRegex() }
    private val format = DecimalFormat("#.##")

    val orderBy
        get() = app.config.grades.orderBy
    val yearAverageMode
        get() = app.profile.config.grades.yearAverageMode
    val colorMode
        get() = app.profile.config.grades.colorMode
    val plusValue
        get() = app.profile.config.grades.plusValue
    val minusValue
        get() = app.profile.config.grades.minusValue
    val dontCountEnabled
        get() = app.profile.config.grades.dontCountEnabled
    val dontCountGrades
        get() = app.profile.config.grades.dontCountGrades
    val hideImproved
        get() = app.profile.config.grades.hideImproved
    val averageWithoutWeight
        get() = app.profile.config.grades.averageWithoutWeight
    val isUniversity
        get() = app.profile.loginStoreType.schoolType == SchoolType.UNIVERSITY


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
        TYPE_NO_GRADE -> context.getString(R.string.grades_weight_no_grade)
        else -> null
    }

    /**
     * Returns the "rounded" grade value as an integer.
     * The decimal value is rounded to ceil if >= 0.75.
     */
    fun getRoundedGrade(value: Float): Int {
        return floor(value.toDouble()).toInt() + if (value % 1.0f >= 0.75) 1 else 0
    }

    /**
     * Get a grade value, either saved in the [grade]
     * or calculated including the [plusValue] and
     * [minusValue].
     */
    fun getGradeValue(grade: Grade): Float {
        if (plusValue == null && minusValue == null)
            return grade.value

        gradeRegex.find(grade.name)?.let { it ->
            var value = it[2].toFloatOrNull() ?: return grade.value
            when (it[1].notEmptyOrNull() ?: it[3]) {
                "+" -> value += plusValue ?: return grade.value
                "-" -> value -= minusValue ?: return grade.value
                else -> return grade.value
            }
            return value
        }
        return grade.value
    }

    /**
     * Returns a weight if the grade should be counted
     * to the average, 0f otherwise.
     */
    fun getGradeWeight(dontCountEnabled: Boolean, dontCountGrades: List<String>, grade: Grade): Float {
        if (!dontCountEnabled)
            return grade.weight
        if (grade.name.lowercase().trim() in dontCountGrades)
            return 0f
        return grade.weight
    }

    fun getGradeColor(grade: Grade): Int {
        val type = grade.type
        val defColor = colorMode == COLOR_MODE_DEFAULT
        val valueMax = grade.valueMax ?: 0f

        val color = when {
            type == TYPE_POINT_SUM && !defColor -> {
                when {
                    grade.id < 0 -> grade.color and 0xffffff /* starting points */
                    grade.value < 0 -> 0xf44336
                    grade.value > 0 -> 0x4caf50
                    else -> 0xbdbdbd
                }
            }
            type == TYPE_POINT_AVG && !defColor ->
                when (valueMax) {
                    0f -> 0xbdbdbd
                    else -> when (grade.value / valueMax * 100f) {
                        in 0f..29f -> 0xf50000 // 1
                        in 30f..49f -> 0xff5722 // 2
                        in 50f..74f -> 0xff9100 // 3
                        in 75f..89f -> 0xffd600 // 4
                        in 90f..97f -> 0x00c853 // 5
                        else -> 0x0091ea // 6
                    }
                }
            type == TYPE_NORMAL && defColor -> grade.color and 0xffffff
            type == TYPE_NORMAL && app.profile.loginStoreType.schoolType == SchoolType.UNIVERSITY -> {
                when (grade.name.lowercase()) {
                    "zal" -> 0x4caf50
                    "nb", "nk" -> 0xff7043
                    "2.0", "nzal" -> 0xff3d00
                    "3.0" -> 0xffff00
                    "3.5" -> 0xc6ff00
                    "4.0" -> 0x76ff03
                    "4.5" -> 0x64dd17
                    "5.0" -> 0x00c853
                    else -> grade.color and 0xffffff
                }
            }
            type in TYPE_NORMAL..TYPE_YEAR_FINAL -> {
                when (grade.name.lowercase()) {
                    "+", "++", "+++" -> 0x4caf50
                    "0", "-", "-,", "-,-,", "np", "np.", "npnp", "np,", "np,np,", "bs", "nk", "bz" -> 0xff7043
                    "1-", "1", "f", "ng" -> 0xff0000
                    "1+", "ef" -> 0xff3d00
                    "2-", "2", "e", "ndp" -> 0xff9100
                    "2+", "de" -> 0xffab00
                    "3-", "3", "d", "popr" -> 0xffff00
                    "3+", "cd" -> 0xc6ff00
                    "4-", "4", "c", "db" -> 0x76ff03
                    "4+", "bc" -> 0x64dd17
                    "5-", "5", "b", "bdb" -> 0x00c853
                    "5+", "ab" -> 0x00bfa5
                    "6-", "6", "a", "wz" -> 0x2196f3
                    "6+", "a+" -> 0x0091ea
                    else -> grade.color and 0xffffff
                }
            }
            else -> grade.color and 0xffffff
        }
        return color or 0xff000000.toInt()
    }

    /**
     * Calculate the grade's value using
     * the specified [name].
     */
    fun getGradeValue(name: String): Float {
        return when (name.lowercase()) {
            "1-" -> 0.75f
            "1" -> 1.00f
            "1+" -> 1.50f
            "2-" -> 1.75f
            "2" -> 2.00f
            "2+" -> 2.50f
            "3-" -> 2.75f
            "3" -> 3.00f
            "3+" -> 3.50f
            "4-" -> 3.75f
            "4" -> 4.00f
            "4+" -> 4.50f
            "5-" -> 4.75f
            "5" -> 5.00f
            "5+" -> 5.50f
            "6-" -> 5.75f
            "6" -> 6.00f
            "6+" -> 6.50f
            "niedostateczny", "f" -> 1f
            "dopuszczający", "e" -> 2f
            "dostateczny", "d" -> 3f
            "dobry", "c" -> 4f
            "bardzo dobry", "b" -> 5f
            "celujący", "a" -> 6f
            else -> 0f
        }
    }

    fun getGradeNumberName(name: String): String {
        return when(name.lowercase()){
            "niedostateczny", "f" -> "1"
            "niedostateczny plus", "f+" -> "1+"
            "niedostateczny minus", "f-" -> "1-"
            "dopuszczający", "e" -> "2"
            "dopuszczający plus", "e+" -> "2+"
            "dopuszczający minus", "e-" -> "2-"
            "dostateczny", "d" -> "3"
            "dostateczny plus", "d+" -> "3+"
            "dostateczny minus", "d-" -> "3-"
            "dobry", "c" -> "4"
            "dobry plus", "c+" -> "4+"
            "dobry minus", "c-" -> "4-"
            "bardzo dobry", "b" -> "5"
            "bardzo dobry plus", "b+" -> "5+"
            "bardzo dobry minus", "b-" -> "5-"
            "celujący", "a" -> "6"
            "celujący plus", "a+" -> "6+"
            "celujący minus", "a-" -> "6-"
            else -> name
        }
    }

    /*    _    _ _____    _____                 _  __ _
         | |  | |_   _|  / ____|               (_)/ _(_)
         | |  | | | |   | (___  _ __   ___  ___ _| |_ _  ___
         | |  | | | |    \___ \| '_ \ / _ \/ __| |  _| |/ __|
         | |__| |_| |_   ____) | |_) |  __/ (__| | | | | (__
          \____/|_____| |_____/| .__/ \___|\___|_|_| |_|\___|
                               | |
                               |*/
    fun markAsSeen(grade: GradeFull) {
        grade.seen = true
        startCoroutineTimer(500L, 0L) {
            app.db.metadataDao().setSeen(grade.profileId, grade, true)
        }
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
