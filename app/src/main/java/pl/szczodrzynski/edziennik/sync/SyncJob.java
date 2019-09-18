package pl.szczodrzynski.edziennik.sync;

import android.content.Context;

import androidx.annotation.NonNull;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

import java.util.Set;

import pl.szczodrzynski.edziennik.App;

import static pl.szczodrzynski.edziennik.utils.Utils.d;

public class SyncJob extends Job {

    public static final String TAG = "SyncJob";
    private static int retryCount = 0;

    public static void schedule(App app) {
        schedule(app, false);
    }

    /**
     * Forces a SyncJob to be scheduled. Does nothing if it's already scheduled.
     */
    public static void schedule(App app, boolean internetError) {
        int count = count();
        if (count > 0) {
            d(TAG, "Job is already scheduled");
            return;
        }
        if (app.appConfig.registerSyncEnabled) {
            //Toast.makeText(app, "Scheduling an alarm", Toast.LENGTH_SHORT).show();
            long timeout = 1000 * app.appConfig.registerSyncInterval;
            if (internetError) {
                retryCount++;
                timeout = 1000 * (30*retryCount*retryCount); // 30sec, 2min, 4min 30sec, 8min, 12min 30sec, ...
                timeout = Math.min(timeout, 30 * 60 * 1000); // max 30min between retries
                if (timeout > 1000 * app.appConfig.registerSyncInterval) {
                    timeout = 1000 * app.appConfig.registerSyncInterval;
                }
            }

            new JobRequest.Builder(SyncJob.TAG)
                    .setExecutionWindow((long) (timeout - (0.1 * timeout)), (long) (timeout + (0.1 * timeout)))
                    //.setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .build()
                    .schedule();
            if (!internetError) {
                retryCount = 0;
            }
        }
        else {
            clear();
        }
    }
    public static int count() {
        Set<JobRequest> jobRequests = JobManager.instance().getAllJobRequestsForTag(SyncJob.TAG);
        if (App.devMode) {
            for (JobRequest jobRequest: jobRequests) {
                d(TAG, "JOBLIST: Found at "+jobRequest.getStartMs()+" - "+jobRequest.getEndMs());
            }
        }
        return jobRequests.size();
    }
    public static void clear() {
        JobManager.instance().cancelAllForTag(SyncJob.TAG);
    }

    /**
     * Deletes every scheduled SyncJob and then reschedules it.
     */
    public static void update(App app) {
        update(app, false);
    }
    public static void update(App app, boolean internetError) {
        app.debugLogAsync("SyncJob update internetError="+internetError);
        clear();
        schedule(app, internetError);
    }
    public static void run(App app) {
        app.debugLogAsync("SyncJob run with no IDs");
        clear();
        runService(app, app.getContext(), -1, -1);
        /*new JobRequest.Builder(SyncJob.TAG)
                .startNow()
                .build()
                .schedule();*/
    }

    public static void run(App app, int firstProfileId, int targetProfileId) {
        app.debugLogAsync("SyncJob run with firstProfileId="+firstProfileId+", targetProfileId="+targetProfileId);
        clear();
        runService(app, app.getContext(), firstProfileId, targetProfileId);
       /* PersistableBundleCompat extras = new PersistableBundleCompat();
        extras.putInt("firstProfileId", firstProfileId);
        extras.putInt("targetProfileId", targetProfileId);
        new JobRequest.Builder(SyncJob.TAG)
                .startNow()
                .setExtras(extras)
                .build()
                .schedule();*/
    }

    @Override
    @NonNull
    protected Result onRunJob(@NonNull Params params) {
        //Log.d(TAG, "Job is running!");

        App app = (App) getContext().getApplicationContext();

        int firstProfileId = params.getExtras().getInt("firstProfileId", -1);
        int targetProfileId = params.getExtras().getInt("targetProfileId", -1);

        runService(app, getContext(), firstProfileId, targetProfileId);

        return Result.SUCCESS;
    }

    private static void runService(App app, Context context, int firstProfileId, int targetProfileId) {

        SyncService.firstProfileId = firstProfileId;
        SyncService.targetProfileId = targetProfileId;

        app.debugLog("SyncJob runService with firstProfileId="+firstProfileId+", targetProfileId="+targetProfileId);

        boolean connected;

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (app.appConfig.registerSyncOnlyWifi) {
            NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            connected = mWifi != null && mWifi.isConnected();
        }
        else {
            NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            connected = netInfo != null && netInfo.isConnected();
        }

        if (!connected) {
            app.debugLog("SyncJob cancelling: no internet");
            update(app, true);
        }
        else {
            SyncService.start(context);
        }
    }
}
