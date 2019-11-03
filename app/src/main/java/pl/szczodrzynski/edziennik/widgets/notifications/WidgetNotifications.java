package pl.szczodrzynski.edziennik.widgets.notifications;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import com.mikepenz.iconics.IconicsColor;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSize;
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.api.v2.events.task.EdziennikTask;
import pl.szczodrzynski.edziennik.widgets.WidgetConfig;

public class WidgetNotifications extends AppWidgetProvider {

    public static final String ACTION_SYNC_DATA = "ACTION_SYNC_DATA";
    private static final String TAG = "WidgetNotifications";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_SYNC_DATA.equals(intent.getAction())){
            EdziennikTask.Companion.sync().enqueue(context);
        }
        super.onReceive(context, intent);
    }

    public static PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, WidgetNotifications.class);
        intent.setAction(action);
        return getPendingSelfIntent(context, intent);
    }
    public static PendingIntent getPendingSelfIntent(Context context, Intent intent) {
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ComponentName thisWidget = new ComponentName(context, WidgetNotifications.class);

        App app = (App)context.getApplicationContext();

        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : allWidgetIds) {

            WidgetConfig widgetConfig = app.appConfig.widgetTimetableConfigs.get(appWidgetId);
            if (widgetConfig == null) {
                widgetConfig = new WidgetConfig(-1);
                app.appConfig.widgetTimetableConfigs.put(appWidgetId, widgetConfig);
                app.appConfig.savePending = true;
            }

            RemoteViews views;
            if (widgetConfig.bigStyle) {
                views = new RemoteViews(context.getPackageName(), widgetConfig.darkTheme ? R.layout.widget_notifications_dark_big : R.layout.widget_notifications_big);
            }
            else {
                views = new RemoteViews(context.getPackageName(), widgetConfig.darkTheme ? R.layout.widget_notifications_dark : R.layout.widget_notifications);
            }

            PorterDuff.Mode mode = PorterDuff.Mode.DST_IN;
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
                                method.invoke(views, R.id.widgetNotificationsListView, true, -1, (int) colorFilter, mode, -1);
                                method.invoke(views, R.id.widgetNotificationsHeader, true, -1, (int) colorFilter, mode, -1);
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

            Intent refreshIntent = new Intent(context, WidgetNotifications.class);
            refreshIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            PendingIntent pendingRefreshIntent = PendingIntent.getBroadcast(context,
                    0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widgetNotificationsRefresh, pendingRefreshIntent);

            views.setOnClickPendingIntent(R.id.widgetNotificationsSync, getPendingSelfIntent(context, ACTION_SYNC_DATA));

            views.setImageViewBitmap(R.id.widgetNotificationsRefresh, new IconicsDrawable(context, CommunityMaterial.Icon2.cmd_refresh)
                    .color(IconicsColor.colorInt(Color.WHITE))
                    .size(IconicsSize.dp(widgetConfig.bigStyle ? 24 : 16)).toBitmap());

            views.setImageViewBitmap(R.id.widgetNotificationsSync, new IconicsDrawable(context, CommunityMaterial.Icon2.cmd_sync)
                    .color(IconicsColor.colorInt(Color.WHITE))
                    .size(IconicsSize.dp(widgetConfig.bigStyle ? 24 : 16)).toBitmap());

            //d(TAG, "Profiles: "+ Arrays.toString(profileList.toArray()));

            if (app.appConfig.notifications.size() == 0) {
                views.setViewVisibility(R.id.widgetNotificationsLoading, View.VISIBLE);
                views.setRemoteAdapter(R.id.widgetNotificationsListView, new Intent());
                views.setTextViewText(R.id.widgetNotificationsLoading, app.getString(R.string.widget_notifications_no_data));
            }
            else {
                views.setViewVisibility(R.id.widgetNotificationsLoading, View.GONE);

                Intent listIntent = new Intent(context, WidgetNotificationsService.class);
                listIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                listIntent.putExtra("bigStyle", widgetConfig.bigStyle);
                listIntent.putExtra("darkTheme", widgetConfig.darkTheme);
                listIntent.setData(Uri.parse(listIntent.toUri(Intent.URI_INTENT_SCHEME)));
                views.setRemoteAdapter(R.id.widgetNotificationsListView, listIntent);

                // template to handle the click listener for each item
                Intent intentTemplate = new Intent(context, MainActivity.class);
                intentTemplate.setAction("android.intent.action.MAIN");
                PendingIntent pendingIntentNotifications = PendingIntent.getActivity(context, 0, intentTemplate, 0);
                views.setPendingIntentTemplate(R.id.widgetNotificationsListView, pendingIntentNotifications);
            }

            Intent openIntent = new Intent(context, MainActivity.class);
            openIntent.setAction("android.intent.action.MAIN");
            openIntent.putExtra("fragmentId", MainActivity.DRAWER_ITEM_NOTIFICATIONS);
            PendingIntent pendingOpenIntent = PendingIntent.getActivity(context,
                    appWidgetId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widgetNotificationsHeader, pendingOpenIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetNotificationsListView);
        }
        //modeInt++;
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


