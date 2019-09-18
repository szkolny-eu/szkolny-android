package pl.szczodrzynski.edziennik.receivers;

import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import androidx.core.content.FileProvider;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.network.ServerRequest;
import pl.szczodrzynski.edziennik.sync.SyncJob;
import pl.szczodrzynski.edziennik.utils.Utils;

import static android.content.Context.DOWNLOAD_SERVICE;
import static pl.szczodrzynski.edziennik.App.APP_URL;
import static pl.szczodrzynski.edziennik.App.UPDATES_ON_PLAY_STORE;

public class BootReceiver extends BroadcastReceiver {

    private static final int NO_INTERNET_RETRY_TIMEOUT = 60;
    private static boolean alarmSet = false;
    public static long update_download_id;
    public static String update_url;
    public static String update_filename;
    private static final String TAG = "receivers.BootReceiver";
    private App app;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        app = (App)context.getApplicationContext();
        if (intent.getBooleanExtra("ExecutedByAlarm", false))
        {
            alarmSet = false;
        }
        if (intent.getAction() != null && intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        {
            long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if(referenceId == update_download_id) {
                Intent downloadIntent;
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (!app.permissionChecker.canRequestApkInstall()) {
                            app.permissionChecker.requestApkInstall();
                            return;
                        }
                    }
                    File fileLocation = new File(app.getContext().getExternalFilesDir(null), update_filename);
                    Uri apkUri = FileProvider.getUriForFile(app.getContext(), app.getContext().getApplicationContext().getPackageName() + ".provider", fileLocation);

                    downloadIntent = new Intent(Intent.ACTION_VIEW);
                    downloadIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    downloadIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    downloadIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");

                    List<ResolveInfo> resInfoList = app.getContext().getPackageManager().queryIntentActivities(downloadIntent, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        app.getContext().grantUriPermission(packageName, apkUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }

                } else {
                    File fileLocation = new File(app.getContext().getExternalFilesDir(null), update_filename);
                    downloadIntent = new Intent(Intent.ACTION_VIEW);
                    downloadIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    downloadIntent.setDataAndType(Uri.fromFile(fileLocation), "application/vnd.android.package-archive");
                }
                app.getContext().startActivity(downloadIntent);
            }
        }
        else
        {
            SyncJob.schedule(app);
            if (app.networkUtils.isOnline())
            {
                checkUpdate(context, intent);
            }
            else
            {
                //Toast.makeText(context, "No internet, retrying in "+NO_INTERNET_RETRY_TIMEOUT+" seconds", Toast.LENGTH_SHORT).show();
                scheduleUpdateCheck(context, NO_INTERNET_RETRY_TIMEOUT);
            }
        }
    }

    private boolean scheduleUpdateCheck(Context context, int secondsLater)
    {
        Intent alarmIntent = new Intent(context, BootReceiver.class);
        alarmIntent.putExtra("ExecutedByAlarm", true);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(context, 234324243, alarmIntent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (secondsLater * 1000), alarmPendingIntent);
            return true;
        }
        return false;
    }

    private void checkUpdate(final Context context, final Intent intent)
    {
        if (!alarmSet) {
            //Toast.makeText(context, "Update scheduled for 48 hours since now", Toast.LENGTH_SHORT).show();
            alarmSet = scheduleUpdateCheck(context, 48 * 60 * 60);
        }

        //app.networkUtils.setSelfSignedSSL(app.getContext(), null);

        new ServerRequest(app, app.requestScheme + APP_URL + "main.php?get_update", "BootReceiver/UPD")
                .run(((e, result) -> {
                    if (result != null) {
                        if (result.get("update_available").getAsBoolean()) {
                            String updateVersion = result.get("update_version").getAsString();
                            String updateUrl = result.get("update_url").getAsString();
                            String updateFilename = result.get("update_filename").getAsString();
                            boolean updateMandatory = result.get("update_mandatory").getAsBoolean();

                            if (app.appConfig.updateVersion == null || !app.appConfig.updateVersion.equals(updateVersion)) {
                                app.appConfig.updateVersion = updateVersion;
                                app.appConfig.updateUrl = updateUrl;
                                app.appConfig.updateFilename = updateFilename;
                                app.appConfig.updateMandatory = updateMandatory;
                                app.saveConfig();
                            }
                            if (!UPDATES_ON_PLAY_STORE || intent.getBooleanExtra("UserChecked", false)) {
                                app.notifier.notificationUpdatesShow(
                                        updateVersion,
                                        updateUrl,
                                        updateFilename);
                            }
                        } else {
                            if (app.appConfig.updateVersion == null || !app.appConfig.updateVersion.equals("")) {
                                app.appConfig.updateVersion = "";
                                app.appConfig.updateMandatory = false;
                                app.saveConfig();
                            }
                            app.notifier.notificationUpdatesHide();

                            if (intent.getBooleanExtra("UserChecked", false)) {
                                Toast.makeText(context, context.getString(R.string.notification_no_update), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                    else
                    {
                        //Toast.makeText(context, "Server returned nothing, retrying in "+NO_INTERNET_RETRY_TIMEOUT+" seconds", Toast.LENGTH_SHORT).show();
                        scheduleUpdateCheck(context, NO_INTERNET_RETRY_TIMEOUT);
                    }
                }));
            /*Ion.with(app.getContext())
                    .load(app.requestScheme + APP_URL + "main.php?get_update")
                    .setBodyParameter("username", (app.profile.autoRegistrationAllowed ? app.profile.registrationUsername : app.appConfig.deviceId))
                    .setBodyParameter("app_version_build_type", BuildConfig.BUILD_TYPE)
                    .setBodyParameter("app_version_code", Integer.toString(BuildConfig.VERSION_CODE))
                    .setBodyParameter("app_version", BuildConfig.VERSION_NAME + " " + BuildConfig.BUILD_TYPE + " (" + BuildConfig.VERSION_CODE + ")")
                    .setBodyParameter("device_id", Settings.Secure.getString(app.getContext().getContentResolver(), Settings.Secure.ANDROID_ID))
                    .setBodyParameter("device_model", Build.MANUFACTURER+" "+Build.MODEL)
                    .setBodyParameter("device_os_version", Build.VERSION.RELEASE)
                    .setBodyParameter("fcm_token", app.appConfig.fcmToken)
                    .asJsonObject()
                    .setCallback((e, result) -> {
                        // do stuff with the result or error
                        if (result != null) {
                            if (result.get("update_available").getAsBoolean()) {
                                String updateVersion = result.get("update_version").getAsString();
                                String updateUrl = result.get("update_url").getAsString();
                                String updateFilename = result.get("update_filename").getAsString();

                                if (app.appConfig.updateVersion == null || !app.appConfig.updateVersion.equals(updateVersion)) {
                                    app.appConfig.updateVersion = updateVersion;
                                    app.appConfig.updateUrl = updateUrl;
                                    app.appConfig.updateFilename = updateFilename;
                                    app.saveConfig();
                                }
                                app.notifier.notificationUpdatesShow(
                                        updateVersion,
                                        updateUrl,
                                        updateFilename);
                            } else {
                                if (app.appConfig.updateVersion == null || !app.appConfig.updateVersion.equals("")) {
                                    app.appConfig.updateVersion = "";
                                    app.saveConfig();
                                }
                                app.notifier.notificationUpdatesHide();

                                if (intent.getBooleanExtra("UserChecked", false)) {
                                    Toast.makeText(context, context.getString(R.string.notification_no_update), Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                        else
                        {
                            //Toast.makeText(context, "Server returned nothing, retrying in "+NO_INTERNET_RETRY_TIMEOUT+" seconds", Toast.LENGTH_SHORT).show();
                            scheduleUpdateCheck(context, NO_INTERNET_RETRY_TIMEOUT);
                        }
                    });*/
    }

    private static DownloadManager downloadManager;

    public static long downloadFile(Context context) {
        Toast.makeText(context, R.string.downloading, Toast.LENGTH_SHORT).show();
        File dir = new File(context.getExternalFilesDir(null)+""/*, update_filename*/);
        /*if (existingFile.exists())
        {
            existingFile.delete();
        }*/
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++)
            {
                new File(dir, children[i]).delete();
            }
        }

        Uri uri = Uri.parse(update_url);
        long downloadReference;
        // Create request for android download manager
        downloadManager = (DownloadManager)context.getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        //Setting title of request
        request.setTitle(context.getString(R.string.app_name));
        //Setting description of request
        request.setDescription(context.getString(R.string.notification_downloading_update));
        //Set the local destination for the downloaded file to a path within the application's external files directory
        try {
            request.setDestinationInExternalFilesDir(context, null, update_filename);
        }
        catch (java.lang.IllegalStateException e)
        {
            e.printStackTrace();
            Toast.makeText(context, "Failed to get external storage files directory", Toast.LENGTH_SHORT).show();
        }
        //Enqueue download and save into referenceId
        downloadReference = downloadManager.enqueue(request);
        return downloadReference;
    }

    public static class NotificationActionService extends IntentService {
        private static final String TAG = "BootReceiver/NAS";

        public NotificationActionService() {
            super(NotificationActionService.class.getSimpleName());
            //IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            //registerReceiver(downloadReceiver, filter);
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            if (UPDATES_ON_PLAY_STORE) {
                Utils.openGooglePlay(this, "pl.szczodrzynski.edziennik");
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                App app = (App)getApplication();
                if (!app.permissionChecker.canRequestApkInstall()) {
                    app.permissionChecker.requestApkInstall();
                    app.notifier.notificationUpdatesShow(
                            intent.getStringExtra("update_version"),
                            intent.getStringExtra("update_url"),
                            intent.getStringExtra("update_filename"));
                    return;
                }
            }
            update_url = intent.getStringExtra("update_url");
            update_filename = intent.getStringExtra("update_filename");
            //update_filename = "Edziennik_update.apk";
            update_download_id = downloadFile(this);
        }
    }
}
