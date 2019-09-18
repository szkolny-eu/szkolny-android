package pl.szczodrzynski.edziennik.widgets.timetable;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.mikepenz.iconics.IconicsColor;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSize;
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;

import java.util.List;

import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.WidgetTimetable;
import pl.szczodrzynski.edziennik.models.Date;
import pl.szczodrzynski.edziennik.models.ItemWidgetTimetableModel;
import pl.szczodrzynski.edziennik.models.Time;

public class WidgetTimetableListProvider implements RemoteViewsService.RemoteViewsFactory {

    private static final String TAG = "WidgetTimetableProvider";
    private Context context;
    private int appWidgetId;
    private List<ItemWidgetTimetableModel> lessons = null;
    private static boolean triedToReload = false;

    //For obtaining the activity's context and intent
    public WidgetTimetableListProvider(Context context, Intent intent) {
        this.context = context;
        this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
        // executed only ONCE
        Log.d(TAG, "appWidgetId: "+appWidgetId);
        //App app = (App) context.getApplicationContext();
        //this.lessons = widgetsLessons.get(appWidgetId);
        /*this.lessons = new ArrayList<>();
        if (json != null) {
            lessons = app.gson.fromJson(json, new TypeToken<List<ItemWidgetTimetableModel>>(){}.getType());
        }*/
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
        // executed EVERY TIME
        Log.d(TAG, "onDataSetChanged for appWidgetId: "+appWidgetId);
        lessons = WidgetTimetable.timetables == null ? null : WidgetTimetable.timetables.get(appWidgetId);
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public int getCount() {
        return (lessons == null ? 0 : lessons.size());
    }

    public static int dpToPx(int dp)
    {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(int px)
    {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    public static Bitmap getColoredBitmap (Context context, @DrawableRes int resId, int color, int width, int height) {
        Bitmap bitmap;

        Drawable drawable = context.getResources().getDrawable(resId);

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Paint p = new Paint();
        ColorFilter filter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN);
        p.setColorFilter(filter);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        canvas.drawBitmap(bitmap, 0, 0, p);
        return bitmap;
    }

    private Bitmap changeBitmapColor(Bitmap sourceBitmap, int color) {
        Bitmap resultBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0,
                sourceBitmap.getWidth(), sourceBitmap.getHeight());
        Paint p = new Paint();
        ColorFilter filter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN);
        p.setColorFilter(filter);

        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(resultBitmap, 0, 0, p);

        return resultBitmap;
    }

    @Override
    public RemoteViews getViewAt(int i) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.row_widget_timetable_item);

        if (lessons != null) {
            if (i > lessons.size()-1)
                return views;
            ItemWidgetTimetableModel lesson = lessons.get(i);

            if (lesson.bigStyle) {
                views = new RemoteViews(context.getPackageName(), lesson.darkTheme ? R.layout.row_widget_timetable_dark_big_item : R.layout.row_widget_timetable_big_item);
            }
            else if (lesson.darkTheme) {
                views = new RemoteViews(context.getPackageName(), R.layout.row_widget_timetable_dark_item);
            }

            if (lesson.separatorProfileName != null) {
                //views.setViewVisibility(R.id.widgetTimetableBackground, View.GONE);
                Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
                Bitmap bmp = Bitmap.createBitmap(100, 5, conf); // this creates a MUTABLE bitmap
                Canvas canvas = new Canvas(bmp);

                canvas.drawARGB(0x70, 0, 0, 0);

                views.setImageViewBitmap(R.id.widgetTimetableBackground, bmp);

                views.setViewVisibility(R.id.widgetTimetableProfileName, View.VISIBLE);
                views.setViewVisibility(R.id.widgetTimetableContent, View.GONE);
                views.setTextViewText(R.id.widgetTimetableProfileName, lesson.separatorProfileName);

                Intent intent = new Intent();
                intent.putExtra("profileId", lesson.profileId);
                intent.putExtra("separatorItem", true);
                views.setOnClickFillInIntent(R.id.widgetTimetableRoot, intent);

                return views;
            }

            views.setViewVisibility(R.id.widgetTimetableBackground, View.VISIBLE);
            views.setViewVisibility(R.id.widgetTimetableProfileName, View.GONE);
            views.setViewVisibility(R.id.widgetTimetableContent, View.VISIBLE);

            Intent intent = new Intent();
            intent.putExtra("profileId", lesson.profileId);
            intent.putExtra("date", lesson.lessonDate.getStringValue());
            intent.putExtra("startTime", lesson.startTime.getStringValue());
            intent.putExtra("endTime", lesson.endTime.getStringValue());
            views.setOnClickFillInIntent(R.id.widgetTimetableRoot, intent);

            views.setTextViewText(R.id.widgetTimetableTime, lesson.startTime.getStringHM() + " - " + lesson.endTime.getStringHM());

            views.setViewVisibility(R.id.widgetTimetableEvent1, View.GONE);
            views.setViewVisibility(R.id.widgetTimetableEvent2, View.GONE);
            views.setViewVisibility(R.id.widgetTimetableEvent3, View.GONE);

            Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
            Bitmap bmp = Bitmap.createBitmap(100, 5, conf); // this creates a MUTABLE bitmap
            Canvas canvas = new Canvas(bmp);

            Paint p = new Paint();
            p.setStyle(Paint.Style.FILL_AND_STROKE);
            p.setAntiAlias(true);
            p.setFilterBitmap(true);
            p.setDither(true);
            p.setColor(lesson.darkTheme ? 0x30FFFFFF : 0x30000000);

            if (lesson.lessonDate.getValue() == Date.getToday().getValue()) {
                long startTime = lesson.startTime.getInMillis();
                long endTime = lesson.endTime.getInMillis();
                long now = Time.getNow().getInMillis();
                //Log.d("WidgetTimetableList", "startTime = "+startTime+" endTime = "+endTime+" now = "+now+" sync diff = "+lesson.bellSyncDiffMillis+" now synced = "+(now+lesson.bellSyncDiffMillis));
                now += lesson.bellSyncDiffMillis;

                if (now > endTime) {
                    // the lesson is over
                    canvas.drawRect(0, 0, 100, 5, p);
                }
                else if (now < startTime) {
                    // the lesson hasn't started yet
                    canvas.drawRect(0, 0, 0, 5, p);
                }
                else {
                    // we are during the lesson
                    long length = endTime - startTime;
                    long current = now - startTime;
                    float percent = (float)current / (float)length * 100.0f;
                    //Log.d("WidgetTimetableList", "length = "+length+" current = "+current+" percent = "+percent);
                    canvas.drawRect(0, 0, (int)percent, 5, p);
                }
            }
            else {
                canvas.drawRect(0, 0, 0, 5, p);
            }
            views.setImageViewBitmap(R.id.widgetTimetableBackground, bmp);

            int eventIndicatorSize = dpToPx(10);

            if (lesson.eventColors != null) {
                if (lesson.eventColors.size() >= 1) {
                    views.setViewVisibility(R.id.widgetTimetableEvent1, View.VISIBLE);
                    if (lesson.eventColors.get(0) == -1)
                        views.setBitmap(R.id.widgetTimetableEvent1, "setImageBitmap", new IconicsDrawable(context).color(IconicsColor.colorRes(R.color.md_red_500)).size(IconicsSize.dp(10)).icon(CommunityMaterial.Icon2.cmd_home).toBitmap());
                    else
                        views.setBitmap(R.id.widgetTimetableEvent1, "setImageBitmap", getColoredBitmap(context, R.drawable.event_color_circle, lesson.eventColors.get(0), eventIndicatorSize, eventIndicatorSize));
                    if (lesson.eventColors.size() >= 2) {
                        views.setViewVisibility(R.id.widgetTimetableEvent2, View.VISIBLE);
                        if (lesson.eventColors.get(1) == -1)
                            views.setBitmap(R.id.widgetTimetableEvent2, "setImageBitmap", new IconicsDrawable(context).color(IconicsColor.colorRes(R.color.md_red_500)).size(IconicsSize.dp(10)).icon(CommunityMaterial.Icon2.cmd_home).toBitmap());
                        else
                            views.setBitmap(R.id.widgetTimetableEvent2, "setImageBitmap", getColoredBitmap(context, R.drawable.event_color_circle, lesson.eventColors.get(1), eventIndicatorSize, eventIndicatorSize));
                        if (lesson.eventColors.size() >= 3) {
                            views.setViewVisibility(R.id.widgetTimetableEvent3, View.VISIBLE);
                            if (lesson.eventColors.get(2) == -1)
                                views.setBitmap(R.id.widgetTimetableEvent3, "setImageBitmap", new IconicsDrawable(context).color(IconicsColor.colorRes(R.color.md_red_500)).size(IconicsSize.dp(10)).icon(CommunityMaterial.Icon2.cmd_home).toBitmap());
                            else
                                views.setBitmap(R.id.widgetTimetableEvent3, "setImageBitmap", getColoredBitmap(context, R.drawable.event_color_circle, lesson.eventColors.get(2), eventIndicatorSize, eventIndicatorSize));
                        }
                    }
                }
            }
            //views.setViewVisibility(R.id.widgetTimetableEvent1, View.VISIBLE);
            //views.setBitmap(R.id.widgetTimetableEvent1, "setImageBitmap", getColoredBitmap(context, R.drawable.event_color_circle, 0xff4caf50, eventIndicatorSize, eventIndicatorSize));

            views.setViewVisibility(R.id.widgetTimetableOldSubjectName, View.GONE);
            if (lesson.lessonChange) {
                if (lesson.newSubjectName == null) {
                    views.setTextViewText(R.id.widgetTimetableSubjectName, Html.fromHtml("<i>"+lesson.subjectName+"</i>"));
                }
                else {
                    views.setViewVisibility(R.id.widgetTimetableOldSubjectName, View.VISIBLE);
                    views.setTextViewText(R.id.widgetTimetableOldSubjectName, Html.fromHtml("<del>"+lesson.subjectName+"</del>"));
                    views.setTextViewText(R.id.widgetTimetableSubjectName, lesson.newSubjectName);
                }

                if (lesson.newClassroomName == null) {
                    if (lesson.classroomName == null || lesson.classroomName.equals("")) {
                        lesson.classroomName = context.getString(R.string.timetable_no_classroom);
                    }
                    views.setTextViewText(R.id.widgetTimetableClassroomName, lesson.classroomName);
                }
                else {
                    views.setTextViewText(R.id.widgetTimetableClassroomName, Html.fromHtml("<i>"+lesson.newClassroomName+"</i>"));
                }

            }
            else if (lesson.lessonCancelled) {
                views.setTextViewText(R.id.widgetTimetableSubjectName, Html.fromHtml("<del>"+lesson.subjectName+"</del>"));
                views.setTextViewText(R.id.widgetTimetableClassroomName, Html.fromHtml("<del>"+lesson.classroomName+"</del>"));
            }
            else {
                views.setTextViewText(R.id.widgetTimetableSubjectName, lesson.subjectName);
                views.setTextViewText(R.id.widgetTimetableClassroomName, lesson.classroomName);
            }

        }
        else if (!triedToReload) {
            // try to reload the widget
            // only once
            triedToReload = true;
            Intent intent = new Intent(context, WidgetTimetable.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
            // since it seems the onUpdate() is only fired on that:
            int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, WidgetTimetable.class));
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            context.sendBroadcast(intent);
        }
        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}