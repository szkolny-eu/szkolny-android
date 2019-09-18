package pl.szczodrzynski.edziennik.receivers;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import pl.szczodrzynski.edziennik.WidgetTimetable;

public class UserPresentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null) {
            if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                //Toast.makeText(context, "User is present", Toast.LENGTH_SHORT).show();
                Intent widgetIntent = new Intent(context, WidgetTimetable.class);
                widgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
                // since it seems the onUpdate() is only fired on that:
                int[] ids = AppWidgetManager.getInstance(context)
                        .getAppWidgetIds(new ComponentName(context, WidgetTimetable.class));
                widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                context.sendBroadcast(widgetIntent);
            }
        }
    }
}
