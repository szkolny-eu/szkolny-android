/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-4.
 */

package pl.szczodrzynski.edziennik.ui.modules.grades

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial.Icon2
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.MainActivity.Companion.TARGET_GRADES_EDITOR
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Metadata.TYPE_GRADE
import pl.szczodrzynski.edziennik.data.db.full.GradeFull
import pl.szczodrzynski.edziennik.databinding.GradesFragmentBinding
import pl.szczodrzynski.edziennik.ui.dialogs.grade.GradeDetailsDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.GradesConfigDialog
import pl.szczodrzynski.edziennik.ui.modules.grades.models.GradesAverages
import pl.szczodrzynski.edziennik.ui.modules.grades.models.GradesSemester
import pl.szczodrzynski.edziennik.ui.modules.grades.models.GradesStats
import pl.szczodrzynski.edziennik.ui.modules.grades.models.GradesSubject
import pl.szczodrzynski.edziennik.utils.managers.GradesManager
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem
import kotlin.coroutines.CoroutineContext
import kotlin.math.max


class GradesFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "GradesFragment"
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: GradesFragmentBinding

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here
    private val adapter by lazy {
        GradesAdapter(activity)
    }
    private val manager by lazy { app.gradesManager }
    private val dontCountEnabled by lazy { manager.dontCountEnabled }
    private val dontCountGrades by lazy { manager.dontCountGrades }
    private var expandSubjectId = 0L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = GradesFragmentBinding.inflate(inflater)
        b.refreshLayout.setParent(activity.swipeRefreshLayout)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded)
            return

        expandSubjectId = arguments?.getLong("gradesSubjectId") ?: 0L

        app.db.gradeDao()
                .getAllOrderBy(App.profileId, app.gradesManager.getOrderByString())
                .observe(this, Observer { grades ->
                    if (b.gradesRecyclerView.adapter == null) {
                        b.gradesRecyclerView.adapter = adapter
                        b.gradesRecyclerView.apply {
                            setHasFixedSize(true)
                            layoutManager = LinearLayoutManager(context)
                            //addItemDecoration(SimpleDividerItemDecoration(context))
                            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                                    if (recyclerView.canScrollVertically(-1)) {
                                        b.refreshLayout.isEnabled = false
                                    }
                                    if (!recyclerView.canScrollVertically(-1) && newState == SCROLL_STATE_IDLE) {
                                        b.refreshLayout.isEnabled = true
                                    }
                                }
                            })
                        }
                    }

                    launch(Dispatchers.Default) {
                        processGrades(grades)
                    }

                    if (grades != null && grades.isNotEmpty()) {
                        b.gradesRecyclerView.visibility = View.VISIBLE
                        b.gradesNoData.visibility = View.GONE
                    } else {
                        b.gradesRecyclerView.visibility = View.GONE
                        b.gradesNoData.visibility = View.VISIBLE
                    }
                })

        adapter.onGradeClick = {
            GradeDetailsDialog(activity, it)
        }

        adapter.onGradesEditorClick = { subject, semester ->
            val otherSemester = subject.semesters.firstOrNull { it != semester }
            var gradeSumOtherSemester = otherSemester?.averages?.normalWeightedSum
            var gradeCountOtherSemester = otherSemester?.averages?.normalWeightedCount
            if (gradeSumOtherSemester ?: 0f == 0f || gradeCountOtherSemester ?: 0f == 0f) {
                gradeSumOtherSemester = otherSemester?.averages?.normalSum
                gradeCountOtherSemester = otherSemester?.averages?.normalCount?.toFloat()
            }

            activity.loadTarget(TARGET_GRADES_EDITOR, Bundle(
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

        activity.bottomSheet.prependItems(
                BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_grades_config)
                        .withIcon(Icon2.cmd_settings_outline)
                        .withOnClickListener(View.OnClickListener {
                            activity.bottomSheet.close()
                            GradesConfigDialog(activity, true, null, null)
                        }),
                BottomSheetSeparatorItem(true),
                BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_mark_as_read)
                        .withIcon(CommunityMaterial.Icon.cmd_eye_check_outline)
                        .withOnClickListener(View.OnClickListener {
                            activity.bottomSheet.close()
                            AsyncTask.execute { App.db.metadataDao().setAllSeen(App.profileId, TYPE_GRADE, true) }
                            Toast.makeText(activity, R.string.main_menu_mark_as_read_success, Toast.LENGTH_SHORT).show()
                        })
        )
        activity.gainAttention()
    }

    @Suppress("SuspendFunctionOnCoroutineScope")
    private suspend fun processGrades(grades: List<GradeFull>) {
        val items = mutableListOf<GradesSubject>()

        var subjectId = -1L
        var semesterNumber = 0
        var subject = GradesSubject(subjectId, "")
        var semester = GradesSemester(0, 1)

        val hideImproved = manager.hideImproved

        // grades returned by the query are ordered
        // by the subject ID, so it's easier and probably
        // a bit faster to build all the models
        for (grade in grades) {
            /*if (grade.parentId != null && grade.parentId != -1L)
                continue // the grade is hidden as a new, improved one is available*/
            if (grade.subjectId != subjectId) {
                subjectId = grade.subjectId
                semesterNumber = 0

                subject = items.firstOrNull { it.subjectId == subjectId }
                        ?: GradesSubject(grade.subjectId, grade.subjectLongName ?: "").also {
                            items += it
                            it.semester = 2
                        }
            }
            if (grade.semester != semesterNumber) {
                semesterNumber = grade.semester

                semester = subject.semesters.firstOrNull { it.number == semesterNumber }
                        ?: GradesSemester(subject.subjectId, grade.semester).also { subject.semesters += it }
            }

            grade.showAsUnseen = !grade.seen
            if (!grade.seen) {
                semester.hasUnseen = true
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

        val stats = GradesStats()

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

        when (manager.orderBy) {
            GradesManager.ORDER_BY_DATE_DESC -> items.sortByDescending { it.lastAddedDate }
            GradesManager.ORDER_BY_DATE_ASC -> items.sortBy { it.lastAddedDate }
        }

        adapter.items = items.toMutableList()
        adapter.items.add(stats)

        var expandSubjectModel: GradesSubject? = null
        if (expandSubjectId != 0L) {
            expandSubjectModel = items.firstOrNull { it.subjectId == expandSubjectId }
            adapter.expandModel(
                    model = expandSubjectModel,
                    view = null,
                    notifyAdapter = false
            )
        }

        withContext(Dispatchers.Main) {
            adapter.notifyDataSetChanged()
        }

        startCoroutineTimer(500L, 0L) {
            if (expandSubjectModel != null) {
                b.gradesRecyclerView.smoothScrollToPosition(
                        items.indexOf(expandSubjectModel) + expandSubjectModel.semesters.size + (expandSubjectModel.semesters.firstOrNull()?.grades?.size ?: 0)
                )
            }
        }
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
