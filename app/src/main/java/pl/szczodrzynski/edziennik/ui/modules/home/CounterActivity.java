package pl.szczodrzynski.edziennik.ui.modules.home;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonFull;
import pl.szczodrzynski.edziennik.databinding.ActivityCounterBinding;
import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.models.Time;

import static pl.szczodrzynski.edziennik.ui.modules.home.HomeFragment.updateInterval;

public class CounterActivity extends AppCompatActivity {

    private static final String TAG = "CounterActivity";
    private App app;
    private ActivityCounterBinding b;

    Timer timetableTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (App) getApplication();
        b = DataBindingUtil.inflate(getLayoutInflater(), R.layout.activity_counter, null, false);
        setContentView(b.getRoot());

        timetableTimer = new Timer();

        update();
    }

    private List<LessonFull> lessons = new ArrayList<>();

    private void update() {
        // BELL SYNCING
        Time now = Time.getNow();
        Time syncedNow = now;
        //Time updateDiff = null;
        if (app.appConfig.bellSyncDiff != null) {
            if (app.appConfig.bellSyncMultiplier < 0) {
                // the bell is too fast, need to step further to go with it
                // add some time
                syncedNow = Time.sum(now, app.appConfig.bellSyncDiff);
                //Toast.makeText(c, "Bell sync diff is "+app.appConfig.bellSyncDiff.getStringHMS()+"\n\n  Synced now is "+syncedNow.getStringHMS(), Toast.LENGTH_LONG).show();
            }
            if (app.appConfig.bellSyncMultiplier > 0) {
                // the bell is delayed, need to roll the "now" time back
                // subtract some time
                syncedNow = Time.diff(now, app.appConfig.bellSyncDiff);
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
                    runOnUiThread(() -> update());
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
            lessons = app.db.lessonDao().getAllNearestNow(App.profileId, today.getWeekStart(), today, syncedNow);

            if (lessons != null && lessons.size() != 0) {
                Date displayingDate = lessons.get(0).lessonDate;
                if (displayingDate == null) {
                    runOnUiThread(() -> scheduleUpdate(updateViews(null, syncedNow, 0, 0)));
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

                Date finalDisplayingDate = displayingDate;
                int finalNotPassedIndex = notPassedIndex;
                int finalLastIndex = lastIndex;
                runOnUiThread(() -> scheduleUpdate(updateViews(finalDisplayingDate, syncedNow, finalNotPassedIndex, finalLastIndex)));
            }
            else {
                runOnUiThread(() -> scheduleUpdate(updateViews(null, syncedNow, 0, 0)));
            }

        });
    }

    private Time counterTarget = new Time(0, 0, 0);
    private static final short TIME_TILL = 0;
    private static final short TIME_LEFT = 1;
    private short counterType = TIME_LEFT;
    private long updateCounter(Time syncedNow) {
        Time diff = Time.diff(counterTarget, syncedNow);
        b.timeLeft.setText(counterType == TIME_TILL ? HomeFragment.timeTill(app, diff, app.appConfig.countInSeconds, "\n") : HomeFragment.timeLeft(app, diff, app.appConfig.countInSeconds, "\n"));
        return updateInterval(app, diff);
    }

    private long updateViews(Date displayingDate, Time syncedNow, int notPassedIndex, int lastIndex) {
        long newRefreshInterval = 1000*5;

        if (displayingDate == null) {
            return newRefreshInterval;
        }

        int dayDiff = Date.diffDays(displayingDate, Date.getToday());
        if (displayingDate.getValue() != Date.getToday().getValue() && dayDiff == 0) {
            dayDiff++;
        }

        LessonFull lessonFirst = lessons.get(dayDiff == 0 ? 0 : notPassedIndex);
        // should never be out of range
        LessonFull lessonLast = lessons.get(lastIndex);

        boolean duringLessons = Time.inRange(lessonFirst.startTime, lessonLast.endTime, syncedNow) && dayDiff == 0;
        if (duringLessons) {
            LessonFull lessonCurrent = null;
            LessonFull lessonNext = null;

            if (lessons.get(notPassedIndex).lessonCurrent) {
                lessonCurrent = lessons.get(notPassedIndex);
                if (lessons.size() > notPassedIndex+1 && lessons.get(notPassedIndex+1).weekDay == displayingDate.getWeekDay())
                    lessonNext = lessons.get(notPassedIndex+1);
            }
            else {
                lessonNext = lessons.get(notPassedIndex);
            }

            if (lessonCurrent != null) { // show time to the end of this lesson
                b.lessonName.setText(lessonCurrent.subjectLongName);

                counterType = TIME_LEFT;
                counterTarget = lessonCurrent.endTime;
                newRefreshInterval = updateCounter(syncedNow);
            }
            else if (lessonNext != null) { // it's break time, show time to the start of next lesson
                b.lessonName.setText(R.string.lesson_break);

                counterType = TIME_LEFT;
                counterTarget = lessonNext.startTime;
                newRefreshInterval = updateCounter(syncedNow);
            }
            else { // idk what it is now (during lessons, but not during lesson or a break)
                b.lessonName.setText(R.string.card_timetable_wtf);
                b.timeLeft.setText(R.string.card_timetable_wtf_report);
                newRefreshInterval = 1000*60*2;
                finish();
            }
        }
        else {
            if (syncedNow.getValue() < lessonFirst.startTime.getValue()) {
                // before lessons
                b.lessonName.setText(R.string.lesson_break);

                counterType = TIME_LEFT;
                counterTarget = lessonFirst.startTime;
                newRefreshInterval = updateCounter(syncedNow);
            }
            else {
                finish();
            }
        }

        return newRefreshInterval;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Log.d(TAG, "OnDestroy");
        try {
            timetableTimer.cancel();
            timetableTimer.purge();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
