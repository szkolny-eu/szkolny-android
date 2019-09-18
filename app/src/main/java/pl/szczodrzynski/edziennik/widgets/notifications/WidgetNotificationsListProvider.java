package pl.szczodrzynski.edziennik.widgets.notifications;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.models.Date;
import pl.szczodrzynski.edziennik.models.ItemWidgetTimetableModel;
import pl.szczodrzynski.edziennik.models.Notification;

public class WidgetNotificationsListProvider implements RemoteViewsService.RemoteViewsFactory {

    private static final String TAG = "WidgetNotificationsProv";
    private App app;
    private Context context;
    private int appWidgetId;
    private boolean bigStyle;
    private boolean darkTheme;

    //For obtaining the activity's context and intent
    public WidgetNotificationsListProvider(Context context, Intent intent) {
        this.app = (App) context.getApplicationContext();
        this.context = context;
        this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
        this.bigStyle = intent.getBooleanExtra("bigStyle", false);
        this.darkTheme = intent.getBooleanExtra("darkTheme", false);
        // executed only ONCE
        Log.d(TAG, "appWidgetId: "+appWidgetId);
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
        // executed EVERY TIME
        Log.d(TAG, "onDataSetChanged for appWidgetId: "+appWidgetId);
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public int getCount() {
        return (app.appConfig.notifications == null ? 0 : app.appConfig.notifications.size());
    }

    @Override
    public RemoteViews getViewAt(int i) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.row_widget_notifications_item);
        if (i > app.appConfig.notifications.size()-1)
            return views;
        Notification notification = app.appConfig.notifications.get(i);

        if (bigStyle) {
            views = new RemoteViews(context.getPackageName(), darkTheme ? R.layout.row_widget_notifications_dark_big_item : R.layout.row_widget_notifications_big_item);
        }
        else if (darkTheme) {
            views = new RemoteViews(context.getPackageName(), R.layout.row_widget_notifications_dark_item);
        }

        Intent intent = new Intent();
        notification.fillIntent(intent);
        views.setOnClickFillInIntent(R.id.widgetNotificationsRoot, intent);

        views.setTextViewText(R.id.widgetNotificationsTitle, app.getString(R.string.widget_notifications_title_format, notification.title, Notification.stringType(context, notification.type)));
        views.setTextViewText(R.id.widgetNotificationsText, notification.text);
        views.setTextViewText(R.id.widgetNotificationsDate, Date.fromMillis(notification.addedDate).getFormattedString());

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
