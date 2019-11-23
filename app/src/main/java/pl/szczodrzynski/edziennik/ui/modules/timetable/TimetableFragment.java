package pl.szczodrzynski.edziennik.ui.modules.timetable;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.tabs.TabLayout;
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;
import com.mikepenz.iconics.typeface.library.szkolny.font.SzkolnyFont;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonFull;
import pl.szczodrzynski.edziennik.databinding.FragmentTimetableBinding;
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventManualDialog;
import pl.szczodrzynski.edziennik.ui.modules.error.ErrorDialog;
import pl.szczodrzynski.edziennik.ui.modules.home.HomeFragment;
import pl.szczodrzynski.edziennik.utils.SpannableHtmlTagHandler;
import pl.szczodrzynski.edziennik.utils.Themes;
import pl.szczodrzynski.edziennik.utils.Utils;
import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.models.Time;
import pl.szczodrzynski.edziennik.utils.models.Week;
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem;
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem;

import static pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange.TYPE_CANCELLED;
import static pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange.TYPE_CHANGE;
import static pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata.TYPE_LESSON_CHANGE;
import static pl.szczodrzynski.edziennik.utils.Utils.bs;
import static pl.szczodrzynski.edziennik.utils.Utils.d;

public class TimetableFragment extends Fragment {
    private static final String TAG = "RegisterTimetable";

    private App app = null;
    private MainActivity activity = null;
    private FragmentTimetableBinding b = null;

    private ViewPager viewPager;
    private static int pageSelection = -1;
    private static Date displayingDate;

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
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_timetable, container, false);
        return b.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (app == null || app.profile == null || activity == null || b == null || !isAdded())
            return;

        activity.getBottomSheet().prependItems(
                new BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_add_event)
                        .withDescription(R.string.menu_add_event_desc)
                        .withIcon(SzkolnyFont.Icon.szf_calendar_plus_outline)
                        .withOnClickListener(v3 -> {
                            activity.getBottomSheet().close();
                            new MaterialDialog.Builder(activity)
                                    .title(R.string.main_menu_add)
                                    .items(R.array.main_menu_add_options)
                                    .itemsCallback((dialog, itemView, position, text) -> {
                                        switch (position) {
                                            case 0:
                                                new EventManualDialog(activity).show(app, null, displayingDate, null, EventManualDialog.DIALOG_EVENT);
                                                break;
                                            case 1:
                                                new EventManualDialog(activity).show(app, null, displayingDate, null, EventManualDialog.DIALOG_HOMEWORK);
                                                break;
                                        }
                                    })
                                    .show();
                        }),
                new BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_generate_block_timetable)
                        .withDescription(R.string.menu_generate_block_timetable_desc)
                        .withIcon(CommunityMaterial.Icon2.cmd_table_large)
                        .withOnClickListener(v3 -> {
                            activity.getBottomSheet().close();
                            generateBlockTimetable();
                        }),
                new BottomSheetSeparatorItem(true),
                new BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_mark_as_read)
                        .withIcon(CommunityMaterial.Icon.cmd_eye_check_outline)
                        .withOnClickListener(v3 -> {
                            activity.getBottomSheet().close();
                            AsyncTask.execute(() -> app.db.metadataDao().setAllSeen(App.profileId, TYPE_LESSON_CHANGE, true));
                            Toast.makeText(activity, R.string.main_menu_mark_as_read_success, Toast.LENGTH_SHORT).show();
                        })
        );
        activity.gainAttention();

        // Setting ViewPager for each Tabs
        viewPager = b.viewpager;
        Adapter adapter = new Adapter(getChildFragmentManager());

        Date today = Date.getToday();

        Date date = Date.getToday();
        int weekBeginning = app.appConfig.timetableDisplayDaysBackward - date.getWeekDay();
        int weekEnd = weekBeginning + 6;
        date.stepForward(0, 0, 0 - (app.appConfig.timetableDisplayDaysBackward));
        for (int i = 0; i < app.appConfig.timetableDisplayDaysForward + app.appConfig.timetableDisplayDaysBackward + 1; i++) {
            Bundle args = new Bundle();
            args.putLong("date", date.getValue());
            TimetableDayFragment timetableDayFragment = new TimetableDayFragment();
            timetableDayFragment.setArguments(args);
            StringBuilder pageTitle = new StringBuilder(Week.getFullDayName(date.getWeekDay()));
            if (i > weekEnd || i < weekBeginning) {
                pageTitle.append(", ").append(date.getStringDm());
            }
            adapter.addFragment(timetableDayFragment, pageTitle.toString());
            date.stepForward(0, 0, 1);
        }
        viewPager.setAdapter(adapter);


        if (getArguments() != null && getArguments().getLong("timetableDate", 0) != 0) {
            Date gotDate = new Date().parseFromYmd(Long.toString(getArguments().getLong("timetableDate", 0))); // OVERRIDE HERE
            // DAMNIT
            // THE TIMETABLE WAS DOING LOTS OF WEIRD THINGS (incorrect default days, sometimes scrolling to the beginning)
            // BECAUSE THESE TWO LINES WERE SWAPPED.
            //pageSelection += Date.diffDays(gotDate, displayingDate);

            Log.d(TAG, "Got date "+getArguments().getLong("timetableDate", 0));

            pageSelection = app.appConfig.timetableDisplayDaysBackward + Date.diffDays(gotDate, today);
            displayingDate = gotDate;
        }
        else if (pageSelection == -1) {
            AsyncTask.execute(() -> {
                if (app == null || app.profile == null || activity == null || b == null || !isAdded())
                    return;

                List<LessonFull> lessons = app.db.lessonDao().getAllWeekNow(App.profileId, today.getWeekStart(), today);
                displayingDate = HomeFragment.findDateWithLessons(App.profileId, lessons);
                pageSelection = app.appConfig.timetableDisplayDaysBackward + Date.diffDays(displayingDate, today); // DEFAULT HERE

                activity.runOnUiThread(() -> {
                    viewPager.setCurrentItem(pageSelection, false);
                });
            });

        }

        viewPager.setCurrentItem(pageSelection, false);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }
            @Override public void onPageScrollStateChanged(int state) { }
            @Override public void onPageSelected(int position) {
                pageSelection = position;
                Fragment fragment = adapter.getItem(position);
                assert fragment.getArguments() != null;
                displayingDate = new Date().parseFromYmd(Long.toString(fragment.getArguments().getLong("date", 20181009)));
                /*
                Fragment fragment = adapter.getItem(position);
                int scrolledDate = fragment.getArguments().getInt("date", 0);
                    //Toast.makeText(app, "Date: "+scrolledDate, Toast.LENGTH_SHORT).show();
                    Collection<Integer> removeDates = new ArrayList<>();
                    for (Integer lessonChangeDate: unreadLessonChangesDates) {
                        if (lessonChangeDate.equals(scrolledDate)) {
                            for (RegisterLessonChange lessonChange: app.profile.timetable.lessonChanges) {
                                if (lessonChange.lessonDate.getValue() == lessonChangeDate) {
                                    lessonChange.notified = true;
                                }
                            }
                            removeDates.add(lessonChangeDate);
                        }
                    }
                    unreadLessonChangesDates.removeAll(removeDates);

                    if (app.profile.unreadLessonChanges != unreadLessonChangesDates.size()) {
                        app.profile.unreadLessonChanges = unreadLessonChangesDates.size();
                        app.profile.savePending = true;
                        Intent i = new Intent("android.intent.action.MAIN").putExtra(MainActivity.ACTION_UPDATE_BADGES, "yes, sure");
                        getContext().sendBroadcast(i);
                    }
                }*/
            }
        });

        // Set Tabs inside Toolbar
        TabLayout tabs = view.findViewById(R.id.result_tabs);
        tabs.setupWithViewPager(viewPager);

        /*if (!app.appConfig.tapTargetSetAsRead) {
            new MaterialTapTargetPrompt.Builder(activity)
                    .setTarget(activity.findViewById(R.id.action_mark_as_read))
                    .setPrimaryText(R.string.tap_target_set_as_read_title)
                    .setSecondaryText(R.string.tap_target_set_as_read_text)
                    .setFocalColour(Color.TRANSPARENT)
                    .setPromptStateChangeListener((prompt, state) -> {
                        if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED) {
                            // User has pressed the prompt target
                        }
                    })
                    .show();
            app.appConfig.tapTargetSetAsRead = true;
            app.appConfig.savePending = true;
        }*/
    }

    public static Bitmap getBitmapFromView(View view) {
        //Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getLayoutParams().width, view.getLayoutParams().height, Bitmap.Config.ARGB_8888);
        //Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        //Get the view's background
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable!=null)
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas);
        else
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.TRANSPARENT);
        // draw the view on the canvas
        view.draw(canvas);
        //return the bitmap
        return returnedBitmap;
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, true);
        bm.recycle();
        return resizedBitmap;
    }

    public void generateBlockTimetable() {
        if (getActivity() == null) {
            return;
        }

        Date weekCurrentStart = Week.getWeekStart();
        Date weekCurrentEnd = Week.getWeekEnd();
        Date weekNextStart = weekCurrentEnd.clone().stepForward(0, 0, 1);
        Date weekNextEnd = weekNextStart.clone().stepForward(0, 0, 6);

        new MaterialDialog.Builder(getActivity())
                .title(R.string.timetable_generate_range)
                .items(
                        getString(R.string.timetable_generate_no_changes),
                        getString(R.string.timetable_generate_current_week_format, weekCurrentStart.getFormattedStringShort(), weekCurrentEnd.getFormattedStringShort()),
                        getString(R.string.timetable_generate_next_week_format, weekNextStart.getFormattedStringShort(), weekNextEnd.getFormattedStringShort()),
                        getString(R.string.timetable_generate_for_printout))
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .checkBoxPromptRes(R.string.timetable_generate_include_profile_name, true, null)
                .itemsCallbackSingleChoice(0, (dialog, itemView, which, text) -> {
                    Toast.makeText(app, "Selected "+which, Toast.LENGTH_SHORT).show();
                    AsyncTask.execute(() -> {
                        switch (which) {
                            case 0:
                                generateBlockTimetableWithLessons(app.db.lessonDao().getAllWeekNow(App.profileId, weekCurrentStart, weekCurrentStart), false, dialog.isPromptCheckBoxChecked(), null, null, false);
                                break;
                            case 1:
                                generateBlockTimetableWithLessons(app.db.lessonDao().getAllWeekNow(App.profileId, weekCurrentStart, weekCurrentStart), true, dialog.isPromptCheckBoxChecked(), weekCurrentStart, weekCurrentEnd, false);
                                break;
                            case 2:
                                generateBlockTimetableWithLessons(app.db.lessonDao().getAllWeekNow(App.profileId, weekNextStart, weekNextStart), true, dialog.isPromptCheckBoxChecked(), weekNextStart, weekNextEnd, false);
                                break;
                            case 3:
                                generateBlockTimetableWithLessons(app.db.lessonDao().getAllWeekNow(App.profileId, weekCurrentStart, weekCurrentStart), false, dialog.isPromptCheckBoxChecked(), null, null, true);
                                break;
                        }
                    });
                    if (which == 0) {

                    }
                    return false;
                })
                .show();

    }

    private MaterialDialog progressDialog;

    private void generateBlockTimetableWithLessons(List<LessonFull> lessonList, boolean markChanges, boolean showProfileName, Date weekStart, Date weekEnd, boolean noColors) {
        d(TAG, Arrays.toString(lessonList.toArray()));

        activity.runOnUiThread(() -> {
            progressDialog = new MaterialDialog.Builder(activity)
                    .title(R.string.timetable_generate_progress_title)
                    .content(R.string.timetable_generate_progress_text)
                    .progress(true, 0)
                    .show();
        });

        // block size: 190x90, so one minute is 2px
        // spacing: 15x10
        // left size: 45px
        // header size: 45px
        // overall width: 45 + n*(190+15)
        // overall height: 45 + n*(90+10)
        // footer size 30px

        int WIDTH_CONSTANT = 70;
        int WIDTH_WEEKDAY = 285;
        int WIDTH_SPACING = 15;
        int HEIGHT_PROFILE_NAME = showProfileName ? 100 : 0;
        int HEIGHT_CONSTANT = 60;
        int HEIGHT_MINUTE = 3;
        int HEIGHT_FOOTER = 40;

        List<List<LessonFull>> weekdays = new ArrayList<>();
        for(int i = 0; i < 7; i++) {
            weekdays.add(new ArrayList<>());
        }
        int maxWeekDay = 5;
        Time minTime = null;
        Time maxTime = null;

        TreeMap<Integer, Integer> lessonRanges = new TreeMap<>();

        for (LessonFull lesson: lessonList) {
            if (lesson.weekDay > maxWeekDay)
                maxWeekDay = lesson.weekDay;
            List<LessonFull> weekdayLessons = weekdays.get(lesson.weekDay);
            weekdayLessons.add(lesson);
            lessonRanges.put(lesson.startTime.getValue(), lesson.endTime.getValue());
            if (minTime == null || lesson.startTime.getValue() < minTime.getValue()) {
                minTime = lesson.startTime;
            }
            if (maxTime == null || lesson.endTime.getValue() > maxTime.getValue()) {
                maxTime = lesson.endTime;
            }
        }

        if (minTime != null) {
            d(TAG, "Min time "+minTime.getValue()+" max time "+maxTime.getValue());
            Time diff = Time.diff(maxTime, minTime);
            int minutes = diff.hour*60+diff.minute;

            Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
            int imgWidth = WIDTH_CONSTANT + maxWeekDay * WIDTH_WEEKDAY + (maxWeekDay-1) * WIDTH_SPACING;
            int imgHeight = HEIGHT_PROFILE_NAME + HEIGHT_CONSTANT + minutes*HEIGHT_MINUTE + HEIGHT_FOOTER;
            Bitmap bmp = Bitmap.createBitmap(imgWidth+20, imgHeight+30, conf); // this creates a MUTABLE bitmap
            Canvas canvas = new Canvas(bmp);
            if (noColors)
                canvas.drawARGB(255, 255, 255, 255);
            else
                canvas.drawARGB(255, 225, 225, 225);

            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setDither(true);

            for (LessonFull lesson: lessonList) {
                Time lessonLength = Time.diff(lesson.endTime, lesson.startTime);
                Time firstOffset = Time.diff(lesson.startTime, minTime);

                int left = WIDTH_CONSTANT + lesson.weekDay*WIDTH_WEEKDAY + lesson.weekDay * WIDTH_SPACING;
                int top = HEIGHT_PROFILE_NAME + HEIGHT_CONSTANT + (firstOffset.hour*60+firstOffset.minute)*HEIGHT_MINUTE;
                int blockWidth = WIDTH_WEEKDAY;
                int blockHeight = (lessonLength.hour*60+lessonLength.minute)*HEIGHT_MINUTE;
                int viewWidth = Utils.dpToPx(380);
                int viewHeight = Utils.dpToPx((lessonLength.hour*60+lessonLength.minute)*4);

                LinearLayout linearLayout;
                try {
                    linearLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.row_timetable_block_item, null);
                }
                catch (Exception e) {
                    new ErrorDialog(activity, e);
                    return;
                }

                LinearLayout layout = linearLayout.findViewById(R.id.timetableItemLayout);
                CardView card = linearLayout.findViewById(R.id.timetableItemCard);
                TextView subjectName = linearLayout.findViewById(R.id.timetableItemSubjectName);
                TextView classroomName = linearLayout.findViewById(R.id.timetableItemClassroomName);
                TextView teacherName = linearLayout.findViewById(R.id.timetableItemTeacherName);
                TextView teamName = linearLayout.findViewById(R.id.timetableItemTeamName);

                if (noColors) {
                    card.setCardBackgroundColor(0xffffffff);
                    card.setCardElevation(0.0f);
                    layout.setBackgroundResource(R.drawable.bg_rounded_16dp_outline);
                    subjectName.setTextColor(0xff000000);
                    classroomName.setTextColor(0xffaaaaaa);
                    teacherName.setTextColor(0xffaaaaaa);
                    teamName.setTextColor(0xffaaaaaa);
                }

                subjectName.setText(lesson.subjectLongName);
                classroomName.setText(bs(lesson.classroomName));
                teacherName.setText(bs(lesson.teacherFullName));
                teamName.setText(bs(lesson.teamName));

                if (markChanges) {
                    if (lesson.changeId != 0) {
                        if (lesson.changeType == TYPE_CANCELLED) {
                            card.setCardBackgroundColor(Color.BLACK);
                            subjectName.setText(Html.fromHtml("<del>"+lesson.subjectLongName+"</del>", null, new SpannableHtmlTagHandler()));
                        }
                        else if (lesson.changeType == TYPE_CHANGE) {
                            card.setCardBackgroundColor(0xff234158); // 0x40 x primary
                            subjectName.setTypeface(null, Typeface.BOLD_ITALIC);
                        }
                    }
                }

                linearLayout.setDrawingCacheEnabled(true);
                linearLayout.measure(View.MeasureSpec.makeMeasureSpec(viewWidth, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(viewHeight, View.MeasureSpec.EXACTLY));
                linearLayout.layout(0, 0, linearLayout.getMeasuredWidth(), linearLayout.getMeasuredHeight());
                linearLayout.buildDrawingCache(true);

                Bitmap bm = linearLayout.getDrawingCache();
                canvas.drawBitmap(bm, null, new Rect(left, top, left+blockWidth, top+blockHeight), paint);
            }

            Paint textPaint = new Paint();
            textPaint.setARGB(255, 0, 0, 0);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(30);
            textPaint.setAntiAlias(true);
            textPaint.setFilterBitmap(true);
            textPaint.setDither(true);

            for (int w = 0; w < maxWeekDay+1; w++) {
                int x = WIDTH_CONSTANT + w*WIDTH_WEEKDAY + w * WIDTH_SPACING;
                canvas.drawText(Week.getFullDayName(w), x + (WIDTH_WEEKDAY/2), HEIGHT_PROFILE_NAME + HEIGHT_CONSTANT/2+10, textPaint);
            }

            if (showProfileName) {
                textPaint.setTextSize(50);
                if (weekStart != null && weekEnd != null) {
                    canvas.drawText(app.profile.getName() + " - plan lekcji, "+weekStart.getFormattedStringShort() + " - " + weekEnd.getFormattedStringShort(), (imgWidth + 20) / 2, 80, textPaint);
                }
                else {
                    canvas.drawText(app.profile.getName() + " - plan lekcji", (imgWidth + 20) / 2, 80, textPaint);
                }
            }

            textPaint.setARGB(128, 0, 0, 0);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            textPaint.setTextSize(26);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
            textPaint.setAntiAlias(true);
            textPaint.setFilterBitmap(true);
            textPaint.setDither(true);
            int textPaintCenter = Math.round((textPaint.descent() + textPaint.ascent()) / 2);
            canvas.drawText("Wygenerowano w aplikacji Szkolny.eu", imgWidth - 10, imgHeight - textPaintCenter - 10, textPaint);

            textPaint.setARGB(255, 127, 127, 127);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(16);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            textPaint.setAntiAlias(true);
            textPaint.setFilterBitmap(true);
            textPaint.setDither(true);
            textPaintCenter = Math.round((textPaint.descent() + textPaint.ascent()) / 2); // it's probably negative

            Paint linePaint = new Paint();
            linePaint.setARGB(255, 100, 100, 100);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
            linePaint.setAntiAlias(true);
            linePaint.setFilterBitmap(true);
            linePaint.setDither(true);

            int minTimeVal = minTime.getValue();
            int minTimeInt = (minTimeVal/10000)*60 + ((minTimeVal/100)%100);
            for (int startTime: lessonRanges.keySet()) {
                Integer endTime = lessonRanges.get(startTime);
                if (endTime == null)
                    continue;

                int hour = startTime/10000;
                int minute = (startTime/100)%100;
                int firstOffset = hour * 60 + minute - minTimeInt; // offset in minutes
                int top = HEIGHT_PROFILE_NAME + HEIGHT_CONSTANT + firstOffset*HEIGHT_MINUTE;
                String text = hour+":"+(minute < 10 ? "0"+minute : minute);
                canvas.drawText(text, WIDTH_CONSTANT/2, top-textPaintCenter, textPaint);
                canvas.drawLine(WIDTH_CONSTANT, top, imgWidth, top, linePaint);

                hour = endTime/10000;
                minute = (endTime/100)%100;
                firstOffset = hour * 60 + minute - minTimeInt; // offset in minutes
                top = HEIGHT_PROFILE_NAME + HEIGHT_CONSTANT + firstOffset*HEIGHT_MINUTE;
                text = hour+":"+(minute < 10 ? "0"+minute : minute);
                canvas.drawText(text, WIDTH_CONSTANT/2, top-textPaintCenter, textPaint);
                canvas.drawLine(WIDTH_CONSTANT, top, imgWidth, top, linePaint);
            }

            File outputDir = Environment.getExternalStoragePublicDirectory("Szkolny.eu");
            outputDir.mkdirs();

            File outputFile = new File(outputDir, "plan_lekcji_"+app.profile.getName()+"_"+Date.getToday().getStringY_m_d()+"_"+Time.getNow().getStringH_M()+".png");

            FileOutputStream fos;
            try {
                fos = new FileOutputStream(outputFile);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
            } catch (Exception e) {
                Log.e("SAVE_IMAGE", e.getMessage(), e);
                return;
            }

            activity.runOnUiThread(() -> {
                if (progressDialog != null)
                    progressDialog.dismiss();
                new MaterialDialog.Builder(activity)
                        .title(R.string.timetable_generate_success_title)
                        .content(R.string.timetable_generate_success_text)
                        .positiveText(R.string.share)
                        .negativeText(R.string.open)
                        .neutralText(R.string.do_nothing)
                        .onPositive(((dialog, which) -> {
                            Uri uri = Uri.parse("file://" + outputFile.getAbsolutePath());
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                uri = FileProvider.getUriForFile(activity, app.getPackageName() + ".provider", outputFile);
                            }

                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setDataAndType(null, "image/*");
                            intent.putExtra(Intent.EXTRA_STREAM, uri);
                            startActivity(Intent.createChooser(intent, getString(R.string.share_intent)));
                        }))
                        .onNegative(((dialog, which) -> {
                            Uri uri = Uri.parse("file://" + outputFile.getAbsolutePath());
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                uri = FileProvider.getUriForFile(activity, app.getPackageName() + ".provider", outputFile);
                            }

                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(uri, "image/*");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                        }))
                        .show();
            });
        }
    }

    static class Adapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public Adapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}
