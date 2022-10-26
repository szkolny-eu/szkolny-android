/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-8.
 */

package pl.szczodrzynski.edziennik.ui.agenda

import android.util.SparseIntArray
import android.widget.AbsListView
import android.widget.AbsListView.OnScrollListener
import androidx.core.util.forEach
import androidx.core.util.set
import androidx.core.view.isVisible
import com.github.tibolte.agendacalendarview.CalendarManager
import com.github.tibolte.agendacalendarview.CalendarPickerController
import com.github.tibolte.agendacalendarview.agenda.AgendaAdapter
import com.github.tibolte.agendacalendarview.models.BaseCalendarEvent
import com.github.tibolte.agendacalendarview.models.CalendarEvent
import com.github.tibolte.agendacalendarview.models.IDayItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.databinding.FragmentAgendaDefaultBinding
import pl.szczodrzynski.edziennik.ui.agenda.event.AgendaEvent
import pl.szczodrzynski.edziennik.ui.agenda.event.AgendaEventGroup
import pl.szczodrzynski.edziennik.ui.agenda.event.AgendaEventGroupRenderer
import pl.szczodrzynski.edziennik.ui.agenda.event.AgendaEventRenderer
import pl.szczodrzynski.edziennik.ui.agenda.lessonchanges.LessonChangesDialog
import pl.szczodrzynski.edziennik.ui.agenda.lessonchanges.LessonChangesEvent
import pl.szczodrzynski.edziennik.ui.agenda.lessonchanges.LessonChangesEventRenderer
import pl.szczodrzynski.edziennik.ui.agenda.teacherabsence.TeacherAbsenceDialog
import pl.szczodrzynski.edziennik.ui.agenda.teacherabsence.TeacherAbsenceEvent
import pl.szczodrzynski.edziennik.ui.agenda.teacherabsence.TeacherAbsenceEventRenderer
import pl.szczodrzynski.edziennik.ui.event.EventDetailsDialog
import pl.szczodrzynski.edziennik.utils.models.Date
import java.util.*

class AgendaFragmentDefault(
    private val activity: MainActivity,
    private val app: App,
    private val b: FragmentAgendaDefaultBinding
) : OnScrollListener, CoroutineScope {
    companion object {
        var selectedDate: Date = Date.getToday()
    }

    override val coroutineContext = Job() + Dispatchers.Main

    private val unreadDates = mutableSetOf<Int>()
    private val events = mutableListOf<CalendarEvent>()
    private var isInitialized = false

    private val listView
        get() = b.agendaDefaultView.agendaView.agendaListView
    private val adapter
        get() = listView.adapter as? AgendaAdapter
    private val manager
        get() = CalendarManager.getInstance()

    private var scrollState = OnScrollListener.SCROLL_STATE_IDLE
    private var updatePending = false
    private var notifyPending = false
    override fun onScrollStateChanged(view: AbsListView?, newScrollState: Int) {
        b.agendaDefaultView.agendaScrollListener.onScrollStateChanged(view, scrollState)
        scrollState = newScrollState
        if (updatePending) updateData()
        if (notifyPending) notifyDataSetChanged()
    }

    override fun onScroll(
        view: AbsListView?,
        firstVisibleItem: Int,
        visibleItemCount: Int,
        totalItemCount: Int
    ) = b.agendaDefaultView.agendaScrollListener.onScroll(
        view,
        firstVisibleItem,
        visibleItemCount,
        totalItemCount
    )

    /**
     * Mark the data as needing update, either after 1 second (when
     * not scrolling) or 1 second after scrolling stops.
     */
    private fun updateData() = launch {
        if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
            updatePending = false
            delay(1000)
            notifyDataSetChanged()
        } else updatePending = true
    }

    /**
     * Notify the adapter about changes, either instantly or after
     * scrolling stops.
     */
    private fun notifyDataSetChanged() {
        if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
            notifyPending = false
            adapter?.notifyDataSetChanged()
        } else notifyPending = true
    }

    suspend fun initView(fragment: AgendaFragment) {
        isInitialized = false

        withContext(Dispatchers.Default) {
            if (app.profile.config.ui.agendaLessonChanges)
                addLessonChanges(events)

            if (app.profile.config.ui.agendaTeacherAbsence)
                addTeacherAbsence(events)
        }

        app.db.eventDao().getAll(app.profileId).observe(fragment) {
            addEvents(events, it)
            if (isInitialized)
                updateView()
            else
                initViewPriv()
        }
    }

    private fun initViewPriv() {
        val dateStart = app.profile.dateSemester1Start.asCalendar
        val dateEnd = app.profile.dateYearEnd.asCalendar

        val isCompactMode = app.profile.config.ui.agendaCompactMode

        b.agendaDefaultView.init(
            events,
            dateStart,
            dateEnd,
            Locale.getDefault(),
            object : CalendarPickerController {
                override fun onDaySelected(dayItem: IDayItem) {
                    val c = Calendar.getInstance()
                    c.time = dayItem.date
                    if (c.timeInMillis == selectedDate.inMillis) {
                        DayDialog(activity, app.profileId, selectedDate).show()
                    }
                }

                override fun onEventSelected(event: CalendarEvent) {
                    val date = Date.fromCalendar(event.instanceDay)

                    when (event) {
                        is AgendaEvent -> EventDetailsDialog(activity, event.event).show()
                        is LessonChangesEvent -> LessonChangesDialog(
                            activity = activity,
                            profileId = app.profileId,
                            defaultDate = date
                        ).show()
                        is TeacherAbsenceEvent -> TeacherAbsenceDialog(
                            activity = activity,
                            profileId = app.profileId,
                            date = date
                        ).show()
                        is AgendaEventGroup -> DayDialog(
                            activity = activity,
                            profileId = app.profileId,
                            date = date,
                            eventTypeId = event.typeId,
                        ).show()
                        is BaseCalendarEvent -> if (event.isPlaceHolder)
                            DayDialog(activity, app.profileId, date).show()
                    }

                    if (event is BaseEvent && event.showItemBadge) {
                        val unreadCount = manager.events.count {
                            it.instanceDay.equals(event.instanceDay) && it.showBadge
                        }
                        // only clicked event is unread, remove the day badge
                        if (unreadCount == 1 && event.showBadge) {
                            event.dayReference.showBadge = false
                            unreadDates.remove(date.value)
                        }
                        setAsRead(event)
                    }
                }

                override fun onScrollToDate(calendar: Calendar) {
                    selectedDate = Date.fromCalendar(calendar)

                    // Mark as read scrolled date
                    if (selectedDate.value in unreadDates) {
                        setAsRead(calendar)
                        activity.launch(Dispatchers.Default) {
                            app.db.eventDao().setSeenByDate(app.profileId, selectedDate, true)
                        }
                        unreadDates.remove(selectedDate.value)
                    }
                }
            },
            AgendaEventRenderer(app.eventManager, isCompactMode),
            AgendaEventGroupRenderer(),
            LessonChangesEventRenderer(),
            TeacherAbsenceEventRenderer()
        )

        listView.setOnScrollListener(this)

        isInitialized = true
        b.progressBar.isVisible = false
    }

    private fun updateView() {
        manager.events.clear()
        manager.loadEvents(events, BaseCalendarEvent())

        adapter?.updateEvents(manager.events)
        //listView.scrollToCurrentDate(selectedDate.asCalendar)
    }

    private fun setAsRead(date: Calendar) {
        // get all events matching the date
        val events = manager.events.filter {
            if (it.instanceDay.equals(date) && it.showBadge && it is AgendaEvent) {
                // hide the day badge for the date
                it.dayReference.showBadge = false
                return@filter true
            }
            false
        }
        // set this date's events as read
        setAsRead(*events.toTypedArray())
    }

    private fun setAsRead(vararg event: CalendarEvent) {
        // hide per-event badges
        for (e in event) {
            events.firstOrNull {
                it == e
            }?.showBadge = false
            e.showBadge = false
        }

        listView.setOnScrollListener(this)
        updateData()
    }

    private fun addEvents(
        events: MutableList<CalendarEvent>,
        eventList: List<EventFull>
    ) {
        events.removeAll { it is AgendaEvent || it is AgendaEventGroup }

        if (!app.profile.config.ui.agendaGroupByType) {
            events += eventList.map {
                if (!it.seen)
                    unreadDates.add(it.date.value)
                AgendaEvent(it)
            }
            return
        }

        eventList.groupBy {
            it.date.value to it.type
        }.forEach { (_, list) ->
            val event = list.first()
            if (list.size == 1) {
                if (!event.seen)
                    unreadDates.add(event.date.value)
                events += AgendaEvent(event)
            } else {
                events.add(0, AgendaEventGroup(
                    profileId = event.profileId,
                    date = event.date,
                    typeId = event.type,
                    typeName = event.typeName ?: "-",
                    typeColor = event.typeColor ?: event.eventColor,
                    count = list.size,
                    showBadge = list.any { !it.seen }
                ))
            }
        }
    }

    private fun addLessonChanges(events: MutableList<CalendarEvent>) {
        val lessons = app.db.timetableDao().getChangesNow(app.profileId)

        val grouped = lessons.groupBy {
            it.displayDate
        }

        events += grouped.mapNotNull { (date, changes) ->
            LessonChangesEvent(
                app.profileId,
                date = date ?: return@mapNotNull null,
                count = changes.size,
                showBadge = changes.any { !it.seen }
            )
        }
    }

    private fun addTeacherAbsence(events: MutableList<CalendarEvent>) {
        val teacherAbsence = app.db.teacherAbsenceDao().getAllNow(app.profileId)

        val countMap = SparseIntArray()

        for (absence in teacherAbsence) {
            while (absence.dateFrom <= absence.dateTo) {
                countMap[absence.dateFrom.value] += 1
                absence.dateFrom.stepForward(0, 0, 1)
            }
        }

        countMap.forEach { dateInt, count ->
            events += TeacherAbsenceEvent(
                app.profileId,
                date = Date.fromValue(dateInt),
                count = count
            )
        }
    }
}
