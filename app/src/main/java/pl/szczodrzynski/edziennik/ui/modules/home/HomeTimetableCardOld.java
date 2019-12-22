package pl.szczodrzynski.edziennik.ui.modules.home;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.db.modules.events.EventFull;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonFull;
import pl.szczodrzynski.edziennik.databinding.CardTimetableBinding;
import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.models.Time;
import pl.szczodrzynski.edziennik.utils.models.Week;

import static pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange.TYPE_CANCELLED;
import static pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange.TYPE_CHANGE;
import static pl.szczodrzynski.edziennik.ui.modules.home.HomeFragmentOld.updateInterval;
import static pl.szczodrzynski.edziennik.utils.Utils.bs;

public class HomeTimetableCardOld {
    private static final String TAG = "HomeTimetableCardOld";
    private App app;
    private MainActivity a;
    private HomeFragmentOld f;
    private LayoutInflater layoutInflater;
    private ViewGroup insertPoint;
    private CardTimetableBinding b;
    private Timer timetableTimer;
    private Time bellSyncTime = null;

    public HomeTimetableCardOld(App app, MainActivity a, HomeFragmentOld f, LayoutInflater layoutInflater, ViewGroup insertPoint) {
        this.app = app;
        this.a = a;
        this.f = f;
        this.layoutInflater = layoutInflater;
        this.insertPoint = insertPoint;
    }

    public void run() {
        timetableTimer = new Timer();
        b = DataBindingUtil.inflate(layoutInflater, R.layout.card_timetable, null, false);
        update();
        insertPoint.addView(b.getRoot(), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        b.cardTimetableFullscreenCounter.setOnClickListener((v -> {
            Intent intent = new Intent(a, CounterActivityOld.class);
            a.startActivity(intent);
        }));

        b.cardTimetableBellSync.setOnClickListener(v -> {
            if (bellSyncTime == null) {
                new MaterialDialog.Builder(a)
                        .title(R.string.bell_sync_title)
                        .content(R.string.bell_sync_cannot_now)
                        .positiveText(R.string.ok)
                        .show();
            }
            else {
                new MaterialDialog.Builder(a)
                        .title(R.string.bell_sync_title)
                        .content(app.getString(R.string.bell_sync_howto, bellSyncTime.getStringHM())+
                                (app.config.getTimetable().getBellSyncDiff() != null ?
                                        ""+app.getString(R.string.bell_sync_current_dialog, (app.config.getTimetable().getBellSyncMultiplier() == -1 ? "-" : "+")+app.config.getTimetable().getBellSyncDiff().getStringHMS())
                                        : ""))
                        .positiveText(R.string.ok)
                        .negativeText(R.string.cancel)
                        .neutralText(R.string.reset)
                        .onPositive((dialog, which) -> {
                            Time bellDiff = Time.diff(Time.getNow(), bellSyncTime);
                            app.config.getTimetable().setBellSyncDiff(bellDiff);
                            app.config.getTimetable().setBellSyncMultiplier(bellSyncTime.getValue() > Time.getNow().getValue() ? -1 : 1);
                            new MaterialDialog.Builder(a)
                                    .title(R.string.bell_sync_title)
                                    .content(app.getString(R.string.bell_sync_results, (bellSyncTime.getValue() > Time.getNow().getValue() ? "-" : "+"), bellDiff.getStringHMS()))
                                    .positiveText(R.string.ok)
                                    .show();
                        })
                        .onNeutral((dialog, which) -> {
                            new MaterialDialog.Builder(a)
                                    .title(R.string.bell_sync_title)
                                    .content(R.string.bell_sync_reset_confirm)
                                    .positiveText(R.string.yes)
                                    .negativeText(R.string.no)
                                    .onPositive(((dialog1, which1) -> {
                                        app.config.getTimetable().setBellSyncDiff(null);
                                        app.config.getTimetable().setBellSyncMultiplier(0);
                                    }))
                                    .show();
                        })
                        .show();
            }
        });

        HomeFragmentOld.buttonAddDrawable(a, b.cardTimetableButton, CommunityMaterial.Icon.cmd_arrow_right);
    }

    private List<LessonFull> lessons = new ArrayList<>();
    private List<EventFull> events = new ArrayList<>();

    private void update() {
        //Log.d(TAG, "Now "+System.currentTimeMillis());
        if (a == null || !f.isAdded())
            return;
        // BELL SYNCING
        Time now = Time.getNow();
        Time syncedNow = now;
        //Time updateDiff = null;
        if (app.config.getTimetable().getBellSyncDiff() != null) {
            if (app.config.getTimetable().getBellSyncMultiplier() < 0) {
                // the bell is too fast, need to step further to go with it
                // add some time
                syncedNow = Time.sum(now, app.config.getTimetable().getBellSyncDiff());
                //Toast.makeText(c, "Bell sync diff is "+app.appConfig.bellSyncDiff.getStringHMS()+"\n\n  Synced now is "+syncedNow.getStringHMS(), Toast.LENGTH_LONG).show();
            }
            if (app.config.getTimetable().getBellSyncMultiplier() > 0) {
                // the bell is delayed, need to roll the "now" time back
                // subtract some time
                syncedNow = Time.diff(now, app.config.getTimetable().getBellSyncDiff());
            }
        }

        assert counterTarget != null;
        if (lessons.size() == 0 || syncedNow.getValue() > counterTarget.getValue()) {
            findLessons(syncedNow);
        }
        else {
            scheduleUpdate(updateCounter(syncedNow));
        }
    }

    private void scheduleUpdate(long newRefreshInterval) {
        try {
            timetableTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    a.runOnUiThread(() -> update());
                }
            }, newRefreshInterval);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void findLessons(Time syncedNow) {
        AsyncTask.execute(() -> {
            Date today = Date.getToday();
            lessons = app.db.lessonDao().getAllNearestNow(App.profileId, today.clone().stepForward(0, 0, -today.getWeekDay()), today, syncedNow);

            if (lessons != null && lessons.size() != 0) {
                Date displayingDate = lessons.get(0).lessonDate;
                if (displayingDate == null) {
                    a.runOnUiThread(() -> scheduleUpdate(updateViews(null, syncedNow, 0, 0)));
                    return;
                }
                int displayingWeekDay = displayingDate.getWeekDay();

                Log.d(TAG, "Displaying date is "+displayingDate.getStringY_m_d()+", weekDay is "+displayingWeekDay);

                int notPassedIndex = -1;
                int notPassedWeekDay = -1;
                //int firstIndex = -1;
                int lastIndex = -1;
                int index = 0;
                for (LessonFull lesson: lessons) {
                    if (notPassedIndex == -1 && !lesson.lessonPassed) {
                        if (lesson.lessonDate != null)
                            displayingDate = lesson.lessonDate;
                        displayingWeekDay = lesson.weekDay;
                        notPassedIndex = index;
                        notPassedWeekDay = lesson.weekDay;
                    }
                    if (lesson.weekDay == notPassedWeekDay) {
                        /*if (firstIndex == -1)
                            firstIndex = index;*/
                        lastIndex = index;
                    }

                    index++;
                }

                // for safety
                /*if (firstIndex == -1)
                    firstIndex++;*/
                if (notPassedIndex == -1)
                    notPassedIndex++;
                if (lastIndex == -1)
                    lastIndex++;

                Log.d(TAG, "Not passed index is "+notPassedIndex);
                Log.d(TAG, "Last index is "+lastIndex);
                Log.d(TAG, "New Displaying date is "+displayingDate.getStringY_m_d()+", weekDay is "+displayingWeekDay);
                events = app.db.eventDao().getAllByDateNow(App.profileId, displayingDate);

                Date finalDisplayingDate = displayingDate;
                int finalNotPassedIndex = notPassedIndex;
                int finalLastIndex = lastIndex;
                a.runOnUiThread(() -> scheduleUpdate(updateViews(finalDisplayingDate, syncedNow, finalNotPassedIndex, finalLastIndex)));
            }
            else {
                a.runOnUiThread(() -> scheduleUpdate(updateViews(null, syncedNow, 0, 0)));
            }

        });
    }

    private Time counterTarget = new Time(0, 0, 0);
    private static final short TIME_TILL = 0;
    private static final short TIME_LEFT = 1;
    private short counterType = TIME_TILL;
    private long updateCounter(Time syncedNow) {
        Time diff = Time.diff(counterTarget, syncedNow);
        b.cardTimetableTimeLeft.setText(counterType == TIME_TILL ? HomeFragmentOld.timeTill(app, diff, app.config.getTimetable().getCountInSeconds()) : HomeFragmentOld.timeLeft(app, diff, app.config.getTimetable().getCountInSeconds()));
        bellSyncTime = counterTarget;
        b.cardTimetableFullscreenCounter.setVisibility(View.VISIBLE);
        return updateInterval(app, diff);
    }

    private long updateViews(Date displayingDate, Time syncedNow, int notPassedIndex, int lastIndex) {
        //Date weekEnd = Date.getToday().stepForward(0, 0, 6 - Week.getTodayWeekDay()); // Sunday of the current week

        if (displayingDate == null) {
            b.cardTimetableTitle.setText(R.string.card_timetable_no_timetable);
            b.cardTimetableNoData.setVisibility(View.VISIBLE);
            b.cardTimetableContent.setVisibility(View.GONE);
            return 1000*60*30;
        }

        // at this point lessons.size() must be greater than 0

        int displayingWeekDay = displayingDate.getWeekDay();



        String weekDayStr = Week.getFullDayName(displayingWeekDay);
        int dayDiff = Date.diffDays(displayingDate, Date.getToday());
        if (displayingDate.getValue() != Date.getToday().getValue() && dayDiff == 0) {
            dayDiff++;
        }
        String dayDiffStr = (dayDiff == 0 ? app.getString(R.string.day_today_format, weekDayStr) : (dayDiff == 1 ? app.getString(R.string.day_tomorrow_format,weekDayStr) : app.getString(R.string.day_other_format, weekDayStr, displayingDate.getStringDm())));

        b.cardTimetableTitle.setText(app.getString(R.string.card_timetable_title, dayDiffStr));

        long newRefreshInterval = 1000*5; // 5 seconds

        b.cardTimetableNoData.setVisibility(View.GONE);
        b.cardTimetableContent.setVisibility(View.VISIBLE);

        LessonFull lessonFirst = lessons.get(dayDiff == 0 ? 0 : notPassedIndex);
        // should never be out of range
        LessonFull lessonLast = lessons.get(lastIndex);

        boolean duringLessons = Time.inRange(lessonFirst.startTime, lessonLast.endTime, syncedNow) && dayDiff == 0;
        if (!duringLessons) {

            LessonFull lessonSecond = null;
            if (lessons.size() > notPassedIndex+1 && lessons.get(notPassedIndex+1).weekDay == displayingDate.getWeekDay())
                lessonSecond = lessons.get(notPassedIndex+1);

            b.cardTimetableType.setText(R.string.card_timetable_lesson_duration);
            b.cardTimetableSummary.setText(app.getString(R.string.card_timetable_lesson_duration_format, lessonFirst.startTime.getStringHM(), lessonLast.endTime.getStringHM()));
            if (dayDiff == 0) {
                counterTarget = lessonFirst.startTime;
                counterType = TIME_TILL;
                newRefreshInterval = updateCounter(syncedNow);
            }
            else {
                b.cardTimetableTimeLeft.setText("");
                newRefreshInterval = 1000*60*30;
                b.cardTimetableFullscreenCounter.setVisibility(View.GONE);
            }

            String lessonFirstStr = null;
            if (lessonFirst.changeId != 0) {
                if (lessonFirst.changeType == TYPE_CANCELLED) {
                    lessonFirstStr = "<del>" + bs(lessonFirst.getSubjectLongName()) + "</del>";
                } else if (lessonFirst.changeType == TYPE_CHANGE) {
                    lessonFirstStr = "<i>" + bs(lessonFirst.getSubjectLongName()) + "</i>";
                }
            } else {
                lessonFirstStr = bs(lessonFirst.subjectLongName);
            }
            String lessonSecondStr = null;
            if (lessonSecond != null) {
                if (lessonSecond.changeId != 0) {
                    if (lessonSecond.changeType == TYPE_CANCELLED) {
                        lessonSecondStr = "<del>" + bs(lessonSecond.getSubjectLongName()) + "</del>";
                    } else if (lessonSecond.changeType == TYPE_CHANGE) {
                        lessonSecondStr = "<i>" + bs(lessonSecond.getSubjectLongName()) + "</i>";
                    }
                } else {
                    lessonSecondStr = bs(lessonSecond.getSubjectLongName());
                }
            }
            b.cardTimetableLessonOverview.setText(
                    Html.fromHtml(
                            app.getString(R.string.card_timetable_lesson_overview,
                                    lessonFirst.startTime.getStringHM(),
                                    bs(null, lessonFirstStr, ", ")+lessonFirst.getClassroomName(),
                                    (lessonSecond == null ? "" : lessonSecond.startTime.getStringHM()),
                                    (lessonSecond == null ? "" : bs(null, lessonSecondStr, ", "+lessonSecond.getClassroomName())))
                    )
            );

            StringBuilder eventSummary = new StringBuilder();
            eventSummary.append(app.getString(R.string.card_timetable_event_overview));
            for (EventFull event : events) {
                if (displayingDate.getValue() == event.eventDate.getValue()) {
                    eventSummary.append(app.getString(
                            R.string.card_timetable_event_overview_format,
                            event.startTime == null ? app.getString(R.string.event_all_day) : event.startTime.getStringHM(),
                            event.typeName,
                            event.topic));
                }
            }
            b.cardTimetableEventOverview.setText(eventSummary.toString());
        }
        else {
            b.cardTimetableFullscreenCounter.setVisibility(View.VISIBLE);
            LessonFull lessonCurrent = null;
            LessonFull lessonInAMoment = null;
            LessonFull lessonNext = null;
            LessonFull lessonAfterNext = null;

            if (lessons.get(notPassedIndex).lessonCurrent) {
                lessonCurrent = lessons.get(notPassedIndex);
                if (lessons.size() > notPassedIndex+1 && lessons.get(notPassedIndex+1).weekDay == displayingDate.getWeekDay())
                    lessonNext = lessons.get(notPassedIndex+1);
                if (lessons.size() > notPassedIndex+2 && lessons.get(notPassedIndex+2).weekDay == displayingDate.getWeekDay())
                    lessonAfterNext = lessons.get(notPassedIndex+2);
            }
            else {
                lessonInAMoment = lessons.get(notPassedIndex);
                if (lessons.size() > notPassedIndex+1 && lessons.get(notPassedIndex+1).weekDay == displayingDate.getWeekDay())
                    lessonNext = lessons.get(notPassedIndex+1);
                if (lessons.size() > notPassedIndex+2 && lessons.get(notPassedIndex+2).weekDay == displayingDate.getWeekDay())
                    lessonAfterNext = lessons.get(notPassedIndex+2);
            }

            if (lessonCurrent != null) { // show time to the end of this lesson
                b.cardTimetableType.setText(R.string.card_timetable_now);
                if (lessonCurrent.changeId != 0) {
                    if (lessonCurrent.changeType == TYPE_CANCELLED) {
                        b.cardTimetableSummary.setText(Html.fromHtml("<del>"+bs(lessonCurrent.getSubjectLongName())+"</del><br>"+lessonCurrent.getClassroomName()));
                    }
                    else if (lessonCurrent.changeType == TYPE_CHANGE) {
                        b.cardTimetableSummary.setText(Html.fromHtml("<i>"+bs(lessonCurrent.getSubjectLongName())+"<br>"+lessonCurrent.getClassroomName()+"</i>"));
                    }
                }
                else {
                    b.cardTimetableSummary.setText(bs(lessonCurrent.getSubjectLongName())+"\n"+lessonCurrent.getClassroomName());
                }

                counterTarget = lessonCurrent.endTime;
                counterType = TIME_LEFT;
                newRefreshInterval = updateCounter(syncedNow);
            }
            else if (lessonInAMoment != null) { // it's break time, show time to the start of next lesson
                b.cardTimetableType.setText(R.string.card_timetable_following);
                if (lessonInAMoment.changeId != 0) {
                    if (lessonInAMoment.changeType == TYPE_CANCELLED) {
                        b.cardTimetableSummary.setText(Html.fromHtml("<del>"+bs(lessonInAMoment.getSubjectLongName())+"</del><br>"+lessonInAMoment.getClassroomName()));
                    }
                    else if (lessonInAMoment.changeType == TYPE_CHANGE) {
                        b.cardTimetableSummary.setText(Html.fromHtml("<i>"+bs(lessonInAMoment.getSubjectLongName())+"<br>"+lessonInAMoment.getClassroomName()+"</i>"));
                    }
                }
                else {
                    b.cardTimetableSummary.setText(bs(lessonInAMoment.getSubjectLongName())+"\n"+lessonInAMoment.getClassroomName());
                }

                counterTarget = lessonInAMoment.startTime;
                counterType = TIME_TILL;
                newRefreshInterval = updateCounter(syncedNow);
            }
            else { // idk what it is now (during lessons, but not during lesson or a break)
                b.cardTimetableType.setText(R.string.card_timetable_wtf);
                b.cardTimetableSummary.setText(R.string.card_timetable_wtf_report);
                b.cardTimetableTimeLeft.setText("");
                newRefreshInterval = 1000*60*2;
            }
            String lessonNextStr = null;
            if (lessonNext != null) {
                if (lessonNext.changeId != 0) {
                    if (lessonNext.changeType == TYPE_CANCELLED) {
                        lessonNextStr = "<del>" + lessonNext.getSubjectLongName() + "</del>";
                    } else if (lessonNext.changeType == TYPE_CHANGE) {
                        lessonNextStr = "<i>" + lessonNext.getSubjectLongName() + "</i>";
                    }
                } else {
                    lessonNextStr = lessonNext.getSubjectLongName();
                }
            }
            String lessonAfterNextStr = null;
            if (lessonAfterNext != null) {
                if (lessonAfterNext.changeId != 0) {
                    if (lessonAfterNext.changeType == TYPE_CANCELLED) {
                        lessonAfterNextStr = "<del>" + lessonAfterNext.getSubjectLongName() + "</del>";
                    } else if (lessonAfterNext.changeType == TYPE_CHANGE) {
                        lessonAfterNextStr = "<i>" + lessonAfterNext.getSubjectLongName() + "</i>";
                    }
                } else {
                    lessonAfterNextStr = lessonAfterNext.getSubjectLongName();
                }
            }
            b.cardTimetableLessonOverview.setText(
                    Html.fromHtml(app.getString(
                            R.string.card_timetable_lesson_overview_ongoing,
                            (lessonNext == null ? "" : lessonNext.startTime.getStringHM()),
                            (lessonNext == null ? "" : bs(null, lessonNextStr, ", "+lessonNext.getClassroomName())),
                            (lessonAfterNext == null ? "" : lessonAfterNext.startTime.getStringHM()),
                            (lessonAfterNext == null ? "" : bs(null, lessonAfterNextStr, ", "+lessonAfterNext.getClassroomName()))
                    ))
            );

            StringBuilder eventSummary = new StringBuilder();
            eventSummary.append(app.getString(R.string.card_timetable_event_overview));
            for (EventFull event : events) {
                // display the event only if it's AllDay or it hasn't started yet
                boolean timeMatching = event.startTime == null || syncedNow.getValue() < event.startTime.getValue();
                if (displayingDate.getValue() == event.eventDate.getValue()
                        && timeMatching) {
                    eventSummary.append(app.getString(
                            R.string.card_timetable_event_overview_format,
                            event.startTime == null ? app.getString(R.string.agenda_event_all_day)+" - " : event.startTime.getStringHM(),
                            event.typeName,
                            event.topic));
                }
            }
            b.cardTimetableEventOverview.setText(eventSummary.toString());
        }

        b.cardTimetableButton.setOnClickListener((v1 -> new Handler().postDelayed(() -> a.runOnUiThread(() -> {
            Bundle arguments = new Bundle();
            arguments.putInt("timetableDate", displayingDate.getValue());
            a.loadTarget(MainActivity.DRAWER_ITEM_TIMETABLE, arguments);
        }), 100)));

        return newRefreshInterval;

        //new Handler().postDelayed(() -> a.runOnUiThread(() -> updateCardTimetable(c, a, root)), newRefreshInterval);
    }

    void destroy() {
        //Log.d(TAG, "OnDestroy");
        try {
            timetableTimer.cancel();
            timetableTimer.purge();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
