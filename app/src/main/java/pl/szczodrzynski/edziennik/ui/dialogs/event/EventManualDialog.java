package pl.szczodrzynski.edziennik.ui.dialogs.event;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.Observer;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;
import com.mikepenz.iconics.IconicsColor;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSize;
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.api.AppError;
import pl.szczodrzynski.edziennik.data.db.modules.events.Event;
import pl.szczodrzynski.edziennik.data.db.modules.events.EventFull;
import pl.szczodrzynski.edziennik.data.db.modules.events.EventType;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonFull;
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.ProfileFull;
import pl.szczodrzynski.edziennik.data.db.modules.subjects.Subject;
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher;
import pl.szczodrzynski.edziennik.data.db.modules.teams.Team;
import pl.szczodrzynski.edziennik.network.ServerRequest;
import pl.szczodrzynski.edziennik.utils.TextInputDropDown;
import pl.szczodrzynski.edziennik.utils.Themes;
import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.models.Time;
import pl.szczodrzynski.edziennik.utils.models.Week;

import static pl.szczodrzynski.edziennik.App.APP_URL;
import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_OTHER;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_DEFAULT;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_HOMEWORK;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_UNDEFINED;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.REGISTRATION_ENABLED;
import static pl.szczodrzynski.edziennik.utils.Utils.bs;
import static pl.szczodrzynski.edziennik.utils.Utils.ns;

public class EventManualDialog {
    private static final String TAG = "EventManualDialog";
    private App app;
    private Context context;
    private int profileId;
    private ProfileFull profile = null;

    public EventManualDialog(Context context, int profileId) {
        this.context = context;
        this.activity = (AppCompatActivity) context;
        this.profileId = profileId;
    }
    public EventManualDialog(Context context) {
        this.context = context;
        this.activity = (AppCompatActivity) context;
        this.profileId = App.profileId;
    }


    private AppCompatActivity activity;
    private MaterialDialog dialog;
    private View dialogView;
    private TextInputLayout registerEventManualDateLayout;
    private TextInputDropDown registerEventManualDate;
    private TextInputLayout registerEventManualLessonLayout;
    private TextInputDropDown registerEventManualLesson;
    private TextInputLayout registerEventManualTeamLayout;
    private TextInputDropDown registerEventManualTeam;
    private SwitchCompat registerEventManualShare;
    private TextView registerEventManualShareText;
    private TextInputLayout registerEventManualTypeLayout;
    private TextInputDropDown registerEventManualType;
    private TextInputLayout registerEventManualTopicLayout;
    private TextInputEditText registerEventManualTopic;
    private TextInputDropDown registerEventManualTeacher;
    private TextInputDropDown registerEventManualSubject;
    private View registerEventManualColorPreview;
    private Button registerEventManualColorChoose;
    private Date lessonDate = Date.getToday();
    private Time lessonStartTime = null;
    private long lessonTeamId = -1;
    private long lessonTeacherId = -1;
    private long lessonSubjectId = -1;
    private boolean lessonSelected = false;
    private int eventType = TYPE_UNDEFINED;
    private int eventColor = -1;
    private EventFull editingEvent = null;
    private MaterialDialog progressDialog;

    private void updateButtonCaption()
    {
        String buttonCaption = "";
        buttonCaption += Week.getFullDayName(Week.getWeekDayFromDate(lessonDate));
        buttonCaption += ", ";
        buttonCaption += lessonDate.getFormattedString();
        registerEventManualDate.setText(buttonCaption);

        registerEventManualLesson.setText(R.string.dialog_event_manual_choose_lesson);
        lessonStartTime = null;

        //lessonSubjectId = -1;
        //lessonTeacherId = -1;
    }

    private void updateSubjectTeacher() {
        AsyncTask.execute(() -> {
            Subject subject = app.db.subjectDao().getByIdNow(profileId, lessonSubjectId);
            Teacher teacher = app.db.teacherDao().getByIdNow(profileId, lessonTeacherId);

            activity.runOnUiThread(() -> {
                if (lessonTeacherId == -1 || teacher == null)
                    registerEventManualTeacher.setText(R.string.dialog_event_manual_no_teacher);
                else
                    registerEventManualTeacher.setText(teacher.getFullName());

                if (lessonSubjectId == -1 || subject == null)
                    registerEventManualSubject.setText(R.string.dialog_event_manual_no_subject);
                else
                    registerEventManualSubject.setText(subject.longName);
            });
        });

    }

    private void addEvent()
    {
        registerEventManualDateLayout.setError(null);
        registerEventManualLessonLayout.setError(null);
        registerEventManualTeamLayout.setError(null);
        registerEventManualTopicLayout.setError(null);
        registerEventManualTypeLayout.setError(null);
        boolean errors = false;
        if (!lessonSelected) {
            registerEventManualLessonLayout.setError(app.getString(R.string.dialog_event_manual_lesson_choose));
            errors = true;
        }
        if (registerEventManualShare.isChecked() && lessonTeamId == -1) {
            registerEventManualTeamLayout.setError(app.getString(R.string.dialog_event_manual_team_choose));
            errors = true;
        }
        if (eventType == -2) {
            registerEventManualTypeLayout.setError(app.getString(R.string.dialog_event_manual_type_choose));
            errors = true;
        }
        if (registerEventManualTopic.getText() == null || registerEventManualTopic.getText().toString().isEmpty()) {
            registerEventManualTopicLayout.setError(app.getString(R.string.dialog_event_manual_topic_choose));
            errors = true;
        }
        if (errors)
            return;

        long id = System.currentTimeMillis();
        Event event = new Event(
                profileId,
                id,
                lessonDate,
                lessonStartTime,
                registerEventManualTopic.getText().toString(),
                eventColor,
                eventType,
                true,
                lessonTeacherId,
                lessonSubjectId,
                lessonTeamId
        );
        Metadata metadata = new Metadata(
                profileId,
                eventType == TYPE_HOMEWORK ? Metadata.TYPE_HOMEWORK : Metadata.TYPE_EVENT,
                event.id,
                true,
                true,
                System.currentTimeMillis()
        );
        if (editingEvent != null) {
            event.id = editingEvent.id;
            metadata.thingId = event.id;
            metadata.addedDate = editingEvent.addedDate;
        }
        if (registerEventManualShare.isChecked()) {
            if (profile.getRegistration() != REGISTRATION_ENABLED) {
                new MaterialDialog.Builder(context)
                        .title(R.string.dialog_event_manual_must_register_title)
                        .content(R.string.dialog_event_manual_must_register_text)
                        .positiveText(R.string.i_agree)
                        .negativeText(R.string.no_thanks)
                        .neutralText(R.string.more)
                        .onPositive(((dialog1, which) -> {
                            profile.setRegistration(REGISTRATION_ENABLED);
                            app.profileSaveAsync(profile);
                        }))
                        .onNeutral(((dialog1, which) -> {
                            new MaterialDialog.Builder(context)
                                    .title(R.string.help)
                                    .content(app.getString(R.string.help_register_agreement))
                                    .positiveText(R.string.ok)
                                    .show();
                        }))
                        .show();
                return;
            }
            if (!profile.getEnableSharedEvents()) {
                new MaterialDialog.Builder(context)
                        .title(R.string.dialog_event_manual_shared_disabled_title)
                        .content(R.string.dialog_event_manual_shared_disabled_text)
                        .positiveText(R.string.sure)
                        .negativeText(R.string.no_thanks)
                        .onPositive(((dialog1, which) -> {
                            profile.setEnableSharedEvents(true);
                            app.profileSaveAsync(profile);
                        }))
                        .show();
                return;
            }
            if (editingEvent != null
                    && editingEvent.sharedBy != null
                    && !editingEvent.sharedBy.equals("self")) {
                // editing a shared event, and event was shared by someone else
                // send a edit request
                shareEvent(event, metadata, MODE_REQUEST);
            }
            else {
                // editing a self-shared event, not-shared event or a new event, but want to share it
                // post a new event to database
                shareEvent(event, metadata, MODE_SHARE);
            }
        }
        else if (editingEvent != null
                && editingEvent.sharedBy != null
                && editingEvent.sharedBy.equals("self")) {
            // the event was self-shared, but needs to be removed now (from the server)
            shareEvent(event, metadata, MODE_UNSHARE);
        }
        else {
            // editing a local event
            finishAdding(event, metadata);
        }
    }

    private static final int MODE_SHARE = 0;
    private static final int MODE_UNSHARE = 1;
    private static final int MODE_REQUEST = 2;
    private static final int MODE_UNSHARE_REMOVE = 3;
    private void shareEvent(Event event, Metadata metadata, int mode) {
        AsyncTask.execute(() -> {
            ServerRequest syncRequest =
                    new ServerRequest(
                            app,
                            app.requestScheme + APP_URL + "main.php?share",
                            "RegisterEventManualDialog/SH",
                            profile);
            String teamUnshare = "";
            if (mode == MODE_SHARE) {
                if (editingEvent != null
                        && event.teamId != editingEvent.teamId) {
                    // team ID was changed. Need to remove the event for other teams
                    teamUnshare = app.db.teamDao().getCodeByIdNow(profile.getId(), editingEvent.teamId);
                    syncRequest.setBodyParameter("team_unshare", teamUnshare);
                }
                syncRequest.setBodyParameter("event", app.gson.toJson(event));
                syncRequest.setBodyParameter("event_addedDate", Long.toString(metadata.addedDate));
                syncRequest.setBodyParameter("team", app.db.teamDao().getCodeByIdNow(profile.getId(), event.teamId));
            }
            else if (mode == MODE_UNSHARE
                    && editingEvent != null) {
                teamUnshare = app.db.teamDao().getCodeByIdNow(profile.getId(), editingEvent.teamId);
                syncRequest.setBodyParameter("remove_id", Long.toString(editingEvent.id));
                syncRequest.setBodyParameter("team_unshare", teamUnshare);
            }
            else if (mode == MODE_UNSHARE_REMOVE
                    && editingEvent != null) {
                teamUnshare = app.db.teamDao().getCodeByIdNow(profile.getId(), editingEvent.teamId);
                syncRequest.setBodyParameter("remove_id", Long.toString(editingEvent.id));
                syncRequest.setBodyParameter("team_unshare", teamUnshare);
            }
            else if (mode == MODE_REQUEST) {
                activity.runOnUiThread(() -> {
                    new MaterialDialog.Builder(context)
                            .title(R.string.error_occured)
                            .content(R.string.error_shared_edit_requests_not_implemented)
                            .positiveText(R.string.ok)
                            .show();
                });
                return;
            }


            activity.runOnUiThread(() -> {
                progressDialog = new MaterialDialog.Builder(context)
                        .title(R.string.loading)
                        .content(R.string.sharing_event)
                        .progress(true, 0)
                        .canceledOnTouchOutside(false)
                        .show();
            });
            String finalTeamUnshare = teamUnshare;

            try {
                JsonObject result = syncRequest.runSync();

                activity.runOnUiThread(() -> progressDialog.dismiss());
                if (result == null) {
                    activity.runOnUiThread(() -> new MaterialDialog.Builder(context)
                            .title(R.string.error_occured)
                            .content(R.string.sync_error_no_internet)
                            .positiveText(R.string.ok)
                            .show());

                    return;
                }
                if (result.get("success") == null || !result.get("success").getAsString().equals("true")) {
                    activity.runOnUiThread(() -> new MaterialDialog.Builder(context)
                            .title(R.string.error_occured)
                            .content(R.string.sync_error_app_server)
                            .positiveText(R.string.ok)
                            .show());
                    return;
                }
                if (mode == MODE_SHARE) {
                    event.sharedBy = "self";
                    event.sharedByName = profile.getStudentNameLong();
                }
                else if (mode == MODE_UNSHARE) {
                    event.sharedBy = null;
                }
                // check all profiles
                // remove if necessary (team matching)
                // this will hardly ever need to run
                // (only if the user un-shares an event and has two profiles sharing the same team in common)
                if (finalTeamUnshare != null && !finalTeamUnshare.equals("")) {
                    app.db.eventDao().removeByTeamId(editingEvent.teamId, editingEvent.id);
                }

                if (mode == MODE_UNSHARE_REMOVE
                        && editingEvent != null) {
                    editingEvent.sharedBy = null;
                    AsyncTask.execute(() -> {
                        app.db.eventDao().remove(editingEvent);
                    });
                    activity.runOnUiThread(() -> {
                        Toast.makeText(app, R.string.removed, Toast.LENGTH_SHORT).show();
                        if (activity instanceof MainActivity)
                            ((MainActivity) activity).reloadTarget();
                        dialog.dismiss();
                    });
                }
                else {
                    activity.runOnUiThread(() -> {
                        finishAdding(event, metadata);
                    });
                }
            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    app.apiEdziennik.guiShowErrorDialog(activity, new AppError(TAG, 379, CODE_OTHER, null, e), R.string.error_occured);
                });
            }
        });
    }

    private void finishAdding(Event event, Metadata metadata) {
        AsyncTask.execute(() -> {
            app.db.eventDao().add(event);
            app.db.metadataDao().add(metadata);
            if (editingEvent != null && (event.type == TYPE_HOMEWORK) != (editingEvent.type == TYPE_HOMEWORK)) {
                // editingEvent's type changed
                // the old metadata will not work because either the event changed *to* or *from* homework.
                app.db.metadataDao().delete(
                        profileId,
                        editingEvent.type == TYPE_HOMEWORK ? Metadata.TYPE_HOMEWORK : Metadata.TYPE_EVENT,
                        editingEvent.id
                );
            }
        });
        Toast.makeText(app, R.string.saved, Toast.LENGTH_SHORT).show();
        dialog.dismiss();
        if (activity instanceof MainActivity)
            ((MainActivity) activity).reloadTarget();
    }

    private DialogInterface.OnDismissListener dismissListener;
    private boolean callDismissListener = true;

    public EventManualDialog withDismissListener(DialogInterface.OnDismissListener dismissListener) {
        this.dismissListener = dismissListener;
        return this;
    }

    private void performDismiss(DialogInterface dialogInterface) {
        if (!dialog.isCancelled())
            dialog.dismiss();
        if (callDismissListener && dismissListener != null) {
            dismissListener.onDismiss(dialogInterface);
        }
        callDismissListener = true;
    }

    public static final int DIALOG_EVENT = 0;
    public static final int DIALOG_HOMEWORK = 1;

    public void show(App _app, EventFull editingEvent, Date defaultDate, Time defaultTime, int dialogType) {
        if (!(context instanceof Activity))
            return;
        this.app = _app;
        AsyncTask.execute(() -> {
            this.profile = app.db.profileDao().getFullByIdNow(profileId);
            if (profile != null) {
                ((Activity) context).runOnUiThread(() -> {
                    actualShow(editingEvent, defaultDate, defaultTime, dialogType);
                });
            }
            else {
                activity.runOnUiThread(() -> {
                    new MaterialDialog.Builder(activity)
                            .title(R.string.error_occured)
                            .content(R.string.sync_error_profile_not_found)
                            .positiveText(R.string.ok)
                            .show();
                });
            }
        });
    }
    private void actualShow(EventFull editingEvent, Date defaultDate, Time defaultTime, int dialogType) {
        if (dialogType == DIALOG_HOMEWORK) {
            eventType = TYPE_HOMEWORK;
            eventColor = -1;
        }
        dialog = new MaterialDialog.Builder(context)
                .title((dialogType == DIALOG_EVENT ? R.string.dialog_register_event_manual_title : R.string.dialog_register_homework_manual_title))
                .customView(R.layout.dialog_event_manual, true)
                .positiveText(R.string.save)
                .negativeText(R.string.cancel)
                .autoDismiss(false)
                .onPositive((dialog, which) -> addEvent())
                .onNegative((dialog, which) -> dialog.dismiss())
                .onNeutral((dialog, which) -> {
                    if (editingEvent != null) {
                        String sharedNotice = "";
                        // notify the user that the event will be removed from all users
                        if (editingEvent.sharedBy != null
                                && editingEvent.sharedBy.equals("self")) {
                            sharedNotice += "\n\n"+context.getString(R.string.dialog_event_manual_remove_shared_self);
                        }
                        // notify the user that the event will be blacklisted and removed only locally
                        else if (editingEvent.sharedBy != null) {
                            sharedNotice += "\n\n"+context.getString(R.string.dialog_event_manual_remove_shared);
                        }
                        new MaterialDialog.Builder(context)
                                .title(R.string.are_you_sure)
                                .content(context.getString(R.string.dialog_register_event_manual_remove_confirmation)+sharedNotice)
                                .positiveText(R.string.yes)
                                .negativeText(R.string.no)
                                .onPositive((dialog1, which1) -> {
                                    if (editingEvent.sharedBy != null
                                            && editingEvent.sharedBy.equals("self")) {
                                        // editing event was self-shared
                                        // the shared event needs to be unshared, then removed locally
                                        shareEvent(editingEvent, null, MODE_UNSHARE_REMOVE);
                                        return;
                                    }
                                    else if (editingEvent.sharedBy != null) {
                                        // the event is shared by someone else
                                        // blacklist it and remove only locally
                                        AsyncTask.execute(() -> {
                                            editingEvent.blacklisted = true;
                                            app.db.eventDao().setBlacklisted(editingEvent.profileId, editingEvent.id, true);
                                        });
                                    }
                                    else {
                                        // the event is not shared nor blacklisted - just remove it
                                        AsyncTask.execute(() -> {
                                            app.db.eventDao().remove(editingEvent);
                                        });
                                    }
                                    dialog.dismiss();
                                    dialog1.dismiss();
                                    Toast.makeText(app, R.string.removed, Toast.LENGTH_SHORT).show();
                                    if (activity instanceof MainActivity)
                                        ((MainActivity) activity).reloadTarget();
                                })
                                .show();
                    }
                })
                .dismissListener((this::performDismiss))
                .show();

        if (editingEvent != null) {
                //&& (editingEvent.sharedBy == null || editingEvent.sharedBy.equals("self"))) {
            dialog.setActionButton(DialogAction.NEUTRAL, R.string.remove);
        }

        dialogView = dialog.getCustomView();
        assert dialogView != null;

        @ColorInt int primaryTextColor = Themes.INSTANCE.getPrimaryTextColor(context);

        registerEventManualDateLayout = dialogView.findViewById(R.id.registerEventManualDateLayout);
        registerEventManualDate = dialogView.findViewById(R.id.registerEventManualDate);
        registerEventManualDate.setCompoundDrawablesWithIntrinsicBounds(null, null, new IconicsDrawable(context, CommunityMaterial.Icon.cmd_calendar).size(IconicsSize.dp(16)).color(IconicsColor.colorInt(primaryTextColor)), null);
        //registerEventManualDate.setCompoundDrawablePadding(Utils.dpToPx(6));
        registerEventManualLessonLayout = dialogView.findViewById(R.id.registerEventManualLessonLayout);
        registerEventManualLesson = dialogView.findViewById(R.id.registerEventManualLesson);
        registerEventManualTeamLayout = dialogView.findViewById(R.id.registerEventManualTeamLayout);
        registerEventManualTeam = dialogView.findViewById(R.id.registerEventManualTeam);
        registerEventManualTeacher = dialogView.findViewById(R.id.registerEventManualTeacher);
        registerEventManualSubject = dialogView.findViewById(R.id.registerEventManualSubject);
        registerEventManualTypeLayout = dialogView.findViewById(R.id.registerEventManualTypeLayout);
        registerEventManualType = dialogView.findViewById(R.id.registerEventManualType);
        registerEventManualTopicLayout = dialogView.findViewById(R.id.registerEventManualTopicLayout);
        registerEventManualTopic = dialogView.findViewById(R.id.registerEventManualTopic);
        registerEventManualShare = dialogView.findViewById(R.id.registerEventManualShare);
        registerEventManualShareText = dialogView.findViewById(R.id.registerEventManualShareText);
        registerEventManualShare.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            // show notice text if share is enabled or event is already shared
            registerEventManualShareText.setVisibility(isChecked || (editingEvent != null && editingEvent.sharedBy != null) ? View.VISIBLE : View.GONE);
            // notify the user if he wants to share the event with no team selected
            if (isChecked && lessonTeamId == -1) {
                Toast.makeText(context, R.string.dialog_event_manual_cannot_share, Toast.LENGTH_SHORT).show();
            }
            if (editingEvent != null && editingEvent.sharedBy != null) {
                // if the event is already shared, show the (change) or (removal) notice text
                if (isChecked)
                    registerEventManualShareText.setText(R.string.dialog_event_manual_share_will_change);
                else
                    registerEventManualShareText.setText(R.string.dialog_event_manual_share_will_remove);
            }
        }));


        registerEventManualDate.setOnClickListener(v -> DatePickerDialog.newInstance(
                (view, year, monthOfYear, dayOfMonth) -> {
                    lessonDate.year = year;
                    lessonDate.month = monthOfYear + 1;
                    lessonDate.day = dayOfMonth;
                    updateButtonCaption();
                },
                lessonDate.year,
                lessonDate.month - 1,
                lessonDate.day
        ).show(((AppCompatActivity) context).getSupportFragmentManager(), "DatePickerDialog"));

        registerEventManualLesson.setOnClickListener(v -> {
            app.db.lessonDao().getAllByDateWithoutChanges(profileId, lessonDate).observe(activity, new Observer<List<LessonFull>>() {
                @Override
                public void onChanged(@Nullable List<LessonFull> lessons) {
                    PopupMenu popup = new PopupMenu(context, registerEventManualLesson);

                    if (lessons != null && lessons.size() != 0) {
                        for (LessonFull lesson: lessons) {
                            int index = lessons.indexOf(lesson);
                            popup.getMenu().add(
                                    0,
                                    index,
                                    index+1,
                                    lesson.startTime.getStringHM()+" "+bs(lesson.subjectLongName));
                        }
                    }
                    popup.getMenu().add(1, -1, 0, R.string.dialog_event_manual_all_day);
                    popup.getMenu().add(1, 0, 100, R.string.dialog_event_manual_custom_time);

                    popup.setOnMenuItemClickListener(item -> {
                        if (lessons != null && item.getGroupId() == 0) {
                            LessonFull lesson = lessons.get(item.getItemId());
                            lessonTeacherId = lesson.teacherId;
                            lessonStartTime = lesson.startTime;
                            lessonSubjectId = lesson.subjectId;
                            registerEventManualTeacher.setText(bs(lesson.teacherFullName));
                            registerEventManualSubject.setText(bs(lesson.subjectLongName));
                            lessonTeamId = lesson.teamId;
                            registerEventManualTeam.setText(ns(context.getString(R.string.dialog_event_manual_no_team), bs(lesson.teamName)));
                            registerEventManualLesson.setText(item.getTitle());
                            lessonSelected = true;
                        }
                        else if (item.getGroupId() == 1) {
                            if (item.getItemId() == -1) {
                                lessonStartTime = null;
                                lessonTeacherId = -1;
                                lessonSubjectId = -1;
                                if (editingEvent != null) {
                                    lessonTeacherId = editingEvent.teacherId;
                                    lessonSubjectId = editingEvent.subjectId;
                                }
                                updateSubjectTeacher();
                                registerEventManualLesson.setText(item.getTitle());
                                lessonSelected = true;
                                return true;
                            }
                            Time now = Time.getNow();
                            TimePickerDialog.newInstance((view, hourOfDay, minute, second) -> {
                                lessonStartTime = new Time(hourOfDay, minute, second);
                                lessonTeacherId = -1;
                                lessonSubjectId = -1;
                                if (editingEvent != null) {
                                    lessonTeacherId = editingEvent.teacherId;
                                    lessonSubjectId = editingEvent.subjectId;
                                }
                                updateSubjectTeacher();
                                //lessonTeamId = -1;
                                registerEventManualLesson.setText(lessonStartTime.getStringHM());
                                lessonSelected = true;
                            }, now.hour, now.minute, 0, true).show(((AppCompatActivity) context).getSupportFragmentManager(), "TimePickerDialog");
                        }
                        return true;
                    });

                    popup.show();
                    app.db.lessonDao().getAllByDateWithoutChanges(profileId, lessonDate).removeObserver(this);
                }
            });
        });

        registerEventManualTeam.setOnClickListener(v -> {
            app.db.teamDao().getAll(profileId).observe(activity, new Observer<List<Team>>() {
                @Override
                public void onChanged(@Nullable List<Team> teams) {
                    PopupMenu popup = new PopupMenu(context, registerEventManualTeam);
                    if (teams != null && teams.size() != 0) {
                        int index = 0;
                        for (Team team : teams) {
                            popup.getMenu().add(0, index++, (index)+1, team.name);
                        }
                    }
                    popup.getMenu().add(1, 0, 100, context.getString(R.string.dialog_event_manual_no_team));
                    popup.setOnMenuItemClickListener(item -> {
                        long id = -1;
                        if (item.getGroupId() == 0 && teams != null) {
                            Team team = teams.get(item.getItemId());
                            id = team.id;
                        }
                        lessonTeamId = id;
                        registerEventManualTeam.setText(item.getTitle());
                        return true;
                    });
                    popup.show();
                    app.db.teamDao().getAll(profileId).removeObserver(this);
                }
            });
        });

        registerEventManualTeacher.setOnClickListener(v -> {
            app.db.teacherDao().getAllTeachers(profileId).observe(activity, new Observer<List<Teacher>>() {
                @Override
                public void onChanged(@Nullable List<Teacher> teachers) {
                    PopupMenu popup = new PopupMenu(context, registerEventManualTeacher);
                    if (teachers != null && teachers.size() != 0) {
                        int index = 0;
                        for (Teacher teacher : teachers) {
                            popup.getMenu().add(0, index++, (index)+1, teacher.getFullName());
                        }
                    }
                    popup.getMenu().add(1, 0, 0, context.getString(R.string.dialog_event_manual_no_teacher));
                    popup.setOnMenuItemClickListener(item -> {
                        long id = -1;
                        if (item.getGroupId() == 0 && teachers != null) {
                            Teacher teacher = teachers.get(item.getItemId());
                            id = teacher.id;
                        }
                        lessonTeacherId = id;
                        registerEventManualTeacher.setText(item.getTitle());
                        return true;
                    });
                    popup.show();
                    app.db.teacherDao().getAllTeachers(profileId).removeObserver(this);
                }
            });
        });
        registerEventManualSubject.setOnClickListener(v -> {
            app.db.subjectDao().getAll(profileId).observe(activity, new Observer<List<Subject>>() {
                @Override
                public void onChanged(@Nullable List<Subject> subjects) {
                    PopupMenu popup = new PopupMenu(context, registerEventManualSubject);
                    if (subjects != null && subjects.size() != 0) {
                        int index = 0;
                        for (Subject subject: subjects) {
                            popup.getMenu().add(0, index++, (index)+1, subject.longName);
                        }
                    }
                    popup.getMenu().add(1, 0, 0, context.getString(R.string.dialog_event_manual_no_subject));
                    popup.setOnMenuItemClickListener(item -> {
                        long id = -1;
                        if (item.getGroupId() == 0 && subjects != null) {
                            Subject subject = subjects.get(item.getItemId());
                            id = subject.id;
                        }
                        lessonSubjectId = id;
                        registerEventManualSubject.setText(item.getTitle());
                        return true;
                    });
                    popup.show();
                    app.db.subjectDao().getAll(profileId).removeObserver(this);
                }
            });
        });
        registerEventManualType.setOnClickListener(v -> {
            app.db.eventTypeDao().getAll(profileId).observe(activity, new Observer<List<EventType>>() {
                @Override
                public void onChanged(@Nullable List<EventType> eventTypes) {
                    PopupMenu popup = new PopupMenu(context, registerEventManualType);
                    if (eventTypes != null && eventTypes.size() != 0) {
                        int index = 0;
                        for (EventType eventType : eventTypes) {
                            popup.getMenu().add(0, index++, (index)+1, eventType.name);
                        }
                    }
                    popup.setOnMenuItemClickListener(item -> {
                        if (item.getGroupId() == 0 && eventTypes != null) {
                            EventType typeObj = eventTypes.get(item.getItemId());
                            eventType = (int)typeObj.id;
                            eventColor = -1; // set -1 as it's the event type's default color
                            registerEventManualColorPreview.setBackgroundColor(typeObj.color); // set event type's color here to show how will it look
                        }
                        registerEventManualType.setText(item.getTitle());
                        return true;
                    });
                    popup.show();
                    app.db.eventTypeDao().getAll(profileId).removeObserver(this);
                }
            });
        });

        View.OnClickListener colorChooseOnClick = (v -> {
            AsyncTask.execute(() -> {
                EventType type = app.db.eventTypeDao().getByIdNow(profileId, eventType);
                activity.runOnUiThread(() -> {
                    ColorPickerDialog colorPickerDialog = ColorPickerDialog.newBuilder()
                            .setColor(eventColor != -1 ? eventColor : type != null ? type.color : COLOR_DEFAULT)
                            .create();
                    colorPickerDialog.setColorPickerDialogListener(
                            new ColorPickerDialogListener() {
                                @Override
                                public void onColorSelected(int dialogId, int color) {
                                    registerEventManualColorPreview.setBackgroundColor(color);
                                    eventColor = color;
                                }

                                @Override
                                public void onDialogDismissed(int dialogId) {

                                }
                            });
                    colorPickerDialog.show(((Activity)context).getFragmentManager(), "color-picker-dialog");
                });
            });
        });

        registerEventManualColorPreview = dialogView.findViewById(R.id.registerEventManualColorPreview);
        registerEventManualColorPreview.setOnClickListener(colorChooseOnClick);
        registerEventManualColorChoose = dialogView.findViewById(R.id.registerEventManualColorChoose);
        registerEventManualColorChoose.setOnClickListener(colorChooseOnClick);

        if (eventType != TYPE_UNDEFINED) {
            AsyncTask.execute(() -> {
                EventType type = app.db.eventTypeDao().getByIdNow(profileId, eventType);
                activity.runOnUiThread(() -> {
                    if (type != null) {
                        eventColor = -1;
                        registerEventManualColorPreview.setBackgroundColor(type.color);
                        registerEventManualType.setText(type.name);
                    }
                    else {
                        eventColor = -1;
                        eventType = TYPE_UNDEFINED;
                        registerEventManualColorPreview.setBackgroundColor(COLOR_DEFAULT);
                        registerEventManualType.setText("");
                    }
                });
            });
        }
        else {
            registerEventManualColorPreview.setBackgroundColor(COLOR_DEFAULT);
            eventColor = -1;
        }

        /*if (dialogType == DIALOG_HOMEWORK) {
            dialogView.findViewById(R.id.registerEventManualColorContainer).setVisibility(View.GONE);
            registerEventManualType.setVisibility(View.GONE);
            eventType = TYPE_HOMEWORK;
        }
        else {


        }*/

        updateButtonCaption();

        if (editingEvent != null) {
            this.editingEvent = editingEvent;
            lessonDate = editingEvent.eventDate.clone();
            lessonTeamId = editingEvent.teamId;
            // check the (shared) checkbox if the event is already shared by someone
            registerEventManualShare.setChecked(editingEvent.sharedBy != null);
            // .. but disable the ability to un-share, if it's shared by someone else
            registerEventManualShare.setEnabled(editingEvent.sharedBy == null || editingEvent.sharedBy.equals("self"));
            registerEventManualShare.jumpDrawablesToCurrentState();
            // show the notice text if the event is already shared
            registerEventManualShareText.setVisibility(editingEvent.sharedBy != null ? View.VISIBLE : View.GONE);
            if (editingEvent.sharedBy != null) {
                // show the (change) notice if the event is shared by (self)
                if (editingEvent.sharedBy.equals("self"))
                    registerEventManualShareText.setText(R.string.dialog_event_manual_share_will_change);
                // otherwise show the (edit request) notice
                else
                    registerEventManualShareText.setText(app.getString(R.string.dialog_event_manual_share_will_request, editingEvent.sharedByName));
            }

            updateButtonCaption(); // updates the lesson date and clears the time

            lessonSubjectId = editingEvent.subjectId;
            lessonTeacherId = editingEvent.teacherId;
            lessonStartTime = editingEvent.startTime == null ? null : editingEvent.startTime.clone();
            lessonSelected = true;

            registerEventManualTeam.setText(ns(context.getString(R.string.dialog_event_manual_no_team), editingEvent.teamName));
            registerEventManualTopic.setText(editingEvent.topic);
            registerEventManualType.setText(editingEvent.typeName);
            eventColor = editingEvent.getColor();
            eventType = editingEvent.type;
            registerEventManualColorPreview.setBackgroundColor(eventColor);

            if (lessonSubjectId == -1) {
                //lessonTeacherId = -1; // why tho?
                registerEventManualLesson.setText(lessonStartTime == null ? app.getString(R.string.dialog_event_manual_all_day) : lessonStartTime.getStringHM());
            }
            else {
                registerEventManualLesson.setText((lessonStartTime == null ? app.getString(R.string.dialog_event_manual_all_day) + "," : lessonStartTime.getStringHM())+" "+bs(editingEvent.subjectLongName));
            }
        }
        else {
            if (defaultDate != null) {
                lessonDate = defaultDate.clone();
                updateButtonCaption();
            }
            if (defaultDate != null && defaultTime != null) { // ONLY used when adding an event to the previously selected lesson
                AsyncTask.execute(() -> {
                    LessonFull lesson = app.db.lessonDao().getByDateTimeWithoutChangesNow(profileId, defaultDate, defaultTime);

                    lessonTeacherId = lesson.teacherId;
                    lessonStartTime = lesson.startTime.clone();
                    lessonTeamId = lesson.teamId;
                    lessonSubjectId = lesson.subjectId;
                    lessonSelected = true;
                    activity.runOnUiThread(() -> {
                        registerEventManualTeam.setText(ns(context.getString(R.string.dialog_event_manual_no_team), lesson.teamName));
                        registerEventManualLesson.setText(lesson.startTime.getStringHM()+" "+bs(lesson.subjectLongName));
                        registerEventManualTeacher.setText(ns(context.getString(R.string.dialog_event_manual_no_teacher), lesson.teacherFullName));
                        registerEventManualSubject.setText(ns(context.getString(R.string.dialog_event_manual_no_subject), lesson.subjectLongName));
                    });
                });
            }
            else {
                AsyncTask.execute(() -> {
                    Team team = app.db.teamDao().getClassNow(profileId);
                    if (team != null) {
                        activity.runOnUiThread(() -> {
                            lessonTeamId = team.id;
                            registerEventManualTeam.setText(ns(context.getString(R.string.dialog_event_manual_no_team), team.name));
                        });
                    }
                });
            }
        }
        updateSubjectTeacher();
    }
}
