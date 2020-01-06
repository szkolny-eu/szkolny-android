package pl.szczodrzynski.edziennik.ui.modules.attendance;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.LongSparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.ColorUtils;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator;
import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.db.full.AttendanceFull;
import pl.szczodrzynski.edziennik.data.db.entity.Subject;
import pl.szczodrzynski.edziennik.databinding.FragmentAttendanceBinding;
import pl.szczodrzynski.edziennik.utils.Themes;
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem;

import static pl.szczodrzynski.edziennik.data.db.entity.Attendance.TYPE_ABSENT;
import static pl.szczodrzynski.edziennik.data.db.entity.Attendance.TYPE_ABSENT_EXCUSED;
import static pl.szczodrzynski.edziennik.data.db.entity.Attendance.TYPE_BELATED;
import static pl.szczodrzynski.edziennik.data.db.entity.Attendance.TYPE_BELATED_EXCUSED;
import static pl.szczodrzynski.edziennik.data.db.entity.Attendance.TYPE_PRESENT;
import static pl.szczodrzynski.edziennik.data.db.entity.Attendance.TYPE_RELEASED;
import static pl.szczodrzynski.edziennik.data.db.entity.LoginStore.LOGIN_TYPE_MOBIDZIENNIK;
import static pl.szczodrzynski.edziennik.data.db.entity.LoginStore.LOGIN_TYPE_VULCAN;
import static pl.szczodrzynski.edziennik.data.db.entity.Metadata.TYPE_ATTENDANCE;

public class AttendanceFragment extends Fragment {

    private App app = null;
    private MainActivity activity = null;
    private FragmentAttendanceBinding b = null;

    private int displayMode = MODE_YEAR;
    private static final int MODE_YEAR = 0;
    private static final int MODE_SEMESTER_1 = 1;
    private static final int MODE_SEMESTER_2 = 2;
    private long subjectIdFilter = -1;
    private LongSparseArray<int[]> subjectTotalCount;
    private LongSparseArray<int[]> subjectAbsentCount;
    private LongSparseArray<Float> subjectAttendancePercentage;

    private List<AttendanceFull> attendanceList = null;

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
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_attendance, container, false);
        b.refreshLayout.setParent(activity.getSwipeRefreshLayout());
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (app == null || app.profile == null || activity == null || b == null || !isAdded())
            return;

        activity.getBottomSheet().prependItems(
                new BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_mark_as_read)
                        .withIcon(CommunityMaterial.Icon.cmd_eye_check_outline)
                        .withOnClickListener(v3 -> {
                            activity.getBottomSheet().close();
                            AsyncTask.execute(() -> app.db.metadataDao().setAllSeen(App.profileId, TYPE_ATTENDANCE, true));
                            Toast.makeText(activity, R.string.main_menu_mark_as_read_success, Toast.LENGTH_SHORT).show();
                        })
        );

        /*b.refreshLayout.setOnRefreshListener(() -> {
            activity.syncCurrentFeature(MainActivity.DRAWER_ITEM_ATTENDANCE, b.refreshLayout);
        });*/

        b.attendancePercentage.setProgressTextAdapter(PERCENTAGE_ADAPTER);
        b.attendancePercentage.setMaxProgress(100.0f);

        b.attendanceSummaryTitle.setOnClickListener((v -> {
            PopupMenu popupMenu = new PopupMenu(activity, b.attendanceSummaryTitle, Gravity.END);
            popupMenu.getMenu().add(0, 0, 0, R.string.summary_mode_year);
            popupMenu.getMenu().add(0, 1, 1, R.string.summary_mode_semester_1);
            popupMenu.getMenu().add(0, 2, 2, R.string.summary_mode_semester_2);
            popupMenu.setOnMenuItemClickListener((item -> {
                displayMode = item.getItemId();
                updateList();
                return true;
            }));
            popupMenu.show();
        }));

        /*if (app.profile.getLoginStoreType() == LOGIN_TYPE_MOBIDZIENNIK) {
            long attendanceLastSync = app.profile.getStudentData("attendanceLastSync", (long)0);
            if (attendanceLastSync == 0) {
                attendanceLastSync = app.profile.getSemesterStart(1).getInMillis();
            }
            Date lastSyncDate = Date.fromMillis(attendanceLastSync);
            if (lastSyncDate.getValue() < Week.getWeekStart().getValue()) {
                CafeBar.builder(activity)
                        .to(activity.getNavView().getCoordinator())
                        .content(R.string.sync_old_data_info)
                        .icon(new IconicsDrawable(activity).icon(CommunityMaterial.Icon.cmd_download_outline).size(IconicsSize.dp(20)).color(IconicsColor.colorInt(Themes.INSTANCE.getPrimaryTextColor(activity))))
                        .positiveText(R.string.refresh)
                        .positiveColor(0xff4caf50)
                        .negativeText(R.string.ok)
                        .negativeColor(0x66ffffff)
                        .onPositive((cafeBar -> {
                            if (!activity.getSwipeRefreshLayout().isRefreshing()) {
                                cafeBar.dismiss();
                                activity.syncCurrentFeature();
                            }
                            else {
                                Toast.makeText(app, R.string.please_wait, Toast.LENGTH_SHORT).show();
                            }
                        }))
                        .onNegative(CafeBar::dismiss)
                        .autoDismiss(false)
                        .swipeToDismiss(true)
                        .floating(true)
                        .show();
            }
        }*/

        if (app.profile.getLoginStoreType() == LOGIN_TYPE_MOBIDZIENNIK) {
            b.attendanceSummarySubject.setVisibility(View.GONE);
        }
        else {
            b.attendanceSummarySubject.setOnClickListener((v -> {
                AsyncTask.execute(() -> {
                    List<Subject> subjectList = app.db.subjectDao().getAllNow(App.profileId);
                    PopupMenu popupMenu = new PopupMenu(activity, b.attendanceSummarySubject, Gravity.END);
                    popupMenu.getMenu().add(0, -1, 0, R.string.subject_filter_disabled);
                    int index = 0;
                    DecimalFormat format = new DecimalFormat("0.00");
                    for (Subject subject: subjectList) {
                        int total = subjectTotalCount.get(subject.id, new int[3])[displayMode];
                        int absent = subjectAbsentCount.get(subject.id, new int[3])[displayMode];
                        if (total == 0)
                            continue;
                        int present = total - absent;
                        float percentage = (float)present / (float)total * 100.0f;
                        String percentageStr = format.format(percentage);
                        popupMenu.getMenu().add(0, (int)subject.id, index++, getString(R.string.subject_filter_format, subject.longName, percentageStr));
                    }
                    popupMenu.setOnMenuItemClickListener((item -> {
                        subjectIdFilter = item.getItemId();
                        b.attendanceSummarySubject.setText(item.getTitle().toString().replaceAll("\\s-\\s[0-9]{1,2}\\.[0-9]{1,2}%", ""));
                        updateList();
                        return true;
                    }));
                    new Handler(activity.getMainLooper()).post(popupMenu::show);
                });

            }));
        }

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());

        b.attendanceView.setHasFixedSize(true);
        b.attendanceView.setLayoutManager(linearLayoutManager);

        app.db.attendanceDao().getAll(App.profileId).observe(this, attendance -> {
            if (app == null || app.profile == null || activity == null || b == null || !isAdded())
                return;

            if (attendance == null) {
                b.attendanceView.setVisibility(View.GONE);
                b.attendanceNoData.setVisibility(View.VISIBLE);
                return;
            }

            attendanceList = attendance;

            countSubjectStats();

            updateList();
        });
    }

    private void countSubjectStats() {
        subjectTotalCount = new LongSparseArray<>();
        subjectAbsentCount = new LongSparseArray<>();
        for (AttendanceFull attendance: attendanceList) {
            if (app.profile.getLoginStoreType() == LOGIN_TYPE_VULCAN && attendance.type == TYPE_RELEASED)
                continue;
            int[] subjectTotal = subjectTotalCount.get(attendance.subjectId, new int[3]);
            int[] subjectAbsent = subjectAbsentCount.get(attendance.subjectId, new int[3]);

            subjectTotal[0]++;
            subjectTotal[attendance.semester]++;

            if (attendance.type == TYPE_ABSENT || attendance.type == TYPE_ABSENT_EXCUSED) {
                subjectAbsent[0]++;
                subjectAbsent[attendance.semester]++;
            }

            subjectTotalCount.put(attendance.subjectId, subjectTotal);
            subjectAbsentCount.put(attendance.subjectId, subjectAbsent);
        }
    }

    private void updateList() {
        if (app == null || app.profile == null || activity == null || b == null || !isAdded())
            return;

        int presentCount = 0;
        int absentCount = 0;
        int absentUnexcusedCount = 0;
        int belatedCount = 0;
        int releasedCount = 0;

        List<AttendanceFull> filteredList = new ArrayList<>();
        for (AttendanceFull attendance: attendanceList) {
            if (displayMode != MODE_YEAR && attendance.semester != displayMode)
                continue;
            if (subjectIdFilter != -1 && attendance.subjectId != subjectIdFilter)
                continue;
            if (attendance.type != TYPE_PRESENT)
                filteredList.add(attendance);
            switch (attendance.type) {
                case TYPE_PRESENT:
                    presentCount++;
                    break;
                case TYPE_ABSENT:
                    absentCount++;
                    absentUnexcusedCount++;
                    break;
                case TYPE_ABSENT_EXCUSED:
                    absentCount++;
                    break;
                case TYPE_BELATED_EXCUSED:
                case TYPE_BELATED:
                    belatedCount++;
                    break;
                case TYPE_RELEASED:
                    releasedCount++;
                    break;
            }
        }
        if (filteredList.size() > 0) {
            AttendanceAdapter adapter;
            b.attendanceView.setVisibility(View.VISIBLE);
            b.attendanceNoData.setVisibility(View.GONE);
            if ((adapter = (AttendanceAdapter) b.attendanceView.getAdapter()) != null) {
                adapter.attendanceList = filteredList;
                adapter.notifyDataSetChanged();
            }
            else {
                adapter = new AttendanceAdapter(getContext(), filteredList);
                b.attendanceView.setAdapter(adapter);
            }
        }
        else {
            b.attendanceView.setVisibility(View.GONE);
            b.attendanceNoData.setVisibility(View.VISIBLE);
        }

        // SUMMARY
        if (displayMode == MODE_YEAR) {
            b.attendanceSummaryTitle.setText(getString(R.string.attendance_summary_title_year));
        }
        else {
            b.attendanceSummaryTitle.setText(getString(R.string.attendance_summary_title_semester_format, displayMode));
        }
        b.presentCountContainer.setVisibility(presentCount == 0 ? View.GONE : View.VISIBLE);
        b.presentCount.setText(String.format(Locale.getDefault(), "%d", presentCount));
        b.absentCount.setText(String.format(Locale.getDefault(), "%d", absentCount));
        b.absentUnexcusedCount.setText(String.format(Locale.getDefault(), "%d", absentUnexcusedCount));
        b.belatedCount.setText(String.format(Locale.getDefault(), "%d", belatedCount));
        b.releasedCount.setText(String.format(Locale.getDefault(), "%d", releasedCount));
        if (absentUnexcusedCount >= 5) {
            b.absentUnexcusedCount.setTextColor(Color.RED);
        }
        else {
            b.absentUnexcusedCount.setTextColor(Themes.INSTANCE.getPrimaryTextColor(activity));
        }

        float attendancePercentage;

        // in Mobidziennik there are no TYPE_PRESENT records so we cannot calculate the percentage
        if (app.profile.getLoginStoreType() == LOGIN_TYPE_VULCAN) {
            float allCount = presentCount + absentCount + belatedCount; // do not count releases
            float present = allCount - absentCount;
            attendancePercentage = present / allCount * 100.0f;
        }
        else {
            float allCount = presentCount + absentCount + belatedCount + releasedCount;
            float present = allCount - absentCount;
            attendancePercentage = present / allCount * 100.0f;
        }
        // if it's still 0%, hide the indicator
        if (attendancePercentage <= 0.0f) {
            b.attendancePercentage.setVisibility(View.GONE);
            return;
        }
        animatePercentageIndicator(attendancePercentage);
    }

    private void animatePercentageIndicator(float percentage) {
        Animation a = new Animation() {
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                float progress = percentage *interpolatedTime;
                if (interpolatedTime == 1.0f) {
                    progress = percentage;
                }
                int color = ColorUtils.blendARGB(Color.RED, Color.GREEN, progress/100.0f);
                b.attendancePercentage.setTextColor(color);
                b.attendancePercentage.setProgressColor(color);
                b.attendancePercentage.setCurrentProgress(progress);
            }
            public boolean willChangeBounds() {
                return true;
            }
        };
        a.setDuration(1300);
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        b.attendancePercentage.postDelayed(() -> b.attendancePercentage.startAnimation(a), 500);
    }

    private static final CircularProgressIndicator.ProgressTextAdapter PERCENTAGE_ADAPTER = value -> {
        DecimalFormat df = new DecimalFormat("0.##");
        return df.format(value)+"%";
    };
}
