package pl.szczodrzynski.edziennik.sync;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.BuildConfig;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.datamodels.Event;
import pl.szczodrzynski.edziennik.datamodels.EventFull;
import pl.szczodrzynski.edziennik.datamodels.EventType;
import pl.szczodrzynski.edziennik.datamodels.FeedbackMessage;
import pl.szczodrzynski.edziennik.datamodels.ProfileFull;
import pl.szczodrzynski.edziennik.datamodels.Team;
import pl.szczodrzynski.edziennik.ui.modules.base.DebugFragment;
import pl.szczodrzynski.edziennik.utils.models.Notification;
import pl.szczodrzynski.edziennik.network.ServerRequest;

import static pl.szczodrzynski.edziennik.App.APP_URL;
import static pl.szczodrzynski.edziennik.datamodels.Event.TYPE_HOMEWORK;
import static pl.szczodrzynski.edziennik.datamodels.LoginStore.LOGIN_TYPE_MOBIDZIENNIK;
import static pl.szczodrzynski.edziennik.utils.Utils.d;
import static pl.szczodrzynski.edziennik.utils.Utils.strToInt;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FirebaseMessaging";

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);

        Log.d(TAG, "New token: "+s);
        App app = (App)getApplicationContext();
        if (app.appConfig.fcmToken == null || !app.appConfig.fcmToken.equals(s)) {
            app.appConfig.fcmToken = s;
            app.saveConfig();
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        App app = ((App) getApplicationContext());
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ

        String from = remoteMessage.getFrom();
        if (from != null) {
            switch (from) {
                case "640759989760":
                    app.debugLog("Firebase got push from App "+remoteMessage.getData().toString());
                    //processAppPush
                    processAppPush(app, remoteMessage);
                    break;
                case "1029629079999":
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
        }
    }

    private void processMobidziennikPush(App app, RemoteMessage remoteMessage) {
        SharedPreferences sharedPreferences = getSharedPreferences("pushtest_mobidziennik", Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(Long.toString(System.currentTimeMillis()), remoteMessage.getData().toString()+"\n"+remoteMessage.toString()+"\n"+remoteMessage.getMessageType()).apply();
        String studentIdStr = remoteMessage.getData().get("id_ucznia");
        if (studentIdStr != null) {
            int studentId = strToInt(studentIdStr);
            AsyncTask.execute(() -> {
                List<ProfileFull> profileList = app.db.profileDao().getAllFullNow();

                ProfileFull profile = null;

                for (ProfileFull profileFull: profileList) {
                    if (profileFull.getLoginStoreType() == LOGIN_TYPE_MOBIDZIENNIK
                            && studentId == profileFull.getStudentData("studentId", -1)) {
                        profile = profileFull;
                        break;
                    }
                }

                if (profile != null) {
                    if (remoteMessage.getData().get("id_wiadomosci") != null) {
                        /*app.notifier.add(new Notification(app.getContext(), remoteMessage.getData().get("message"))
                                .withProfileData(profile.id, profile.name)
                                .withTitle(remoteMessage.getData().get("title"))
                                .withType(Notification.TYPE_NEW_MESSAGE)
                                .withFragmentRedirect(MainActivity.DRAWER_ITEM_MESSAGES)
                        );
                        app.notifier.postAll(profile);
                        app.saveConfig("notifications");*/
                        d(TAG, "Syncing profile " + profile.getId());
                        SyncJob.run(app, -1, profile.getId());
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
                        SyncJob.run(app, -1, profile.getId());
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
        String type = remoteMessage.getData().get("type");
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

                        if (app.appConfig.updateVersion == null || !app.appConfig.updateVersion.equals(updateVersion)) {
                            app.appConfig.updateVersion = updateVersion;
                            app.appConfig.updateUrl = updateUrl;
                            app.appConfig.updateFilename = updateFilename;
                            app.appConfig.updateMandatory = updateMandatory;
                            app.saveConfig("updateVersion", "updateUrl", "updateFilename", "updateMandatory");
                        }
                        if (!remoteMessage.getData().containsKey("update_silent")) {
                            app.notifier.notificationUpdatesShow(
                                    updateVersion,
                                    updateUrl,
                                    updateFilename);
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
                            .withType(Notification.TYPE_SERVER_MESSAGE)
                            .withFragmentRedirect(MainActivity.DRAWER_ITEM_NOTIFICATIONS)
                    );
                    app.notifier.postAll(null);
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
                                app.appConfig.devModePassword = feedbackMessage.text.replace("devmode", "");
                                app.saveConfig("devModePassword");
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
                                    .withType(Notification.TYPE_FEEDBACK_MESSAGE)
                                    .withFragmentRedirect(MainActivity.TARGET_FEEDBACK)
                            );
                            app.notifier.postAll(null);
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
                            .withType(Notification.TYPE_FEEDBACK_MESSAGE)
                            .withFragmentRedirect(MainActivity.TARGET_FEEDBACK)
                    );
                    app.notifier.postAll(null);
                    app.saveConfig("notifications");
                    break;
                case "ping":
                    // just a ping
                    break;
            /*    ______               _                  _
                 |  ____|             | |                | |
                 | |____   _____ _ __ | |_   ______   ___| |__   __ _ _ __ ___
                 |  __\ \ / / _ \ '_ \| __| |______| / __| '_ \ / _` | '__/ _ \
                 | |___\ V /  __/ | | | |_           \__ \ | | | (_| | | |  __/
                 |______\_/ \___|_| |_|\__|          |___/_| |_|\__,_|_|  \__*/
                case "event":
                case "event_removed":
                    AsyncTask.execute(() -> {
                        String teamCode = remoteMessage.getData().get("team");
                        String teamUnshareCode = remoteMessage.getData().get("team_unshare");
                        while (teamCode != null || teamUnshareCode != null) {
                            d(TAG, "Got an event for teamCode " + teamCode + " and teamUnshareCode " + teamUnshareCode);
                            // get the target Profile by the corresponding teamCode
                            List<ProfileFull> profiles = app.db.profileDao().getByTeamCodeNowWithRegistration(teamCode == null ? teamUnshareCode : teamCode);
                            for (ProfileFull profile : profiles) {
                                d(TAG, "Matched profile " + profile.getName());
                                if (teamCode != null) {
                                    // SHARING
                                    JsonObject jEvent = new JsonParser().parse(remoteMessage.getData().get("data")).getAsJsonObject();
                                    d(TAG, "An event is there! " + jEvent.toString());
                                    // get the target Team from teamCode
                                    Team team = app.db.teamDao().getByCodeNow(profile.getId(), teamCode);
                                    if (team != null) {
                                        d(TAG, "The target team is " + team.name + ", ID " + team.id);
                                        // create the event from Json. Add the missing teamId and !!profileId!!
                                        Event event = app.gson.fromJson(jEvent.toString(), Event.class);
                                        if (jEvent.get("colorDefault") != null) {
                                            event.color = -1;
                                        }
                                        event.profileId = profile.getId();
                                        event.teamId = team.id;
                                        d(TAG, "Created the event! " + event);

                                        // TODO? i guess
                                        Event oldEvent = app.db.eventDao().getByIdNow(profile.getId(), event.id);
                                        if (event.sharedBy != null && event.sharedBy.equals(profile.getUsernameId())) {
                                            d(TAG, "Shared by self! Changing name");
                                            event.sharedBy = "self";
                                            event.sharedByName = profile.getStudentNameLong();
                                        }
                                        d(TAG, "Old event found? " + oldEvent);
                                        EventType eventType = app.db.eventTypeDao().getByIdNow(profile.getId(), event.type);
                                        app.notifier.add(new Notification(app.getContext(), app.getString((oldEvent == null ? R.string.notification_shared_event_format : R.string.notification_shared_event_modified_format), event.sharedByName, eventType == null ? "wydarzenie" : eventType.name, event.eventDate.getFormattedString(), event.topic))
                                                .withProfileData(profile.getId(), profile.getName())
                                                .withType(event.type == TYPE_HOMEWORK ? Notification.TYPE_NEW_SHARED_HOMEWORK : Notification.TYPE_NEW_SHARED_EVENT)
                                                .withFragmentRedirect(event.type == TYPE_HOMEWORK ? MainActivity.DRAWER_ITEM_HOMEWORK : MainActivity.DRAWER_ITEM_AGENDA)
                                                .withLongExtra("eventDate", event.eventDate.getValue())
                                        );
                                        d(TAG, "Finishing adding event " + event);
                                        app.db.eventDao().add(event);
                                        try {
                                            app.db.metadataDao().setBoth(profile.getId(), event, false, true, jEvent.get("addedDate").getAsLong());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                } else {
                                    // UNSHARING
                                    long eventId = Long.parseLong(remoteMessage.getData().get("remove_id"));
                                    EventFull oldEvent = app.db.eventDao().getByIdNow(profile.getId(), eventId);
                                    if (oldEvent != null) {
                                        app.notifier.add(new Notification(app.getContext(), app.getString(R.string.notification_shared_event_removed_format, oldEvent.sharedByName, oldEvent.typeName, oldEvent.eventDate.getFormattedString(), oldEvent.topic))
                                                .withProfileData(profile.getId(), profile.getName())
                                                .withType(oldEvent.type == TYPE_HOMEWORK ? Notification.TYPE_NEW_SHARED_HOMEWORK : Notification.TYPE_NEW_SHARED_EVENT)
                                                .withFragmentRedirect(oldEvent.type == TYPE_HOMEWORK ? MainActivity.DRAWER_ITEM_HOMEWORK : MainActivity.DRAWER_ITEM_AGENDA)
                                                .withLongExtra("eventDate", oldEvent.eventDate.getValue())
                                        );
                                        app.db.eventDao().remove(oldEvent);
                                    }
                                }
                            }
                            if (teamCode != null) {
                                teamCode = null;
                            } else {
                                teamUnshareCode = null;
                            }
                        }
                        app.notifier.postAll(null);
                        app.saveConfig();
                    });
                    break;
            }
        }
    }
}
