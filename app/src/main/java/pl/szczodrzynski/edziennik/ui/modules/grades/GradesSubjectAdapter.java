package pl.szczodrzynski.edziennik.ui.modules.grades;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mikepenz.iconics.view.IconicsImageView;

import java.text.DecimalFormat;
import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.data.db.AppDb;
import pl.szczodrzynski.edziennik.data.db.modules.grades.GradeFull;
import pl.szczodrzynski.edziennik.data.db.modules.subjects.Subject;
import pl.szczodrzynski.edziennik.utils.models.ItemGradesSubjectModel;
import pl.szczodrzynski.edziennik.utils.Anim;
import pl.szczodrzynski.edziennik.utils.Colors;
import pl.szczodrzynski.edziennik.utils.Utils;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static pl.szczodrzynski.edziennik.MainActivity.TARGET_GRADES_EDITOR;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.COLOR_MODE_DEFAULT;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.YEAR_1_AVG_2_SEM;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.YEAR_1_SEM_2_AVG;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.YEAR_1_SEM_2_SEM;

public class GradesSubjectAdapter extends ArrayAdapter<ItemGradesSubjectModel> implements View.OnClickListener {
    private static final String TAG = "GradesSubjectAdapter";
    private MainActivity activity;
    public List<ItemGradesSubjectModel> subjectList;

    private void updateBadges(Context context, ItemGradesSubjectModel model) {
        // do not need this since we have an Observer for unread counters..
        //((App)getContext().getApplicationContext()).saveRegister(); // I don't like this.
    }

    private boolean invalidAvg(float avg) {
        return avg == 0.0f || Float.isNaN(avg);
    }

    private void updateSubjectSemesterBadges(ViewHolder holder, ItemGradesSubjectModel model) {
        if (model.semester1Unread > 0 || model.semester2Unread > 0) {
            holder.gradesSubjectTitle.setBackground(getContext().getResources().getDrawable(R.drawable.bg_rounded_4dp));
            holder.gradesSubjectTitle.getBackground().setColorFilter(new PorterDuffColorFilter(0x692196f3, PorterDuff.Mode.MULTIPLY));
        }
        else {
            holder.gradesSubjectTitle.setBackground(null);
        }
        if (model.semester1Unread > 0) {
            holder.gradesSubjectSemester1Title.setBackground(getContext().getResources().getDrawable(R.drawable.bg_rounded_4dp));
            holder.gradesSubjectSemester1Title.getBackground().setColorFilter(new PorterDuffColorFilter(0x692196f3, PorterDuff.Mode.MULTIPLY));
        }
        else {
            holder.gradesSubjectSemester1Title.setBackground(null);
        }
        if (model.semester2Unread > 0) {
            holder.gradesSubjectSemester2Title.setBackground(getContext().getResources().getDrawable(R.drawable.bg_rounded_4dp));
            holder.gradesSubjectSemester2Title.getBackground().setColorFilter(new PorterDuffColorFilter(0x692196f3, PorterDuff.Mode.MULTIPLY));
        }
        else {
            holder.gradesSubjectSemester2Title.setBackground(null);
        }
    }

    private boolean gradesSetAsRead(ViewHolder holder, ItemGradesSubjectModel model, int semester) {
        boolean somethingChanged = false;
        AppDb db = AppDb.getDatabase(null);
        if (semester == 1) {
            model.semester1Unread = 0;
            for (GradeFull grade : model.grades1) {
                if (!grade.seen) {
                    db.metadataDao().setSeen(App.profileId, grade, somethingChanged = true);
                }
            }
            if (model.semester1Proposed != null && !model.semester1Proposed.seen)
                db.metadataDao().setSeen(App.profileId, model.semester1Proposed, somethingChanged = true);
            if (model.semester1Final != null && !model.semester1Final.seen)
                db.metadataDao().setSeen(App.profileId, model.semester1Final, somethingChanged = true);
        }
        else if (semester == 2) {
            model.semester2Unread = 0;
            for (GradeFull grade : model.grades2) {
                if (!grade.seen) {
                    db.metadataDao().setSeen(App.profileId, grade, somethingChanged = true);
                }
            }
            if (model.semester2Proposed != null && !model.semester2Proposed.seen)
                db.metadataDao().setSeen(App.profileId, model.semester2Proposed, somethingChanged = true);
            if (model.semester2Final != null && !model.semester2Final.seen)
                db.metadataDao().setSeen(App.profileId, model.semester2Final, somethingChanged = true);
            if (model.yearProposed != null && !model.yearProposed.seen)
                db.metadataDao().setSeen(App.profileId, model.yearProposed, somethingChanged = true);
            if (model.yearFinal != null && !model.yearFinal.seen)
                db.metadataDao().setSeen(App.profileId, model.yearFinal, somethingChanged = true);
        }
        if (somethingChanged) updateSubjectSemesterBadges(holder, model);
        return somethingChanged;
    }

    private void expandSubject(ViewHolder holder, ItemGradesSubjectModel model) {
        Anim.fadeOut(holder.gradesSubjectPreviewContainer, 200, new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) { }
            @Override public void onAnimationRepeat(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                boolean somethingChanged = false;
                if (holder.gradesSubjectSemester1Container.getVisibility() != View.GONE) {
                    somethingChanged = gradesSetAsRead(holder, model, 1);
                }
                if (holder.gradesSubjectSemester2Container.getVisibility() != View.GONE) {
                    somethingChanged = gradesSetAsRead(holder, model, 2);
                }
                if (somethingChanged) updateBadges(getContext(), model);
                //holder.gradesSubjectPreviewContent.setVisibility(View.INVISIBLE);
                Anim.expand(holder.gradesSubjectContent, 500, null);
            }
        });
    }

    private void collapseSubject(ViewHolder holder, ItemGradesSubjectModel model) {
        Anim.collapse(holder.gradesSubjectContent, 500, new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) { }
            @Override public void onAnimationRepeat(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                //holder.gradesSubjectPreviewContent.setVisibility(View.VISIBLE);
                Anim.fadeIn(holder.gradesSubjectPreviewContainer, 200, null);
            }
        });
    }

    class BuildGradeViews extends AsyncTask<Void, Void, Void> {
        boolean findViews;
        ItemGradesSubjectModel model;
        //ViewGroup parent;
        //int position;
        ViewHolder holder;

        BuildGradeViews(ItemGradesSubjectModel model, ViewHolder holder, ViewGroup parent, int position, boolean findViews) {
            this.model = model;
            this.holder = holder;
            this.findViews = findViews;
            //this.parent = parent;
            //this.position = position;
        }

        protected Void doInBackground(Void... params) {
            if (this.findViews) {
                findViews(holder, holder.gradesSubjectRoot);
            }
            return null;
        }

        protected void onPostExecute(Void aVoid) {
            DecimalFormat df = new DecimalFormat("#.00");
            if (this.findViews) {
                // TODO NIE WIEM CO TO ROBI XD
                //this.viewHolder.semestrTitle1.setText(C0193R.string.semestr1);
                //this.viewHolder.semestrTitle2.setText(C0193R.string.semestr2);
                /*if (GradesSubjectAdapter.this.columns == 0) {
                    DisplayMetrics metrics = GradeListAdapterBySubject.this.context.getResources().getDisplayMetrics();
                    if (GradeListAdapterBySubject.this.context.getResources().getBoolean(C0193R.bool.tablet)) {
                        GradeListAdapterBySubject.this.columns = ((int) ((((float) (this.parent.getWidth() / 2)) / metrics.density) - 30.0f)) / 58;
                    } else {
                        GradeListAdapterBySubject.this.columns = ((int) ((((float) this.parent.getWidth()) / metrics.density) - 30.0f)) / 58;
                    }
                }
                this.viewHolder.semestrGridView1.setColumnCount(GradeListAdapterBySubject.this.columns);
                this.viewHolder.semestrGridView2.setColumnCount(GradeListAdapterBySubject.this.columns);*/
            }
            if (model != null && model.subject != null && model.subject.id != holder.lastSubject) {
                holder.gradesSubjectRoot.setBackground(Colors.getAdaptiveBackgroundDrawable(model.subject.color, model.subject.color));
                updateSubjectSemesterBadges(holder, model);
                holder.gradesSubjectTitle.setText(model.subject.longName);
                holder.gradesSubjectPreviewContent.removeAllViews();

                if (model.expandView) {
                    // commented is without animations, do not use now (with unread badges)
                    //holder.gradesSubjectContent.setVisibility(View.VISIBLE);
                    //holder.gradesSubjectPreviewContent.setVisibility(View.INVISIBLE);
                    expandSubject(holder, model);
                    model.expandView = false;
                }
                int showSemester = model.profile.getCurrentSemester();

                List<GradeFull> gradeList = (showSemester == 1 ? model.grades1 : model.grades2);
                if (gradeList.size() == 0) {
                    showSemester = (showSemester == 1 ? 2 : 1);
                    gradeList = (showSemester == 1 ? model.grades1 : model.grades2);
                }

                App app = (App) getContext().getApplicationContext();

                float scale = getContext().getResources().getDisplayMetrics().density;
                int _5dp = (int) (5 * scale + 0.5f);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                layoutParams.setMargins(0, 0, _5dp, 0);

                DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
                int maxWidthPx = displayMetrics.widthPixels - Utils.dpToPx((app.appConfig.miniDrawerVisible ? 72 : 0)/*miniDrawer size*/ + 8 + 8/*left and right offsets*/ + 24/*ellipsize width*/);
                int totalWidthPx = 0;
                boolean ellipsized = false;

                if (showSemester != model.profile.getCurrentSemester()) {
                    // showing different semester, because of no grades in the selected one
                    holder.gradesSubjectPreviewSemester.setVisibility(View.VISIBLE);
                    holder.gradesSubjectPreviewSemester.setText(getContext().getString(R.string.grades_semester_header_format, showSemester));
                    // decrease the max preview width. DONE below
                    /*holder.gradesSubjectPreviewSemester.measure(WRAP_CONTENT, WRAP_CONTENT);
                    maxWidthPx -= holder.gradesSubjectPreviewSemester.getMeasuredWidth();
                    maxWidthPx -= _5dp;*/
                }
                else {
                    holder.gradesSubjectPreviewSemester.setVisibility(View.GONE);
                }


                if (model.grades1.size() > 0) {
                    holder.gradesSubjectSemester1Nest.setNestedScrollingEnabled(false);
                    holder.gradesSubjectSemester1Content.setHasFixedSize(false);
                    holder.gradesSubjectSemester1Content.setNestedScrollingEnabled(false);
                    holder.gradesSubjectSemester1Content.setLayoutManager(new LinearLayoutManager(activity));
                    holder.gradesSubjectSemester1Content.setAdapter(new GradesListAdapter(activity, model.grades1));
                    holder.gradesSubjectSemester1Header.setVisibility(View.VISIBLE);
                    if (showSemester == 1) {
                        holder.gradesSubjectSemester1Container.setVisibility(View.VISIBLE);
                    }
                    else {
                        holder.gradesSubjectSemester1Container.setVisibility(View.GONE);
                    }

                    if (model.isDescriptiveSubject && !model.isNormalSubject && !model.isPointSubject && !model.isBehaviourSubject) {
                        holder.gradesSubjectPreviewAverage.setVisibility(View.GONE);
                        holder.gradesSubjectSemester1Average.setVisibility(View.GONE);
                    }
                    else {
                        holder.gradesSubjectPreviewAverage.setVisibility(View.VISIBLE);
                        holder.gradesSubjectSemester1Average.setVisibility(View.VISIBLE);
                        int formatSingle = (model.isPointSubject ? R.string.grades_average_single_percent_format : model.isBehaviourSubject ? R.string.grades_average_single_point_format : R.string.grades_average_single_format);
                        int format = (model.isPointSubject ? R.string.grades_semester_average_percent_format : model.isBehaviourSubject ? R.string.grades_semester_average_point_format : R.string.grades_semester_average_format);
                        // PREVIEW AVERAGE
                        if (showSemester == 1) {
                            holder.gradesSubjectPreviewAverage.setText(
                                    getContext().getString(
                                            formatSingle,
                                            invalidAvg(model.semester1Average) ? "-" : df.format(model.semester1Average)
                                    )
                            );
                        }
                        // AVERAGE value
                        holder.gradesSubjectSemester1Average.setText(
                                getContext().getString(
                                        format,
                                        1,
                                        invalidAvg(model.semester1Average) ? "-" : df.format(model.semester1Average)
                                )
                        );
                    }

                    // PROPOSED grade
                    if (model.semester1Proposed != null) {
                        holder.gradesSubjectSemester1Proposed.setVisibility(View.VISIBLE);
                        holder.gradesSubjectSemester1Proposed.setText(model.semester1Proposed.name);
                        holder.gradesSubjectSemester1Proposed.getBackground().setColorFilter(new PorterDuffColorFilter(Colors.gradeNameToColor(model.semester1Proposed.name), PorterDuff.Mode.MULTIPLY));
                        holder.gradesSubjectSemester1Proposed.setTextColor(Colors.gradeNameToColor(model.semester1Proposed.name));
                        if (showSemester == 1) {
                            holder.gradesSubjectPreviewProposed.setVisibility(View.VISIBLE);
                            holder.gradesSubjectPreviewProposed.setText(model.semester1Proposed.name);
                            holder.gradesSubjectPreviewProposed.getBackground().setColorFilter(new PorterDuffColorFilter(Colors.gradeNameToColor(model.semester1Proposed.name), PorterDuff.Mode.MULTIPLY));
                            holder.gradesSubjectPreviewProposed.setTextColor(Colors.gradeNameToColor(model.semester1Proposed.name));
                        }
                    }
                    else {
                        holder.gradesSubjectSemester1Proposed.setVisibility(View.GONE);
                        if (showSemester == 1) {
                            holder.gradesSubjectPreviewProposed.setVisibility(View.GONE);
                        }
                    }
                    // FINAL grade
                    if (model.semester1Final != null) {
                        holder.gradesSubjectSemester1Final.setVisibility(View.VISIBLE);
                        holder.gradesSubjectSemester1Final.setText(model.semester1Final.name);
                        holder.gradesSubjectSemester1Final.getBackground().setColorFilter(new PorterDuffColorFilter(Colors.gradeNameToColor(model.semester1Final.name), PorterDuff.Mode.MULTIPLY));
                        if (showSemester == 1) {
                            holder.gradesSubjectPreviewFinal.setVisibility(View.VISIBLE);
                            holder.gradesSubjectPreviewFinal.setText(model.semester1Final.name);
                            holder.gradesSubjectPreviewFinal.getBackground().setColorFilter(new PorterDuffColorFilter(Colors.gradeNameToColor(model.semester1Final.name), PorterDuff.Mode.MULTIPLY));
                        }
                    }
                    else {
                        holder.gradesSubjectSemester1Final.setVisibility(View.GONE);
                        if (showSemester == 1) {
                            holder.gradesSubjectPreviewFinal.setVisibility(View.GONE);
                        }
                    }
                }
                else {
                    holder.gradesSubjectSemester1Header.setVisibility(View.GONE);
                    holder.gradesSubjectSemester1Container.setVisibility(View.GONE);
                }

                if (model.grades2.size() > 0) {
                    holder.gradesSubjectSemester2Nest.setNestedScrollingEnabled(false);
                    holder.gradesSubjectSemester2Content.setHasFixedSize(false);
                    holder.gradesSubjectSemester2Content.setNestedScrollingEnabled(false);
                    holder.gradesSubjectSemester2Content.setLayoutManager(new LinearLayoutManager(activity));
                    holder.gradesSubjectSemester2Content.setAdapter(new GradesListAdapter(activity, model.grades2));
                    holder.gradesSubjectSemester2Header.setVisibility(View.VISIBLE);
                    if (showSemester == 2) {
                        holder.gradesSubjectSemester2Container.setVisibility(View.VISIBLE);
                    }
                    else {
                        holder.gradesSubjectSemester2Container.setVisibility(View.GONE);
                    }

                    if (model.isDescriptiveSubject && !model.isNormalSubject && !model.isPointSubject && !model.isBehaviourSubject) {
                        holder.gradesSubjectPreviewAverage.setVisibility(View.GONE);
                        holder.gradesSubjectSemester2Average.setVisibility(View.GONE);
                    }
                    else {
                        holder.gradesSubjectPreviewAverage.setVisibility(View.VISIBLE);
                        holder.gradesSubjectSemester2Average.setVisibility(View.VISIBLE);
                        // PREVIEW AVERAGE
                        int formatDouble = (model.isPointSubject ? R.string.grades_average_double_percent_format : model.isBehaviourSubject ? R.string.grades_average_double_point_format : R.string.grades_average_double_format);
                        int format = (model.isPointSubject ? R.string.grades_semester_average_percent_format : model.isBehaviourSubject ? R.string.grades_semester_average_point_format : R.string.grades_semester_average_format);
                        if (showSemester == 2) {
                            if (model.semester2Proposed != null || model.semester2Final != null) {
                                holder.gradesSubjectPreviewAverage.setText(invalidAvg(model.semester2Average) ? "-" : df.format(model.semester2Average));
                                holder.gradesSubjectPreviewYearAverage.setText(invalidAvg(model.yearAverage) ? "-" : df.format(model.yearAverage));
                                holder.gradesSubjectPreviewYearAverage.setVisibility(View.VISIBLE);
                            }
                            else {
                                holder.gradesSubjectPreviewAverage.setText(
                                        getContext().getString(
                                                formatDouble,
                                                invalidAvg(model.semester2Average) ? "-" : df.format(model.semester2Average),
                                                invalidAvg(model.yearAverage) ? "-" : df.format(model.yearAverage)
                                        )
                                );
                                holder.gradesSubjectPreviewYearAverage.setVisibility(View.GONE);
                            }
                        }
                        // AVERAGE value
                        holder.gradesSubjectSemester2Average.setText(
                                getContext().getString(
                                        format,
                                        2,
                                        invalidAvg(model.semester2Average) ? "-" : df.format(model.semester2Average)
                                )
                        );
                    }

                    // PROPOSED grade
                    if (model.semester2Proposed != null) {
                        holder.gradesSubjectSemester2Proposed.setVisibility(View.VISIBLE);
                        holder.gradesSubjectSemester2Proposed.setText(model.semester2Proposed.name);
                        holder.gradesSubjectSemester2Proposed.getBackground().setColorFilter(new PorterDuffColorFilter(Colors.gradeNameToColor(model.semester2Proposed.name), PorterDuff.Mode.MULTIPLY));
                        holder.gradesSubjectSemester2Proposed.setTextColor(Colors.gradeNameToColor(model.semester2Proposed.name));
                        if (showSemester == 2) {
                            holder.gradesSubjectPreviewProposed.setVisibility(View.VISIBLE);
                            holder.gradesSubjectPreviewProposed.setText(model.semester2Proposed.name);
                            holder.gradesSubjectPreviewProposed.getBackground().setColorFilter(new PorterDuffColorFilter(Colors.gradeNameToColor(model.semester2Proposed.name), PorterDuff.Mode.MULTIPLY));
                            holder.gradesSubjectPreviewProposed.setTextColor(Colors.gradeNameToColor(model.semester2Proposed.name));
                        }
                    }
                    else {
                        holder.gradesSubjectSemester2Proposed.setVisibility(View.GONE);
                        if (showSemester == 2) {
                            holder.gradesSubjectPreviewProposed.setVisibility(View.GONE);
                        }
                    }
                    // FINAL grade
                    if (model.semester2Final != null) {
                        holder.gradesSubjectSemester2Final.setVisibility(View.VISIBLE);
                        holder.gradesSubjectSemester2Final.setText(model.semester2Final.name);
                        holder.gradesSubjectSemester2Final.getBackground().setColorFilter(new PorterDuffColorFilter(Colors.gradeNameToColor(model.semester2Final.name), PorterDuff.Mode.MULTIPLY));
                        if (showSemester == 2) {
                            holder.gradesSubjectPreviewFinal.setVisibility(View.VISIBLE);
                            holder.gradesSubjectPreviewFinal.setText(model.semester2Final.name);
                            holder.gradesSubjectPreviewFinal.getBackground().setColorFilter(new PorterDuffColorFilter(Colors.gradeNameToColor(model.semester2Final.name), PorterDuff.Mode.MULTIPLY));
                        }
                    }
                    else {
                        holder.gradesSubjectSemester2Final.setVisibility(View.GONE);
                        if (showSemester == 2) {
                            holder.gradesSubjectPreviewFinal.setVisibility(View.GONE);
                        }
                    }

                    if (model.isDescriptiveSubject && !model.isNormalSubject && !model.isPointSubject && !model.isBehaviourSubject) {
                        holder.gradesSubjectYearAverage.setVisibility(View.GONE);
                    }
                    else {
                        holder.gradesSubjectYearAverage.setVisibility(View.VISIBLE);
                        // AVERAGE value
                        int format = (model.isPointSubject ? R.string.grades_year_average_percent_format : model.isBehaviourSubject ? R.string.grades_year_average_point_format : R.string.grades_year_average_format);
                        holder.gradesSubjectYearAverage.setText(
                                getContext().getString(
                                        format,
                                        invalidAvg(model.yearAverage) ? "-" : df.format(model.yearAverage)
                                )
                        );
                    }

                    // PROPOSED grade
                    if (model.yearProposed != null) {
                        holder.gradesSubjectYearProposed.setVisibility(View.VISIBLE);
                        holder.gradesSubjectYearProposed.setText(model.yearProposed.name);
                        holder.gradesSubjectYearProposed.getBackground().setColorFilter(new PorterDuffColorFilter(Colors.gradeNameToColor(model.yearProposed.name), PorterDuff.Mode.MULTIPLY));
                        holder.gradesSubjectYearProposed.setTextColor(Colors.gradeNameToColor(model.yearProposed.name));
                        if (showSemester == 2) {
                            holder.gradesSubjectPreviewYearProposed.setVisibility(View.VISIBLE);
                            holder.gradesSubjectPreviewYearProposed.setText(model.yearProposed.name);
                            holder.gradesSubjectPreviewYearProposed.getBackground().setColorFilter(new PorterDuffColorFilter(Colors.gradeNameToColor(model.yearProposed.name), PorterDuff.Mode.MULTIPLY));
                            holder.gradesSubjectPreviewYearProposed.setTextColor(Colors.gradeNameToColor(model.yearProposed.name));
                        }
                    }
                    else {
                        holder.gradesSubjectYearProposed.setVisibility(View.GONE);
                        if (showSemester == 2) {
                            holder.gradesSubjectPreviewYearProposed.setVisibility(View.GONE);
                        }
                    }
                    // FINAL grade
                    if (model.yearFinal != null) {
                        holder.gradesSubjectYearFinal.setVisibility(View.VISIBLE);
                        holder.gradesSubjectYearFinal.setText(model.yearFinal.name);
                        holder.gradesSubjectYearFinal.getBackground().setColorFilter(new PorterDuffColorFilter(Colors.gradeNameToColor(model.yearFinal.name), PorterDuff.Mode.MULTIPLY));
                        if (showSemester == 2) {
                            holder.gradesSubjectPreviewYearFinal.setVisibility(View.VISIBLE);
                            holder.gradesSubjectPreviewYearFinal.setText(model.yearFinal.name);
                            holder.gradesSubjectPreviewYearFinal.getBackground().setColorFilter(new PorterDuffColorFilter(Colors.gradeNameToColor(model.yearFinal.name), PorterDuff.Mode.MULTIPLY));
                        }
                    }
                    else {
                        holder.gradesSubjectYearFinal.setVisibility(View.GONE);
                        if (showSemester == 2) {
                            holder.gradesSubjectPreviewYearFinal.setVisibility(View.GONE);
                        }
                    }
                }
                else {
                    holder.gradesSubjectSemester2Header.setVisibility(View.GONE);
                    holder.gradesSubjectSemester2Container.setVisibility(View.GONE);
                }

                // decrease the width by average, proposed, final and semester TextViews
                holder.gradesSubjectPreviewContainer.measure(WRAP_CONTENT, MATCH_PARENT);
                //Log.d(TAG, "gradesSubjectPreviewContainer "+holder.gradesSubjectPreviewContainer.getMeasuredWidth());

                /*holder.gradesSubjectPreviewAverage.measure(WRAP_CONTENT, WRAP_CONTENT);
                maxWidthPx -= holder.gradesSubjectPreviewAverage.getMeasuredWidth();
                maxWidthPx -= 2*_5dp;
                if (holder.gradesSubjectPreviewProposed.getVisibility() == View.VISIBLE) {
                    holder.gradesSubjectPreviewProposed.measure(WRAP_CONTENT, WRAP_CONTENT);
                    maxWidthPx -= holder.gradesSubjectPreviewProposed.getMeasuredWidth();
                    maxWidthPx -= _5dp;
                }
                if (holder.gradesSubjectPreviewFinal.getVisibility() == View.VISIBLE) {
                    holder.gradesSubjectPreviewFinal.measure(WRAP_CONTENT, WRAP_CONTENT);
                    maxWidthPx -= holder.gradesSubjectPreviewFinal.getMeasuredWidth();
                    maxWidthPx -= _5dp;
                }*/
                maxWidthPx -= holder.gradesSubjectPreviewContainer.getMeasuredWidth();
                maxWidthPx -= _5dp;

                for (GradeFull grade: gradeList) {
                    if (grade.semester != showSemester)
                        continue;

                    int gradeColor;
                    if (model.profile.getGradeColorMode() == COLOR_MODE_DEFAULT) {
                        gradeColor = grade.color;
                    } else {
                        gradeColor = Colors.gradeToColor(grade);
                    }

                    TextView gradeName = new TextView(activity);
                    gradeName.setText(grade.name);
                    gradeName.setTextColor(ColorUtils.calculateLuminance(gradeColor) > 0.25 ? 0xff000000 : 0xffffffff);
                    gradeName.setPadding(_5dp, 0, _5dp, 0);
                    gradeName.setBackgroundResource(R.drawable.bg_rounded_4dp);
                    gradeName.getBackground().setColorFilter(new PorterDuffColorFilter(gradeColor, PorterDuff.Mode.MULTIPLY));
                    gradeName.setTypeface(null, Typeface.BOLD);

                    gradeName.measure(WRAP_CONTENT, WRAP_CONTENT);
                    totalWidthPx += gradeName.getMeasuredWidth() + _5dp;
                    //Log.d(TAG, "totalWidthPx " + totalWidthPx);
                    if (totalWidthPx >= maxWidthPx) {
                        if (ellipsized)
                            continue;
                        ellipsized = true;
                        TextView ellipsisText = new TextView(activity);
                        ellipsisText.setText(R.string.ellipsis);
                        ellipsisText.setTextAppearance(activity, R.style.NavView_TextView);
                        ellipsisText.setTypeface(null, Typeface.BOLD);
                        ellipsisText.setPadding(0, 0, 0, 0);
                        holder.gradesSubjectPreviewContent.addView(ellipsisText, layoutParams);
                    }
                    else {
                        holder.gradesSubjectPreviewContent.addView(gradeName, layoutParams);
                    }
                }
            }
            if (findViews) {
                //Log.d("GradesSubjectAdapter", "runOnUiThread");
                //this.viewHolder.gradesSubjectContent.setTag(View.VISIBLE);
                //this.viewHolder.gradesSubjectPreviewContainer.setTag(View.VISIBLE);
                activity.runOnUiThread(() -> {

                    holder.gradesSubjectRoot.setOnClickListener(v -> {
                        if (holder.gradesSubjectContent.getVisibility() == View.GONE) {
                            expandSubject(holder, model);
                        }
                        else {
                            collapseSubject(holder, model);
                        }
                    });

                    holder.gradesSubjectSemester1Header.setOnClickListener(v -> {
                        if (holder.gradesSubjectSemester1Container.getVisibility() == View.GONE) {
                            if (gradesSetAsRead(holder, model, 1)) updateBadges(getContext(), model);

                            Anim.expand(holder.gradesSubjectSemester1Container, 500, null);
                            if (holder.gradesSubjectSemester2Container.getVisibility() != View.GONE) {
                                Anim.collapse(holder.gradesSubjectSemester2Container, 500, null);
                            }
                        }
                        else {
                            Anim.collapse(holder.gradesSubjectSemester1Container, 500, null);
                        }
                    });
                    holder.gradesSubjectSemester2Header.setOnClickListener(v -> {
                        if (holder.gradesSubjectSemester2Container.getVisibility() == View.GONE) {
                            if (gradesSetAsRead(holder, model, 2)) updateBadges(getContext(), model);

                            Anim.expand(holder.gradesSubjectSemester2Container, 500, null);
                            if (holder.gradesSubjectSemester1Container.getVisibility() != View.GONE) {
                                Anim.collapse(holder.gradesSubjectSemester1Container, 500, null);
                            }
                        }
                        else {
                            Anim.collapse(holder.gradesSubjectSemester2Container, 500, null);
                        }
                    });

                    // hide the grade simulator when there are point, behaviour or descriptive grades
                    if (model.isPointSubject || model.isBehaviourSubject || (model.isDescriptiveSubject && !model.isNormalSubject)) {
                        holder.gradesSubjectSemester1EditButton.setVisibility(View.GONE);
                        holder.gradesSubjectSemester2EditButton.setVisibility(View.GONE);
                    }
                    else {
                        holder.gradesSubjectSemester1EditButton.setVisibility(View.VISIBLE);
                        holder.gradesSubjectSemester1EditButton.setOnClickListener(v -> {
                            Bundle arguments = new Bundle();

                            if (model.subject != null) {
                                arguments.putLong("subjectId", model.subject.id);
                            }
                            arguments.putInt("semester", 1);
                            //d(TAG, "Model is " + model);
                            switch (model.profile.getYearAverageMode()) {
                                case YEAR_1_SEM_2_AVG:
                                case YEAR_1_SEM_2_SEM:
                                    arguments.putInt("averageMode", -1);
                                    break;
                                default:
                                    arguments.putInt("averageMode", model.semester2Final == null && model.profile.getYearAverageMode() == YEAR_1_AVG_2_SEM ? -1 : model.profile.getYearAverageMode());
                                    arguments.putFloat("yearAverageBefore", model.yearAverage);
                                    arguments.putFloat("gradeSumOtherSemester", model.gradeSumSemester2);
                                    arguments.putFloat("gradeCountOtherSemester", model.gradeCountSemester2);
                                    arguments.putFloat("averageOtherSemester", model.semester2Average);
                                    arguments.putFloat("finalOtherSemester", model.semester2Final == null ? -1 : model.semester2Final.value);
                                    break;
                            }

                            activity.loadTarget(TARGET_GRADES_EDITOR, arguments);
                        });
                        holder.gradesSubjectSemester2EditButton.setVisibility(View.VISIBLE);
                        holder.gradesSubjectSemester2EditButton.setOnClickListener(v -> {
                            Bundle arguments = new Bundle();

                            if (model.subject != null) {
                                arguments.putLong("subjectId", model.subject.id);
                            }
                            arguments.putInt("semester", 2);
                            //d(TAG, "Model is " + model);
                            switch (model.profile.getYearAverageMode()) {
                                case YEAR_1_AVG_2_SEM:
                                case YEAR_1_SEM_2_SEM:
                                    arguments.putInt("averageMode", -1);
                                    break;
                                default:
                                    arguments.putInt("averageMode", model.semester1Final == null && model.profile.getYearAverageMode() == YEAR_1_SEM_2_AVG ? -1 : model.profile.getYearAverageMode());
                                    arguments.putFloat("yearAverageBefore", model.yearAverage);
                                    arguments.putFloat("gradeSumOtherSemester", model.gradeSumSemester1);
                                    arguments.putFloat("gradeCountOtherSemester", model.gradeCountSemester1);
                                    arguments.putFloat("averageOtherSemester", model.semester1Average);
                                    arguments.putFloat("finalOtherSemester", model.semester1Final == null ? -1 : model.semester1Final.value);
                                    break;
                            }

                            activity.loadTarget(TARGET_GRADES_EDITOR, arguments);
                        });
                    }

                });
            }
            if (model != null && model.subject != null) {
                holder.lastSubject = model.subject.id;
            }
            super.onPostExecute(aVoid);
        }
    }

    //getting the context and product list with constructor
    public GradesSubjectAdapter(List<ItemGradesSubjectModel> data, MainActivity context) {
        super(context, R.layout.row_grades_subject_item, data);
        this.activity = context;
        this.subjectList = data;
    }

    @Override
    public void onClick(View v) {
        int position = (Integer) v.getTag();
        Object object = getItem(position);
        ItemGradesSubjectModel dataModel = (ItemGradesSubjectModel)object;

    }

    private void findViews(ViewHolder holder, View root) {
        //holder.gradesSubjectRoot = root.findViewById(R.id.gradesSubjectRoot);
        holder.gradesSubjectTitle = root.findViewById(R.id.gradesSubjectTitle);
        holder.gradesSubjectExpandIndicator = root.findViewById(R.id.gradesSubjectExpandIndicator);

        holder.gradesSubjectPreviewContainer = root.findViewById(R.id.gradesSubjectPreviewContainer);
        holder.gradesSubjectPreviewSemester = root.findViewById(R.id.gradesSubjectPreviewSemester);
        holder.gradesSubjectPreviewContent = root.findViewById(R.id.gradesSubjectPreviewContent);
        holder.gradesSubjectPreviewAverage = root.findViewById(R.id.gradesSubjectPreviewAverage);
        holder.gradesSubjectPreviewProposed = root.findViewById(R.id.gradesSubjectPreviewProposed);
        holder.gradesSubjectPreviewFinal = root.findViewById(R.id.gradesSubjectPreviewFinal);
        holder.gradesSubjectPreviewYearAverage = root.findViewById(R.id.gradesSubjectPreviewYearAverage);
        holder.gradesSubjectPreviewYearProposed = root.findViewById(R.id.gradesSubjectPreviewYearProposed);
        holder.gradesSubjectPreviewYearFinal = root.findViewById(R.id.gradesSubjectPreviewYearFinal);

        holder.gradesSubjectContent = root.findViewById(R.id.gradesSubjectContent);

        holder.gradesSubjectSemester1Header = root.findViewById(R.id.gradesSubjectSemester1Header);
        holder.gradesSubjectSemester1Title = root.findViewById(R.id.gradesSubjectSemester1Title);
        holder.gradesSubjectSemester1ExpandIndicator = root.findViewById(R.id.gradesSubjectSemester1ExpandIndicator);
        holder.gradesSubjectSemester1Average = root.findViewById(R.id.gradesSubjectSemester1Average);
        holder.gradesSubjectSemester1Proposed = root.findViewById(R.id.gradesSubjectSemester1Proposed);
        holder.gradesSubjectSemester1Final = root.findViewById(R.id.gradesSubjectSemester1Final);
        holder.gradesSubjectSemester1EditButton = root.findViewById(R.id.gradesSubjectSemester1EditButton);
        holder.gradesSubjectSemester1Container = root.findViewById(R.id.gradesSubjectSemester1Container);
        holder.gradesSubjectSemester1Nest = root.findViewById(R.id.gradesSubjectSemester1Nest);
        holder.gradesSubjectSemester1Content = root.findViewById(R.id.gradesSubjectSemester1Content);

        holder.gradesSubjectSemester2Header = root.findViewById(R.id.gradesSubjectSemester2Header);
        holder.gradesSubjectSemester2Title = root.findViewById(R.id.gradesSubjectSemester2Title);
        holder.gradesSubjectSemester2ExpandIndicator = root.findViewById(R.id.gradesSubjectSemester2ExpandIndicator);
        holder.gradesSubjectSemester2Average = root.findViewById(R.id.gradesSubjectSemester2Average);
        holder.gradesSubjectSemester2Proposed = root.findViewById(R.id.gradesSubjectSemester2Proposed);
        holder.gradesSubjectSemester2Final = root.findViewById(R.id.gradesSubjectSemester2Final);
        holder.gradesSubjectSemester2EditButton = root.findViewById(R.id.gradesSubjectSemester2EditButton);
        holder.gradesSubjectSemester2Container = root.findViewById(R.id.gradesSubjectSemester2Container);
        holder.gradesSubjectSemester2Nest = root.findViewById(R.id.gradesSubjectSemester2Nest);
        holder.gradesSubjectSemester2Content = root.findViewById(R.id.gradesSubjectSemester2Content);

        holder.gradesSubjectYearAverage = root.findViewById(R.id.gradesSubjectYearAverage);
        holder.gradesSubjectYearProposed = root.findViewById(R.id.gradesSubjectYearProposed);
        holder.gradesSubjectYearFinal = root.findViewById(R.id.gradesSubjectYearFinal);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ItemGradesSubjectModel model = subjectList.get(position);
        if (model == null) {
            //Toast.makeText(activity, "return convertView;", Toast.LENGTH_SHORT).show();
            return convertView;
        }
        ViewHolder holder;
        if (convertView == null) {
            try {
                convertView = LayoutInflater.from(activity).inflate(R.layout.row_grades_subject_item, parent, false);
                holder = new ViewHolder();
                holder.gradesSubjectRoot = convertView.findViewById(R.id.gradesSubjectRoot);
                convertView.setTag(holder);
                new BuildGradeViews(model, holder, parent, position, true).execute();
                return convertView;
            } catch (Exception e) {
                return new View(getContext());
            }
        }
        holder = (ViewHolder) convertView.getTag();
        Subject subject = model.subject;
        holder.gradesSubjectTitle.setText(subject != null ? subject.longName : "");
        /*if (model.getNotSeen() > 0) {
            viewHolder.notseen.setVisibility(0);
            viewHolder.notseen.setText(model.getNotSeen() + "");
        } else {
            viewHolder.notseen.setVisibility(8);
        }*/
        new BuildGradeViews(model, holder, parent, position, false).execute();
        return convertView;
    }

    public int getViewTypeCount() {
        return getCount();
    }

    public int getItemViewType(int position) {
        return position;
    }

    class ViewHolder {
        long lastSubject;

        TextView gradesSubjectTitle;
        ConstraintLayout gradesSubjectRoot;
        IconicsImageView gradesSubjectExpandIndicator;

        LinearLayout gradesSubjectPreviewContainer;
        TextView gradesSubjectPreviewSemester;
        LinearLayout gradesSubjectPreviewContent;
        TextView gradesSubjectPreviewAverage;
        TextView gradesSubjectPreviewProposed;
        TextView gradesSubjectPreviewFinal;
        TextView gradesSubjectPreviewYearAverage;
        TextView gradesSubjectPreviewYearProposed;
        TextView gradesSubjectPreviewYearFinal;

        LinearLayout gradesSubjectContent;

        LinearLayout gradesSubjectSemester1Header;
        TextView gradesSubjectSemester1Title;
        IconicsImageView gradesSubjectSemester1ExpandIndicator;
        TextView gradesSubjectSemester1Average;
        TextView gradesSubjectSemester1Proposed;
        TextView gradesSubjectSemester1Final;
        IconicsImageView gradesSubjectSemester1EditButton;
        LinearLayout gradesSubjectSemester1Container;
        NestedScrollView gradesSubjectSemester1Nest;
        RecyclerView gradesSubjectSemester1Content;

        LinearLayout gradesSubjectSemester2Header;
        TextView gradesSubjectSemester2Title;
        IconicsImageView gradesSubjectSemester2ExpandIndicator;
        TextView gradesSubjectSemester2Average;
        TextView gradesSubjectSemester2Proposed;
        TextView gradesSubjectSemester2Final;
        IconicsImageView gradesSubjectSemester2EditButton;
        LinearLayout gradesSubjectSemester2Container;
        NestedScrollView gradesSubjectSemester2Nest;
        RecyclerView gradesSubjectSemester2Content;

        TextView gradesSubjectYearAverage;
        TextView gradesSubjectYearProposed;
        TextView gradesSubjectYearFinal;
    }
}
