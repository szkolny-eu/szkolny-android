package pl.szczodrzynski.edziennik.sync;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.data.api.AppError;
import pl.szczodrzynski.edziennik.data.api.interfaces.SyncCallback;
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.ProfileFull;

import static pl.szczodrzynski.edziennik.Notifier.ID_GET_DATA;
import static pl.szczodrzynski.edziennik.Notifier.ID_GET_DATA_ERROR;
import static pl.szczodrzynski.edziennik.utils.Utils.d;

public class SyncService extends Service {
    public static final String TAG = "SyncService";
    private App app;
    public static boolean running = false;
    private List<Profile> profiles;
    public static final int PROFILE_MAX_PROGRESS = 110;
    private Profile profile = null;
    private int profileId = -1;
    public static int progress = 0;
    private int profileProgress = 0;
    public static int maxProgress;
    public static int firstProfileId = -1;
    public static int targetProfileId = -1;
    public static final String ACTION_CANCEL = "SyncService.ACTION_CANCEL";
    public static SyncCallback customCallback = null;
    private static final int PROFILE_TIMEOUT = 40000;

    private Handler timeoutHandler = new Handler();
    private Runnable timeoutRunnable = new Runnable() {
        public void run() {
            if (!running)
                return;
            error(AppError.stringErrorCode(app, AppError.CODE_TIMEOUT, ""), profile);
            finishDownload();
        }
    };

    public static void start(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, SyncService.class));
        } else {
            context.startService(new Intent(context, SyncService.class));
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        app = (App) getApplicationContext();
        app.debugLog("SyncService created");
        startForeground(ID_GET_DATA, app.notifier.notificationGetDataShow(0));
    }

    private void reNotify() {
        //app.notifier.notificationCancel(ID_GET_DATA_ERROR);
        app.notifier.notificationPost(ID_GET_DATA, app.notifier.notificationGetDataProgress(progress, maxProgress));
    }
    private void error(String error, Profile profile) {
        app.notifier.notificationPost(ID_GET_DATA_ERROR, app.notifier.notificationGetDataError(profile == null ? "" : profile.getName(), error, profile == null ? -1 : profile.getId()));
    }

    long millisStart;

    private void downloadData() {
        if (profiles.size() > 0) {
            profile = profiles.get(0);
            profileId = profile.getId();
            app.debugLog("SyncService downloading profileId="+profileId);
            profiles.remove(0);
            // mamy profileID wiec nie trzeba nic ładować. DAO musi używać profileId
            millisStart = System.currentTimeMillis();
            app.notifier.notificationPost(ID_GET_DATA, app.notifier.notificationGetDataProfile(profile.getName()));
            app.apiEdziennik.sync(app, SyncService.this, new SyncCallback() {
                @Override
                public void onLoginFirst(List<Profile> profileList, LoginStore loginStore) {
                    if (customCallback != null)
                        customCallback.onLoginFirst(profileList, loginStore);
                }

                @Override
                public void onSuccess(Context activityContext, ProfileFull profile) {
                    profileProgress++;
                    progress = profileProgress*PROFILE_MAX_PROGRESS;
                    updateTimeoutHandler();
                    reNotify();
                    app.debugLog("SyncService profileId="+profileId+" done in "+(System.currentTimeMillis() - millisStart)+"ms");
                    if (customCallback != null)
                        customCallback.onSuccess(activityContext, profile);
                    downloadData();
                }

                @Override
                public void onError(Context activityContext, AppError error) {
                    error(error.asReadableString(activityContext), profile);
                    if (customCallback != null)
                        customCallback.onError(activityContext, error);
                    if (error.errorCode == AppError.CODE_NO_INTERNET) {
                        finishDownload(CODE_NO_INTERNET); // use finishDownload instead of scheduleNext so it always changes the profile back to previousProfile
                    } else {
                        finishDownload(CODE_ERROR);
                    }
                }

                @Override
                public void onProgress(int progressStep) {
                    if (customCallback != null)
                        customCallback.onProgress(Math.min(progressStep, PROFILE_MAX_PROGRESS));
                    progress = Math.min(progress+progressStep, (profileProgress+1)*PROFILE_MAX_PROGRESS);
                    reNotify();
                }

                @Override
                public void onActionStarted(int stringResId) {
                    if (customCallback != null)
                        customCallback.onActionStarted(stringResId);
                    app.notifier.notificationPost(ID_GET_DATA, app.notifier.notificationGetDataAction(stringResId));
                }
            }, profileId);
        }
        else {
            finishDownload();
        }
    }

    private void updateTimeoutHandler() {
        timeoutHandler.removeCallbacks(timeoutRunnable);
        timeoutHandler.postDelayed(timeoutRunnable, PROFILE_TIMEOUT);
    }

    private static final int CODE_OK = 0;
    private static final int CODE_ERROR = 1;
    private static final int CODE_NO_INTERNET = 2;
    private void finishDownload() {
        finishDownload(CODE_OK);
    }
    private void finishDownload(int errorCode) {
        timeoutHandler.removeCallbacks(timeoutRunnable);

        app.apiEdziennik.notifyAndReload();

        app.debugLog("SyncService finishing with profileId="+profileId);
        if (errorCode == CODE_OK) {
            app.notifier.notificationCancel(ID_GET_DATA_ERROR);
        }
        SyncJob.update((App) getApplication(), errorCode == CODE_NO_INTERNET);
        stopSelf();
        if (customCallback != null && errorCode == CODE_OK) {
            customCallback.onSuccess(null, null);
        }
        customCallback = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        d(TAG, "Got an intent. Service is running "+running);
        if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_CANCEL)) {
            d(TAG, "ACTION_CANCEL intent");
            finishDownload();
            running = true;
        }

        if (running) {
            app.debugLog("SyncService already running; return");
            return Service.START_STICKY;
        }
        running = true;

        timeoutHandler.postDelayed(timeoutRunnable, PROFILE_TIMEOUT);

        profiles = app.db.profileDao().getProfilesForSyncNow();

        app.debugLog("SyncService source profileList="+ Arrays.toString(profiles.toArray())+". firstProfileId="+firstProfileId+", targetProfileId="+targetProfileId);

        if (profiles.size() == 0) {
            stopSelf();
            return Service.START_STICKY;
        }

        if (firstProfileId != -1) {
            d(TAG, "Profile list from first "+firstProfileId);
            Collection<Profile> toRemove = new ArrayList<>();
            for (Profile profile: profiles) {
                if (firstProfileId == profile.getId()) {
                    break;
                }
                toRemove.add(profile);
            }
            profiles.removeAll(toRemove);
            firstProfileId = -1;
        }
        if (targetProfileId != -1) {
            d(TAG, "Profile list only target "+targetProfileId);
            Profile targetProfile = null;
            for (Profile profile: profiles) {
                if (targetProfileId == profile.getId()) {
                    targetProfile = profile;
                    break;
                }
            }
            profiles.clear();
            if (targetProfile != null)
                profiles.add(targetProfile);
            targetProfileId = -1;
        }

        app.debugLog("SyncService target profileList="+ Arrays.toString(profiles.toArray()));

        progress = 0;
        profileProgress = 0;
        maxProgress = profiles.size()*PROFILE_MAX_PROGRESS;

        app.notifier.notificationPost(ID_GET_DATA, app.notifier.notificationGetDataShow(maxProgress));

        if (!app.networkUtils.isOnline()) {
            app.debugLog("SyncService no internet; update,stop");
            error(AppError.stringErrorCode(app, AppError.CODE_NO_INTERNET, ""), profiles.size() >= 1 ? profiles.get(0) : null);
            SyncJob.update((App) getApplication(), true);
            if (customCallback != null) {
                customCallback.onError(getApplicationContext(), new AppError(TAG, 241, AppError.CODE_NO_INTERNET, null, (Throwable) null));
            }
            stopSelf();
            return Service.START_STICKY;
        }

        downloadData();

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        app.debugLog("SyncService destroyed");
        running = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
