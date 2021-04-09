/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-8.
 */

package pl.szczodrzynski.edziennik.ui.modules.agenda

import android.util.SparseIntArray
import androidx.core.util.forEach
import androidx.core.util.set
import com.github.tibolte.agendacalendarview.CalendarPickerController
import com.github.tibolte.agendacalendarview.models.CalendarEvent
import com.github.tibolte.agendacalendarview.models.IDayItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.databinding.FragmentAgendaDefaultBinding
import pl.szczodrzynski.edziennik.ui.dialogs.day.DayDialog
import pl.szczodrzynski.edziennik.ui.dialogs.lessonchange.LessonChangeDialog
import pl.szczodrzynski.edziennik.ui.dialogs.teacherabsence.TeacherAbsenceDialog
import pl.szczodrzynski.edziennik.ui.modules.agenda.event.AgendaEvent
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
    private val unreadDates = mutableSetOf<Int>()
    var selectedDate: Date = Date.getToday()

    suspend fun initView(fragment: AgendaFragment) {
        val dateStart = app.profile.dateSemester1Start.asCalendar
        val dateEnd = app.profile.dateYearEnd.asCalendar

        val events = withContext(Dispatchers.Default) {
            val events = mutableListOf<CalendarEvent>()

            addLessonChanges(events)

            val showTeacherAbsences = app.profile.getStudentData("showTeacherAbsences", true)
            if (showTeacherAbsences) {
                addTeacherAbsence(events)
            }

            addEvents(events)

            return@withContext events
        }

        if (!fragment.isAdded)
            return
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
                        is AgendaEvent -> DayDialog(activity, app.profileId, date)
                        is LessonChangesEvent -> LessonChangeDialog(activity, app.profileId, date)
                        is TeacherAbsenceEvent -> TeacherAbsenceDialog(activity, app.profileId, date)
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
            AgendaEventRenderer(),
            LessonChangesEventRenderer(),
            TeacherAbsenceEventRenderer()
        )
    }

    private fun addEvents(events: MutableList<CalendarEvent>) {
        val eventList = app.db.eventDao().getAllNow(app.profileId)

        events += eventList.map {
            if (!it.seen)
                unreadDates.add(it.date.value)
            AgendaEvent(it)
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
