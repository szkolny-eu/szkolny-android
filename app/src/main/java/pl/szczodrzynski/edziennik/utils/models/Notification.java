package pl.szczodrzynski.edziennik.utils.models;

import android.content.Context;
import android.content.Intent;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Map;
import java.util.Random;

import pl.szczodrzynski.edziennik.R;

import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_AUTO_ARCHIVING;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_ERROR;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_FEEDBACK_MESSAGE;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_GENERAL;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_LUCKY_NUMBER;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_NEW_ANNOUNCEMENT;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_NEW_ATTENDANCE;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_NEW_EVENT;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_NEW_GRADE;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_NEW_HOMEWORK;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_NEW_MESSAGE;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_NEW_NOTICE;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_NEW_SHARED_EVENT;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_SERVER_MESSAGE;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_TIMETABLE_CHANGED;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_TIMETABLE_LESSON_CHANGE;
import static pl.szczodrzynski.edziennik.data.db.modules.notification.Notification.TYPE_UPDATE;

public class Notification {
    public int profileId;
    public String title;
    public boolean notified;
    public boolean seen;
    public int redirectFragmentId;
    public JsonObject extras;
    public int type;
    public String text;
    public long addedDate;
    public int id;

    public Notification(Context context, String text) {
        this.profileId = -1;
        this.title = context.getString(R.string.notification);
        this.type = TYPE_GENERAL;
        this.text = text;
        this.redirectFragmentId = -1;
        this.extras = new JsonObject();
        this.notified = false;
        this.seen = false;
        this.addedDate = System.currentTimeMillis();
        this.id = new Random().nextInt(Integer.MAX_VALUE);
    }

    public Notification withProfileData(int profileId, String profileName) {
        this.profileId = profileId;
        this.title = profileName;
        return this;
    }

    public Notification withProfileData(int profileId) {
        this.profileId = profileId;
        return this;
    }

    public Notification withTitle(String title) {
        this.title = title;
        return this;
    }

    public Notification withType(int type) {
        this.type = type;
        return this;
    }

    public Notification withFragmentRedirect(int redirectFragmentId) {
        this.redirectFragmentId = redirectFragmentId;
        return this;
    }

    public Notification withLongExtra(String key, long value) {
        this.extras.addProperty(key, value);
        return this;
    }

    public Notification withStringExtra(String key, String value) {
        this.extras.addProperty(key, value);
        return this;
    }

    public Notification withAddedDate(long addedDate) {
        this.addedDate = addedDate;
        return this;
    }

    public static String stringType(Context context, int errorCode)
    {
        switch (errorCode) {
            case TYPE_UPDATE:
                return context.getString(R.string.notification_type_update);
            case TYPE_ERROR:
                return context.getString(R.string.notification_type_error);
            case TYPE_TIMETABLE_CHANGED:
                return context.getString(R.string.notification_type_timetable_change);
            case TYPE_TIMETABLE_LESSON_CHANGE:
                return context.getString(R.string.notification_type_timetable_lesson_change);
            case TYPE_NEW_GRADE:
                return context.getString(R.string.notification_type_new_grade);
            case TYPE_NEW_EVENT:
                return context.getString(R.string.notification_type_new_event);
            case TYPE_NEW_HOMEWORK:
                return context.getString(R.string.notification_type_new_homework);
            case TYPE_NEW_SHARED_EVENT:
                return context.getString(R.string.notification_type_new_shared_event);
            case TYPE_NEW_MESSAGE:
                return context.getString(R.string.notification_type_new_message);
            case TYPE_NEW_NOTICE:
                return context.getString(R.string.notification_type_notice);
            case TYPE_NEW_ATTENDANCE:
                return context.getString(R.string.notification_type_attendance);
            case TYPE_SERVER_MESSAGE:
                return context.getString(R.string.notification_type_server_message);
            case TYPE_LUCKY_NUMBER:
                return context.getString(R.string.notification_type_lucky_number);
            case TYPE_FEEDBACK_MESSAGE:
                return context.getString(R.string.notification_type_feedback_message);
            case TYPE_NEW_ANNOUNCEMENT:
                return context.getString(R.string.notification_type_new_announcement);
            case TYPE_AUTO_ARCHIVING:
                return context.getString(R.string.notification_type_auto_archiving);
            default:
            case TYPE_GENERAL:
                return context.getString(R.string.notification_type_general);
        }
    }

    public void fillIntent(Intent intent) {
        if (profileId != -1)
            intent.putExtra("profileId", profileId);

        /*if (redirectFragmentId == DRAWER_ITEM_MESSAGES)
            redirectFragmentId = DRAWER_ITEM_MESSAGES_INBOX;*/

        if (redirectFragmentId != -1)
            intent.putExtra("fragmentId", redirectFragmentId);

        try {
            for (Map.Entry<String, JsonElement> entry: extras.entrySet()) {
                JsonElement value = entry.getValue();
                if (!value.isJsonPrimitive())
                    continue;
                JsonPrimitive primitive = value.getAsJsonPrimitive();
                if (primitive.isNumber()) {
                    intent.putExtra(entry.getKey(), primitive.getAsLong());
                }
                else if (primitive.isString()) {
                    intent.putExtra(entry.getKey(), primitive.getAsString());
                }
            }
        }
        catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
}
