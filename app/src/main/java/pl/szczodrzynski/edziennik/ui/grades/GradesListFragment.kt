/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-4.
 */

package pl.szczodrzynski.edziennik.ui.grades

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.core.manager.GradesManager
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_NO_GRADE
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.data.db.full.GradeFull
import pl.szczodrzynski.edziennik.data.enums.FeatureType
import pl.szczodrzynski.edziennik.data.enums.MetadataType
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.databinding.GradesListFragmentBinding
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ext.averageOrNull
import pl.szczodrzynski.edziennik.ext.isNotNullNorEmpty
import pl.szczodrzynski.edziennik.ext.startCoroutineTimer
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.ui.dialogs.settings.GradesConfigDialog
import pl.szczodrzynski.edziennik.ui.grades.models.GradesAverages
import pl.szczodrzynski.edziennik.ui.grades.models.GradesSemester
import pl.szczodrzynski.edziennik.ui.grades.models.GradesStats
import pl.szczodrzynski.edziennik.ui.grades.models.GradesSubject
import pl.szczodrzynski.edziennik.utils.TextInputDropDown
import pl.szczodrzynski.edziennik.utils.managers.GradesManager
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.UNIVERSITY_AVERAGE_MODE_ECTS
import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.UNIVERSITY_AVERAGE_MODE_SIMPLE
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import kotlin.math.max

class GradesListFragment : BaseFragment<GradesListFragmentBinding, MainActivity>(
    inflater = GradesListFragmentBinding::inflate,
) {

    override fun getScrollingView() = b.list
    override fun getMarkAsReadType() = MetadataType.GRADE
    override fun getSyncParams() = FeatureType.GRADES to null
    override fun getBottomSheetItems() = listOf(
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_grades_config)
            .withIcon(CommunityMaterial.Icon.cmd_cog_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                GradesConfigDialog(activity, true).show()
            },
    )

    private val manager
        get() = app.gradesManager
    private val dontCountEnabled
        get() = manager.dontCountEnabled
    private val dontCountGrades
        get() = manager.dontCountGrades
    private var expandSubjectId = 0L

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        expandSubjectId = arguments?.getLong("gradesSubjectId") ?: 0L

        val adapter = GradesAdapter(activity)
        var firstRun = true

        app.db.gradeDao().getAllOrderBy(App.profileId, app.gradesManager.getOrderByString()).observe(viewLifecycleOwner, Observer { grades -> this@GradesListFragment.launch {
            if (!isAdded) return@launch

            grades.forEach {
                it.filterNotes()
            }

            val items = when {
                app.profile.config.grades.hideSticksFromOld && App.devMode -> grades.filter { it.value != 1.0f }
                else -> grades
            }

            if (manager.isUniversity) {
                val termIds = grades.map { it.comment }.toSet().toMutableList()
                val termNames: MutableMap<String, String> = mutableMapOf()
                // deserialize to a map of termId to (orderKey, termName)
                val terms = app.profile.getStudentData("termNames", null)
                    ?.let { app.gson.fromJson(it, termNames::class.java) }
                    ?.mapValues { (_, value) -> value.split('$', limit = 2) }
                    ?.mapValues { (_, value) -> Pair(value[0].toIntOrNull() ?: 0, value[1]) }
                    ?: mapOf()
                // sort by order key
                termIds.sortByDescending { termId -> terms[termId]?.first ?: 0 }
                // populate the dropdown
                b.semesterLayout.isVisible = true
                b.semesterDropdown.items = termIds.mapIndexed { id, termId ->
                    TextInputDropDown.Item(
                        id.toLong(),
                        terms[termId]?.second ?: termId ?: "-",
                        tag = termId,
                    )
                }.toMutableList()
                b.semesterDropdown.select(index = 0)
                b.semesterDropdown.setOnChangeListener { item ->
                    b.semesterDropdown.select(item)
                    adapter.items = processGrades(items)
                    adapter.notifyDataSetChanged()
                    return@setOnChangeListener true
                }
            }

            // load & configure the adapter
            adapter.items = withContext(Dispatchers.Default) { processGrades(items) }
            if (items.isNotNullNorEmpty() && b.list.adapter == null) {
                b.list.adapter = adapter
                b.list.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                }
            }
            adapter.notifyDataSetChanged()

            if (firstRun) {
                expandSubject(adapter)
                firstRun = false
            }

            // show/hide relevant views
            b.progressBar.isVisible = false
            if (items.isNullOrEmpty()) {
                b.list.isVisible = false
                b.noData.isVisible = true
            } else {
                b.list.isVisible = true
                b.noData.isVisible = false
            }
        }})

        adapter.onGradeClick = {
            GradeDetailsDialog(activity, it).show()
        }

        adapter.onGradesEditorClick = { subject, semester ->
            val otherSemester = subject.semesters.firstOrNull { it != semester }
            var gradeSumOtherSemester = otherSemester?.averages?.normalWeightedSum
            var gradeCountOtherSemester = otherSemester?.averages?.normalWeightedCount
            if ((gradeSumOtherSemester ?: 0f) == 0f || (gradeCountOtherSemester ?: 0f) == 0f) {
                gradeSumOtherSemester = otherSemester?.averages?.normalSum
                gradeCountOtherSemester = otherSemester?.averages?.normalCount?.toFloat()
            }

            activity.navigate(navTarget = NavTarget.GRADES_EDITOR, args = Bundle(
                    "subjectId" to subject.subjectId,
                    "semester" to semester.number,
                    "averageMode" to manager.yearAverageMode,
                    "yearAverageBefore" to subject.averages.normalAvg,
                    "gradeSumOtherSemester" to gradeSumOtherSemester,
                    "gradeCountOtherSemester" to gradeCountOtherSemester,
                    "averageOtherSemester" to otherSemester?.averages?.normalAvg,
                    "finalOtherSemester" to otherSemester?.finalGrade?.value
            ))
        }
    }

    private fun expandSubject(adapter: GradesAdapter) {
        var expandSubjectModel: GradesSubject? = null
        if (expandSubjectId != 0L) {
            expandSubjectModel = adapter.items.firstOrNull { it is GradesSubject && it.subjectId == expandSubjectId } as? GradesSubject
            adapter.expandModel(
                    model = expandSubjectModel,
                    view = null,
                    notifyAdapter = false
            )
        }

        startCoroutineTimer(500L) {
            if (expandSubjectModel != null) {
                b.list.smoothScrollToPosition(
                        adapter.items.indexOf(expandSubjectModel) + expandSubjectModel.semesters.size + (expandSubjectModel.semesters.firstOrNull()?.grades?.size ?: 0)
                )
            }
        }
    }

    @Suppress("SuspendFunctionOnCoroutineScope")
    private fun processGrades(grades: List<GradeFull>): MutableList<Any> {
        val items = mutableListOf<GradesSubject>()
        var unknownSubjectItem: GradesSubject? = null

        var subjectId = -1L
        var semesterNumber = 0
        var subject = GradesSubject(subjectId, "")
        var semester = GradesSemester(0, 1)
        val isUniversity = manager.isUniversity
        val filterTermId = b.semesterDropdown.selected?.tag

        val hideNoGrade = app.profile.config.grades.hideNoGrade
        val countEctsInProgress = app.profile.config.grades.countEctsInProgress
        val universityAverageMode = app.profile.config.grades.universityAverageMode

        // grades returned by the query are ordered
        // by the subject ID, so it's easier and probably
        // a bit faster to build all the models
        for (grade in grades) {
            if (isUniversity && filterTermId != null && grade.comment != filterTermId)
                continue

            if (hideNoGrade && grade.type == TYPE_NO_GRADE)
                continue

            /*if (grade.parentId != null && grade.parentId != -1L)
                continue // the grade is hidden as a new, improved one is available*/
            if (grade.subjectId != subjectId) {
                subjectId = grade.subjectId
                semesterNumber = 0

                subject = items.firstOrNull { it.subjectId == subjectId } ?: run {
                    if (grade.subjectLongName != null) {
                        return@run GradesSubject(grade.subjectId, grade.subjectLongName!!).also {
                            items += it
                            it.semester = 2
                        }
                    }
                    if (unknownSubjectItem == null) {
                        unknownSubjectItem = GradesSubject(-1, "unknown").also {
                            items += it
                            it.semester = 2
                            it.isUnknown = true
                        }
                    }
                    return@run unknownSubjectItem!!
                }
            }
            if (grade.semester != semesterNumber) {
                semesterNumber = grade.semester

                semester = subject.semesters.firstOrNull { it.number == semesterNumber }
                    ?: GradesSemester(subject.subjectId, grade.semester).also {
                        subject.semesters += it
                        it.hideEditor = subject.isUnknown
                    }
            }

            grade.showAsUnseen = !grade.seen
            if (!grade.seen) {
                if (grade.type == Grade.TYPE_YEAR_PROPOSED || grade.type == Grade.TYPE_YEAR_FINAL)
                    subject.hasUnseen = true // set an override flag
                else
                    semester.hasUnseen = true
            }

            if (subject.isUnknown) {
                // unknown subjects may have final grades (i.e. Mobidziennik)
                grade.type = Grade.TYPE_NORMAL
            }

            when (grade.type) {
                Grade.TYPE_SEMESTER1_PROPOSED,
                Grade.TYPE_SEMESTER2_PROPOSED -> semester.proposedGrade = grade
                Grade.TYPE_SEMESTER1_FINAL,
                Grade.TYPE_SEMESTER2_FINAL -> semester.finalGrade = grade
                Grade.TYPE_YEAR_PROPOSED -> subject.proposedGrade = grade
                Grade.TYPE_YEAR_FINAL -> subject.finalGrade = grade
                else -> {
                    semester.grades += grade
                    countGrade(grade, subject.averages)
                    countGrade(grade, semester.averages)
                }
            }

            subject.lastAddedDate = max(subject.lastAddedDate, grade.addedDate)
        }

        when (manager.orderBy) {
            GradesManager.ORDER_BY_DATE_DESC -> items.sortByDescending { it.lastAddedDate }
            GradesManager.ORDER_BY_DATE_ASC -> items.sortBy { it.lastAddedDate }
        }

        val stats = GradesStats()

        if (isUniversity) {
            val semesterSum = mutableListOf<Float>()
            val semesterCount = mutableListOf<Float>()
            val totalSum = mutableListOf<Float>()
            val totalCount = mutableListOf<Float>()
            val ectsPoints = mutableMapOf<Pair<Long, String?>, Float>()
            for (grade in grades) {
                val pointsPair = grade.subjectId to grade.comment
                if (grade.type == TYPE_NO_GRADE && !countEctsInProgress)
                    // reset points if there's an exam that isn't passed yet
                    ectsPoints[pointsPair] = 0.0f

                if (grade.value == 0.0f || grade.weight == 0.0f)
                    continue
                if (universityAverageMode == UNIVERSITY_AVERAGE_MODE_ECTS)
                    totalSum.add(grade.value * grade.weight)
                else
                    totalSum.add(grade.value)
                totalCount.add(grade.weight)

                if (grade.value < 3.0)
                    // exam not passed, reset points for this subject
                    ectsPoints[pointsPair] = 0.0f
                else if (pointsPair !in ectsPoints)
                    // no points for this subject, simply assign
                    ectsPoints[pointsPair] = grade.weight

                if (filterTermId != null && grade.comment != filterTermId)
                    continue

                if (universityAverageMode == UNIVERSITY_AVERAGE_MODE_ECTS)
                    semesterSum.add(grade.value * grade.weight)
                else
                    semesterSum.add(grade.value)
                semesterCount.add(grade.weight)
            }
            when (universityAverageMode) {
                UNIVERSITY_AVERAGE_MODE_SIMPLE -> {
                    stats.universitySem = semesterSum.sum() / semesterCount.size
                    stats.universityTotal = totalSum.sum() / totalCount.size
                }
                UNIVERSITY_AVERAGE_MODE_ECTS -> {
                    stats.universitySem = semesterSum.sum() / semesterCount.sum()
                    stats.universityTotal = totalSum.sum() / totalCount.sum()
                }
            }
            stats.universityEcts = ectsPoints.values.sum()
            return (items + stats).toMutableList()
        }

        val sem1Expected = mutableListOf<Float>()
        val sem2Expected = mutableListOf<Float>()
        val yearlyExpected = mutableListOf<Float>()
        val sem1Proposed = mutableListOf<Float>()
        val sem2Proposed = mutableListOf<Float>()
        val yearlyProposed = mutableListOf<Float>()
        val sem1Final = mutableListOf<Float>()
        val sem2Final = mutableListOf<Float>()
        val yearlyFinal = mutableListOf<Float>()

        val sem1Point = mutableListOf<Float>()
        val sem2Point = mutableListOf<Float>()
        val yearlyPoint = mutableListOf<Float>()

        for (item in items) {
            if (item.isUnknown) {
                // do not count averages for "unknown" subjects
                continue
            }
            item.semesters.forEach { sem ->
                manager.calculateAverages(sem.averages)
                if (sem.number == 1) {
                    sem.proposedGrade?.value?.let { sem1Proposed += it }
                    sem.finalGrade?.value?.let {
                        sem1Final += it
                        sem1Expected += it
                    } ?: run {
                        sem.averages.normalAvg?.let { sem1Expected += manager.getRoundedGrade(it).toFloat() }
                    }
                    sem.averages.pointAvgPercent?.let { sem1Point += it }
                }
                if (sem.number == 2) {
                    sem.proposedGrade?.value?.let { sem2Proposed += it }
                    sem.finalGrade?.value?.let {
                        sem2Final += it
                        sem2Expected += it
                    } ?: run {
                        sem.averages.normalAvg?.let { sem2Expected += manager.getRoundedGrade(it).toFloat() }
                    }
                    sem.averages.pointAvgPercent?.let { sem2Point += it }
                }
            }
            manager.calculateAverages(item.averages, item.semesters)
            item.proposedGrade?.value?.let { yearlyProposed += it }
            item.finalGrade?.value?.let {
                yearlyFinal += it
                yearlyExpected += it
            } ?: run {
                item.averages.normalAvg?.let { yearlyExpected += manager.getRoundedGrade(it).toFloat() }
            }
            item.averages.pointAvgPercent?.let { yearlyPoint += it }
        }

        stats.normalSem1 = sem1Expected.averageOrNull()?.toFloat() ?: 0f
        stats.normalSem1Proposed = sem1Proposed.averageOrNull()?.toFloat() ?: 0f
        stats.normalSem1Final = sem1Final.averageOrNull()?.toFloat() ?: 0f
        stats.sem1NotAllFinal = sem1Final.size < sem1Expected.size
        stats.normalSem2 = sem2Expected.averageOrNull()?.toFloat() ?: 0f
        stats.normalSem2Proposed = sem2Proposed.averageOrNull()?.toFloat() ?: 0f
        stats.normalSem2Final = sem2Final.averageOrNull()?.toFloat() ?: 0f
        stats.sem2NotAllFinal = sem2Final.size < sem2Expected.size
        stats.normalYearly = yearlyExpected.averageOrNull()?.toFloat() ?: 0f
        stats.normalYearlyProposed = yearlyProposed.averageOrNull()?.toFloat() ?: 0f
        stats.normalYearlyFinal = yearlyFinal.averageOrNull()?.toFloat() ?: 0f
        stats.yearlyNotAllFinal = yearlyFinal.size < yearlyExpected.size

        stats.pointSem1 = sem1Point.averageOrNull()?.toFloat() ?: 0f
        stats.pointSem2 = sem2Point.averageOrNull()?.toFloat() ?: 0f
        stats.pointYearly = yearlyPoint.averageOrNull()?.toFloat() ?: 0f

        return (items + stats).toMutableList()
    }

    private fun countGrade(grade: Grade, averages: GradesAverages) {
        val value = manager.getGradeValue(grade)
        val weight = manager.getGradeWeight(dontCountEnabled, dontCountGrades, grade)
        when (grade.type) {
            Grade.TYPE_NORMAL -> {
                if (grade.value > 0f) {
                    // count to the arithmetic average
                    // only if value more than 0
                    // to exclude "+", "-", "np" etc.
                    averages.normalSum += value
                    averages.normalCount++
                }
                averages.normalWeightedSum += value * weight
                averages.normalWeightedCount += weight
            }
            Grade.TYPE_POINT_AVG -> {
                averages.pointAvgSum += grade.value
                averages.pointAvgMax += grade.valueMax ?: value
            }
            Grade.TYPE_POINT_SUM -> {
                averages.pointSum += grade.value
            }
        }
    }
}
