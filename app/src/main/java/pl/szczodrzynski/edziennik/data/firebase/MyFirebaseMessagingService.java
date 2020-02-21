/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-11.
 */

package pl.szczodrzynski.edziennik.data.firebase;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask;
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore;
import pl.szczodrzynski.edziennik.data.db.entity.Profile;

import static pl.szczodrzynski.edziennik.utils.Utils.d;
import static pl.szczodrzynski.edziennik.utils.Utils.strToInt;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FirebaseMessaging";

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);

       /* Log.d(TAG, "New token: "+s);
        App app = (App)getApplicationContext();
        if (app.config.getSync().getTokenApp() == null || !app.config.getSync().getTokenApp().equals(s)) {
            app.config.getSync().setTokenApp(s);
        }*/
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        /*App app = ((App) getApplicationContext());
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ

        String from = remoteMessage.getFrom();
        if (from != null) {
            switch (from) {
                case "640759989760":
                    app.debugLog("Firebase got push from App "+remoteMessage.getData().toString());
                    //processAppPush
                    processAppPush(app, remoteMessage);
                    break;
                case "747285019373":
                    app.debugLog("Firebase got push from Mobidziennik "+remoteMessage.getData().toString());
                    processMobidziennikPush(app, remoteMessage);
                    break;
                case "513056078587":
                    app.debugLog("Firebase got push from Librus "+remoteMessage.getData().toString());
                    processLibrusPush(app, remoteMessage);
                    break;
                case "987828170337":
                    app.debugLog("Firebase got push from Vulcan "+remoteMessage.getData().toString());
                    processVulcanPush(app, remoteMessage);
                    break;
            }
        }*/
    }

    private void processMobidziennikPush(App app, RemoteMessage remoteMessage) {
        SharedPreferences sharedPreferences = getSharedPreferences("pushtest_mobidziennik", Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(Long.toString(System.currentTimeMillis()), remoteMessage.getData().toString()+"\n"+remoteMessage.toString()+"\n"+remoteMessage.getMessageType()).apply();
        String studentIdStr = remoteMessage.getData().get("id_ucznia");
        if (studentIdStr != null) {
            int studentId = strToInt(studentIdStr);
            AsyncTask.execute(() -> {
                List<Profile> profileList = app.db.profileDao().getAllNow();

                Profile profile = null;

                for (Profile profileFull: profileList) {
                    if (profileFull.getLoginStoreType() == LoginStore.LOGIN_TYPE_MOBIDZIENNIK
                            && studentId == profileFull.getStudentData("studentId", -1)) {
                        profile = profileFull;
                        break;
                    }
                }

                if (profile != null) {
                    if (remoteMessage.getData().get("id_wiadomosci") != null) {

                        d(TAG, "Syncing profile " + profile.getId());
                        EdziennikTask.Companion.syncProfile(profile.getId(), null, null, null).enqueue(app);
                    } else {
                        /*app.notifier.add(new Notification(app.getContext(), remoteMessage.getData().get("message"))
                                .withProfileData(profile.id, profile.name)
                                .withTitle(remoteMessage.getData().get("title"))
                                .withType(Notification.TYPE_SERVER_MESSAGE)
                                .withFragmentRedirect(MainActivity.DRAWER_ITEM_HOME)
                        );
                        app.notifier.postAll(profile);
                        app.saveConfig("notifications");*/
                        d(TAG, "Syncing profile " + profile.getId());
                        EdziennikTask.Companion.syncProfile(profile.getId(), null, null, null).enqueue(app);
                    }
                }
            });
        }
    }

    private void processLibrusPush(App app, RemoteMessage remoteMessage) {
        SharedPreferences sharedPreferences = getSharedPreferences("pushtest_librus", Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(Long.toString(System.currentTimeMillis()), remoteMessage.getData().toString()+"\n"+remoteMessage.toString()+"\n"+remoteMessage.getMessageType()).apply();
    }

    private void processVulcanPush(App app, RemoteMessage remoteMessage) {
        SharedPreferences sharedPreferences = getSharedPreferences("pushtest_vulcan", Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(Long.toString(System.currentTimeMillis()), remoteMessage.getData().toString()+"\n"+remoteMessage.toString()+"\n"+remoteMessage.getMessageType()).apply();
    }

    private void processAppPush(App app, RemoteMessage remoteMessage) {
        // Check if message contains a data payload.
        /*String type = remoteMessage.getData().get("type");
        if (remoteMessage.getData().size() > 0
            && type != null) {
            //Log.d(TAG, "Message data payload: " + remoteMessage.sync());
            switch (type) {
                case "app_update":
                    int versionCode = Integer.parseInt(remoteMessage.getData().get("update_version_code"));
                    if (BuildConfig.VERSION_CODE < versionCode) {
                        String updateVersion = remoteMessage.getData().get("update_version");
                        String updateUrl = remoteMessage.getData().get("update_url");
                        String updateFilename = remoteMessage.getData().get("update_filename");
                        boolean updateMandatory = Boolean.parseBoolean(remoteMessage.getData().get("update_mandatory"));
                        boolean updateDirect = Boolean.parseBoolean(remoteMessage.getData().get("update_direct"));

                        if (app.appConfig.updateVersion == null || !app.appConfig.updateVersion.equals(updateVersion)) {
                            app.appConfig.updateVersion = updateVersion;
                            app.appConfig.updateUrl = updateUrl;
                            app.appConfig.updateFilename = updateFilename;
                            app.appConfig.updateMandatory = updateMandatory;
                            app.appConfig.updateDirect = updateDirect;
                            app.saveConfig("updateVersion", "updateUrl", "updateFilename", "updateMandatory");
                        }
                        if (!remoteMessage.getData().containsKey("update_silent")) {
                            app.notifier.notificationUpdatesShow(
                                    updateVersion,
                                    updateUrl,
                                    updateFilename,
                                    updateDirect);
                        }
                    } else {
                        if (app.appConfig.updateVersion == null || !app.appConfig.updateVersion.equals("")) {
                            app.appConfig.updateVersion = "";
                            app.appConfig.updateMandatory = false;
                            app.saveConfig("updateVersion", "updateMandatory");
                        }
                        app.notifier.notificationUpdatesHide();
                    }
                    break;
                case "message":
                    app.notifier.add(new Notification(app.getContext(), remoteMessage.getData().get("message"))
                            .withTitle(remoteMessage.getData().get("title"))
                            .withType(pl.szczodrzynski.edziennik.data.db.entity.Notification.TYPE_SERVER_MESSAGE)
                            .withFragmentRedirect(MainActivity.DRAWER_ITEM_NOTIFICATIONS)
                    );
                    app.notifier.postAll();
                    app.saveConfig("notifications");
                    break;
                case "feedback_message_from_dev":
                    AsyncTask.execute(() -> {
                        FeedbackMessage feedbackMessage = new FeedbackMessage(true, remoteMessage.getData().get("message"));
                        if (feedbackMessage.text.startsWith("test")) {
                            // todo
                        }
                        else {
                            feedbackMessage.sentTime = Long.parseLong(remoteMessage.getData().get("sent_time"));
                            if (feedbackMessage.text.startsWith("devmode")) {
                                app.config.setDevModePassword(feedbackMessage.text.replace("devmode", ""));
                                app.checkDevModePassword();
                                feedbackMessage.text = "devmode "+(App.devMode ? "allowed" : "disallowed");
                            }
                            Intent intent = new Intent("pl.szczodrzynski.edziennik.ui.modules.base.FeedbackActivity");
                            intent.putExtra("type", "user_chat");
                            intent.putExtra("message", app.gson.toJson(feedbackMessage));
                            app.sendBroadcast(intent);
                            app.db.feedbackMessageDao().add(feedbackMessage);

                            app.notifier.add(new Notification(app.getContext(), feedbackMessage.text)
                                    .withTitle(remoteMessage.getData().get("title"))
                                    .withType(pl.szczodrzynski.edziennik.data.db.entity.Notification.TYPE_FEEDBACK_MESSAGE)
                                    .withFragmentRedirect(MainActivity.TARGET_FEEDBACK)
                            );
                            app.notifier.postAll();
                            app.saveConfig("notifications");
                        }
                    });
                    break;
                case "feedback_message_from_user":
                    AsyncTask.execute(() -> {
                        FeedbackMessage feedbackMessage = new FeedbackMessage(true, remoteMessage.getData().get("message"));
                        feedbackMessage.fromUser = remoteMessage.getData().get("from_user");
                        feedbackMessage.fromUserName = remoteMessage.getData().get("from_user_name");
                        feedbackMessage.sentTime = Long.parseLong(remoteMessage.getData().get("sent_time"));
                        Intent intent = new Intent("pl.szczodrzynski.edziennik.ui.modules.base.FeedbackActivity");
                        intent.putExtra("type", "user_chat");
                        intent.putExtra("message", app.gson.toJson(feedbackMessage));
                        app.sendBroadcast(intent);
                        app.db.feedbackMessageDao().add(feedbackMessage);
                    });
                    app.notifier.add(new Notification(app.getContext(), remoteMessage.getData().get("message"))
                            .withTitle(remoteMessage.getData().get("title"))
                            .withType(pl.szczodrzynski.edziennik.data.db.entity.Notification.TYPE_FEEDBACK_MESSAGE)
                            .withFragmentRedirect(MainActivity.TARGET_FEEDBACK)
                    );
                    app.notifier.postAll();
                    app.saveConfig("notifications");
                    break;
                case "ping":
                    // just a ping
                    break
            }
        }*/
    }
}
