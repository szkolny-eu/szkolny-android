/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-4.
 */

package pl.szczodrzynski.edziennik.ui.modules.attendance

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
import pl.szczodrzynski.edziennik.ui.modules.attendance.AttendanceFragment.Companion.VIEW_SUMMARY
import pl.szczodrzynski.edziennik.ui.modules.attendance.models.AttendanceSubject
import pl.szczodrzynski.edziennik.ui.modules.base.lazypager.LazyFragment
import pl.szczodrzynski.edziennik.ui.modules.grades.models.GradesSubject
import kotlin.coroutines.CoroutineContext

class AttendanceSummaryFragment : LazyFragment(), CoroutineScope {
    companion object {
        private const val TAG = "AttendanceSummaryFragment"
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: AttendanceListFragmentBinding

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here
    private val manager by lazy { app.attendanceManager }
    private var expandSubjectId = 0L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = AttendanceListFragmentBinding.inflate(inflater)
        b.refreshLayout.setParent(activity.swipeRefreshLayout)
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
            adapter.items = withContext(Dispatchers.Default) { processAttendance(items) }
            if (items.isNotNullNorEmpty() && b.list.adapter == null) {
                b.list.adapter = adapter
                b.list.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                    addOnScrollListener(onScrollListener)
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

        adapter.onAttendanceClick = {
            //GradeDetailsDialog(activity, it)
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

        val items = attendance
                .groupBy { it.subjectId }
                .map { AttendanceSubject(
                        subjectId = it.key,
                        subjectName = it.value.firstOrNull()?.subjectLongName ?: "",
                        items = it.value.toMutableList()
                ) }
                .sortedBy { it.subjectName.toLowerCase() }

        items.forEach { subject ->
            subject.typeCountMap = subject.items
                    .groupBy { it.baseType }
                    .map { it.key to it.value.size }
                    .sortedBy { it.first }
                    .toMap()

            val totalCount = subject.typeCountMap.entries.sumBy { it.value }
            val presenceCount = subject.typeCountMap.entries.sumBy {
                when (it.key) {
                    Attendance.TYPE_PRESENT,
                    Attendance.TYPE_PRESENT_CUSTOM,
                    Attendance.TYPE_BELATED,
                    Attendance.TYPE_BELATED_EXCUSED,
                    Attendance.TYPE_RELEASED -> it.value
                    else -> 0
                }
            }

            subject.percentage = if (totalCount == 0)
                0f
            else
                presenceCount.toFloat() / totalCount.toFloat() * 100f

            if (!false /* showPresenceInSubject */)
                subject.items.removeAll { it.baseType == Attendance.TYPE_PRESENT }
        }

        return items.toMutableList()
    }
}
