package pl.szczodrzynski.edziennik;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.SparseArray;
import android.view.View;
import android.widget.RemoteViews;

import com.mikepenz.iconics.IconicsColor;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSize;
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import pl.szczodrzynski.edziennik.data.db.modules.events.EventFull;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonFull;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile;
import pl.szczodrzynski.edziennik.ui.modules.home.HomeFragment;
import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.models.ItemWidgetTimetableModel;
import pl.szczodrzynski.edziennik.utils.models.Time;
import pl.szczodrzynski.edziennik.utils.models.Week;
import pl.szczodrzynski.edziennik.widgets.WidgetConfig;
import pl.szczodrzynski.edziennik.sync.SyncJob;
import pl.szczodrzynski.edziennik.widgets.timetable.LessonDetailsActivity;
import pl.szczodrzynski.edziennik.widgets.timetable.WidgetTimetableService;

import static pl.szczodrzynski.edziennik.ExtensionsKt.filterOutArchived;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_HOMEWORK;
import static pl.szczodrzynski.edziennik.utils.Utils.bs;


public class WidgetTimetable extends AppWidgetProvider {


    public static final String ACTION_SYNC_DATA = "ACTION_SYNC_DATA";
    private static final String TAG = "WidgetTimetable";
    private static int modeInt = 0;

    public WidgetTimetable() {
        // Start the worker thread
        //HandlerThread sWorkerThread = new HandlerThread("WidgetTimetable-worker");
        //sWorkerThread.start();
        //Handler sWorkerQueue = new Handler(sWorkerThread.getLooper());
    }

    public static SparseArray<List<ItemWidgetTimetableModel>> timetables = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_SYNC_DATA.equals(intent.getAction())){
            SyncJob.run((App) context.getApplicationContext());
        }
        super.onReceive(context, intent);
    }

    public static PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, WidgetTimetable.class);
        intent.setAction(action);
        return getPendingSelfIntent(context, intent);
    }
    public static PendingIntent getPendingSelfIntent(Context context, Intent intent) {
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ComponentName thisWidget = new ComponentName(context, WidgetTimetable.class);

        timetables = new SparseArray<>();
        //timetables.clear();

        App app = (App)context.getApplicationContext();

        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : allWidgetIds) {

            //d(TAG, "thr "+Thread.currentThread().getName());

            WidgetConfig widgetConfig = app.appConfig.widgetTimetableConfigs.get(appWidgetId);
            if (widgetConfig == null) {
                widgetConfig = new WidgetConfig(app.profileFirstId());
                app.appConfig.widgetTimetableConfigs.put(appWidgetId, widgetConfig);
                app.appConfig.savePending = true;
            }

            RemoteViews views;
            if (widgetConfig.bigStyle) {
                views = new RemoteViews(context.getPackageName(), widgetConfig.darkTheme ? R.layout.widget_timetable_dark_big : R.layout.widget_timetable_big);
            }
            else {
                views = new RemoteViews(context.getPackageName(), widgetConfig.darkTheme ? R.layout.widget_timetable_dark : R.layout.widget_timetable);
            }

            PorterDuff.Mode mode = PorterDuff.Mode.DST_IN;
            /*if (widgetConfig.darkTheme) {
                switch (modeInt) {
                    case 0:
                        mode = PorterDuff.Mode.ADD;
                        d(TAG, "ADD");
                        break;
                    case 1:
                        mode = PorterDuff.Mode.DST_ATOP;
                        d(TAG, "DST_ATOP");
                        break;
                    case 2:
                        mode = PorterDuff.Mode.DST_IN;
                        d(TAG, "DST_IN");
                        break;
                    case 3:
                        mode = PorterDuff.Mode.DST_OUT;
                        d(TAG, "DST_OUT");
                        break;
                    case 4:
                        mode = PorterDuff.Mode.DST_OVER;
                        d(TAG, "DST_OVER");
                        break;
                    case 5:
                        mode = PorterDuff.Mode.LIGHTEN;
                        d(TAG, "LIGHTEN");
                        break;
                    case 6:
                        mode = PorterDuff.Mode.MULTIPLY;
                        d(TAG, "MULTIPLY");
                        break;
                    case 7:
                        mode = PorterDuff.Mode.OVERLAY;
                        d(TAG, "OVERLAY");
                        break;
                    case 8:
                        mode = PorterDuff.Mode.SCREEN;
                        d(TAG, "SCREEN");
                        break;
                    case 9:
                        mode = PorterDuff.Mode.SRC_ATOP;
                        d(TAG, "SRC_ATOP");
                        break;
                    case 10:
                        mode = PorterDuff.Mode.SRC_IN;
                        d(TAG, "SRC_IN");
                        break;
                    case 11:
                        mode = PorterDuff.Mode.SRC_OUT;
                        d(TAG, "SRC_OUT");
                        break;
                    case 12:
                        mode = PorterDuff.Mode.SRC_OVER;
                        d(TAG, "SRC_OVER");
                        break;
                    case 13:
                        mode = PorterDuff.Mode.XOR;
                        d(TAG, "XOR");
                        break;
                    default:
                        modeInt = 0;
                        mode = PorterDuff.Mode.ADD;
                        d(TAG, "ADD");
                        break;
                }
            }*/
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                // this code seems to crash the launcher on >= P
                float transparency = widgetConfig.opacity;  //0...1
                long colorFilter = 0x01000000L * (long) (255f * transparency);
                try {
                    final Method[] declaredMethods = Class.forName("android.widget.RemoteViews").getDeclaredMethods();
                    final int len = declaredMethods.length;
                    if (len > 0) {
                        for (int m = 0; m < len; m++) {
                            final Method method = declaredMethods[m];
                            if (method.getName().equals("setDrawableParameters")) {
                                method.setAccessible(true);
                                method.invoke(views, R.id.widgetTimetableListView, true, -1, (int) colorFilter, mode, -1);
                                method.invoke(views, R.id.widgetTimetableHeader, true, -1, (int) colorFilter, mode, -1);
                                break;
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }

            Intent refreshIntent = new Intent(context, WidgetTimetable.class);
            refreshIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            PendingIntent pendingRefreshIntent = PendingIntent.getBroadcast(context,
                    0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widgetTimetableRefresh, pendingRefreshIntent);

            views.setOnClickPendingIntent(R.id.widgetTimetableSync, WidgetTimetable.getPendingSelfIntent(context, ACTION_SYNC_DATA));

            views.setImageViewBitmap(R.id.widgetTimetableRefresh, new IconicsDrawable(context, CommunityMaterial.Icon2.cmd_refresh)
                    .color(IconicsColor.colorInt(Color.WHITE))
                    .size(IconicsSize.dp(widgetConfig.bigStyle ? 24 : 16)).toBitmap());

            views.setImageViewBitmap(R.id.widgetTimetableSync, new IconicsDrawable(context, CommunityMaterial.Icon2.cmd_sync)
                    .color(IconicsColor.colorInt(Color.WHITE))
                    .size(IconicsSize.dp(widgetConfig.bigStyle ? 24 : 16)).toBitmap());

            boolean unified = widgetConfig.profileId == -1;

            List<Profile> profileList = new ArrayList<>();
            if (unified) {
                profileList = app.db.profileDao().getAllNow();
                filterOutArchived(profileList);
            }
            else {
                Profile profile = app.db.profileDao().getByIdNow(widgetConfig.profileId);
                if (profile != null) {
                    profileList.add(profile);
                }
            }

            //d(TAG, "Profiles: "+ Arrays.toString(profileList.toArray()));

            if (profileList == null || profileList.size() == 0) {
                views.setViewVisibility(R.id.widgetTimetableLoading, View.VISIBLE);
                views.setTextViewText(R.id.widgetTimetableLoading, app.getString(R.string.widget_timetable_profile_doesnt_exist));
            }
            else {
                views.setViewVisibility(R.id.widgetTimetableLoading, View.GONE);
                //Register profile;

                long bellSyncDiffMillis = 0;
                if (app.appConfig.bellSyncDiff != null) {
                    bellSyncDiffMillis = app.appConfig.bellSyncDiff.hour * 60 * 60 * 1000 + app.appConfig.bellSyncDiff.minute * 60 * 1000 + app.appConfig.bellSyncDiff.second * 1000;
                    bellSyncDiffMillis *= app.appConfig.bellSyncMultiplier;
                    bellSyncDiffMillis *= -1;
                }

                List<ItemWidgetTimetableModel> lessonList = new ArrayList<>();

                Time syncedNow = Time.fromMillis(Time.getNow().getInMillis() + bellSyncDiffMillis);

                Date today = Date.getToday();

                int openProfileId = -1;
                Date displayingDate = null;
                int displayingWeekDay = 0;
                if (unified) {
                    views.setTextViewText(R.id.widgetTimetableSubtitle, app.getString(R.string.widget_timetable_title_unified));
                }
                else {
                    views.setTextViewText(R.id.widgetTimetableSubtitle, profileList.get(0).getName());
                    openProfileId = profileList.get(0).getId();
                }

                List<LessonFull> lessons = app.db.lessonDao().getAllWeekNow(unified ? -1 : openProfileId, today.clone().stepForward(0, 0, -today.getWeekDay()), today);

                int scrollPos = 0;

                for (Profile profile: profileList) {
                    Date profileDisplayingDate = HomeFragment.findDateWithLessons(profile.getId(), lessons, syncedNow, 1);
                    int profileDisplayingWeekDay = profileDisplayingDate.getWeekDay();
                    int dayDiff = Date.diffDays(profileDisplayingDate, Date.getToday());

                    //d(TAG, "For profile "+profile.name+" displayingDate is "+profileDisplayingDate.getStringY_m_d());
                    if (displayingDate == null || profileDisplayingDate.getValue() < displayingDate.getValue()) {
                        displayingDate = profileDisplayingDate;
                        displayingWeekDay = profileDisplayingWeekDay;
                        //d(TAG, "Setting as global dd");
                        if (dayDiff == 0) {
                            views.setTextViewText(R.id.widgetTimetableTitle, app.getString(R.string.day_today_format, Week.getFullDayName(displayingWeekDay)));
                        } else if (dayDiff == 1) {
                            views.setTextViewText(R.id.widgetTimetableTitle, app.getString(R.string.day_tomorrow_format, Week.getFullDayName(displayingWeekDay)));
                        } else {
                            views.setTextViewText(R.id.widgetTimetableTitle, Week.getFullDayName(displayingWeekDay) + " " + profileDisplayingDate.getStringDm());
                        }
                    }
                }

                for (Profile profile: profileList) {
                    int pos = 0;

                    List<EventFull> events = app.db.eventDao().getAllByDateNow(profile.getId(), displayingDate);
                    if (events == null)
                        events = new ArrayList<>();

                    if (unified) {
                        ItemWidgetTimetableModel separator = new ItemWidgetTimetableModel();
                        separator.profileId = profile.getId();
                        separator.bigStyle = widgetConfig.bigStyle;
                        separator.darkTheme = widgetConfig.darkTheme;
                        separator.separatorProfileName = profile.getName();
                        lessonList.add(separator);
                    }

                    for (LessonFull lesson : lessons) {
                        //d(TAG, "Profile "+profile.id+" Lesson profileId "+lesson.profileId+" weekDay "+lesson.weekDay+", "+lesson);
                        if (profile.getId() != lesson.profileId || displayingWeekDay != lesson.weekDay)
                            continue;
                        //d(TAG, "Not skipped");
                        ItemWidgetTimetableModel model = new ItemWidgetTimetableModel();

                        model.bigStyle = widgetConfig.bigStyle;
                        model.darkTheme = widgetConfig.darkTheme;

                        model.profileId = profile.getId();

                        model.lessonDate = displayingDate;
                        model.startTime = lesson.startTime;
                        model.endTime = lesson.endTime;

                        model.lessonPassed = (syncedNow.getValue() > lesson.endTime.getValue()) && displayingWeekDay == Week.getTodayWeekDay();
                        model.lessonCurrent = (Time.inRange(lesson.startTime, lesson.endTime, syncedNow)) && displayingWeekDay == Week.getTodayWeekDay();

                        if (model.lessonCurrent) {
                            scrollPos = pos;
                        } else if (model.lessonPassed) {
                            scrollPos = pos + 1;
                        }
                        pos++;

                        model.subjectName = bs(lesson.subjectLongName);
                        model.classroomName = lesson.classroomName;

                        model.bellSyncDiffMillis = bellSyncDiffMillis;

                        if (lesson.changeId != 0) {
                            if (lesson.changeType == LessonChange.TYPE_CHANGE) {
                                model.lessonChange = true;
                                if (lesson.changedClassroomName()) {
                                    model.newClassroomName = lesson.changeClassroomName;
                                }

                                if (lesson.changedSubjectLongName()) {
                                    model.newSubjectName = lesson.changeSubjectLongName;
                                }
                            }
                            if (lesson.changeType == LessonChange.TYPE_CANCELLED) {
                                model.lessonCancelled = true;
                            }
                        }

                        for (EventFull event : events) {
                            if (event.startTime == null)
                                continue;
                            if (event.eventDate.getValue() == displayingDate.getValue()
                                    && event.startTime.getValue() == lesson.startTime.getValue()) {
                                model.eventColors.add(event.type == TYPE_HOMEWORK ? ItemWidgetTimetableModel.EVENT_COLOR_HOMEWORK : event.getColor());
                            }
                        }

                        lessonList.add(model);
                    }
                }

                if (lessonList.size() == 0) {
                    views.setViewVisibility(R.id.widgetTimetableLoading, View.VISIBLE);
                    views.setRemoteAdapter(R.id.widgetTimetableListView, new Intent());
                    views.setTextViewText(R.id.widgetTimetableLoading, app.getString(R.string.widget_timetable_no_lessons));
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }
                else {
                    views.setViewVisibility(R.id.widgetTimetableLoading, View.GONE);

                    timetables.put(appWidgetId, lessonList);
                    //WidgetTimetableListProvider.widgetsLessons.put(appWidgetId, lessons);
                    //views.setRemoteAdapter(R.id.widgetTimetableListView, new Intent());
                    Intent listIntent = new Intent(context, WidgetTimetableService.class);
                    listIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    listIntent.setData(Uri.parse(listIntent.toUri(Intent.URI_INTENT_SCHEME)));
                    views.setRemoteAdapter(R.id.widgetTimetableListView, listIntent);

                    // template to handle the click listener for each item
                    Intent intentTemplate = new Intent(context, LessonDetailsActivity.class);
                    // Old activities shouldn't be in the history stack
                    intentTemplate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    PendingIntent pendingIntentTimetable = PendingIntent.getActivity(context,
                            0,
                            intentTemplate,
                            0);
                    views.setPendingIntentTemplate(R.id.widgetTimetableListView, pendingIntentTimetable);

                    Intent openIntent = new Intent(context, MainActivity.class);
                    openIntent.setAction("android.intent.action.MAIN");
                    if (!unified) {
                        openIntent.putExtra("profileId", openProfileId);
                        openIntent.putExtra("timetableDate", displayingDate.getValue());
                    }
                    openIntent.putExtra("fragmentId", MainActivity.DRAWER_ITEM_TIMETABLE);
                    PendingIntent pendingOpenIntent = PendingIntent.getActivity(context,
                            appWidgetId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    views.setOnClickPendingIntent(R.id.widgetTimetableHeader, pendingOpenIntent);

                    if (!unified)
                        views.setScrollPosition(R.id.widgetTimetableListView, scrollPos);
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetTimetableListView);
        }
        //modeInt++;
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        App app = (App) context.getApplicationContext();
        for (int appWidgetId: appWidgetIds) {
            app.appConfig.widgetTimetableConfigs.remove(appWidgetId);
        }
        app.saveConfig("widgetTimetableConfigs");
    }
}

