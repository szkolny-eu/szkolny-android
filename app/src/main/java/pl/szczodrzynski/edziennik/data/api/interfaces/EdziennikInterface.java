package pl.szczodrzynski.edziennik.data.api.interfaces;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore;
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageFull;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.ProfileFull;
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher;
import pl.szczodrzynski.edziennik.ui.modules.messages.MessagesComposeInfo;
import pl.szczodrzynski.edziennik.utils.models.Endpoint;

public interface EdziennikInterface {

    /**
     * Sync all Edziennik data.
     * Ran always on worker thread.
     *
     * @param activityContext a {@link Context}, used for resource extractions, passed back to {@link SyncCallback}
     * @param callback ran on worker thread.
     * @param profileId
     * @param profile
     * @param loginStore
     */
    void sync(@NonNull Context activityContext, @NonNull SyncCallback callback, int profileId, @Nullable Profile profile, @NonNull LoginStore loginStore);
    void syncMessages(@NonNull Context activityContext, @NonNull SyncCallback errorCallback, @NonNull ProfileFull profile);
    void syncFeature(@NonNull Context activityContext, @NonNull SyncCallback callback, @NonNull ProfileFull profile, int ... featureList);

    int FEATURE_ALL = 0;
    int FEATURE_TIMETABLE = 1;
    int FEATURE_AGENDA = 2;
    int FEATURE_GRADES = 3;
    int FEATURE_HOMEWORK = 4;
    int FEATURE_NOTICES = 5;
    int FEATURE_ATTENDANCE = 6;
    int FEATURE_MESSAGES_INBOX = 7;
    int FEATURE_MESSAGES_OUTBOX = 8;
    int FEATURE_ANNOUNCEMENTS = 9;

    /**
     * Download a single message or get its recipient list if it's already downloaded.
     *
     * May be executed on any thread.
     *
     * @param activityContext
     * @param errorCallback used for error reporting. Ran on a background thread.
     * @param profile
     * @param message a message of which body and recipient list should be downloaded.
     * @param messageCallback always executed on UI thread.
     */
    void getMessage(@NonNull Context activityContext, @NonNull SyncCallback errorCallback, @NonNull ProfileFull profile, @NonNull MessageFull message, @NonNull MessageGetCallback messageCallback);
    void getAttachment(@NonNull Context activityContext, @NonNull SyncCallback errorCallback, @NonNull ProfileFull profile, @NonNull MessageFull message, long attachmentId, @NonNull AttachmentGetCallback attachmentCallback);
    //void getMessageList(@NonNull Context activityContext, @NonNull SyncCallback errorCallback, @NonNull ProfileFull profile, int type, @NonNull MessageListCallback messageCallback);
    /**
     * Download a list of available message recipients.
     *
     * Updates a database-saved {@code teacherList} with {@code loginId}s.
     *
     * A {@link Teacher} is considered as a recipient when its {@code loginId} is not null.
     *
     * May be executed on any thread.
     *
     * @param activityContext
     * @param errorCallback used for error reporting. Ran on a background thread.
     * @param profile
     * @param recipientListGetCallback always executed on UI thread.
     */
    void getRecipientList(@NonNull Context activityContext, @NonNull SyncCallback errorCallback, @NonNull ProfileFull profile, @NonNull RecipientListGetCallback recipientListGetCallback);
    MessagesComposeInfo getComposeInfo(@NonNull ProfileFull profile);


    /**
     *
     * @param profile a {@link Profile} containing already changed endpoints
     * @return a map of configurable {@link Endpoint}s along with their names, {@code null} when unsupported
     */
    Map<String, Endpoint> getConfigurableEndpoints(Profile profile);

    /**
     * Check if the specified endpoint is enabled for the current profile.
     *
     * @param profile a {@link Profile} containing already changed endpoints
     * @param defaultActive if the endpoint is enabled by default.
     * @param name the endpoint's name
     * @return {@code true} if the endpoint is enabled, {@code false} when it's not. Return {@code defaultActive} if unsupported.
     */
    boolean isEndpointEnabled(Profile profile, boolean defaultActive, String name);
}
