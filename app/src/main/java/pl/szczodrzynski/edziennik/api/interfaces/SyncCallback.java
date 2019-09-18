package pl.szczodrzynski.edziennik.api.interfaces;

import android.content.Context;

import java.util.List;

import androidx.annotation.StringRes;

import pl.szczodrzynski.edziennik.api.AppError;
import pl.szczodrzynski.edziennik.datamodels.LoginStore;
import pl.szczodrzynski.edziennik.datamodels.Profile;
import pl.szczodrzynski.edziennik.datamodels.ProfileFull;

/**
 * A callback used for error reporting, progress information.
 * All the methods are always ran on a worker thread.
 */
public interface SyncCallback extends ProgressCallback {
    void onLoginFirst(List<Profile> profileList, LoginStore loginStore);
    void onSuccess(Context activityContext, ProfileFull profileFull);

}
