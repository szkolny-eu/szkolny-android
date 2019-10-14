package pl.szczodrzynski.edziennik.ui.modules.login;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.BuildConfig;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance;
import pl.szczodrzynski.edziennik.data.db.modules.events.Event;
import pl.szczodrzynski.edziennik.data.db.modules.events.EventType;
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.Lesson;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange;
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore;
import pl.szczodrzynski.edziennik.data.db.modules.luckynumber.LuckyNumber;
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata;
import pl.szczodrzynski.edziennik.data.db.modules.notices.Notice;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile;
import pl.szczodrzynski.edziennik.data.db.modules.subjects.Subject;
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher;
import pl.szczodrzynski.edziennik.data.db.modules.teams.Team;
import pl.szczodrzynski.edziennik.databinding.FragmentLoginMigrationBinding;
import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.models.Time;

import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_CLASS_EVENT;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_DEFAULT;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_ESSAY;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_EXAM;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_EXCURSION;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_HOMEWORK;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_INFORMATION;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_PROJECT;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_PT_MEETING;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_READING;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_SHORT_QUIZ;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_CLASS_EVENT;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_DEFAULT;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_ESSAY;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_EXAM;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_EXCURSION;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_HOMEWORK;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_INFORMATION;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_PROJECT;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_PT_MEETING;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_READING;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_SHORT_QUIZ;
import static pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore.LOGIN_TYPE_DEMO;
import static pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore.LOGIN_TYPE_IUCZNIOWIE;
import static pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore.LOGIN_TYPE_LIBRUS;
import static pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore.LOGIN_TYPE_MOBIDZIENNIK;
import static pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore.LOGIN_TYPE_VULCAN;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.REGISTRATION_DISABLED;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.REGISTRATION_ENABLED;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.REGISTRATION_UNSPECIFIED;

public class LoginMigrationFragment extends Fragment {

    private App app;
    private NavController nav;
    private FragmentLoginMigrationBinding b;
    private static final String TAG = "LoginMigration";

    public LoginMigrationFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (getActivity() != null) {
            app = (App) getActivity().getApplicationContext();
            nav = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
        }
        else {
            return null;
        }
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_login_migration, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        assert getContext() != null;
        assert getActivity() != null;

        String profilesStr;
        if (app.appConfig.lastAppVersion == 198 && (profilesStr = app.appSharedPrefs.getString("app.appConfig.profiles", null)) != null) {
            Toast.makeText(app, getString(R.string.login_migration_toast), Toast.LENGTH_SHORT).show();
            AsyncTask.execute(() -> {
                try {
                    migrate(profilesStr);
                }
                catch (Exception e) {
                    getActivity().runOnUiThread(() -> {
                        new MaterialDialog.Builder(getActivity())
                                .title(R.string.error_occured)
                                .content(getString(R.string.login_migration_error_format))
                                .positiveText(R.string.ok)
                                .onPositive(((dialog, which) -> {
                                    dialog.dismiss();
                                    getActivity().setResult(Activity.RESULT_OK);
                                    getActivity().finish();
                                }))
                                .autoDismiss(false)
                                .canceledOnTouchOutside(false)
                                .show();
                    });
                }
                finally {
                    if (app.db.profileDao().getIdsNow().size() != 0) {
                        app.appSharedPrefs.edit().remove("app.appConfig.profiles").apply();
                    }
                    app.appConfig.lastAppVersion = BuildConfig.VERSION_CODE;
                    app.saveConfig("lastAppVersion");
                }
            });
        }

        b.doneButton.setOnClickListener((v -> {
            nav.navigate(R.id.loginMigrationSyncFragment, null, LoginActivity.navOptions);
        }));
    }

    private String gp(SharedPreferences p, String key, String defValue) {
        String s = p.getString(key, defValue);
        if (s == null)
            s = defValue;
        return s;
    }
    private void migrate(String profilesStr) {
        Context c = getContext();
        if (c == null)
            return;
        List<Metadata> metadataList = new ArrayList<>();
        JsonArray profiles = new JsonParser().parse(profilesStr).getAsJsonArray();
        Map<Integer, JsonObject> loginStores = app.gson.fromJson(app.appSharedPrefs.getString("app.appConfig.loginStores", "{}"), new TypeToken<Map<Integer, JsonObject>>(){}.getType());
        if (loginStores == null) {
            Toast.makeText(c, "Błąd odczytywania słoików z danymi.", Toast.LENGTH_SHORT).show();
            return;
        }
        for (JsonElement profileEl: profiles) {
            JsonObject jProfile = profileEl.getAsJsonObject();
            int profileId = jProfile.get("id").getAsInt();
            SharedPreferences p = app.getSharedPreferences(String.format(getString(R.string.preference_file_register), profileId), Context.MODE_PRIVATE);

            if (!Boolean.parseBoolean(p.getString("app.register.loggedIn", Boolean.toString(false))))
                return;
            Profile profile = new Profile();
            profile.setId(profileId);
            profile.setName(jProfile.get("name").getAsString());
            profile.setSubname(jProfile.get("subname").getAsString());
            profile.setImage(jProfile.get("image").getAsString());

            profile.setSyncEnabled(Boolean.parseBoolean(p.getString("app.register.syncThisProfile", Boolean.toString(profile.getSyncEnabled()))));
            profile.setSyncNotifications(Boolean.parseBoolean(p.getString("app.register.syncNotificationsEnabled", Boolean.toString(profile.getSyncNotifications()))));
            profile.setEnableSharedEvents(Boolean.parseBoolean(p.getString("app.register.eventsShared", Boolean.toString(profile.getEnableSharedEvents()))));
            app.appConfig.countInSeconds = Boolean.parseBoolean(p.getString("app.register.countInSeconds", Boolean.toString(app.appConfig.countInSeconds)));
            // so in some APIs we force a full, clean sync
            profile.setEmpty(true);//Boolean.parseBoolean(p.getString("app.register.empty", Boolean.toString(profile.empty)));
            profile.setArchived(false);

            String s;
            s = gp(p, "app.register.studentNameLong", "\"\"");
            profile.setStudentNameLong(s.replace("\"", ""));
            s = gp(p, "app.register.studentNameShort", "\"\"");
            profile.setStudentNameShort(s.replace("\"", ""));
            profile.setStudentNumber(Integer.parseInt(gp(p, "app.register.studentNumber", "-1")));
            profile.setStudentData(new JsonParser().parse(gp(p, "app.register.studentStore", "[]")).getAsJsonObject());

            boolean autoRegistrationDecided = Boolean.parseBoolean(p.getString("app.register.autoRegistrationDecided", "true"));
            boolean autoRegistrationAllowed = Boolean.parseBoolean(p.getString("app.register.autoRegistrationAllowed", "true"));
            profile.setRegistration(!autoRegistrationAllowed && !autoRegistrationDecided ? REGISTRATION_UNSPECIFIED : !autoRegistrationAllowed ? REGISTRATION_DISABLED : REGISTRATION_ENABLED);

            profile.setGradeColorMode(Integer.parseInt(gp(p, "app.register.gradeColorMode", "1")));
            profile.setAgendaViewType(Integer.parseInt(gp(p, "app.register.agendaViewType", "0")));

            profile.setCurrentSemester(Integer.parseInt(gp(p, "app.register.currentSemester", "1")));

            profile.setAttendancePercentage(Float.parseFloat(gp(p, "app.register.attendancePercentage", "0.0")));

            profile.setDateSemester1Start(app.gson.fromJson(gp(p, "app.register.dateSemester1Start", ""), Date.class));
            profile.setDateSemester2Start(app.gson.fromJson(gp(p, "app.register.dateSemester2Start", ""), Date.class));
            profile.setDateYearEnd(app.gson.fromJson(gp(p, "app.register.dateYearEnd", ""), Date.class));

            profile.setLuckyNumberEnabled(Boolean.parseBoolean(gp(p, "app.register.luckyNumberEnabled", Boolean.toString(profile.getLuckyNumberEnabled()))));
            profile.setLuckyNumber(Integer.parseInt(gp(p, "app.register.luckyNumber", "-1")));
            profile.setLuckyNumberDate(app.gson.fromJson(gp(p, "app.register.luckyNumberDate", ""), Date.class));


            profile.setLoginStoreId(jProfile.get("loginStoreId").getAsInt());



            LoginStore loginStore = new LoginStore(
                    profile.getLoginStoreId(),
                    Integer.parseInt(gp(p, "app.register.loginType", "1")),
                    loginStores.get(profile.getLoginStoreId())
            );

            String teamPrefix;
            switch (loginStore.type) {
                case LOGIN_TYPE_MOBIDZIENNIK:
                    teamPrefix = loginStore.getLoginData("serverName", "MOBI_UN");
                    break;
                case LOGIN_TYPE_LIBRUS:
                    teamPrefix = profile.getStudentData("schoolName", "LIBRUS_UN");
                    break;
                case LOGIN_TYPE_IUCZNIOWIE:
                    teamPrefix = loginStore.getLoginData("schoolName", "IUCZNIOWIE_UN");
                    break;
                case LOGIN_TYPE_VULCAN:
                    teamPrefix = profile.getStudentData("schoolName", "VULCAN_UN");
                    break;
                case LOGIN_TYPE_DEMO:
                    teamPrefix = loginStore.getLoginData("serverName", "DEMO_UN");
                    break;
                default:
                    teamPrefix = "TYPE_UNKNOWN";
                    break;
            }








            JsonArray items = new JsonParser().parse(gp(p, "app.register.users", "[]")).getAsJsonArray();
            if (items != null) {
                List<Teacher> itemList = new ArrayList<>();
                for (JsonElement itemEl : items) {
                    JsonObject item = itemEl.getAsJsonObject();
                    if (item.get("id").getAsLong() == -1)
                        continue;
                    itemList.add(new Teacher(
                            profileId,
                            item.get("id").getAsLong() + (loginStore.type == LOGIN_TYPE_IUCZNIOWIE ? 32768 : 0),
                            item.get("name").getAsString(),
                            item.get("surname").getAsString()
                    ));
                }
                app.db.teacherDao().addAllIgnore(itemList);
            }

            items = new JsonParser().parse(gp(p, "app.register.subjects", "[]")).getAsJsonArray();
            if (items != null) {
                List<Subject> itemList = new ArrayList<>();
                for (JsonElement itemEl : items) {
                    JsonObject item = itemEl.getAsJsonObject();
                    if (item.get("id").getAsLong() == -1)
                        continue;
                    itemList.add(new Subject(
                            profileId,
                            item.get("id").getAsLong() + (loginStore.type == LOGIN_TYPE_IUCZNIOWIE ? 32768 : 0),
                            item.get("longName").getAsString(),
                            item.get("shortName").getAsString()
                    ));
                }
                app.db.subjectDao().addAll(itemList);
            }

            items = new JsonParser().parse(gp(p, "app.register.teams", "[]")).getAsJsonArray();
            List<Team> tItemList = new ArrayList<>();
            if (items != null) {
                for (JsonElement itemEl : items) {
                    JsonObject item = itemEl.getAsJsonObject();
                    if (item.get("id").getAsLong() == -1)
                        continue;
                    tItemList.add(new Team(
                            profileId,
                            item.get("id").getAsLong() + (loginStore.type == LOGIN_TYPE_IUCZNIOWIE ? 32768 : 0),
                            item.get("name").getAsString(),
                            item.get("type").getAsInt(),
                            teamPrefix+":"+item.get("name").getAsString(),
                            item.get("teacherId").getAsInt()
                    ));
                }
            }
            JsonObject tItem = new JsonParser().parse(gp(p, "app.register.teamClass", "")).getAsJsonObject();
            tItemList.add(new Team(
                    profileId,
                    tItem.get("id").getAsLong(),
                    tItem.get("name").getAsString(),
                    tItem.get("type").getAsInt(),
                    teamPrefix+":"+tItem.get("name").getAsString(),
                    tItem.get("teacherId").getAsInt()
            ));
            app.db.teamDao().addAll(tItemList);

            Map<Integer, Pair<String, Integer>> types = app.gson.fromJson(p.getString("app.register.eventTypes", "{}"), new TypeToken<Map<Integer, Pair<String, Integer>>>(){}.getType());
            if (types != null) {
                for (Integer index : types.keySet()) {
                    Pair<String, Integer> type = types.get(index);
                    if (type != null && type.second != null) {
                        int color = type.second;
                        switch (color) {
                            case 0xffdaa520:
                                color = COLOR_DEFAULT;
                                break;
                            case 0xffff0000:
                                color = COLOR_EXAM;
                                break;
                            case 0xffadff2f:
                                color = COLOR_SHORT_QUIZ;
                                break;
                            case 0xff4050b5:
                                color = COLOR_ESSAY;
                                break;
                            case 0xff673ab7:
                                color = COLOR_PROJECT;
                                break;
                            case 0xffabcdef:
                                color = COLOR_PT_MEETING;
                                break;
                            case 0xff4caf50:
                                color = COLOR_EXCURSION;
                                break;
                            case 0xffffeb3b:
                                color = COLOR_READING;
                                break;
                        }
                        app.db.eventTypeDao().add(new EventType(profileId, index, type.first, color));
                    }
                }
            }

            items = new JsonParser().parse(gp(p, "app.register.events", "[]")).getAsJsonArray();
            if (items != null) {
                List<Event> itemList = new ArrayList<>();
                for (JsonElement itemEl : items) {
                    JsonObject item = itemEl.getAsJsonObject();
                    int color = item.get("color").getAsInt();
                    switch (color) {
                        case 0xffdaa520:
                        case 0xffff0000:
                        case 0xffadff2f:
                        case 0xff4050b5:
                        case 0xff673ab7:
                        case 0xffabcdef:
                        case 0xff4caf50:
                        case 0xffffeb3b:
                            color = -1;
                            break;
                    }
                    long id = item.get("id").getAsLong();
                    Event itemObj = new Event(
                            profileId,
                            id,
                            app.gson.fromJson(item.get("eventDate"), Date.class),
                            app.gson.fromJson(item.get("startTime"), Time.class),
                            item.get("topic").getAsString(),
                            color,
                            item.get("type").getAsInt(),
                            (loginStore.type != LOGIN_TYPE_MOBIDZIENNIK || id >= 1420070400000L) && item.get("addedManually").getAsBoolean(),
                            item.get("teacherId").getAsInt(),
                            item.get("subjectId").getAsInt(),
                            item.get("teamId").getAsInt());
                    if (item.get("sharedBy") != null) {
                        itemObj.sharedBy = item.get("sharedBy").getAsString();
                        itemObj.sharedByName = item.get("sharedByName").getAsString();
                    }
                    Metadata metadataObj = new Metadata(
                            profileId,
                            Metadata.TYPE_EVENT,
                            itemObj.id,
                            item.get("seen").getAsBoolean(),
                            item.get("notified").getAsBoolean(),
                            item.get("addedDate").getAsLong()
                    );
                    if (itemObj.id == -1)
                        continue;
                    itemList.add(itemObj);
                    metadataList.add(metadataObj);
                }
                app.db.eventDao().addAll(itemList);
            }
            items = new JsonParser().parse(gp(p, "app.register.homeworksNew", "[]")).getAsJsonArray();
            if (items != null) {
                List<Event> itemList = new ArrayList<>();
                for (JsonElement itemEl : items) {
                    JsonObject item = itemEl.getAsJsonObject();
                    Event itemObj = new Event(
                            profileId,
                            item.get("id").getAsLong(),
                            app.gson.fromJson(item.get("eventDate"), Date.class),
                            app.gson.fromJson(item.get("startTime"), Time.class),
                            item.get("topic").getAsString(),
                            -1,
                            TYPE_HOMEWORK,
                            item.get("addedManually").getAsBoolean(),
                            item.get("teacherId").getAsInt(),
                            item.get("subjectId").getAsInt(),
                            item.get("teamId").getAsInt());
                    if (item.get("sharedBy") != null) {
                        itemObj.sharedBy = item.get("sharedBy").getAsString();
                        itemObj.sharedByName = item.get("sharedByName").getAsString();
                    }
                    Metadata metadataObj = new Metadata(
                            profileId,
                            Metadata.TYPE_HOMEWORK,
                            itemObj.id,
                            item.get("seen").getAsBoolean(),
                            item.get("notified").getAsBoolean(),
                            item.get("addedDate").getAsLong()
                    );
                    if (itemObj.id == -1)
                        continue;
                    itemList.add(itemObj);
                    metadataList.add(metadataObj);
                }
                app.db.eventDao().addAll(itemList);
            }

            items = new JsonParser().parse(gp(p, "app.register.gradeCategories", "[]")).getAsJsonArray();
            LongSparseArray<Pair<String, Integer>> gradeCategories = new LongSparseArray<>();
            if (items != null) {
                for (JsonElement itemEl : items) {
                    JsonObject item = itemEl.getAsJsonObject();
                    if (item.get("id").getAsLong() == -1)
                        continue;
                    gradeCategories.put(item.get("id").getAsLong(),
                            new Pair<>(item.get("description").getAsString(), item.get("color").getAsInt()));
                }
            }

            items = new JsonParser().parse(gp(p, "app.register.grades", "[]")).getAsJsonArray();
            if (items != null) {
                List<Grade> itemList = new ArrayList<>();
                for (JsonElement itemEl : items) {
                    JsonObject item = itemEl.getAsJsonObject();
                    long categoryId = item.get("categoryId").getAsInt();
                    Pair<String, Integer> category = gradeCategories.get(categoryId);
                    Grade itemObj = new Grade(
                            profileId,
                            item.get("id").getAsLong(),
                            category != null ? category.first : "",
                            category != null && category.second != null ? category.second : 0xff0000ff,
                            item.get("description").getAsString(),
                            item.get("name").getAsString(),
                            item.get("value").getAsFloat(),
                            item.get("weight").getAsInt(),
                            item.get("semester").getAsInt(),
                            item.get("teacherId").getAsInt(),
                            item.get("subjectId").getAsInt());
                    itemObj.type = item.get("type").getAsInt();
                    if (loginStore.type == LOGIN_TYPE_IUCZNIOWIE && itemObj.type != 0)
                        continue;
                    Metadata metadataObj = new Metadata(
                            profileId,
                            Metadata.TYPE_GRADE,
                            itemObj.id,
                            item.get("seen").getAsBoolean(),
                            item.get("notified").getAsBoolean(),
                            item.get("addedDate").getAsLong()
                    );
                    if (itemObj.id == -1)
                        continue;
                    itemList.add(itemObj);
                    metadataList.add(metadataObj);
                }
                app.db.gradeDao().addAll(itemList);
            }

            items = new JsonParser().parse(gp(p, "app.register.notices", "[]")).getAsJsonArray();
            if (items != null) {
                List<Notice> itemList = new ArrayList<>();
                for (JsonElement itemEl : items) {
                    JsonObject item = itemEl.getAsJsonObject();
                    Notice itemObj = new Notice(
                            profileId,
                            item.get("id").getAsLong() + (loginStore.type == LOGIN_TYPE_IUCZNIOWIE ? 32768 : 0),
                            item.get("reason").getAsString(),
                            item.get("semester").getAsInt(),
                            item.get("type").getAsInt(),
                            item.get("teacherId").getAsInt());
                    Metadata metadataObj = new Metadata(
                            profileId,
                            Metadata.TYPE_NOTICE,
                            itemObj.id,
                            item.get("seen").getAsBoolean(),
                            item.get("notified").getAsBoolean(),
                            item.get("addedDate").getAsLong()
                    );
                    if (itemObj.id == -1)
                        continue;
                    itemList.add(itemObj);
                    metadataList.add(metadataObj);
                }
                app.db.noticeDao().addAll(itemList);
            }

            items = new JsonParser().parse(gp(p, "app.register.attendances", "[]")).getAsJsonArray();
            if (items != null) {
                List<Attendance> itemList = new ArrayList<>();
                for (JsonElement itemEl : items) {
                    JsonObject item = itemEl.getAsJsonObject();
                    Attendance itemObj = new Attendance(
                            profileId,
                            item.get("id").getAsLong(),
                            item.get("teacherId").getAsInt(),
                            item.get("subjectId").getAsInt(),
                            item.get("semester").getAsInt(),
                            item.get("lessonTopic").getAsString(),
                            app.gson.fromJson(item.get("lessonDate"), Date.class),
                            app.gson.fromJson(item.get("startTime"), Time.class),
                            item.get("type").getAsInt());
                    Metadata metadataObj = new Metadata(
                            profileId,
                            Metadata.TYPE_ATTENDANCE,
                            itemObj.id,
                            item.get("seen").getAsBoolean(),
                            item.get("notified").getAsBoolean(),
                            item.get("addedDate").getAsLong()
                    );
                    if (itemObj.id == -1)
                        continue;
                    itemList.add(itemObj);
                    metadataList.add(metadataObj);
                }
                app.db.attendanceDao().addAll(itemList);
            }


            tItem = new JsonParser().parse(gp(p, "app.register.timetable", "{weekdays: [], lessonChanges: [], lessonAdditions: []}")).getAsJsonObject();
            if (tItem != null) {

                List<Lesson> itemLessonList = new ArrayList<>();
                for (JsonElement weekDayEl : tItem.getAsJsonArray("weekdays")) {
                    JsonObject weekDay = weekDayEl.getAsJsonObject();
                    int weekDayNum = weekDay.get("weekDay").getAsInt();
                    for (JsonElement itemEl : weekDay.getAsJsonArray("lessons")) {
                        JsonObject item = itemEl.getAsJsonObject();
                        Lesson itemObj = new Lesson(
                                profileId,
                                weekDayNum,
                                app.gson.fromJson(item.get("startTime"), Time.class),
                                app.gson.fromJson(item.get("endTime"), Time.class)
                        );
                        itemObj.classroomName = item.get("classroomName").getAsString();
                        itemObj.subjectId = item.get("subjectId").getAsInt();
                        itemObj.teacherId = item.get("teacherId").getAsInt();
                        itemObj.teamId = item.get("teamId").getAsInt();
                        itemLessonList.add(itemObj);
                    }
                }
                app.db.lessonDao().addAll(itemLessonList);

                List<LessonChange> itemList = new ArrayList<>();
                for (JsonElement itemEl : tItem.getAsJsonArray("lessonChanges")) {
                    JsonObject item = itemEl.getAsJsonObject();
                    LessonChange itemObj = new LessonChange(
                            profileId,
                            app.gson.fromJson(item.get("lessonDate"), Date.class),
                            app.gson.fromJson(item.get("startTime"), Time.class),
                            app.gson.fromJson(item.get("endTime"), Time.class));
                    itemObj.classroomName = item.get("classroomName").getAsString();
                    itemObj.subjectId = item.get("subjectId").getAsInt();
                    itemObj.teacherId = item.get("teacherId").getAsInt();
                    itemObj.teamId = item.get("teamId").getAsInt();
                    itemObj.type = item.get("type").getAsInt();
                    Metadata metadataObj = new Metadata(
                            profileId,
                            Metadata.TYPE_LESSON_CHANGE,
                            itemObj.id,
                            item.get("seen").getAsBoolean(),
                            item.get("notified").getAsBoolean(),
                            System.currentTimeMillis()
                    );
                    if (itemObj.id == -1)
                        continue;
                    itemList.add(itemObj);
                    metadataList.add(metadataObj);
                }
                app.db.lessonChangeDao().addAll(itemList);
            }

            app.db.eventTypeDao().add(new EventType(profileId, TYPE_HOMEWORK, getString(R.string.event_type_homework), COLOR_HOMEWORK));
            app.db.eventTypeDao().add(new EventType(profileId, TYPE_DEFAULT, getString(R.string.event_other), COLOR_DEFAULT));
            app.db.eventTypeDao().add(new EventType(profileId, TYPE_EXAM, getString(R.string.event_exam), COLOR_EXAM));
            app.db.eventTypeDao().add(new EventType(profileId, TYPE_SHORT_QUIZ, getString(R.string.event_short_quiz), COLOR_SHORT_QUIZ));
            app.db.eventTypeDao().add(new EventType(profileId, TYPE_ESSAY, getString(R.string.event_essay), COLOR_SHORT_QUIZ));
            app.db.eventTypeDao().add(new EventType(profileId, TYPE_PROJECT, getString(R.string.event_project), COLOR_PROJECT));
            app.db.eventTypeDao().add(new EventType(profileId, TYPE_PT_MEETING, getString(R.string.event_pt_meeting), COLOR_PT_MEETING));
            app.db.eventTypeDao().add(new EventType(profileId, TYPE_EXCURSION, getString(R.string.event_excursion), COLOR_EXCURSION));
            app.db.eventTypeDao().add(new EventType(profileId, TYPE_READING, getString(R.string.event_reading), COLOR_READING));
            app.db.eventTypeDao().add(new EventType(profileId, TYPE_CLASS_EVENT, getString(R.string.event_class_event), COLOR_CLASS_EVENT));
            app.db.eventTypeDao().add(new EventType(profileId, TYPE_INFORMATION, getString(R.string.event_information), COLOR_INFORMATION));

            if (profile.getLuckyNumberDate() != null) {
                app.db.luckyNumberDao().add(new LuckyNumber(profile.getId(), profile.getLuckyNumberDate(), profile.getLuckyNumber()));
            }
            app.db.profileDao().add(profile);
            app.db.loginStoreDao().add(loginStore);
            app.db.metadataDao().addAllIgnore(metadataList);
        }


        try {
            app.appConfig.appInstalledTime = app.getPackageManager().getPackageInfo(app.getPackageName(), 0).firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        app.appConfig.appRateSnackbarTime = app.appConfig.appInstalledTime + 2 * 24 * 60 * 60 * 1000;
        app.appConfig.loginFinished = true;
        app.saveConfig("loginFinished", "appInstalledTime", "appRateSnackbarTime");
    }
}

