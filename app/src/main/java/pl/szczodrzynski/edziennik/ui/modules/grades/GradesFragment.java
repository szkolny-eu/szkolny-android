package pl.szczodrzynski.edziennik.ui.modules.grades;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;

import java.util.ArrayList;
import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.config.ProfileConfigGrades;
import pl.szczodrzynski.edziennik.data.db.entity.Grade;
import pl.szczodrzynski.edziennik.data.db.entity.Subject;
import pl.szczodrzynski.edziennik.data.db.full.GradeFull;
import pl.szczodrzynski.edziennik.databinding.FragmentGradesBinding;
import pl.szczodrzynski.edziennik.ui.dialogs.settings.GradesConfigDialog;
import pl.szczodrzynski.edziennik.utils.Themes;
import pl.szczodrzynski.edziennik.utils.models.ItemGradesSubjectModel;
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem;
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem;

import static pl.szczodrzynski.edziennik.config.ConfigGrades.ORDER_BY_DATE_ASC;
import static pl.szczodrzynski.edziennik.config.ConfigGrades.ORDER_BY_DATE_DESC;
import static pl.szczodrzynski.edziennik.config.ConfigGrades.ORDER_BY_SUBJECT_ASC;
import static pl.szczodrzynski.edziennik.data.db.entity.Metadata.TYPE_GRADE;
import static pl.szczodrzynski.edziennik.data.db.entity.Profile.YEAR_1_AVG_2_AVG;
import static pl.szczodrzynski.edziennik.data.db.entity.Profile.YEAR_1_AVG_2_SEM;
import static pl.szczodrzynski.edziennik.data.db.entity.Profile.YEAR_1_SEM_2_AVG;
import static pl.szczodrzynski.edziennik.data.db.entity.Profile.YEAR_1_SEM_2_SEM;
import static pl.szczodrzynski.edziennik.data.db.entity.Profile.YEAR_ALL_GRADES;

public class GradesFragment extends Fragment {

    private App app = null;
    private MainActivity activity = null;
    private FragmentGradesBinding b = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        if (getActivity() == null || getContext() == null)
            return null;
        app = (App) activity.getApplication();
        getContext().getTheme().applyStyle(Themes.INSTANCE.getAppTheme(), true);
        // activity, context and profile is valid
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_grades, container, false);
        b.refreshLayout.setParent(activity.getSwipeRefreshLayout());
        b.refreshLayout.setNestedScrollingEnabled(true);
        return b.getRoot();
    }

    ListView listView;
    List<ItemGradesSubjectModel> subjectList;

    private String getRegisterCardAverageModeSubText() {
        switch (App.Companion.getConfig().forProfile().getGrades().getYearAverageMode()) {
            default:
            case YEAR_1_AVG_2_AVG:
                return getString(R.string.settings_register_avg_mode_0_short);
            case YEAR_1_SEM_2_AVG:
                return getString(R.string.settings_register_avg_mode_1_short);
            case YEAR_1_AVG_2_SEM:
                return getString(R.string.settings_register_avg_mode_2_short);
            case YEAR_1_SEM_2_SEM:
                return getString(R.string.settings_register_avg_mode_3_short);
            case YEAR_ALL_GRADES:
                return getString(R.string.settings_register_avg_mode_4_short);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (app == null || activity == null || b == null || !isAdded())
            return;

        /*activity.getBottomSheet().setToggleGroupEnabled(true);
        activity.getBottomSheet().toggleGroupRemoveItems();
        activity.getBottomSheet().setToggleGroupSelectionMode(NavBottomSheet.TOGGLE_GROUP_SORTING_ORDER);
        activity.getBottomSheet().toggleGroupAddItem(0, getString(R.string.sort_by_date), (Drawable)null, SORT_MODE_DESCENDING);
        activity.getBottomSheet().toggleGroupAddItem(1, getString(R.string.sort_by_subject), (Drawable)null, SORT_MODE_ASCENDING);
        activity.getBottomSheet().setToggleGroupSortingOrderListener((id, sortMode) -> {
            sortModeChanged = true;
            if (id == 0 && sortMode == SORT_MODE_ASCENDING) {
                app.appConfig.gradesOrderBy = ORDER_BY_DATE_ASC;
            }
            else if (id == 1 && sortMode == SORT_MODE_ASCENDING) {
                app.appConfig.gradesOrderBy = ORDER_BY_SUBJECT_ASC;
            }
            else if (id == 0 && sortMode == SORT_MODE_DESCENDING) {
                app.appConfig.gradesOrderBy = ORDER_BY_DATE_DESC;
            }
            else if (id == 1 && sortMode == SORT_MODE_DESCENDING) {
                app.appConfig.gradesOrderBy = ORDER_BY_SUBJECT_DESC;
            }
            return null;
        });
        activity.getBottomSheet().setToggleGroupTitle("Sortowanie");
        activity.getBottomSheet().toggleGroupCheck(0);
        activity.getBottomSheet().setOnCloseListener(() -> {
            if (sortModeChanged) {
                sortModeChanged = false;
                activity.reloadTarget();
            }
            return null;
        });*/

        activity.getBottomSheet().prependItems(
                new BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_grades_averages)
                        .withDescription(R.string.menu_grades_averages_desc)
                        .withIcon(CommunityMaterial.Icon.cmd_chart_line)
                        .withOnClickListener(v3 -> {
                            activity.getBottomSheet().close();
                            showAverages();
                        }),
                new BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_grades_config)
                        .withIcon(CommunityMaterial.Icon2.cmd_settings_outline)
                        .withOnClickListener(v3 -> {
                            activity.getBottomSheet().close();
                            new GradesConfigDialog(activity, null, null);
                        }),
                new BottomSheetSeparatorItem(true),
                new BottomSheetPrimaryItem(true)
                        .withTitle(R.string.menu_mark_as_read)
                        .withIcon(CommunityMaterial.Icon.cmd_eye_check_outline)
                        .withOnClickListener(v3 -> {
                            activity.getBottomSheet().close();
                            AsyncTask.execute(() -> App.db.metadataDao().setAllSeen(App.Companion.getProfileId(), TYPE_GRADE, true));
                            Toast.makeText(activity, R.string.main_menu_mark_as_read_success, Toast.LENGTH_SHORT).show();
                        })
        );
        activity.gainAttention();

        /*b.refreshLayout.setOnRefreshListener(() -> {
            activity.syncCurrentFeature(MainActivity.DRAWER_ITEM_GRADES, b.refreshLayout);
        });*/

        listView = b.gradesRecyclerView;
        //listView.setHasFixedSize(true);
        //listView.setLayoutManager(new LinearLayoutManager(getContext()));

        long expandSubjectId = -1;
        if (getArguments() != null) {
            expandSubjectId = getArguments().getLong("gradesSubjectId", -1);
        }

        /*b.gradesSwipeLayout.setOnRefreshListener(() -> {
            Toast.makeText(activity, "Works!", Toast.LENGTH_LONG).show();
            // To keep animation for 4 seconds
            new Handler().postDelayed(() -> {
                // Stop animation (This will be after 3 seconds)
                b.gradesSwipeLayout.setRefreshing(false);
            }, 3000);
        });*/

        long finalExpandSubjectId = expandSubjectId;
        String orderBy;
        if (app.getConfig().getGrades().getOrderBy() == ORDER_BY_SUBJECT_ASC) {
            orderBy = "subjectLongName ASC, addedDate DESC";
        }
        else if (app.getConfig().getGrades().getOrderBy() == ORDER_BY_DATE_DESC) {
            orderBy = "addedDate DESC";
        }
        else if (app.getConfig().getGrades().getOrderBy() == ORDER_BY_DATE_ASC) {
            orderBy = "addedDate ASC";
        }
        else {
            orderBy = "subjectLongName DESC, addedDate DESC";
        }

        App.db.gradeDao().getAllOrderBy(App.Companion.getProfileId(), orderBy).observe(this, grades -> {
            if (app == null || activity == null || b == null || !isAdded())
                return;

            subjectList = new ArrayList<>();

            ProfileConfigGrades config = app.getConfig().getFor(App.Companion.getProfileId()).getGrades();

            // now we have all grades from the newest to the oldest
            for (GradeFull grade: grades) {
                ItemGradesSubjectModel model = ItemGradesSubjectModel.searchModelBySubjectId(subjectList, grade.subjectId);
                if (model == null) {
                    model = new ItemGradesSubjectModel(app.getProfile(),
                            new Subject(App.Companion.getProfileId(), grade.subjectId, grade.subjectLongName, grade.subjectShortName),
                            new ArrayList<>(),
                            new ArrayList<>());
                    subjectList.add(model);
                    if (model.subject != null && model.subject.id == finalExpandSubjectId) {
                        model.expandView = true;
                    }
                    model.colorMode = App.Companion.getConfig().forProfile().getGrades().getColorMode();
                    model.yearAverageMode = App.Companion.getConfig().forProfile().getGrades().getYearAverageMode();
                }
                if (!grade.seen && grade.semester == 1) {
                    model.semester1Unread++;
                }
                if (!grade.seen && grade.semester == 2) {
                    model.semester2Unread++;
                }
                // COUNT POINT GRADES
                if (grade.type == Grade.TYPE_POINT_AVG) {
                    model.isPointSubject = true;
                    if (grade.semester == 1) {
                        model.gradeSumOverall += grade.value;
                        model.gradeCountOverall += grade.valueMax;
                        model.gradeSumSemester1 += grade.value;
                        model.gradeCountSemester1 += grade.valueMax;
                        model.semester1Average = model.gradeSumSemester1 / model.gradeCountSemester1 * 100;
                        model.grades1.add(grade);
                    }
                    if (grade.semester == 2) {
                        model.gradeSumOverall += grade.value;
                        model.gradeCountOverall += grade.valueMax;
                        model.gradeSumSemester2 += grade.value;
                        model.gradeCountSemester2 += grade.valueMax;
                        model.semester2Average = model.gradeSumSemester2 / model.gradeCountSemester2 * 100;
                        model.grades2.add(grade);
                    }
                }
                else if (grade.type == Grade.TYPE_POINT_SUM) {
                    model.isBehaviourSubject = true;
                    if (grade.semester == 1) {
                        model.semester1Average += grade.value;
                        model.yearAverage += grade.value;
                        model.grades1.add(grade);
                    }
                    if (grade.semester == 2) {
                        model.semester2Average += grade.value;
                        model.yearAverage += grade.value;
                        model.grades2.add(grade);
                    }
                }
                else if (grade.type == Grade.TYPE_NORMAL) {
                    model.isNormalSubject = true;
                    float weight = grade.weight;
                    if (weight < 0) {
                        // do not show *normal* grades with negative weight - these are historical grades - Iuczniowie
                        continue;
                    }
                    if (!config.getCountZeroToAvg() && grade.name.equals("0")) {
                        weight = 0;
                    }
                    float valueWeighted = grade.value * weight;
                    if (grade.semester == 1) {
                        model.gradeSumOverall += valueWeighted;
                        model.gradeCountOverall += weight;
                        model.gradeSumSemester1 += valueWeighted;
                        model.gradeCountSemester1 += weight;
                        model.semester1Average = model.gradeSumSemester1 / model.gradeCountSemester1;
                        if (grade.parentId == -1) {
                            // show only "current" grades - these which are not historical
                            model.grades1.add(grade);
                        }
                    }
                    if (grade.semester == 2) {
                        model.gradeSumOverall += valueWeighted;
                        model.gradeCountOverall += weight;
                        model.gradeSumSemester2 += valueWeighted;
                        model.gradeCountSemester2 += weight;
                        model.semester2Average = model.gradeSumSemester2 / model.gradeCountSemester2;
                        if (grade.parentId == -1) {
                            // show only "current" grades - these which are not historical
                            model.grades2.add(grade);
                        }
                    }
                }
                else if (grade.type == Grade.TYPE_SEMESTER1_PROPOSED) {
                    model.semester1Proposed = grade;
                }
                else if (grade.type == Grade.TYPE_SEMESTER1_FINAL) {
                    model.semester1Final = grade;
                }
                else if (grade.type == Grade.TYPE_SEMESTER2_PROPOSED) {
                    model.semester2Proposed = grade;
                }
                else if (grade.type == Grade.TYPE_SEMESTER2_FINAL) {
                    model.semester2Final = grade;
                }
                else if (grade.type == Grade.TYPE_YEAR_PROPOSED) {
                    model.yearProposed = grade;
                }
                else if (grade.type == Grade.TYPE_YEAR_FINAL) {
                    model.yearFinal = grade;
                }
                else {
                    // descriptive grades, text grades
                    model.isDescriptiveSubject = true;
                    if (grade.semester == 1) {
                        model.grades1.add(grade);
                    }
                    if (grade.semester == 2) {
                        model.grades2.add(grade);
                    }
                }
            }

            for (ItemGradesSubjectModel model: subjectList) {
                if (model.isPointSubject) {
                    model.yearAverage = model.gradeSumOverall / model.gradeCountOverall * 100.0f; // map the point grade "average" % value from 0.0f-1.0f to 0%-100%
                }
                /*else if (model.isDescriptiveSubject && !model.isNormalSubject) {
                    // applies for only descriptive grades - do nothing. average is hidden
                    //model.isDescriptiveSubject = false;
                    //model.semester1Average = -1;
                    //model.semester2Average = -1;
                    //model.yearAverage = -1;
                }*/
                else if (!model.isBehaviourSubject && model.isNormalSubject) {
                    // applies for normal grades & normal+descriptive grades
                    // calculate the normal grade average based on the user's setting
                    switch (App.Companion.getConfig().forProfile().getGrades().getYearAverageMode()) {
                        case YEAR_1_AVG_2_AVG:
                            model.yearAverage = (model.semester1Average + model.semester2Average) / 2;
                            break;
                        case YEAR_1_SEM_2_AVG:
                            model.yearAverage = model.semester1Final != null ? (model.semester1Final.value + model.semester2Average) / 2 : 0.0f;
                            break;
                        case YEAR_1_AVG_2_SEM:
                            model.yearAverage = model.semester2Final != null ? (model.semester1Average + model.semester2Final.value) / 2 : 0.0f;
                            break;
                        case YEAR_1_SEM_2_SEM:
                            model.yearAverage = model.semester1Final != null && model.semester2Final != null ? (model.semester1Final.value + model.semester2Final.value) / 2 : 0.0f;
                            break;
                        default:
                        case YEAR_ALL_GRADES:
                            model.yearAverage = model.gradeSumOverall / model.gradeCountOverall;
                            break;
                    }
                }
            }

            if (subjectList.size() > 0) {
                GradesSubjectAdapter adapter;
                if ((adapter = (GradesSubjectAdapter) listView.getAdapter()) != null) {
                    adapter.subjectList = subjectList;
                    adapter.notifyDataSetChanged();
                    return;
                }
                adapter = new GradesSubjectAdapter(subjectList, activity);
                listView.setAdapter(adapter);
                listView.setVisibility(View.VISIBLE);
                b.gradesNoData.setVisibility(View.GONE);
                if (finalExpandSubjectId != -1) {
                    int subjectIndex = subjectList.indexOf(ItemGradesSubjectModel.searchModelBySubjectId(subjectList, finalExpandSubjectId));
                    listView.setSelection(subjectIndex > 0 ? subjectIndex - 1 : subjectIndex);
                }
            }
            else {
                listView.setVisibility(View.GONE);
                b.gradesNoData.setVisibility(View.VISIBLE);
            }
        });

    }

    private int gradeFromAverage(float value) {
        int grade = (int)Math.floor(value);
        if (value % 1.0f >= 0.75f)
            grade++;
        return grade;
    }

    public void showAverages() {
        if (app == null || activity == null || b == null || !isAdded() || subjectList == null)
            return;

        float semester1Sum = 0;
        float semester1Count = 0;
        float semester1ProposedSum = 0;
        float semester1ProposedCount = 0;
        float semester1FinalSum = 0;
        float semester1FinalCount = 0;

        float semester2Sum = 0;
        float semester2Count = 0;
        float semester2ProposedSum = 0;
        float semester2ProposedCount = 0;
        float semester2FinalSum = 0;
        float semester2FinalCount = 0;

        float yearSum = 0;
        float yearCount = 0;
        float yearProposedSum = 0;
        float yearProposedCount = 0;
        float yearFinalSum = 0;
        float yearFinalCount = 0;

        for (ItemGradesSubjectModel subject: subjectList) {
            // we cannot skip non-normal subjects because a point subject may also have a final grade
            if (subject.isBehaviourSubject)
                continue;

            // SEMESTER 1 GRADES & AVERAGES
            if (subject.semester1Final != null && subject.semester1Final.value > 0) { // if final available, add to final grades & expected grades
                semester1FinalSum += subject.semester1Final.value;
                semester1FinalCount++;
                semester1Sum += subject.semester1Final.value;
                semester1Count++;
            }
            else if (subject.semester1Proposed != null && subject.semester1Proposed.value > 0) { // if final not available, add proposed to expected grades
                semester1Sum += subject.semester1Proposed.value;
                semester1Count++;
            }
            else if (!Float.isNaN(subject.semester1Average)
                    && subject.semester1Average > 0
                    && !subject.isPointSubject) { // if final&proposed unavailable, calculate from avg
                semester1Sum += gradeFromAverage(subject.semester1Average);
                semester1Count++;
            }
            if (subject.semester1Proposed != null && subject.semester1Proposed.value > 0) { // add proposed to proposed grades even if final is available
                semester1ProposedSum += subject.semester1Proposed.value;
                semester1ProposedCount++;
            }

            // SEMESTER 2 GRADES & AVERAGES
            if (subject.semester2Final != null && subject.semester2Final.value > 0) { // if final available, add to final grades & expected grades
                semester2FinalSum += subject.semester2Final.value;
                semester2FinalCount++;
                semester2Sum += subject.semester2Final.value;
                semester2Count++;
            }
            else if (subject.semester2Proposed != null && subject.semester2Proposed.value > 0) { // if final not available, add proposed to expected grades
                semester2Sum += subject.semester2Proposed.value;
                semester2Count++;
            }
            else if (!Float.isNaN(subject.semester2Average)
                    && subject.semester2Average > 0
                    && !subject.isPointSubject) { // if final&proposed unavailable, calculate from avg
                semester2Sum += gradeFromAverage(subject.semester2Average);
                semester2Count++;
            }
            if (subject.semester2Proposed != null && subject.semester2Proposed.value > 0) { // add proposed to proposed grades even if final is available
                semester2ProposedSum += subject.semester2Proposed.value;
                semester2ProposedCount++;
            }

            // YEAR GRADES & AVERAGES
            if (subject.yearFinal != null && subject.yearFinal.value > 0) { // if final available, add to final grades & expected grades
                yearFinalSum += subject.yearFinal.value;
                yearFinalCount++;
                yearSum += subject.yearFinal.value;
                yearCount++;
            }
            else if (subject.yearProposed != null && subject.yearProposed.value > 0) { // if final not available, add proposed to expected grades
                yearSum += subject.yearProposed.value;
                yearCount++;
            }
            else if (!Float.isNaN(subject.yearAverage)
                    && subject.yearAverage > 0
                    && !subject.isPointSubject) { // if final&proposed unavailable, calculate from avg
                yearSum += gradeFromAverage(subject.yearAverage);
                yearCount++;
            }
            if (subject.yearProposed != null && subject.yearProposed.value > 0) { // add proposed to proposed grades even if final is available
                yearProposedSum += subject.yearProposed.value;
                yearProposedCount++;
            }
        }

        String semester1ExpectedAverageStr = semester1Count > semester1ProposedCount || semester1Count > semester1FinalCount ? getString(R.string.dialog_averages_expected_format, 1, semester1Sum / semester1Count) : "";
        String semester1ProposedAverageStr = semester1ProposedCount > 0 ? getString(R.string.dialog_averages_proposed_format, 1, semester1ProposedSum / semester1ProposedCount) : "";
        String semester1FinalAverageStr = semester1FinalCount > 0 ? getString(R.string.dialog_averages_final_format, 1, semester1FinalSum / semester1FinalCount) : "";

        String semester2ExpectedAverageStr = semester2Count > semester2ProposedCount || semester2Count > semester2FinalCount ? getString(R.string.dialog_averages_expected_format, 2, semester2Sum / semester2Count) : "";
        String semester2ProposedAverageStr = semester2ProposedCount > 0 ? getString(R.string.dialog_averages_proposed_format, 2, semester2ProposedSum / semester2ProposedCount) : "";
        String semester2FinalAverageStr = semester2FinalCount > 0 ? getString(R.string.dialog_averages_final_format, 2, semester2FinalSum / semester2FinalCount) : "";

        String yearExpectedAverageStr = yearCount > yearProposedCount || yearCount > yearFinalCount ? getString(R.string.dialog_averages_expected_yearly_format, yearSum / yearCount) : "";
        String yearProposedAverageStr = yearProposedCount > 0 ? getString(R.string.dialog_averages_proposed_yearly_format, yearProposedSum / yearProposedCount) : "";
        String yearFinalAverageStr = yearFinalCount > 0 ? getString(R.string.dialog_averages_final_yearly_format, yearFinalSum / yearFinalCount) : "";

        if (semester1ExpectedAverageStr.isEmpty() && semester1ProposedAverageStr.isEmpty() && semester1FinalAverageStr.isEmpty()) {
            semester1ExpectedAverageStr = getString(R.string.dialog_averages_unavailable_format, 1);
        }
        if (semester2ExpectedAverageStr.isEmpty() && semester2ProposedAverageStr.isEmpty() && semester2FinalAverageStr.isEmpty()) {
            semester2ExpectedAverageStr = getString(R.string.dialog_averages_unavailable_format, 2);
        }
        if (yearExpectedAverageStr.isEmpty() && yearProposedAverageStr.isEmpty() && yearFinalAverageStr.isEmpty()) {
            yearExpectedAverageStr = getString(R.string.dialog_averages_unavailable_yearly);
        }

        new MaterialDialog.Builder(activity)
                .title(R.string.dialog_averages_title)
                .content(getString(
                        R.string.dialog_averages_format,
                        semester1ExpectedAverageStr,
                        semester1ProposedAverageStr,
                        semester1FinalAverageStr,
                        semester2ExpectedAverageStr,
                        semester2ProposedAverageStr,
                        semester2FinalAverageStr,
                        yearExpectedAverageStr,
                        yearProposedAverageStr,
                        yearFinalAverageStr))
                .positiveText(R.string.ok)
                .show();
    }
}
