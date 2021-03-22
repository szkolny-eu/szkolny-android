/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-25
 */

package pl.szczodrzynski.edziennik.ui.modules.agenda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.applandeo.materialcalendarview.EventDay
import com.github.tibolte.agendacalendarview.CalendarPickerController
import com.github.tibolte.agendacalendarview.models.BaseCalendarEvent
import com.github.tibolte.agendacalendarview.models.CalendarEvent
import com.github.tibolte.agendacalendarview.models.IDayItem
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import eu.szkolny.font.SzkolnyFont
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.databinding.FragmentAgendaCalendarBinding
import pl.szczodrzynski.edziennik.databinding.FragmentAgendaDefaultBinding
import pl.szczodrzynski.edziennik.ui.dialogs.day.DayDialog
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventManualDialog
import pl.szczodrzynski.edziennik.ui.dialogs.lessonchange.LessonChangeDialog
import pl.szczodrzynski.edziennik.ui.dialogs.teacherabsence.TeacherAbsenceDialog
import pl.szczodrzynski.edziennik.ui.modules.agenda.lessonchange.LessonChangeCounter
import pl.szczodrzynski.edziennik.ui.modules.agenda.lessonchange.LessonChangeEvent
import pl.szczodrzynski.edziennik.ui.modules.agenda.lessonchange.LessonChangeEventRenderer
import pl.szczodrzynski.edziennik.ui.modules.agenda.teacherabsence.TeacherAbsenceCounter
import pl.szczodrzynski.edziennik.ui.modules.agenda.teacherabsence.TeacherAbsenceEvent
import pl.szczodrzynski.edziennik.ui.modules.agenda.teacherabsence.TeacherAbsenceEventRenderer
import pl.szczodrzynski.edziennik.utils.Colors
import pl.szczodrzynski.edziennik.utils.Themes
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem
import java.util.*
import kotlin.coroutines.CoroutineContext

class AgendaFragment : Fragment(), CoroutineScope {

    private lateinit var activity: MainActivity
    private lateinit var b: ViewDataBinding

    private val app by lazy { activity.app }

    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private var type: Int = Profile.AGENDA_DEFAULT
    private var actualDate: Date? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (getActivity() == null || context == null) return null
        activity = getActivity() as MainActivity
        context?.theme?.applyStyle(Themes.appTheme, true)
        type = app.config.forProfile().ui.agendaViewType
        b = when (type) {
            Profile.AGENDA_DEFAULT -> FragmentAgendaDefaultBinding.inflate(inflater, container, false)
            Profile.AGENDA_CALENDAR -> FragmentAgendaCalendarBinding.inflate(inflater, container, false)
            else -> return null
        }
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded) return

        activity.bottomSheet.prependItems(
                BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_add_event)
                        .withDescription(R.string.menu_add_event_desc)
                        .withIcon(SzkolnyFont.Icon.szf_calendar_plus_outline)
                        .withOnClickListener(View.OnClickListener {
                            activity.bottomSheet.close()
                            EventManualDialog(activity, app.profileId, defaultDate = actualDate)
                        }),
                BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_agenda_change_view)
                        .withIcon(if (type == Profile.AGENDA_DEFAULT) CommunityMaterial.Icon.cmd_calendar_outline else CommunityMaterial.Icon2.cmd_format_list_bulleted_square)
                        .withOnClickListener(View.OnClickListener {
                            activity.bottomSheet.close()
                            type = if (type == Profile.AGENDA_DEFAULT) Profile.AGENDA_CALENDAR else Profile.AGENDA_DEFAULT
                            app.config.forProfile().ui.agendaViewType = type
                            activity.reloadTarget()
                        }),
                BottomSheetSeparatorItem(true),
                BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_mark_as_read)
                        .withIcon(CommunityMaterial.Icon.cmd_eye_check_outline)
                        .withOnClickListener(View.OnClickListener { launch {
                            activity.bottomSheet.close()
                            withContext(Dispatchers.Default) {
                                App.db.metadataDao().setAllSeen(app.profileId, Metadata.TYPE_EVENT, true)
                            }
                            Toast.makeText(activity, R.string.main_menu_mark_as_read_success, Toast.LENGTH_SHORT).show()
                        }})
        )

        activity.navView.bottomBar.fabEnable = true
        activity.navView.bottomBar.fabExtendedText = getString(R.string.add)
        activity.navView.bottomBar.fabIcon = CommunityMaterial.Icon3.cmd_plus
        activity.navView.setFabOnClickListener(View.OnClickListener {
            EventManualDialog(activity, app.profileId, defaultDate = actualDate)
        })

        activity.gainAttention()
        activity.gainAttentionFAB()

        when (type) {
            Profile.AGENDA_DEFAULT -> createDefaultAgendaView()
            Profile.AGENDA_CALENDAR -> createCalendarAgendaView()
        }
    }

    private fun createDefaultAgendaView() { (b as? FragmentAgendaDefaultBinding)?.let { b -> launch {
        if (!isAdded)
            return@launch
        delay(500)

        val eventList = mutableListOf<CalendarEvent>()

        val minDate = Calendar.getInstance().apply {
            add(Calendar.MONTH, -2)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDate = Calendar.getInstance().apply { add(Calendar.MONTH, 2) }

        /**
         * LESSON CHANGES
         */
        if (!isAdded)
            return@launch

        val lessons = withContext(Dispatchers.Default) { app.db.timetableDao().getChangesNow(app.profileId) }
        val lessonChangeCounters = mutableListOf<LessonChangeCounter>()

        lessons.forEach { lesson ->
            lessonChangeCounters.firstOrNull { it.lessonChangeDate == lesson.displayDate }?.let {
                it.lessonChangeCount += 1
            } ?: run {
                lessonChangeCounters.add(LessonChangeCounter(
                        lesson.displayDate ?: return@forEach,
                        1
                ))
            }
        }

        lessonChangeCounters.forEach { counter ->
            eventList.add(LessonChangeEvent(
                    counter.lessonChangeDate.inMillis,
                    0xff78909c.toInt(),
                    Colors.legibleTextColor(0xff78909c.toInt()),
                    counter.startTime,
                    counter.endTime,
                    app.profileId,
                    counter.lessonChangeDate,
                    counter.lessonChangeCount
            ))
        }

        /**
         * TEACHER ABSENCES
         */
        if (!isAdded)
            return@launch

        val showTeacherAbsences = app.profile.getStudentData("showTeacherAbsences", true)

        if (showTeacherAbsences) {
            val teacherAbsenceList = withContext(Dispatchers.Default) { app.db.teacherAbsenceDao().getAllNow(app.profileId) }
            val teacherAbsenceCounters = mutableListOf<TeacherAbsenceCounter>()

            teacherAbsenceList.forEach { absence ->
                val date = absence.dateFrom.clone()

                while (date <= absence.dateTo) {
                    teacherAbsenceCounters.firstOrNull { it.teacherAbsenceDate == date }?.let {
                        it.teacherAbsenceCount += 1
                    } ?: run {
                        teacherAbsenceCounters.add(TeacherAbsenceCounter(date.clone(), 1))
                    }

                    date.stepForward(0, 0, 1)
                }
            }

            teacherAbsenceCounters.forEach { counter ->
                eventList.add(TeacherAbsenceEvent(
                        counter.teacherAbsenceDate.inMillis,
                        0xffff1744.toInt(),
                        Colors.legibleTextColor(0xffff1744.toInt()),
                        counter.startTime,
                        counter.endTime,
                        app.profileId,
                        counter.teacherAbsenceDate,
                        counter.teacherAbsenceCount
                ))
            }
        }

        /**
         * EVENTS
         */
        if (!isAdded)
            return@launch

        val events = withContext(Dispatchers.Default) { app.db.eventDao().getAllNow(app.profileId) }
        val unreadEventDates = mutableSetOf<Int>()

        events.forEach { event ->
            eventList.add(BaseCalendarEvent(
                    "${event.typeName ?: "wydarzenie"} - ${event.topic}",
                    "",
                    (if (event.time == null) getString(R.string.agenda_event_all_day) else event.time!!.stringHM) +
                            (event.subjectLongName?.let { ", $it" } ?: "") +
                            (event.teacherName?.let { ", $it" } ?: "") +
                            (event.teamName?.let { ", $it" } ?: ""),
                    event.eventColor,
                    Colors.legibleTextColor(event.eventColor),
                    event.startTimeCalendar,
                    event.endTimeCalendar,
                    event.time == null,
                    event.id,
                    !event.seen
            ))

            if (!event.seen) unreadEventDates.add(event.date.value)
        }

        b.agendaDefaultView.init(eventList, minDate, maxDate, Locale.getDefault(), object : CalendarPickerController {
            override fun onDaySelected(dayItem: IDayItem?) {}

            override fun onScrollToDate(calendar: Calendar) { this@AgendaFragment.launch {
                val date = Date.fromCalendar(calendar)
                actualDate = date

                // Mark as read scrolled date
                if (date.value in unreadEventDates) {
                    withContext(Dispatchers.Default) { app.db.eventDao().setSeenByDate(app.profileId, date, true) }
                    unreadEventDates.remove(date.value)
                }
            }}

            override fun onEventSelected(event: CalendarEvent) {
                val date = Date.fromCalendar(event.instanceDay)

                when (event) {
                    is BaseCalendarEvent -> DayDialog(activity, app.profileId, date)
                    is LessonChangeEvent -> LessonChangeDialog(activity, app.profileId, date)
                    is TeacherAbsenceEvent -> TeacherAbsenceDialog(activity, app.profileId, date)
                }
            }

        }, LessonChangeEventRenderer(), TeacherAbsenceEventRenderer())

        b.progressBar.visibility = View.GONE
    }}}

    private fun createCalendarAgendaView() { (b as? FragmentAgendaCalendarBinding)?.let { b -> launch {
        delay(300)

        val dayList = mutableListOf<EventDay>()

        val events = withContext(Dispatchers.Default) { app.db.eventDao().getAllNow(app.profileId) }
        val unreadEventDates = mutableSetOf<Int>()

        events.forEach { event ->
            val eventIcon = IconicsDrawable(activity).apply {
                icon = CommunityMaterial.Icon.cmd_checkbox_blank_circle
                sizeDp = 10
                colorInt = event.eventColor
            }

            dayList.add(EventDay(event.startTimeCalendar, eventIcon))

            if (!event.seen) unreadEventDates.add(event.date.value)
        }

        b.agendaCalendarView.setEvents(dayList)
        b.agendaCalendarView.setOnDayClickListener { day -> this@AgendaFragment.launch {
            val date = Date.fromCalendar(day.calendar)

            if (date.value in unreadEventDates) {
                withContext(Dispatchers.Default) { app.db.eventDao().setSeenByDate(app.profileId, date, true) }
                unreadEventDates.remove(date.value)
            }

            DayDialog(activity, app.profileId, date)
        }}

        b.progressBar.visibility = View.GONE
    }}}
}
