package pl.szczodrzynski.edziennik.ui.modules.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.core.graphics.ColorUtils;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mikepenz.iconics.IconicsColor;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSize;
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;

import java.util.ArrayList;
import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.BuildConfig;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.databinding.CardLuckyNumberBinding;
import pl.szczodrzynski.edziennik.databinding.CardUpdateBinding;
import pl.szczodrzynski.edziennik.databinding.FragmentHomeBinding;
import pl.szczodrzynski.edziennik.data.db.modules.grades.GradeFull;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonFull;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile;
import pl.szczodrzynski.edziennik.data.db.modules.subjects.Subject;
import pl.szczodrzynski.edziennik.ui.modules.messages.MessagesComposeActivity;
import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.models.ItemGradesSubjectModel;
import pl.szczodrzynski.edziennik.utils.models.Time;
import pl.szczodrzynski.edziennik.receivers.BootReceiver;
import pl.szczodrzynski.edziennik.utils.Colors;
import pl.szczodrzynski.edziennik.utils.Themes;
import pl.szczodrzynski.edziennik.utils.Utils;
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem;
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem;

import static pl.szczodrzynski.edziennik.App.UPDATES_ON_PLAY_STORE;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_SEMESTER1_FINAL;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_SEMESTER1_PROPOSED;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_SEMESTER2_FINAL;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_SEMESTER2_PROPOSED;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_YEAR_FINAL;
import static pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_YEAR_PROPOSED;
import static pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore.LOGIN_TYPE_MOBIDZIENNIK;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private App app = null;
    private MainActivity activity = null;
    private FragmentHomeBinding b = null;

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
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false);
        b.refreshLayout.setParent(activity.getSwipeRefreshLayout());
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (app == null || app.profile == null || activity == null || b == null || !isAdded())
            return;

        /*b.refreshLayout.setOnRefreshListener(() -> {
            activity.syncCurrentFeature(MainActivity.DRAWER_ITEM_HOME, b.refreshLayout);
        });*/
        /*b.refreshLayout.setOnTouchListener((v, event) -> {
            d(TAG, "event "+event);
            event.setSource(0x10000000); // set a unique source
            activity.swipeRefreshLayout.onTouchEvent(event);
            return true;
        });*/
        /*b.refreshLayout.setOnDragListener((v, event) -> {
            activity.swipeRefreshLayout.onDragEvent(event);
            return true;
        });*/

        b.composeButton.setVisibility(BuildConfig.DEBUG ? View.VISIBLE : View.GONE);
        b.composeButton.setOnClickListener((v -> {
            startActivity(new Intent(activity, MessagesComposeActivity.class));
        }));

        //((TextView)v.findViewById(R.id.nextSync)).setText(getString(R.string.next_sync_format,Time.fromMillis(app.appJobs.syncJobTime).getStringHMS()));


        LayoutInflater layoutInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup insertPoint = b.cardInsertPoint;
        assert layoutInflater != null;

        if (app.profile.getLoginStoreType() == LOGIN_TYPE_MOBIDZIENNIK && app.appConfig.mobidziennikOldMessages == -1) {
            new MaterialDialog.Builder(activity)
                    .title("Nowy moduł wiadomości")
                    .content("Czy chcesz używać nowego modułu wiadomości?\n\nObejmuje lepsze powiadomienia, działa szybciej, pozwala na przeglądanie pobranych wiadomości bez internetu.\n\nNowy moduł (jeszcze) nie pozwala na wysyłanie wiadomości.")
                    .positiveText(R.string.yes)
                    .negativeText(R.string.no)
                    .onPositive(((dialog, which) -> {
                        if (app.appConfig.mobidziennikOldMessages != 0) {
                            // need to change old to new
                            app.appConfig.mobidziennikOldMessages = 0;
                            app.saveConfig("mobidziennikOldMessages");
                            MainActivity.Companion.setUseOldMessages(false);
                            activity.loadProfile(App.profileId);
                        }
                    }))
                    .onNegative(((dialog, which) -> {
                        if (app.appConfig.mobidziennikOldMessages != 1) {
                            // need to change from ?? to old
                            app.appConfig.mobidziennikOldMessages = 1;
                            app.saveConfig("mobidziennikOldMessages");
                            MainActivity.Companion.setUseOldMessages(true);
                            activity.loadProfile(App.profileId);
                        }
                    }))
                    .show();
        }

        b.mobidziennikMessagesSwitch.setVisibility(app.profile.getLoginStoreType() == LOGIN_TYPE_MOBIDZIENNIK ? View.VISIBLE : View.GONE);
        b.mobidziennikMessagesSwitch.setOnClickListener((v -> {
            new MaterialDialog.Builder(activity)
                    .title("Nowy moduł wiadomości")
                    .content("Czy chcesz używać nowego modułu wiadomości?\n\nObejmuje lepsze powiadomienia, działa szybciej, pozwala na przeglądanie pobranych wiadomości bez internetu.\n\nNowy moduł (jeszcze) nie pozwala na wysyłanie wiadomości.")
                    .positiveText(R.string.yes)
                    .negativeText(R.string.no)
                    .onPositive(((dialog, which) -> {
                        if (app.appConfig.mobidziennikOldMessages != 0) {
                            // need to change old to new
                            app.appConfig.mobidziennikOldMessages = 0;
                            app.saveConfig("mobidziennikOldMessages");
                            MainActivity.Companion.setUseOldMessages(false);
                            activity.loadProfile(App.profileId);
                        }
                    }))
                    .onNegative(((dialog, which) -> {
                        if (app.appConfig.mobidziennikOldMessages != 1) {
                            // need to change from ?? to old
                            app.appConfig.mobidziennikOldMessages = 1;
                            app.saveConfig("mobidziennikOldMessages");
                            MainActivity.Companion.setUseOldMessages(true);
                            activity.loadProfile(App.profileId);
                        }
                    }))
                    .show();
        }));

        /*if (!app.profile.loggedIn) {
            View cardLoginRoot = layoutInflater.inflate(R.layout.card_login, null);

            CardView cardLogin = cardLoginRoot.findViewById(R.id.cardLogin);
            cardLogin.setVisibility(app.profile.loggedIn ? View.GONE : View.VISIBLE);
            Button cardLoginButton = cardLoginRoot.findViewById(R.id.cardLoginButton);
            buttonAddDrawable(c, cardLoginButton, CommunityMaterial.Icon.cmd_arrow_right);
            cardLoginButton.setOnClickListener((v1 -> {
                new Handler().postDelayed(() -> {
                    a.runOnUiThread(() -> {
                        Intent i = new Intent("android.intent.action.MAIN")
                                .putExtra(MainActivity.ACTION_CHANGE_CURRENT_VIEW, "yes, sure")
                                .putExtra("fragmentId", MainActivity.DRAWER_ITEM_SETTINGS)
                                .putExtra("settingsPage", SettingsFragment.PAGE_REGISTER);
                        app.getContext().sendBroadcast(i);
                    });
                }, 100);
            }));

            insertPoint.addView(cardLoginRoot, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }*/



        if (app.appConfig.updateVersion != null && !app.appConfig.updateVersion.equals("")) {
            if (app.appConfig.updateVersion.equals(BuildConfig.VERSION_NAME)) {
                app.appConfig.updateVersion = "";
                app.saveConfig("updateVersion");
            }
            else {
                CardUpdateBinding b;
                b = DataBindingUtil.inflate(layoutInflater, R.layout.card_update, insertPoint, false);
                insertPoint.addView(b.getRoot());

                b.cardUpdateText.setText(getString(R.string.card_update_text_format, BuildConfig.VERSION_NAME, app.appConfig.updateVersion));
                buttonAddDrawable(activity, b.cardUpdateButton, CommunityMaterial.Icon.cmd_arrow_right);
                b.cardUpdateButton.setOnClickListener((v1 -> {
                    if (UPDATES_ON_PLAY_STORE) {
                        Utils.openGooglePlay(activity, "pl.szczodrzynski.edziennik");
                    }
                    else {
                        BootReceiver.update_url = app.appConfig.updateUrl;
                        BootReceiver.update_filename = app.appConfig.updateFilename;
                        BootReceiver.update_download_id = BootReceiver.downloadFile(app);
                    }
                }));
            }
        }

        if (app.profile.getLuckyNumberEnabled()
                && app.profile.getLuckyNumber() != -1
                && app.profile.getLuckyNumberDate() != null && app.profile.getLuckyNumberDate().getValue() == Date.getToday().getValue()) {
            CardLuckyNumberBinding b;
            b = DataBindingUtil.inflate(layoutInflater, R.layout.card_lucky_number, insertPoint, false);
            insertPoint.addView(b.getRoot());

            b.cardLuckyNumberTitle.setText(getString(R.string.card_lucky_number_title_format, app.profile.getLuckyNumber()));
            if (app.profile.getStudentNumber() == -1) {
                b.cardLuckyNumberText.setText(R.string.card_lucky_number_not_set);
            }
            else {
                b.cardLuckyNumberText.setText(getString(R.string.card_lucky_number_text_format, app.profile.getStudentNumber()));
            }

            b.cardLuckyNumber.setOnClickListener(v1 -> setNumberDialog());
        }

        timetableCard = new HomeTimetableCard(app, activity, this, layoutInflater, insertPoint);
        timetableCard.run();

        configCardGrades(activity, layoutInflater, activity, insertPoint);

        activity.getBottomSheet().prependItems(
                new BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_set_student_number)
                        .withIcon(CommunityMaterial.Icon.cmd_counter)
                        .withOnClickListener(v3 -> {
                            activity.getBottomSheet().close();
                            setNumberDialog();
                        }),
                new BottomSheetSeparatorItem(true)
        );
        activity.gainAttention();
    }

    public void setNumberDialog() {
        new MaterialDialog.Builder(activity)
                .title(R.string.card_lucky_number_set_title)
                .content(R.string.card_lucky_number_set_text)
                .inputType(InputType.TYPE_CLASS_NUMBER)
                .input(null, app.profile.getStudentNumber() == -1 ? "" : Utils.intToStr(app.profile.getStudentNumber()), (dialog, input) -> {
                    try {
                        app.profile.setStudentNumber(Utils.strToInt(input.toString()));
                        AsyncTask.execute(() -> app.profileSaveAsync());
                        activity.reloadTarget();
                    }
                    catch (Exception e) {
                        Toast.makeText(activity, R.string.incorrect_format, Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    public static String plural(Context c, @PluralsRes int resId, int value) {
        return c.getResources().getQuantityString(resId, value, value);
    }

    public static long updateInterval(App app, Time diff) {
        //Log.d(TAG, "Millis is "+System.currentTimeMillis() % 1000+", update in "+(1000-(System.currentTimeMillis() % 1000)));
        if (app.appConfig.countInSeconds) {
            return 1000-(System.currentTimeMillis() % 1000);
        }
        if (diff.minute > 5) {
            //Log.d(TAG, "60 secs");
            return 60000-(System.currentTimeMillis() % 60000);
        }
        else if (diff.minute >= 1) {
            //Log.d(TAG, "3 secs");
            return 3000-(System.currentTimeMillis() % 3000);
        }
        else if (diff.second >= 40) {
            //Log.d(TAG, "2 secs");
            return 2000-(System.currentTimeMillis() % 2000);
        }
        else {
            Log.d(TAG, "1 sec");
            return 1000-(System.currentTimeMillis() % 1000);
        }
    }

    public static String timeTill(Context c, Time t1, Time t2) {
        return timeTill(c, Time.diff(t1, t2));
    }
    public static String timeTill(Context c, Time diff) {
        return timeTill(c, diff, false);
    }
    public static String timeTill(Context c, Time diff, boolean countInSeconds) {
        return timeTill(c, diff, countInSeconds, " ");
    }
    public static String timeTill(Context c, Time diff, boolean countInSeconds, String textDelimiter) {
        if (countInSeconds) {
            int seconds = diff.second + diff.minute * 60 + diff.hour * 3600;
            return plural(c, R.plurals.time_till_text, seconds)+textDelimiter+plural(c, R.plurals.time_till_seconds, seconds);
        }
        if (diff.hour < 1 && diff.minute < 1) {
            return plural(c, R.plurals.time_till_text, diff.second)+textDelimiter+plural(c, R.plurals.time_till_seconds, diff.second);
        }
        else if (diff.hour < 1 && diff.minute < 5) {
            return plural(c, R.plurals.time_till_text, diff.minute)+textDelimiter+plural(c, R.plurals.time_till_minutes, diff.minute)+" "+plural(c, R.plurals.time_till_seconds, diff.second);
        }
        else if (diff.hour < 1) {
            return plural(c, R.plurals.time_till_text, diff.minute)+textDelimiter+plural(c, R.plurals.time_till_minutes, diff.minute);
        }
        else { // diff.hour > 1
            return plural(c, R.plurals.time_till_text, diff.hour)+textDelimiter+plural(c, R.plurals.time_till_hours, diff.hour)+" "+plural(c, R.plurals.time_till_minutes, diff.minute);
        }
    }

    public static String timeLeft(Context c, Time diff) {
        return timeLeft(c, diff, false);
    }
    public static String timeLeft(Context c, Time diff, boolean countInSeconds) {
        return timeLeft(c, diff, countInSeconds, " ");
    }
    public static String timeLeft(Context c, Time diff, boolean countInSeconds, String textDelimiter) {
        if (countInSeconds) {
            int seconds = diff.second + diff.minute * 60 + diff.hour * 3600;
            return plural(c, R.plurals.time_left_text, seconds)+textDelimiter+plural(c, R.plurals.time_left_seconds, seconds);
        }
        if (diff.hour < 1 && diff.minute < 1) {
            return plural(c, R.plurals.time_left_text, diff.second)+textDelimiter+plural(c, R.plurals.time_left_seconds, diff.second);
        }
        else if (diff.hour < 1 && diff.minute < 5) {
            return plural(c, R.plurals.time_left_text, diff.minute)+textDelimiter+plural(c, R.plurals.time_left_minutes, diff.minute)+" "+plural(c, R.plurals.time_left_seconds, diff.second);
        }
        else if (diff.hour < 1) {
            return plural(c, R.plurals.time_left_text, diff.minute)+textDelimiter+plural(c, R.plurals.time_left_minutes, diff.minute);
        }
        else { // diff.hour > 1
            return plural(c, R.plurals.time_left_text, diff.hour)+textDelimiter+plural(c, R.plurals.time_left_hours, diff.hour)+" "+plural(c, R.plurals.time_left_minutes, diff.minute);
        }
    }

    public static void buttonAddDrawable(Context c, Button button, CommunityMaterial.Icon icon)
    {
        button.setCompoundDrawables(null, null, new IconicsDrawable(c)
                .icon(icon)
                .color(IconicsColor.colorInt(Utils.getAttr(c, android.R.attr.textColorPrimary)))
                .size(IconicsSize.dp(16)), null);
    }
    public static Date findDateWithLessons(int profileId, List<LessonFull> lessons) {
        return findDateWithLessons(profileId, lessons, 0);
    }
    public static Date findDateWithLessons(int profileId, List<LessonFull> lessons, int nextDayHourThreshold) {
        return findDateWithLessons(profileId, lessons, Time.getNow(), nextDayHourThreshold);
    }
    public static Date findDateWithLessons(int profileId, List<LessonFull> lessons, Time now) {
        return findDateWithLessons(profileId, lessons, now, 0);
    }
    public static Date findDateWithLessons(int profileId, @NonNull List<LessonFull> lessons, Time now, int nextDayHourThreshold) {
        now = now.clone().stepForward(-nextDayHourThreshold, 0, 0);
        Date displayingDate = Date.getToday();
        int displayingWeekDay = displayingDate.getWeekDay();
        //boolean foundSomething = false;
        //int weekDayNum = displayingDate.getWeekDay();//Week.getTodayWeekDay();
        //int checkedDays = 0;

        for (LessonFull lesson: lessons) {
            if (lesson.profileId != profileId)
                continue;
            if (lesson.weekDay == displayingWeekDay
                    && now.getValue() <= lesson.endTime.getValue()) {
                return lesson.lessonDate;
            }
            if (lesson.weekDay != displayingWeekDay) {
                return lesson.lessonDate;
            }
        }
        return displayingDate;

        /*while (!foundSomething && checkedDays < 14)
        {
            weekDay = profile.timetable.weekdays[displayingDate.getWeekDay()];
            if (weekDay.lessons.size() == 0) // this day has no lessons
            {
                displayingDate.stepForward(0, 0, 1);
                checkedDays++;
                continue;
            }
            if (displayingDate.getWeekDay() == Week.getTodayWeekDay() // today
                    && now.getValue() > weekDay.lessons.get(weekDay.lessons.size() - 1).endTime.getValue()) // this day has lessons, but last lesson is over already
            {
                displayingDate.stepForward(0, 0, 1);
                checkedDays++;
                continue;
            }
            // this day has lessons, and we are during the lessons
            foundSomething = true;
        }
        return displayingDate;*/
    }





    private void updateCardGrades(Context c, Activity a, View root, int maxWidthPx) {
        if (a == null || !isAdded())
            return;
        TextView cardGradesNoData = root.findViewById(R.id.cardGradesNoData);

        app.db.gradeDao().getAllWhere(App.profileId, "gradeSemester = "+ app.profile.getCurrentSemester() +" AND addedDate > "+(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)).observe(this, grades -> {
            List<ItemGradesSubjectModel> subjectList = new ArrayList<>();

            // now we have all grades from the newest to the oldest
            for (GradeFull grade: grades) {
                ItemGradesSubjectModel model = ItemGradesSubjectModel.searchModelBySubjectId(subjectList, grade.subjectId);
                if (model == null) {
                    subjectList.add(new ItemGradesSubjectModel(app.profile, new Subject(App.profileId, grade.subjectId, grade.subjectLongName, grade.subjectShortName), new ArrayList<>(), new ArrayList<>()));
                    model = ItemGradesSubjectModel.searchModelBySubjectId(subjectList, grade.subjectId);
                }
                if (model != null) { // should always be not null
                    model.grades1.add(grade);
                }
            }

            float scale = c.getResources().getDisplayMetrics().density;
            int _5dp = (int) (5 * scale + 0.5f);
            int _8dp = (int) (8 * scale + 0.5f);

            LinearLayout cardGradesList = root.findViewById(R.id.cardGradesList);
            cardGradesList.removeAllViews();

            LinearLayout.LayoutParams textLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            textLayoutParams.setMargins(0, 0, _5dp, 0);

            LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            linearLayoutParams.setMargins(_8dp, 0, _8dp, _5dp);

            //Log.d(TAG, "maxWidthPx "+maxWidthPx);

            int count = 0;

            if (subjectList.size() > 0) {
                for (ItemGradesSubjectModel subjectModel : subjectList) {
                    if (count++ >= 8) {
                        //continue;
                    }
                    LinearLayout gradeItem = new LinearLayout(cardGradesList.getContext());
                    gradeItem.setOrientation(LinearLayout.HORIZONTAL);

                    int totalWidthPx = 0;//subjectName.getMeasuredWidth() + _5dp;


                    boolean ellipsized = false;

                    for (GradeFull grade : subjectModel.grades1) {
                        if (ellipsized)
                            continue;
                        int gradeColor;
                        if (app.profile.getGradeColorMode() == Profile.COLOR_MODE_DEFAULT) {
                            gradeColor = grade.color;
                        }
                        else {
                            gradeColor = Colors.gradeToColor(grade);
                        }

                        TextView gradeName = new TextView(gradeItem.getContext());
                        gradeName.setText(grade.name);
                        if (grade.type == TYPE_SEMESTER1_PROPOSED
                                || grade.type == TYPE_SEMESTER2_PROPOSED) {
                            gradeName.setText(getString(R.string.grade_semester_proposed_format, grade.name));
                        }
                        else if (grade.type == TYPE_SEMESTER1_FINAL
                                || grade.type == TYPE_SEMESTER2_FINAL) {
                            gradeName.setText(getString(R.string.grade_semester_final_format, grade.name));
                        }
                        else if (grade.type == TYPE_YEAR_PROPOSED) {
                            gradeName.setText(getString(R.string.grade_year_proposed_format, grade.name));
                        }
                        else if (grade.type == TYPE_YEAR_FINAL) {
                            gradeName.setText(getString(R.string.grade_year_final_format, grade.name));
                        }
                        gradeName.setTextColor(ColorUtils.calculateLuminance(gradeColor) > 0.25 ? 0xff000000 : 0xffffffff);
                        gradeName.setTypeface(null, Typeface.BOLD);
                        gradeName.setBackgroundResource(R.drawable.bg_rounded_4dp);
                        gradeName.getBackground().setColorFilter(new PorterDuffColorFilter(gradeColor, PorterDuff.Mode.MULTIPLY));
                        gradeName.setPadding(_5dp, 0, _5dp, 0);

                        gradeName.measure(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        totalWidthPx += gradeName.getMeasuredWidth() + _5dp;
                        //Log.d(TAG, "totalWidthPx "+totalWidthPx);
                        if (totalWidthPx >= (float)maxWidthPx / 1.5f) {
                            ellipsized = true;
                            TextView ellipsisText = new TextView(gradeItem.getContext());
                            ellipsisText.setText(R.string.ellipsis);
                            ellipsisText.setTextAppearance(gradeItem.getContext(), R.style.NavView_TextView);
                            ellipsisText.setTypeface(null, Typeface.BOLD);
                            ellipsisText.setPadding(0, 0, 0, 0);
                            gradeItem.addView(ellipsisText, textLayoutParams);
                        }
                        else {
                            gradeItem.addView(gradeName, textLayoutParams);
                        }
                    }

                    TextView subjectName = new TextView(gradeItem.getContext());
                    Subject subject = subjectModel.subject;
                    subjectName.setText(" z "+(subject != null ? subject.longName : ""));
                    subjectName.setEllipsize(TextUtils.TruncateAt.END);
                    subjectName.setSingleLine();
                    subjectName.measure(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

                    //Log.d(TAG, "subjectName.width "+totalWidthPx);
                    //subjectName.setMaxWidth(maxWidthPx - totalWidthPx);

                    gradeItem.addView(subjectName, textLayoutParams);

                    cardGradesList.addView(gradeItem, linearLayoutParams);
                }
            }
            else {
                cardGradesNoData.setVisibility(View.VISIBLE);
                cardGradesList.setVisibility(View.GONE);
            }
        });

        Button cardGradesButton = root.findViewById(R.id.cardGradesButton);
        buttonAddDrawable(c, cardGradesButton, CommunityMaterial.Icon.cmd_arrow_right);
        cardGradesButton.setOnClickListener((v1 -> new Handler().postDelayed(() -> a.runOnUiThread(() -> {
            activity.loadTarget(MainActivity.DRAWER_ITEM_GRADES, null);
        }), 100)));

        //new Handler().postDelayed(() -> a.runOnUiThread(() -> updateCardGrades(c, a, root)), newRefreshInterval);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timetableCard != null)
            timetableCard.destroy();
    }

    private void configCardGrades(Context c, LayoutInflater layoutInflater, Activity a, ViewGroup insertPoint) {
        View root = layoutInflater.inflate(R.layout.card_grades, null);
        DisplayMetrics displayMetrics = c.getResources().getDisplayMetrics();
        updateCardGrades(c, a, root, displayMetrics.widthPixels - Utils.dpToPx((app.appConfig.miniDrawerVisible ? 72 : 0)/*miniDrawer size*/ + 24 + 24/*left and right offsets*/ + 16/*ellipsize width*/));
        insertPoint.addView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private HomeTimetableCard timetableCard;
}
