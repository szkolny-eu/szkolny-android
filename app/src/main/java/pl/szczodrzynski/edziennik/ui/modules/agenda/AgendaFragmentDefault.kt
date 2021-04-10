/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-8.
 */

package pl.szczodrzynski.edziennik.ui.modules.agenda

import android.util.SparseIntArray
import androidx.core.util.forEach
import androidx.core.util.set
import androidx.core.view.isVisible
import com.github.tibolte.agendacalendarview.CalendarManager
import com.github.tibolte.agendacalendarview.CalendarPickerController
import com.github.tibolte.agendacalendarview.agenda.AgendaAdapter
import com.github.tibolte.agendacalendarview.models.BaseCalendarEvent
import com.github.tibolte.agendacalendarview.models.CalendarEvent
import com.github.tibolte.agendacalendarview.models.IDayItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.databinding.FragmentAgendaDefaultBinding
import pl.szczodrzynski.edziennik.ui.dialogs.day.DayDialog
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventDetailsDialog
import pl.szczodrzynski.edziennik.ui.dialogs.lessonchange.LessonChangeDialog
import pl.szczodrzynski.edziennik.ui.dialogs.teacherabsence.TeacherAbsenceDialog
import pl.szczodrzynski.edziennik.ui.modules.agenda.event.AgendaEvent
import pl.szczodrzynski.edziennik.ui.modules.agenda.event.AgendaEventGroup
import pl.szczodrzynski.edziennik.ui.modules.agenda.event.AgendaEventGroupRenderer
import pl.szczodrzynski.edziennik.ui.modules.agenda.event.AgendaEventRenderer
import pl.szczodrzynski.edziennik.ui.modules.agenda.lessonchanges.LessonChangesEvent
import pl.szczodrzynski.edziennik.ui.modules.agenda.lessonchanges.LessonChangesEventRenderer
import pl.szczodrzynski.edziennik.ui.modules.agenda.teacherabsence.TeacherAbsenceEvent
import pl.szczodrzynski.edziennik.ui.modules.agenda.teacherabsence.TeacherAbsenceEventRenderer
import pl.szczodrzynski.edziennik.utils.models.Date
import java.util.*

class AgendaFragmentDefault(
    private val activity: MainActivity,
    private val app: App,
    private val b: FragmentAgendaDefaultBinding
) {
    companion object {
        var selectedDate: Date = Date.getToday()
    }

    private val unreadDates = mutableSetOf<Int>()
    private val events = mutableListOf<CalendarEvent>()
    private var isInitialized = false
    private val profileConfig by lazy { app.config.forProfile().ui }

    suspend fun initView(fragment: AgendaFragment) {
        isInitialized = false

        withContext(Dispatchers.Default) {
            if (profileConfig.agendaLessonChanges)
                addLessonChanges(events)

            if (profileConfig.agendaTeacherAbsence)
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

        val isCompactMode = profileConfig.agendaCompactMode

        b.agendaDefaultView.init(
            events,
            dateStart,
            dateEnd,
            Locale.getDefault(),
            object : CalendarPickerController {
                override fun onDaySelected(dayItem: IDayItem) {}

                override fun onEventSelected(event: CalendarEvent) {
                    val date = Date.fromCalendar(event.instanceDay)

                    when (event) {
                        is AgendaEvent -> EventDetailsDialog(activity, event.event)
                        is LessonChangesEvent -> LessonChangeDialog(activity, app.profileId, date)
                        is TeacherAbsenceEvent -> TeacherAbsenceDialog(
                            activity,
                            app.profileId,
                            date
                        )
                        is BaseCalendarEvent -> if (event.isPlaceHolder)
                            DayDialog(activity, app.profileId, date)
                    }
                }

                override fun onScrollToDate(calendar: Calendar) {
                    selectedDate = Date.fromCalendar(calendar)

                    // Mark as read scrolled date
                    if (selectedDate.value in unreadDates) {
                        activity.launch(Dispatchers.Default) {
                            app.db.eventDao().setSeenByDate(app.profileId, selectedDate, true)
                        }
                        unreadDates.remove(selectedDate.value)
                    }
                }
            },
            AgendaEventRenderer(isCompactMode),
            AgendaEventGroupRenderer(),
            LessonChangesEventRenderer(),
            TeacherAbsenceEventRenderer()
        )

        isInitialized = true
        b.progressBar.isVisible = false
    }

    private fun updateView() {
        val manager = CalendarManager.getInstance()
        manager.events.clear()
        manager.loadEvents(events, BaseCalendarEvent())

        val adapter = b.agendaDefaultView.agendaView.agendaListView.adapter as? AgendaAdapter
        adapter?.updateEvents(manager.events)
        b.agendaDefaultView.agendaView.agendaListView.scrollToCurrentDate(selectedDate.asCalendar)
    }

    private fun addEvents(
        events: MutableList<CalendarEvent>,
        eventList: List<EventFull>
    ) {
        events.removeAll { it is AgendaEvent }

        if (!profileConfig.agendaGroupByType) {
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
            }
            else {
                events.add(0, AgendaEventGroup(
                    profileId = event.profileId,
                    date = event.date,
                    typeName = event.typeName ?: "-",
                    typeColor = event.typeColor ?: event.eventColor,
                    eventCount = list.size
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
                changeCount = changes.size
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
                absenceCount = count
            )
        }
    }
}
