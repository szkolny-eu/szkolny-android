package pl.szczodrzynski.edziennik.widgets.luckynumber;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import com.mikepenz.iconics.IconicsColor;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSize;
import com.mikepenz.iconics.typeface.IIcon;
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.datamodels.Profile;
import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.sync.SyncJob;
import pl.szczodrzynski.edziennik.widgets.WidgetConfig;

import static pl.szczodrzynski.edziennik.utils.Utils.getCellsForSize;

public class WidgetLuckyNumber extends AppWidgetProvider {

    public static final String ACTION_SYNC_DATA = "ACTION_SYNC_DATA";
    private static final String TAG = "WidgetLuckyNumber";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_SYNC_DATA.equals(intent.getAction())){
            SyncJob.run((App) context.getApplicationContext());
        }
        super.onReceive(context, intent);
    }

    public static PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, WidgetLuckyNumber.class);
        intent.setAction(action);
        return getPendingSelfIntent(context, intent);
    }
    public static PendingIntent getPendingSelfIntent(Context context, Intent intent) {
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        App app = (App) context.getApplicationContext();

        WidgetConfig widgetConfig = app.appConfig.widgetTimetableConfigs.get(appWidgetId);
        if (widgetConfig == null) {
            widgetConfig = new WidgetConfig(-1);
            app.appConfig.widgetTimetableConfigs.put(appWidgetId, widgetConfig);
            app.appConfig.savePending = true;
        }
        RemoteViews views = getRemoteViews(context, widgetConfig);
        updateLayout(widgetConfig, views, appWidgetManager, appWidgetId);
        appWidgetManager.updateAppWidget(appWidgetId, views);

        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    private RemoteViews getRemoteViews(Context context, WidgetConfig widgetConfig) {
        RemoteViews views;
        views = new RemoteViews(context.getPackageName(), R.layout.widget_lucky_number);
        if (widgetConfig.bigStyle) {
            views = new RemoteViews(context.getPackageName(), widgetConfig.darkTheme ? R.layout.widget_lucky_number_dark_big : R.layout.widget_lucky_number_big);
        }
        else {
            views = new RemoteViews(context.getPackageName(), widgetConfig.darkTheme ? R.layout.widget_lucky_number_dark : R.layout.widget_lucky_number);
        }
        return views;
    }

    private void updateLayout(WidgetConfig widgetConfig, RemoteViews views, AppWidgetManager appWidgetManager, int appWidgetId) {
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);

        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);

        int width = getCellsForSize(minWidth);
        int height = getCellsForSize(minHeight);

        if (width == 1) {
            views.setViewVisibility(R.id.widgetLuckyNumberProfileRight, View.GONE);
            views.setViewVisibility(R.id.widgetLuckyNumberProfileBottom, View.GONE);
            views.setViewVisibility(R.id.widgetLuckyNumberTextRight, View.GONE);
            views.setViewVisibility(R.id.widgetLuckyNumberTextBottom, View.VISIBLE);
        }
        else if (width == 2) {
            views.setViewVisibility(R.id.widgetLuckyNumberProfileRight, View.GONE);
            views.setViewVisibility(R.id.widgetLuckyNumberProfileBottom, View.VISIBLE);
            views.setViewVisibility(R.id.widgetLuckyNumberTextRight, View.VISIBLE);
            views.setViewVisibility(R.id.widgetLuckyNumberTextBottom, View.GONE);
        }
        else {
            views.setViewVisibility(R.id.widgetLuckyNumberProfileRight, widgetConfig.bigStyle ? View.GONE : View.VISIBLE);
            views.setViewVisibility(R.id.widgetLuckyNumberProfileBottom, widgetConfig.bigStyle ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widgetLuckyNumberTextRight, View.VISIBLE);
            views.setViewVisibility(R.id.widgetLuckyNumberTextBottom, View.GONE);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ComponentName thisWidget = new ComponentName(context, WidgetLuckyNumber.class);

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

            RemoteViews views = getRemoteViews(context, widgetConfig);

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
                                method.invoke(views, R.id.widgetLuckyNumberRoot, true, -1, (int) colorFilter, mode, -1);
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

            Profile profile = app.db.profileDao().getByIdNow(widgetConfig.profileId);
            IIcon icon = CommunityMaterial.Icon.cmd_emoticon_dead_outline;
            boolean noNumberText = false;
            if (profile == null) {
                // profile is not available, show error
                noNumberText = true;
                views.setTextViewText(R.id.widgetLuckyNumberTextRight, null);
                views.setTextViewText(R.id.widgetLuckyNumberTextBottom, null);
                views.setTextViewText(R.id.widgetLuckyNumberProfileRight, app.getString(R.string.widget_lucky_number_no_profile));
                views.setTextViewText(R.id.widgetLuckyNumberProfileBottom, app.getString(R.string.widget_lucky_number_no_profile));
            }
            else {
                // profile is available, show its name
                views.setTextViewText(R.id.widgetLuckyNumberProfileRight, profile.getName());
                views.setTextViewText(R.id.widgetLuckyNumberProfileBottom, profile.getName());

                if (profile.getLuckyNumberEnabled()
                        && profile.getLuckyNumber() != -1
                        && profile.getLuckyNumberDate() != null
                        && profile.getLuckyNumberDate().getValue() == Date.getToday().getValue()) {
                    // lucky number for today is set
                    views.setTextViewText(R.id.widgetLuckyNumberTextRight, String.valueOf(profile.getLuckyNumber()));
                    views.setTextViewText(R.id.widgetLuckyNumberTextBottom, String.valueOf(profile.getLuckyNumber()));
                    if (profile.getLuckyNumber() == profile.getStudentNumber()) {
                        // the lucky number is student's number
                        icon = CommunityMaterial.Icon.cmd_emoticon_cool_outline;
                    }
                    else {
                        // the lucky number is set
                        icon = CommunityMaterial.Icon.cmd_emoticon_excited_outline;
                    }
                }
                else {
                    // lucky number for today isn't set
                    noNumberText = true;
                    views.setTextViewText(R.id.widgetLuckyNumberTextRight, null);
                    views.setTextViewText(R.id.widgetLuckyNumberTextBottom, null);
                    icon = CommunityMaterial.Icon.cmd_emoticon_sad_outline;
                }
            }

            views.setViewVisibility(R.id.widgetLuckyNumberTextRightLayout, noNumberText ? View.GONE : View.VISIBLE);
            views.setViewVisibility(R.id.widgetLuckyNumberTextBottomLayout, noNumberText ? View.GONE : View.VISIBLE);

            views.setImageViewBitmap(R.id.widgetLuckyNumberIcon, new IconicsDrawable(context, icon)
                    .color(IconicsColor.colorInt(widgetConfig.darkTheme ? 0xfff3f3f3 : 0xff101010))
                    .size(IconicsSize.dp(widgetConfig.bigStyle ? 60 : 40)).toBitmap());

            updateLayout(widgetConfig, views, appWidgetManager, appWidgetId);

            Intent openIntent = new Intent(context, MainActivity.class);
            openIntent.setAction("android.intent.action.MAIN");
            openIntent.putExtra("fragmentId", MainActivity.DRAWER_ITEM_HOME);
            if (profile != null) {
                openIntent.putExtra("profileId", profile.getId());
            }
            PendingIntent pendingOpenIntent = PendingIntent.getActivity(context,
                    appWidgetId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widgetLuckyNumberRoot, pendingOpenIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
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
