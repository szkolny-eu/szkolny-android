/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-4.
 */

package pl.szczodrzynski.edziennik.ui.modules.attendance

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.full.AttendanceFull
import pl.szczodrzynski.edziennik.databinding.AttendanceSummaryFragmentBinding
import pl.szczodrzynski.edziennik.ui.modules.attendance.AttendanceFragment.Companion.VIEW_SUMMARY
import pl.szczodrzynski.edziennik.ui.modules.attendance.models.AttendanceSubject
import pl.szczodrzynski.edziennik.ui.modules.base.lazypager.LazyFragment
import pl.szczodrzynski.edziennik.ui.modules.grades.models.GradesSubject
import pl.szczodrzynski.edziennik.utils.models.Date
import java.text.DecimalFormat
import kotlin.coroutines.CoroutineContext

class AttendanceSummaryFragment : LazyFragment(), CoroutineScope {
    companion object {
        private const val TAG = "AttendanceSummaryFragment"
        private var periodSelection = 0
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: AttendanceSummaryFragmentBinding

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here
    private val manager by lazy { app.attendanceManager }
    private var expandSubjectId = 0L
    private var attendance = listOf<AttendanceFull>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = AttendanceSummaryFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onPageCreated(): Boolean { startCoroutineTimer(100L) {
        if (!isAdded) return@startCoroutineTimer

        expandSubjectId = arguments?.getLong("gradesSubjectId") ?: 0L

        val adapter = AttendanceAdapter(activity, VIEW_SUMMARY)
        var firstRun = true

        app.db.attendanceDao().getAll(App.profileId).observe(this@AttendanceSummaryFragment, Observer { items -> this@AttendanceSummaryFragment.launch {
            if (!isAdded) return@launch

            // load & configure the adapter
            attendance = items
            adapter.items = withContext(Dispatchers.Default) { processAttendance() }
            if (adapter.items.isNotNullNorEmpty() && b.list.adapter == null) {
                b.list.adapter = adapter
                b.list.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                    isNestedScrollingEnabled = false
                }
            }
            adapter.notifyDataSetChanged()
            setSwipeToRefresh(adapter.items.isNullOrEmpty())

            if (firstRun) {
                expandSubject(adapter)
                firstRun = false
            }

            // show/hide relevant views
            b.progressBar.isVisible = false
            if (adapter.items.isNullOrEmpty()) {
                b.statsLayout.isVisible = false
                b.list.isVisible = false
                b.noData.isVisible = true
            } else {
                b.statsLayout.isVisible = true
                b.list.isVisible = true
                b.noData.isVisible = false
            }
        }})

        adapter.onAttendanceClick = {
            //GradeDetailsDialog(activity, it)
        }

        b.toggleGroup.check(when (periodSelection) {
            0 -> R.id.allYear
            1 -> R.id.semester1
            2 -> R.id.semester2
            else -> R.id.allYear
        })
        b.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked)
                return@addOnButtonCheckedListener
            periodSelection = when (checkedId) {
                R.id.allYear -> 0
                R.id.semester1 -> 1
                R.id.semester2 -> 2
                else -> 0
            }
            this@AttendanceSummaryFragment.launch {
                adapter.items = withContext(Dispatchers.Default) { processAttendance() }
                if (adapter.items.isNullOrEmpty()) {
                    b.statsLayout.isVisible = false
                    b.list.isVisible = false
                    b.noData.isVisible = true
                } else {
                    b.statsLayout.isVisible = true
                    b.list.isVisible = true
                    b.noData.isVisible = false
                }
                adapter.notifyDataSetChanged()
            }
        }
    }; return true}

    private fun expandSubject(adapter: AttendanceAdapter) {
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
    private fun processAttendance(): MutableList<Any> {
        val attendance = when (periodSelection) {
            0 -> attendance
            1 -> attendance.filter { it.semester == 1 }
            2 -> attendance.filter { it.semester == 2 }
            else -> attendance
        }

        if (attendance.isEmpty())
            return mutableListOf()

        val items = attendance
                .groupBy { it.subjectId }
                .map { AttendanceSubject(
                        subjectId = it.key,
                        subjectName = it.value.firstOrNull()?.subjectLongName ?: "",
                        items = it.value.toMutableList()
                ) }
                .sortedBy { it.subjectName.toLowerCase() }

        var totalCountSum = 0
        var presenceCountSum = 0

        items.forEach { subject ->
            subject.typeCountMap = subject.items
                    .groupBy { it.typeObject }
                    .map { it.key to it.value.count { a -> a.isCounted } }
                    .sortedBy { it.first }
                    .toMap()

            val totalCount = subject.typeCountMap.entries.sumBy {
                when (it.key.baseType) {
                    Attendance.TYPE_UNKNOWN -> 0
                    else -> it.value
                }
            }
            val presenceCount = subject.typeCountMap.entries.sumBy {
                when (it.key.baseType) {
                    Attendance.TYPE_PRESENT,
                    Attendance.TYPE_PRESENT_CUSTOM,
                    Attendance.TYPE_BELATED,
                    Attendance.TYPE_BELATED_EXCUSED,
                    Attendance.TYPE_RELEASED -> it.value
                    else -> 0
                }
            }
            totalCountSum += totalCount
            presenceCountSum += presenceCount

            subject.percentage = if (totalCount == 0)
                0f
            else
                presenceCount.toFloat() / totalCount.toFloat() * 100f

            if (!false /* showPresenceInSubject */)
                subject.items.removeAll { it.baseType == Attendance.TYPE_PRESENT }
        }

        val typeCountMap = attendance
                .groupBy { it.typeObject }
                .map { it.key to it.value.count { a -> a.isCounted } }
                .sortedBy { it.first }
                .toMap()

        val percentage = if (totalCountSum == 0)
            0f
        else
            presenceCountSum.toFloat() / totalCountSum.toFloat() * 100f

        launch {
            b.attendanceBar.setAttendanceData(typeCountMap.mapKeys { manager.getAttendanceColor(it.key) })
            b.attendanceBar.isInvisible = typeCountMap.isEmpty()

            b.previewContainer.removeAllViews()
            val sum = typeCountMap.entries.sumBy { it.value }.toFloat()
            typeCountMap.forEach { (type, count) ->
                val layout = LinearLayout(activity)
                val attendanceObject = Attendance(
                        profileId = 0,
                        id = 0,
                        baseType = type.baseType,
                        typeName = "",
                        typeShort = type.typeShort,
                        typeSymbol = type.typeSymbol,
                        typeColor = type.typeColor,
                        date = Date(0, 0, 0),
                        startTime = null,
                        semester = 0,
                        teacherId = 0,
                        subjectId = 0,
                        addedDate = 0
                )
                layout.addView(AttendanceView(activity, attendanceObject, manager))
                layout.addView(TextView(activity).also {
                    it.setText(R.string.attendance_percentage_format, count/sum*100f)
                    it.setPadding(0, 0, 5.dp, 0)
                })
                layout.setPadding(0, 8.dp, 0, 8.dp)
                b.previewContainer.addView(layout)
            }

            if (percentage == 0f) {
                b.percentage.isInvisible = true
                b.percentageCircle.isInvisible = true
            }
            else {
                b.percentage.isVisible = true
                b.percentageCircle.isVisible = true
                b.percentage.setText(R.string.attendance_period_summary_format, percentage)

                val df = DecimalFormat("0.##")
                b.percentageCircle.setProgressTextAdapter { value ->
                    df.format(value) + "%"
                }
                b.percentageCircle.maxProgress = 100.0
                animatePercentageIndicator(percentage.toDouble())
            }
        }

        return items.toMutableList()
    }

    private fun animatePercentageIndicator(targetProgress: Double) {
        val startingProgress = b.percentageCircle.progress
        val progressChange = targetProgress - startingProgress

        val a: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                val progress = startingProgress + (progressChange * interpolatedTime)
                //if (interpolatedTime == 1f)
                //    progress = startingProgress + progressChange

                val color = ColorUtils.blendARGB(Color.RED, Color.GREEN, progress.toFloat() / 100.0f)
                b.percentageCircle.progressColor = color
                b.percentageCircle.setProgress(progress, 100.0)
            }

            override fun willChangeBounds(): Boolean {
                return false
            }
        }
        a.duration = 1300
        a.interpolator = AccelerateDecelerateInterpolator()
        b.percentageCircle.startAnimation(a)
    }
}
