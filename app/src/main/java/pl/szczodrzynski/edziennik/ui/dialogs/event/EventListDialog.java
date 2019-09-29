package pl.szczodrzynski.edziennik.ui.dialogs.event;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonFull;
import pl.szczodrzynski.edziennik.data.db.modules.teachers.TeacherAbsenceFull;
import pl.szczodrzynski.edziennik.ui.dialogs.lessonchange.LessonChangeDialog;
import pl.szczodrzynski.edziennik.ui.dialogs.teacherabsence.TeacherAbsenceDialog;
import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.models.Time;

public class EventListDialog {
    private App app;
    private Context context;
    private int profileId;

    public EventListDialog(Context context) {
        this.context = context;
        this.profileId = App.profileId;
    }
    public EventListDialog(Context context, int profileId) {
        this.context = context;
        this.profileId = profileId;
    }

    public MaterialDialog dialog;
    private View dialogView;
    private RecyclerView examsView;
    private DialogInterface.OnDismissListener dismissListener;
    public boolean callDismissListener = true;
    private LessonFull lesson;

    public EventListDialog withDismissListener(DialogInterface.OnDismissListener dismissListener) {
        this.dismissListener = dismissListener;
        return this;
    }

    public void performDismiss(DialogInterface dialogInterface) {
        if (callDismissListener && dismissListener != null) {
            dismissListener.onDismiss(dialogInterface);
        }
        callDismissListener = true;
    }

    public void show(App _app, Date date)
    {
        show(_app, date, null);
    }
    public void show(App _app, Date date, Time time) {
        show(_app, date, time, false);
    }
    public void show(App _app, Date date, Time time, boolean noDefaultTimeWhenAdding) {
        if (!(context instanceof AppCompatActivity) || date == null)
            return;
        this.app = _app;

        if (time != null) {
            AsyncTask.execute(() -> {
                this.lesson = app.db.lessonDao().getByDateTimeNow(profileId, date, time);
                ((AppCompatActivity) context).runOnUiThread(() -> {
                    actualShow(date, time, noDefaultTimeWhenAdding);
                });
            });
            return;
        }
        actualShow(date, null, noDefaultTimeWhenAdding);
    }

    private void actualShow(Date date, Time time, boolean noDefaultTimeWhenAdding) {
        dialog = new MaterialDialog.Builder(context)
                .title((time == null ? date.getFormattedString() : (lesson != null ? lesson.getSubjectLongName() : date.getFormattedString())+", "+time.getStringHM()))
                .customView(R.layout.dialog_event_list, false)
                .neutralText(R.string.add)
                .positiveText(R.string.close)
                .autoDismiss(false)
                .onNeutral((dialog, which) -> {
                    callDismissListener = false;
                    dialog.dismiss();
                    //performDismiss(dialog);
                    //callDismissListener = true;
                    new MaterialDialog.Builder(context)
                            .title(R.string.main_menu_add)
                            .items(R.array.main_menu_add_options)
                            .autoDismiss(true)
                            .itemsCallback((dialog1, itemView, position, text) -> {
                                callDismissListener = false;
                                //performDismiss(dialog1); // use if the main dialog is already dismissed
                                //dialog1.dismiss();
                                //callDismissListener = true;
                                switch (position) {
                                    case 0:
                                        new EventManualDialog(context, profileId)
                                                .withDismissListener(this::performDismiss)
                                                .show(app, null, date, (noDefaultTimeWhenAdding ? null : time), EventManualDialog.DIALOG_EVENT);
                                        break;
                                    case 1:
                                        new EventManualDialog(context, profileId)
                                                .withDismissListener(this::performDismiss)
                                                .show(app, null, date, (noDefaultTimeWhenAdding ? null : time), EventManualDialog.DIALOG_HOMEWORK);
                                        break;
                                }
                            })
                            .dismissListener((dialog1 -> {
                                callDismissListener = false;
                                performDismiss(dialog1);
                            }))
                            .show();
                })
                .onPositive((dialog, which) -> dialog.dismiss())
                .dismissListener(this::performDismiss)
                .show();

        dialogView = dialog.getCustomView();
        assert dialogView != null;

        LinearLayout eventListLessonDetails = dialogView.findViewById(R.id.eventListLessonDetails);
        eventListLessonDetails.setVisibility((lesson == null ? View.GONE : View.VISIBLE));
        if (lesson != null) {
            TextView eventListClassroom = dialogView.findViewById(R.id.eventListClassroom);
            TextView eventListTeacher = dialogView.findViewById(R.id.eventListTeacher);
            TextView eventListLessonChange = dialogView.findViewById(R.id.eventListLessonChange);
            ((TextView)dialogView.findViewById(R.id.eventListLessonDate)).setText(app.getString(R.string.date_time_format, date.getFormattedString(), ""));

            boolean lessonCancelled = false;
            if (lesson.changeId != 0 && lesson.changeType == LessonChange.TYPE_CANCELLED) {
                lessonCancelled = true;
            }

            if (lessonCancelled) {
                eventListLessonChange.setText(R.string.lesson_cancelled);
                eventListLessonChange.setTypeface(null, Typeface.BOLD_ITALIC);
                eventListTeacher.setVisibility(View.GONE);
                eventListClassroom.setVisibility(View.GONE);
                eventListLessonChange.setVisibility(View.VISIBLE);
            } else {
                eventListLessonChange.setText(lesson.getSubjectLongName(true));
                if (lesson.changedSubjectLongName()) {
                    eventListLessonChange.setTypeface(null, Typeface.ITALIC);
                } else {
                    eventListLessonChange.setVisibility(View.GONE);
                }

                eventListTeacher.setText(lesson.getTeacherFullName(true));
                eventListTeacher.setTypeface(null, lesson.changedTeacherFullName() ? Typeface.ITALIC : Typeface.NORMAL);

                eventListClassroom.setText(lesson.getClassroomName(true));
                eventListClassroom.setTypeface(null, lesson.changedClassroomName() ? Typeface.ITALIC : Typeface.NORMAL);
            }

        }

        //((TextView)dialogView.findViewById(R.id.textLessonDate)).setText(date.getFormattedString()+(time != null ? ", "+time.getStringHM() : ""));

        examsView = dialogView.findViewById(R.id.eventListView);
        examsView.setHasFixedSize(false);
        examsView.setNestedScrollingEnabled(true);
        examsView.setLayoutManager(new LinearLayoutManager(context));

        CardView lessonChangeContainer = dialogView.findViewById(R.id.lessonChangeContainer);
        CardView teacherAbsenceContainer = dialogView.findViewById(R.id.teacherAbsenceContainer);
        //lessonChangeContainer.setVisibility(View.GONE);
        if (time == null) {
            app.db.lessonChangeDao().getLessonChangeCounterByDate(App.profileId, date).observe((LifecycleOwner) context, counter -> {
                if (counter == null)
                    return;
                if (counter.lessonChangeCount > 0) {
                    lessonChangeContainer.setVisibility(View.VISIBLE);
                    TextView lessonChangeCount = dialogView.findViewById(R.id.lessonChangeCount);
                    lessonChangeCount.setText(String.valueOf(counter.lessonChangeCount));
                    lessonChangeContainer.setCardBackgroundColor(0xff78909c);
                    lessonChangeContainer.setOnClickListener((v -> {
                        new LessonChangeDialog(context).show(app, date);
                    }));
                }
            });

            app.db.teacherAbsenceDao().getAllByDateFull(App.profileId, date).observe((LifecycleOwner) context, teacherAbsenceList -> {
                if (teacherAbsenceList == null)
                    return;
                if (teacherAbsenceList.size() > 0) {
                    int count = 0;
                    for (TeacherAbsenceFull teacherAbsence : teacherAbsenceList) {
                        Date dateFrom = teacherAbsence.getDateFrom();
                        Date dateTo = teacherAbsence.getDateTo();

                        if (date.compareTo(dateFrom) >= 0 && date.compareTo(dateTo) <= 0) {
                            count++;
                        }
                    }

                    teacherAbsenceContainer.setVisibility(View.VISIBLE);
                    TextView teacherAbsenceCount = dialogView.findViewById(R.id.teacherAbsenceCount);
                    teacherAbsenceCount.setText(String.valueOf(count));
                    teacherAbsenceContainer.setCardBackgroundColor(0xffff1744);
                    teacherAbsenceContainer.setOnClickListener(( v -> {
                        new TeacherAbsenceDialog(context).show(app, date);
                    }));
                }
            });
        }

        app.db.eventDao().getAllByDateTime(profileId, date, time).observe((LifecycleOwner) context, events -> {
            if (events == null || events.size() == 0) {
                examsView.setVisibility(View.GONE);
                dialogView.findViewById(R.id.textNoEvents).setVisibility(View.VISIBLE);
            }
            else {
                EventListAdapter adapter = new EventListAdapter(context, events, this);
                examsView.setAdapter(adapter);
            }
        });
    }
}
