package pl.szczodrzynski.edziennik.data.api.interfaces;

import android.content.Context;

import java.util.List;

import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.ProfileFull;

/**
 * A callback used for error reporting, progress information.
 * All the methods are always ran on a worker thread.
 */
public interface SyncCallback extends ProgressCallback {
    void onLoginFirst(List<Profile> profileList, LoginStore loginStore);
    void onSuccess(Context activityContext, ProfileFull profileFull);

}
