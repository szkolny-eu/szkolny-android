/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-30.
 */

package pl.szczodrzynski.edziennik.ui.attendance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.full.AttendanceFull
import pl.szczodrzynski.edziennik.databinding.AttendanceListFragmentBinding
import pl.szczodrzynski.edziennik.isNotNullNorEmpty
import pl.szczodrzynski.edziennik.startCoroutineTimer
import pl.szczodrzynski.edziennik.ui.attendance.models.AttendanceDayRange
import pl.szczodrzynski.edziennik.ui.attendance.models.AttendanceMonth
import pl.szczodrzynski.edziennik.ui.attendance.models.AttendanceTypeGroup
import pl.szczodrzynski.edziennik.ui.base.lazypager.LazyFragment
import pl.szczodrzynski.edziennik.ui.grades.models.GradesSubject
import pl.szczodrzynski.edziennik.utils.models.Date
import kotlin.coroutines.CoroutineContext

class AttendanceListFragment : LazyFragment(), CoroutineScope {
    companion object {
        private const val TAG = "AttendanceListFragment"
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: AttendanceListFragmentBinding

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here
    private val manager
        get() = app.attendanceManager
    private var viewType = AttendanceFragment.VIEW_DAYS
    private var expandSubjectId = 0L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = AttendanceListFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onPageCreated(): Boolean { startCoroutineTimer(100L) {
        if (!isAdded) return@startCoroutineTimer

        viewType = arguments?.getInt("viewType") ?: AttendanceFragment.VIEW_DAYS
        expandSubjectId = arguments?.getLong("gradesSubjectId") ?: 0L

        val adapter = AttendanceAdapter(activity, viewType)
        var firstRun = true

        app.db.attendanceDao().getAll(App.profileId).observe(this@AttendanceListFragment, Observer { items -> this@AttendanceListFragment.launch {
            if (!isAdded) return@launch

            // load & configure the adapter
            adapter.items = withContext(Dispatchers.Default) { processAttendance(items) }
            if (adapter.items.isNotNullNorEmpty() && b.list.adapter == null) {
                b.list.adapter = adapter
                b.list.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                    addOnScrollListener(onScrollListener)
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
                b.list.isVisible = false
                b.noData.isVisible = true
            } else {
                b.list.isVisible = true
                b.noData.isVisible = false
            }
        }})

        adapter.onAttendanceClick = {
            AttendanceDetailsDialog(activity, it)
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
    private fun processAttendance(attendance: List<AttendanceFull>): MutableList<Any> {
        if (attendance.isEmpty())
            return mutableListOf()

        val groupConsecutiveDays = app.config.forProfile().attendance.groupConsecutiveDays
        val showPresenceInMonth = app.config.forProfile().attendance.showPresenceInMonth

        if (viewType == AttendanceFragment.VIEW_DAYS) {
            val items = attendance
                    .filter { it.baseType != Attendance.TYPE_PRESENT }
                    .groupBy { it.date }
                    .map { AttendanceDayRange(
                            rangeStart = it.key,
                            rangeEnd = null,
                            items = it.value.toMutableList()
                    ) }
                    .toMutableList()

            if (groupConsecutiveDays) {
                items.sortByDescending { it.rangeStart }
                val iterator = items.listIterator()

                if (!iterator.hasNext())
                    return items.toMutableList()
                var element = iterator.next()
                while (iterator.hasNext()) {
                    var nextElement = iterator.next()
                    while (Date.diffDays(element.rangeStart, nextElement.rangeStart) <= 1 && iterator.hasNext()) {
                        if (element.rangeEnd == null)
                            element.rangeEnd = element.rangeStart

                        element.items.addAll(nextElement.items)
                        element.rangeStart = nextElement.rangeStart
                        iterator.remove()
                        nextElement = iterator.next()
                    }
                    element = nextElement
                }
            }

            return items.toMutableList()
        }
        else if (viewType == AttendanceFragment.VIEW_MONTHS) {
            val items = attendance
                    .groupBy { it.date.year to it.date.month }
                    .map { AttendanceMonth(
                            year = it.key.first,
                            month = it.key.second,
                            items = it.value.toMutableList()
                    ) }

            items.forEach { month ->
                month.typeCountMap = month.items
                        .groupBy { it.typeObject }
                        .map { it.key to it.value.size }
                        .sortedBy { it.first }
                        .toMap()

                val totalCount = month.typeCountMap.entries.sumBy {
                    if (!it.key.isCounted || it.key.baseType == Attendance.TYPE_UNKNOWN)
                        0
                    else it.value
                }
                val presenceCount = month.typeCountMap.entries.sumBy {
                    when (it.key.baseType) {
                        Attendance.TYPE_PRESENT,
                            Attendance.TYPE_PRESENT_CUSTOM,
                            Attendance.TYPE_BELATED,
                            Attendance.TYPE_BELATED_EXCUSED,
                            Attendance.TYPE_RELEASED -> if (it.key.isCounted) it.value else 0
                        else -> 0
                    }
                }

                month.percentage = if (totalCount == 0)
                    0f
                else
                    presenceCount.toFloat() / totalCount.toFloat() * 100f

                if (!showPresenceInMonth)
                    month.items.removeAll { it.baseType == Attendance.TYPE_PRESENT }
            }

            return items.toMutableList()
        }
        else if (viewType == AttendanceFragment.VIEW_TYPES) {
            val items = attendance
                    .groupBy { it.typeObject }
                    .map { AttendanceTypeGroup(
                            type = it.key,
                            items = it.value.toMutableList()
                    ) }
                    .sortedBy { it.items.size }

            items.forEach { type ->
                type.percentage = if (attendance.isEmpty())
                    0f
                else
                    type.items.size.toFloat() / attendance.size.toFloat() * 100f

                type.semesterCount = type.items.count { it.semester == app.profile.currentSemester }
            }

            return items.toMutableList()
        }
        return attendance.filter { it.baseType != Attendance.TYPE_PRESENT }.toMutableList()
    }
}
