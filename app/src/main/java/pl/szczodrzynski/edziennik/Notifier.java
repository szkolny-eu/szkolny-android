package pl.szczodrzynski.edziennik;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pl.szczodrzynski.edziennik.datamodels.ProfileFull;
import pl.szczodrzynski.edziennik.models.Date;
import pl.szczodrzynski.edziennik.models.Time;
import pl.szczodrzynski.edziennik.receivers.BootReceiver;
import pl.szczodrzynski.edziennik.sync.SyncJob;
import pl.szczodrzynski.edziennik.sync.SyncService;

import static androidx.core.app.NotificationCompat.PRIORITY_DEFAULT;
import static androidx.core.app.NotificationCompat.PRIORITY_MAX;
import static pl.szczodrzynski.edziennik.sync.SyncService.ACTION_CANCEL;

public class Notifier {

    private static final String TAG = "Notifier";
    public static final int ID_GET_DATA = 1337000;
    public static final int ID_GET_DATA_ERROR = 1337001;
    private static String CHANNEL_GET_DATA_NAME;
    private static String CHANNEL_GET_DATA_DESC;
    private static final String GROUP_KEY_GET_DATA = "pl.szczodrzynski.edziennik.GET_DATA";

    private static final int ID_NOTIFICATIONS = 1337002;
    private static String CHANNEL_NOTIFICATIONS_NAME;
    private static String CHANNEL_NOTIFICATIONS_DESC;
    public static final String GROUP_KEY_NOTIFICATIONS = "pl.szczodrzynski.edziennik.NOTIFICATIONS";

    private static final int ID_NOTIFICATIONS_QUIET = 1337002;
    private static String CHANNEL_NOTIFICATIONS_QUIET_NAME;
    private static String CHANNEL_NOTIFICATIONS_QUIET_DESC;
    public static final String GROUP_KEY_NOTIFICATIONS_QUIET = "pl.szczodrzynski.edziennik.NOTIFICATIONS_QUIET";

    private static final int ID_UPDATES = 1337003;
    private static String CHANNEL_UPDATES_NAME;
    private static String CHANNEL_UPDATES_DESC;
    private static final String GROUP_KEY_UPDATES = "pl.szczodrzynski.edziennik.UPDATES";

    private App app;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder getDataNotificationBuilder;
    private int notificationColor;

    Notifier(App _app) {
        this.app = _app;

        CHANNEL_GET_DATA_NAME = app.getString(R.string.notification_channel_get_data_name);
        CHANNEL_GET_DATA_DESC = app.getString(R.string.notification_channel_get_data_desc);
        CHANNEL_NOTIFICATIONS_NAME = app.getString(R.string.notification_channel_notifications_name);
        CHANNEL_NOTIFICATIONS_DESC = app.getString(R.string.notification_channel_notifications_desc);
        CHANNEL_NOTIFICATIONS_QUIET_NAME = app.getString(R.string.notification_channel_notifications_quiet_name);
        CHANNEL_NOTIFICATIONS_QUIET_DESC = app.getString(R.string.notification_channel_notifications_quiet_desc);
        CHANNEL_UPDATES_NAME = app.getString(R.string.notification_channel_updates_name);
        CHANNEL_UPDATES_DESC = app.getString(R.string.notification_channel_updates_desc);

        notificationColor = ContextCompat.getColor(app.getContext(), R.color.colorPrimary);
        notificationManager = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channelGetData = new NotificationChannel(GROUP_KEY_GET_DATA, CHANNEL_GET_DATA_NAME, NotificationManager.IMPORTANCE_LOW);
            channelGetData.setDescription(CHANNEL_GET_DATA_DESC);
            notificationManager.createNotificationChannel(channelGetData);

            NotificationChannel channelNotifications = new NotificationChannel(GROUP_KEY_NOTIFICATIONS, CHANNEL_NOTIFICATIONS_NAME, NotificationManager.IMPORTANCE_HIGH);
            channelNotifications.setDescription(CHANNEL_NOTIFICATIONS_DESC);
            channelNotifications.enableLights(true);
            channelNotifications.setLightColor(notificationColor);
            notificationManager.createNotificationChannel(channelNotifications);

            NotificationChannel channelNotificationsQuiet = new NotificationChannel(GROUP_KEY_NOTIFICATIONS_QUIET, CHANNEL_NOTIFICATIONS_QUIET_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channelNotificationsQuiet.setDescription(CHANNEL_NOTIFICATIONS_QUIET_DESC);
            channelNotificationsQuiet.setSound(null, null);
            channelNotificationsQuiet.enableVibration(false);
            notificationManager.createNotificationChannel(channelNotificationsQuiet);

            NotificationChannel channelUpdates = new NotificationChannel(GROUP_KEY_UPDATES, CHANNEL_UPDATES_NAME, NotificationManager.IMPORTANCE_HIGH);
            channelUpdates.setDescription(CHANNEL_UPDATES_DESC);
            notificationManager.createNotificationChannel(channelUpdates);
        }
    }

    public boolean shouldBeQuiet() {
        long now = Time.getNow().getInMillis();
        long start = app.appConfig.quietHoursStart;
        long end = app.appConfig.quietHoursEnd;
        if (start > end) {
            end += 1000 * 60 * 60 * 24;
            //Log.d(TAG, "Night passing");
        }
        if (start > now) {
            now += 1000 * 60 * 60 * 24;
            //Log.d(TAG, "Now is smaller");
        }
        //Log.d(TAG, "Start is "+start+", now is "+now+", end is "+end);
        return app.appConfig.quietHoursStart > 0 && now >= start && now <= end;
    }

    private int getNotificationDefaults() {
        return (shouldBeQuiet() ? 0 : Notification.DEFAULT_ALL);
    }
    private String getNotificationGroup() {
        return shouldBeQuiet() ? GROUP_KEY_NOTIFICATIONS_QUIET : GROUP_KEY_NOTIFICATIONS;
    }
    private int getNotificationPriority() {
        return shouldBeQuiet() ? PRIORITY_DEFAULT : PRIORITY_MAX;
    }

    /*    _____        _           _____      _
         |  __ \      | |         / ____|    | |
         | |  | | __ _| |_ __ _  | |  __  ___| |_
         | |  | |/ _` | __/ _` | | | |_ |/ _ \ __|
         | |__| | (_| | || (_| | | |__| |  __/ |_
         |_____/ \__,_|\__\__,_|  \_____|\___|\_*/
    public Notification notificationGetDataShow(int maxProgress) {
        Intent notificationIntent = new Intent(app.getContext(), SyncService.class);
        notificationIntent.setAction(ACTION_CANCEL);
        PendingIntent pendingIntent = PendingIntent.getService(app.getContext(), 0,
                notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        getDataNotificationBuilder = new NotificationCompat.Builder(app, GROUP_KEY_GET_DATA)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setColor(notificationColor)
                .setContentTitle(app.getString(R.string.notification_get_data_title))
                .setContentText(app.getString(R.string.notification_get_data_text))
                .addAction(R.drawable.ic_notification, app.getString(R.string.notification_get_data_cancel), pendingIntent)
                //.setGroup(GROUP_KEY_GET_DATA)
                .setOngoing(true)
                .setProgress(maxProgress, 0, false)
                .setTicker(app.getString(R.string.notification_get_data_summary))
                .setPriority(NotificationCompat.PRIORITY_LOW);
        return getDataNotificationBuilder.build();
    }

    public Notification notificationGetDataProgress(int progress, int maxProgress) {
        getDataNotificationBuilder.setProgress(maxProgress, progress, false);
        return getDataNotificationBuilder.build();
    }

    public Notification notificationGetDataAction(int stringResId) {
        getDataNotificationBuilder.setContentTitle(app.getString(R.string.sync_action_format, app.getString(stringResId)));
        return getDataNotificationBuilder.build();
    }

    public Notification notificationGetDataProfile(String profileName) {
        getDataNotificationBuilder.setContentText(profileName);
        return getDataNotificationBuilder.build();
    }

    public Notification notificationGetDataError(String profileName, String error, int failedProfileId) {
        Intent notificationIntent = new Intent(app.getContext(), Notifier.GetDataRetryService.class);
        notificationIntent.putExtra("failedProfileId", failedProfileId);
        PendingIntent pendingIntent = PendingIntent.getService(app.getContext(), 0,
                notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        getDataNotificationBuilder.mActions.clear();
        /*try {
            //Use reflection clean up old actions
            Field f = getDataNotificationBuilder.getClass().getDeclaredField("mActions");
            f.setAccessible(true);
            f.set(getDataNotificationBuilder, new ArrayList<NotificationCompat.Action>());
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        getDataNotificationBuilder.setProgress(0, 0, false)
                .setTicker(app.getString(R.string.notification_get_data_error_summary))
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .addAction(R.drawable.ic_notification, app.getString(R.string.notification_get_data_once_again), pendingIntent)
                .setContentTitle(app.getString(R.string.notification_get_data_error_title, profileName))
                .setContentText(error)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(error))
                .setOngoing(false);
        return getDataNotificationBuilder.build();
    }

    public void notificationPost(int id, Notification notification) {
        notificationManager.notify(id, notification);
    }

    public void notificationCancel(int id) {
        notificationManager.cancel(id);
    }

    //public void notificationGetDataHide() {
     //   notificationManager.cancel(ID_GET_DATA);
   // }

    public static class GetDataRetryService extends IntentService {
        private static final String TAG = "Notifier/GetDataRetry";

        public GetDataRetryService() {
            super(Notifier.GetDataRetryService.class.getSimpleName());
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            SyncJob.run((App) getApplication(), intent.getExtras().getInt("failedProfileId", -1), -1);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert notificationManager != null;
            notificationManager.cancel(ID_GET_DATA_ERROR);
        }
    }

    /*    _   _       _   _  __ _           _   _
         | \ | |     | | (_)/ _(_)         | | (_)
         |  \| | ___ | |_ _| |_ _  ___ __ _| |_ _  ___  _ __
         | . ` |/ _ \| __| |  _| |/ __/ _` | __| |/ _ \| '_ \
         | |\  | (_) | |_| | | | | (_| (_| | |_| | (_) | | | |
         |_| \_|\___/ \__|_|_| |_|\___\__,_|\__|_|\___/|_| |*/
    public void add(pl.szczodrzynski.edziennik.models.Notification notification) {
        app.appConfig.notifications.add(notification);
    }

    public void postAll(ProfileFull profile) {
        Collections.sort(app.appConfig.notifications, (o1, o2) -> (o2.addedDate - o1.addedDate > 0) ? 1 : (o2.addedDate - o1.addedDate < 0) ? -1 : 0);
        if (profile != null && !profile.getSyncNotifications())
            return;

        if (app.appConfig.notifications.size() > 40) {
            app.appConfig.notifications.subList(40, app.appConfig.notifications.size() - 1).clear();
        }

        int unreadCount = 0;
        List<pl.szczodrzynski.edziennik.models.Notification> notificationList = new ArrayList<>();
        for (pl.szczodrzynski.edziennik.models.Notification notification: app.appConfig.notifications) {
            if (!notification.notified) {
                notification.seen = false;
                notification.notified = true;
                unreadCount++;
                if (notificationList.size() < 10) {
                    notificationList.add(notification);
                }
            }
            else {
                notification.seen = true;
            }
        }

        for (pl.szczodrzynski.edziennik.models.Notification notification: notificationList) {
            Intent intent = new Intent(app, MainActivity.class);
            notification.fillIntent(intent);
            PendingIntent pendingIntent = PendingIntent.getActivity(app, notification.id, intent, 0);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(app, getNotificationGroup())
                    // title, text, type, date
                    .setContentTitle(notification.title)
                    .setContentText(notification.text)
                    .setSubText(pl.szczodrzynski.edziennik.models.Notification.stringType(app, notification.type))
                    .setWhen(notification.addedDate)
                    .setTicker(app.getString(R.string.notification_ticker_format, pl.szczodrzynski.edziennik.models.Notification.stringType(app, notification.type)))
                    // icon, color, lights, priority
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(notificationColor)
                    .setLights(0xFF00FFFF, 2000, 2000)
                    .setPriority(getNotificationPriority())
                    // channel, group, style
                    .setChannelId(getNotificationGroup())
                    .setGroup(getNotificationGroup())
                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(notification.text))
                    // intent, auto cancel
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);
            if (!shouldBeQuiet()) {
                notificationBuilder.setDefaults(getNotificationDefaults());
            }
            notificationManager.notify(notification.id, notificationBuilder.build());
        }

        if (notificationList.size() > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent intent = new Intent(app, MainActivity.class);
            intent.setAction("android.intent.action.MAIN");
            intent.putExtra("fragmentId", MainActivity.DRAWER_ITEM_NOTIFICATIONS);
            PendingIntent pendingIntent = PendingIntent.getActivity(app, ID_NOTIFICATIONS,
                    intent, 0);

            NotificationCompat.Builder groupBuilder =
                    new NotificationCompat.Builder(app, getNotificationGroup())
                            .setSmallIcon(R.drawable.ic_notification)
                            .setColor(notificationColor)
                            .setContentTitle(app.getString(R.string.notification_new_notification_title_format, unreadCount))
                            .setGroupSummary(true)
                            .setAutoCancel(true)
                            .setChannelId(getNotificationGroup())
                            .setGroup(getNotificationGroup())
                            .setLights(0xFF00FFFF, 2000, 2000)
                            .setPriority(getNotificationPriority())
                            .setContentIntent(pendingIntent)
                            .setStyle(new NotificationCompat.BigTextStyle());
            if (!shouldBeQuiet()) {
                groupBuilder.setDefaults(getNotificationDefaults());
            }
            notificationManager.notify(ID_NOTIFICATIONS, groupBuilder.build());
        }
    }

    /*    _    _           _       _
         | |  | |         | |     | |
         | |  | |_ __   __| | __ _| |_ ___  ___
         | |  | | '_ \ / _` |/ _` | __/ _ \/ __|
         | |__| | |_) | (_| | (_| | ||  __/\__ \
          \____/| .__/ \__,_|\__,_|\__\___||___/
                | |
                |*/
    public void notificationUpdatesShow(String updateVersion, String updateUrl, String updateFilename) {
        if (!app.appConfig.notifyAboutUpdates)
            return;
        Intent notificationIntent = new Intent(app.getContext(), BootReceiver.NotificationActionService.class)
                .putExtra("update_version", updateVersion)
                .putExtra("update_url", updateUrl)
                .putExtra("update_filename", updateFilename);

        PendingIntent pendingIntent = PendingIntent.getService(app.getContext(), 0,
                notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(app, GROUP_KEY_UPDATES)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setColor(notificationColor)
                .setContentTitle(app.getString(R.string.notification_updates_title))
                .setContentText(app.getString(R.string.notification_updates_text, updateVersion))
                .setLights(0xFF00FFFF, 2000, 2000)
                .setContentIntent(pendingIntent)
                .setTicker(app.getString(R.string.notification_updates_summary))
                .setPriority(PRIORITY_MAX)
                .setAutoCancel(true);
        if (!shouldBeQuiet()) {
            notificationBuilder.setDefaults(getNotificationDefaults());
        }
        notificationManager.notify(ID_UPDATES, notificationBuilder.build());
    }

    public void notificationUpdatesHide() {
        if (!app.appConfig.notifyAboutUpdates)
            return;
        notificationManager.cancel(ID_UPDATES);
    }

    public void dump() {
        for (pl.szczodrzynski.edziennik.models.Notification notification: app.appConfig.notifications) {
            Log.d(TAG, "Profile"+notification.profileId+" Notification from "+ Date.fromMillis(notification.addedDate).getFormattedString()+" "+ Time.fromMillis(notification.addedDate).getStringHMS()+" - "+notification.text);
        }
    }
}
