package pl.szczodrzynski.edziennik.ui.modules.agenda;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.github.tibolte.agendacalendarview.AgendaCalendarView;
import com.github.tibolte.agendacalendarview.CalendarPickerController;
import com.github.tibolte.agendacalendarview.models.BaseCalendarEvent;
import com.github.tibolte.agendacalendarview.models.CalendarEvent;
import com.github.tibolte.agendacalendarview.models.IDayItem;
import com.mikepenz.iconics.IconicsColor;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSize;
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.data.db.modules.teachers.TeacherAbsenceFull;
import pl.szczodrzynski.edziennik.databinding.FragmentAgendaCalendarBinding;
import pl.szczodrzynski.edziennik.databinding.FragmentAgendaDefaultBinding;
import pl.szczodrzynski.edziennik.data.db.modules.events.EventFull;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonFull;
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventListDialog;
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventManualDialog;
import pl.szczodrzynski.edziennik.ui.dialogs.lessonchange.LessonChangeDialog;
import pl.szczodrzynski.edziennik.ui.dialogs.teacherabsence.TeacherAbsenceDialog;
import pl.szczodrzynski.edziennik.ui.modules.agenda.lessonchange.LessonChangeCounter;
import pl.szczodrzynski.edziennik.ui.modules.agenda.lessonchange.LessonChangeEvent;
import pl.szczodrzynski.edziennik.ui.modules.agenda.lessonchange.LessonChangeEventRenderer;
import pl.szczodrzynski.edziennik.ui.modules.agenda.teacherabsence.TeacherAbsenceCounter;
import pl.szczodrzynski.edziennik.ui.modules.agenda.teacherabsence.TeacherAbsenceEvent;
import pl.szczodrzynski.edziennik.ui.modules.agenda.teacherabsence.TeacherAbsenceEventRenderer;
import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.models.Time;
import pl.szczodrzynski.edziennik.utils.Colors;
import pl.szczodrzynski.edziennik.utils.Themes;
import pl.szczodrzynski.edziennik.utils.Utils;
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem;
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem;

import static pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata.TYPE_EVENT;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.AGENDA_CALENDAR;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.AGENDA_DEFAULT;
import static pl.szczodrzynski.edziennik.utils.Utils.intToStr;

public class AgendaFragment extends Fragment {

    private App app = null;
    private MainActivity activity = null;
    private FragmentAgendaDefaultBinding b_default = null;
    private FragmentAgendaCalendarBinding b_calendar = null;
    private int viewType = AGENDA_DEFAULT;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        if (getActivity() == null || getContext() == null)
            return null;
        app = (App) activity.getApplication();
        getContext().getTheme().applyStyle(Themes.INSTANCE.getAppTheme(), true);
        if (app.profile == null)
            return inflater.inflate(R.layout.fragment_loading, container, false);
        // activity, context and profile is valid
        viewType = app.profile.getAgendaViewType();
        if (viewType == AGENDA_DEFAULT) {
            b_default = DataBindingUtil.inflate(inflater, R.layout.fragment_agenda_default, container, false);
            return b_default.getRoot();
        }
        else {
            b_calendar = DataBindingUtil.inflate(inflater, R.layout.fragment_agenda_calendar, container, false);
            return b_calendar.getRoot();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (app == null || app.profile == null || activity == null || (b_default == null && b_calendar == null) || !isAdded())
            return;

        activity.getBottomSheet().prependItems(
                new BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_add_event)
                        .withDescription(R.string.menu_add_event_desc)
                        .withIcon(CommunityMaterial.Icon.cmd_calendar_plus)
                        .withOnClickListener(v3 -> {
                            activity.getBottomSheet().close();
                            new MaterialDialog.Builder(activity)
                                    .title(R.string.main_menu_add)
                                    .items(R.array.main_menu_add_options)
                                    .itemsCallback((dialog, itemView, position, text) -> {
                                        switch (position) {
                                            case 0:
                                                new EventManualDialog(activity).show(app, null, null, null, EventManualDialog.DIALOG_EVENT);
                                                break;
                                            case 1:
                                                new EventManualDialog(activity).show(app, null, null, null, EventManualDialog.DIALOG_HOMEWORK);
                                                break;
                                        }
                                    })
                                    .show();
                        }),
                new BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_agenda_change_view)
                        .withIcon(viewType == AGENDA_DEFAULT ? CommunityMaterial.Icon.cmd_calendar : CommunityMaterial.Icon2.cmd_view_list)
                        .withOnClickListener(v3 -> {
                            activity.getBottomSheet().close();
                            viewType = viewType == AGENDA_DEFAULT ? AGENDA_CALENDAR : AGENDA_DEFAULT;
                            app.profile.setAgendaViewType(viewType);
                            app.profileSaveAsync();
                            activity.reloadTarget();
                        }),
                new BottomSheetSeparatorItem(true),
                new BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_mark_as_read)
                        .withIcon(CommunityMaterial.Icon.cmd_eye_check)
                        .withOnClickListener(v3 -> {
                            activity.getBottomSheet().close();
                            AsyncTask.execute(() -> app.db.metadataDao().setAllSeen(App.profileId, TYPE_EVENT, true));
                            Toast.makeText(activity, R.string.main_menu_mark_as_read_success, Toast.LENGTH_SHORT).show();
                        })
        );
        activity.gainAttention();

        if (viewType == AGENDA_DEFAULT) {
            createDefaultAgendaView();
        }
        else {
            createCalendarAgendaView();
        }
    }

    private void createDefaultAgendaView() {
        List<Integer> unreadEventDates = new ArrayList<>();

        final Handler handler = new Handler();
        handler.postDelayed(() -> AsyncTask.execute(() -> {
            if (app == null || app.profile == null || activity == null || b_default == null || !isAdded())
                return;

            List<CalendarEvent> eventList = new ArrayList<>();

            List<LessonChangeCounter> lessonChangeCounters = app.db.lessonChangeDao().getLessonChangeCountersNow(App.profileId);
            for (LessonChangeCounter counter : lessonChangeCounters) {
                Calendar startTime = Calendar.getInstance();
                Calendar endTime = Calendar.getInstance();
                if (counter.lessonChangeDate == null) {
                    continue;
                }
                startTime.set(counter.lessonChangeDate.year, counter.lessonChangeDate.month - 1, counter.lessonChangeDate.day, 10, 0, 0);
                endTime.setTimeInMillis(startTime.getTimeInMillis() + (1000 * 60 * 45));
                eventList.add(new LessonChangeEvent(
                        counter.lessonChangeDate.getInMillis(),
                        0xff78909c,
                        Colors.legibleTextColor(0xff78909c),
                        startTime,
                        endTime,
                        counter.profileId,
                        counter.lessonChangeDate,
                        counter.lessonChangeCount
                ));
            }

            if (app.profile.getStudentData("showTeacherAbsences", true)) {
                List<TeacherAbsenceFull> teacherAbsenceList = app.db.teacherAbsenceDao().getAllFullNow(App.profileId);
                List<TeacherAbsenceCounter> teacherAbsenceCounters = new ArrayList<>();

                for (TeacherAbsenceFull absence : teacherAbsenceList) {
                    for (Date date = absence.getDateFrom().clone(); date.compareTo(absence.getDateTo()) < 1; date.stepForward(0, 0, 1)) {
                        boolean counterFound = false;
                        for (TeacherAbsenceCounter counter : teacherAbsenceCounters) {
                            if (counter.getTeacherAbsenceDate().compareTo(date) == 0) {
                                counter.setTeacherAbsenceCount(counter.getTeacherAbsenceCount() + 1);
                                counterFound = true;
                                break;
                            }
                        }

                        if (!counterFound) {
                            teacherAbsenceCounters.add(new TeacherAbsenceCounter(date.clone(), 1));
                        }
                    }
                }

                for (TeacherAbsenceCounter counter : teacherAbsenceCounters) {
                    Calendar startTime = Calendar.getInstance();
                    Calendar endTime = Calendar.getInstance();
                    Date date = counter.getTeacherAbsenceDate();
                    startTime.set(date.year, date.month - 1, date.day, 10, 0, 0);
                    endTime.setTimeInMillis(startTime.getTimeInMillis() + (1000 * 60 * 45));
                    eventList.add(new TeacherAbsenceEvent(
                            date.getInMillis(),
                            0xffff1744,
                            Colors.legibleTextColor(0xffff1744),
                            startTime,
                            endTime,
                            App.profileId,
                            date,
                            counter.getTeacherAbsenceCount()
                    ));
                }
            }


            List<EventFull> events = app.db.eventDao().getAllNow(App.profileId);
            for (EventFull event : events) {
                Calendar startTime = Calendar.getInstance();
                Calendar endTime = Calendar.getInstance();
                if (event.eventDate == null)
                    continue;
                startTime.set(
                        event.eventDate.year,
                        event.eventDate.month - 1,
                        event.eventDate.day,
                        event.startTime == null ? 0 : event.startTime.hour,
                        event.startTime == null ? 0 : event.startTime.minute,
                        event.startTime == null ? 0 : event.startTime.second
                );
                endTime.setTimeInMillis(startTime.getTimeInMillis() + (1000 * 60 * 45));
                eventList.add(new BaseCalendarEvent(event.typeName + " - " + event.topic,
                        "",
                        (event.startTime == null ? getString(R.string.agenda_event_all_day) : event.startTime.getStringHM()) +
                                Utils.bs(", ", event.subjectLongName) +
                                Utils.bs(", ", event.teacherFullName) +
                                Utils.bs(", ", event.teamName),
                        event.getColor(),
                        Colors.legibleTextColor(event.getColor()),
                        startTime,
                        endTime,
                        event.startTime == null,
                        event.id, !event.seen));
                if (!event.seen) {
                    unreadEventDates.add(event.eventDate.getValue());
                }
            }

            /*List<LessonFull> lessonChanges = app.db.lessonChangeDao().getAllChangesWithLessonsNow(App.profileId);
            for (LessonFull lesson: lessonChanges) {
                Calendar startTime = Calendar.getInstance();
                Calendar endTime = Calendar.getInstance();
                if (lesson.lessonDate == null) {
                    continue;
                }
                startTime.set(lesson.lessonDate.year, lesson.lessonDate.month - 1, lesson.lessonDate.day, lesson.startTime.hour, lesson.startTime.minute, lesson.startTime.second);
                endTime.setTimeInMillis(startTime.getTimeInMillis() + (1000 * 60 * 45));
                String description = lesson.changeTypeStr(activity);
                if (lesson.changeType != TYPE_CANCELLED) {
                    if (lesson.subjectId != lesson.changeSubjectId && lesson.teacherId != lesson.changeTeacherId) {
                        description += " -> " + bs(null, lesson.changeSubjectLongName, ", ") + bs(lesson.changeTeacherFullName);
                    } else if (lesson.subjectId != lesson.changeSubjectId) {
                        description += " -> " + bs(lesson.changeSubjectLongName);
                    } else if (lesson.teacherId != lesson.changeTeacherId) {
                        description += " -> " + bs(lesson.changeTeacherFullName);
                    }
                }
                eventList.add(new BaseCalendarEvent(description,
                        "",
                        (lesson.startTime.getStringHM()) +
                                Utils.bs(", ", lesson.subjectLongName) +
                                Utils.bs(", ", lesson.teacherFullName) +
                                Utils.bs(", ", lesson.teamName),
                        0xff78909c,
                        Colors.legibleTextColor(0xff78909c),
                        startTime,
                        endTime,
                        false,
                        (int)lesson.changeId, false));
            }*/

            activity.runOnUiThread(() -> {
                AgendaCalendarView mAgendaCalendarView = b_default.agendaDefaultView;
                // minimum and maximum date of our calendar
                // 2 month behind, one year ahead, example: March 2015 <-> May 2015 <-> May 2016
                Calendar minDate = Calendar.getInstance();
                Calendar maxDate = Calendar.getInstance();

                minDate.add(Calendar.MONTH, -2);
                minDate.set(Calendar.DAY_OF_MONTH, 1);
                maxDate.add(Calendar.MONTH, 2);


                mAgendaCalendarView.init(eventList, minDate, maxDate, Locale.getDefault(), new CalendarPickerController() {
                    @Override
                    public void onDaySelected(IDayItem dayItem) {
                    }

                    @Override
                    public void onScrollToDate(Calendar calendar) {
                        int scrolledDate = Date.fromCalendar(calendar).getValue();
                        if (unreadEventDates.contains(scrolledDate)) {
                            AsyncTask.execute(() -> app.db.eventDao().setSeenByDate(App.profileId, Date.fromYmd(intToStr(scrolledDate)), true));
                            unreadEventDates.remove(unreadEventDates.indexOf(scrolledDate));
                        }
                    }

                    @Override
                    public void onEventSelected(CalendarEvent calendarEvent) {
                        if (calendarEvent instanceof BaseCalendarEvent) {
                            if (!calendarEvent.isPlaceholder() && !calendarEvent.isAllDay()) {
                                new EventListDialog(activity).show(app, Date.fromCalendar(calendarEvent.getInstanceDay()), Time.fromMillis(calendarEvent.getStartTime().getTimeInMillis()), true);
                            } else {
                                new EventListDialog(activity).show(app, Date.fromCalendar(calendarEvent.getInstanceDay()));
                            }
                        } else if (calendarEvent instanceof LessonChangeEvent) {
                            new LessonChangeDialog(activity).show(app, Date.fromCalendar(calendarEvent.getInstanceDay()));
                            //Toast.makeText(app, "Clicked "+((LessonChangeEvent) calendarEvent).getLessonChangeDate().getFormattedString(), Toast.LENGTH_SHORT).show();
                        } else if (calendarEvent instanceof TeacherAbsenceEvent) {
                            new TeacherAbsenceDialog(activity).show(app, Date.fromCalendar(calendarEvent.getInstanceDay()));
                        }
                    }
                }, new LessonChangeEventRenderer(), new TeacherAbsenceEventRenderer());
                b_default.progressBar.setVisibility(View.GONE);
            });
        }), 500);
    }

    private void createCalendarAgendaView() {
        List<Integer> unreadEventDates = new ArrayList<>();

        final Handler handler = new Handler();
        handler.postDelayed(() -> AsyncTask.execute(() -> {
            if (app == null || app.profile == null || activity == null || b_calendar == null || !isAdded())
                return;
            Context c = getContext();
            Activity a = getActivity();
            assert c != null;
            assert a != null;
            if (!isAdded()) {
                return;
            }

            List<EventDay> eventList = new ArrayList<>();

            List<EventFull> events = app.db.eventDao().getAllNow(App.profileId);
            for (EventFull event : events) {
                if (event.eventDate == null)
                    continue;
                Calendar startTime = Calendar.getInstance();
                startTime.set(
                        event.eventDate.year,
                        event.eventDate.month - 1,
                        event.eventDate.day,
                        event.startTime == null ? 0 : event.startTime.hour,
                        event.startTime == null ? 0 : event.startTime.minute,
                        event.startTime == null ? 0 : event.startTime.second
                );
                Drawable eventIcon = new IconicsDrawable(activity).icon(CommunityMaterial.Icon.cmd_checkbox_blank_circle).size(IconicsSize.dp(10)).color(IconicsColor.colorInt(event.getColor()));
                eventList.add(new EventDay(startTime, eventIcon));
                if (!event.seen) {
                    unreadEventDates.add(event.eventDate.getValue());
                }
            }

            List<LessonFull> lessonChanges = app.db.lessonChangeDao().getAllChangesWithLessonsNow(App.profileId);

            for (LessonFull lesson: lessonChanges) {
                Calendar startTime = Calendar.getInstance();
                if (lesson.lessonDate == null) {
                    continue;
                }
                startTime.set(
                        lesson.lessonDate.year,
                        lesson.lessonDate.month - 1,
                        lesson.lessonDate.day,
                        lesson.startTime.hour,
                        lesson.startTime.minute,
                        lesson.startTime.second);
                Drawable eventIcon = new IconicsDrawable(activity).icon(CommunityMaterial.Icon.cmd_checkbox_blank_circle).size(IconicsSize.dp(10)).color(IconicsColor.colorInt(0xff78909c));
                eventList.add(new EventDay(startTime, eventIcon));
            }

            getActivity().runOnUiThread(() -> {
                //List<EventDay> eventList = new ArrayList<>();

                //Collections.sort(eventList, new EventListComparator());

                CalendarView calendarView = b_calendar.agendaCalendarView;
                calendarView.setEvents(eventList);
                calendarView.setOnDayClickListener(eventDay -> {
                    Date dayDate = Date.fromCalendar(eventDay.getCalendar());
                    int scrolledDate = dayDate.getValue();
                    if (unreadEventDates.contains(scrolledDate)) {
                        AsyncTask.execute(() -> app.db.eventDao().setSeenByDate(App.profileId, Date.fromYmd(intToStr(scrolledDate)), true));
                        unreadEventDates.remove(unreadEventDates.indexOf(scrolledDate));
                    }

                    new EventListDialog(getContext()).show(app, dayDate);
                });
                b_calendar.progressBar.setVisibility(View.GONE);
            });
        }), 300);
    }

    public static class EventListComparator implements java.util.Comparator<CalendarEvent> {
        @Override
        public int compare(CalendarEvent o1, CalendarEvent o2) {
            return Long.compare(o1.getStartTime().getTimeInMillis(), o2.getStartTime().getTimeInMillis());
            //return (int)(o1.getStartTime().getTimeInMillis() - o2.getStartTime().getTimeInMillis());
        }
    }
}
