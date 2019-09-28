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

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.mikepenz.iconics.IconicsColor;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSize;
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.databinding.FragmentRegisterAgendaCalendarBinding;
import pl.szczodrzynski.edziennik.data.db.modules.events.EventFull;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonFull;
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventListDialog;
import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.Themes;

import static pl.szczodrzynski.edziennik.utils.Utils.intToStr;

public class RegisterAgendaCalendarFragment extends Fragment {

    private App app = null;
    private Activity activity = null;
    private FragmentRegisterAgendaCalendarBinding b = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = getActivity();
        if (getActivity() == null || getContext() == null)
            return null;
        app = (App) activity.getApplication();
        getContext().getTheme().applyStyle(Themes.INSTANCE.getAppTheme(), true);
        if (app.profile == null)
            return inflater.inflate(R.layout.fragment_loading, container, false);
        // activity, context and profile is valid
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_register_agenda_calendar, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (app == null || app.profile == null || activity == null || b == null || !isAdded())
            return;

        List<Integer> unreadEventDates = new ArrayList<>();

        final Handler handler = new Handler();
        handler.postDelayed(() -> AsyncTask.execute(() -> {
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

                CalendarView calendarView = b.agendaCalendarView;
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
                b.progressBar.setVisibility(View.GONE);
            });
        }), 300);
    }

}
