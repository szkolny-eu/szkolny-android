package pl.szczodrzynski.edziennik.login;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.api.AppError;
import pl.szczodrzynski.edziennik.api.interfaces.SyncCallback;
import pl.szczodrzynski.edziennik.databinding.FragmentLoginMigrationSyncBinding;
import pl.szczodrzynski.edziennik.datamodels.LoginStore;
import pl.szczodrzynski.edziennik.datamodels.Profile;
import pl.szczodrzynski.edziennik.datamodels.ProfileFull;
import pl.szczodrzynski.edziennik.sync.SyncJob;
import pl.szczodrzynski.edziennik.sync.SyncService;

public class LoginMigrationSyncFragment extends Fragment {

    private App app;
    private NavController nav;
    private FragmentLoginMigrationSyncBinding b;
    private static final String TAG = "LoginMigrationSync";
    private List<String> profileNameList = new ArrayList<>();
    private int profileIndex = 0;

    public LoginMigrationSyncFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (getActivity() != null) {
            app = (App) getActivity().getApplicationContext();
            nav = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
        }
        else {
            return null;
        }
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_login_migration_sync, container, false);
        return b.getRoot();
    }

    private void begin() {
        AsyncTask.execute(() -> {
            if (getActivity() == null) {
                return;
            }

            for (Profile profileObject: app.db.profileDao().getAllNow()) {
                profileNameList.add(profileObject.getName());
            }

            getActivity().runOnUiThread(() -> {
                profileIndex = 0;
                b.loginSyncSubtitle1.setText(Html.fromHtml(getString(R.string.login_sync_subtitle_1_format, profileNameList.size() > profileIndex ? profileNameList.get(profileIndex) : " ")));
            });
            SyncJob.run(app, -1, -1);
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        assert getContext() != null;
        assert getActivity() != null;

        SyncService.customCallback = new SyncCallback() {
            @Override public void onLoginFirst(List<Profile> profileList, LoginStore loginStore) { }

            @Override
            public void onSuccess(Context activityContext, ProfileFull profileFull) {
                if (getActivity() == null)
                    return;
                getActivity().runOnUiThread(() -> {
                    if (profileFull != null) {
                        // a profile is finished
                        profileIndex++;
                        b.loginSyncSubtitle1.setText(Html.fromHtml(getString(R.string.login_sync_subtitle_1_format, profileIndex < profileNameList.size() ? profileNameList.get(profileIndex) : profileNameList.get(profileNameList.size()-1))));
                    }
                    else {
                        // all profiles are finished
                        getActivity().setResult(Activity.RESULT_OK);
                        getActivity().finish();
                    }
                });
            }

            @Override
            public void onError(Context activityContext, AppError error) {
                if (getActivity() == null)
                    return;
                getActivity().setResult(Activity.RESULT_OK);
                getActivity().finish();
            }

            @Override
            public void onProgress(int progressStep) {
                if (getActivity() == null)
                    return;
                getActivity().runOnUiThread(() -> {
                    b.loginSyncProgressBar.setMax(SyncService.maxProgress);
                    b.loginSyncProgressBar.setProgress(SyncService.progress);
                });
            }

            @Override
            public void onActionStarted(int stringResId) {
                if (getActivity() == null)
                    return;
                getActivity().runOnUiThread(() -> {
                    b.loginSyncSubtitle2.setText(getString(R.string.login_sync_subtitle_2_format, getString(stringResId)));
                });
            }
        };

        begin();
    }
}

