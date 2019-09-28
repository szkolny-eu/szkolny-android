package pl.szczodrzynski.edziennik.ui.modules.login;


import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.api.AppError;
import pl.szczodrzynski.edziennik.data.api.interfaces.SyncCallback;
import pl.szczodrzynski.edziennik.databinding.FragmentLoginSyncBinding;
import pl.szczodrzynski.edziennik.data.db.modules.events.EventType;
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile;
import pl.szczodrzynski.edziennik.data.db.modules.profiles.ProfileFull;
import pl.szczodrzynski.edziennik.sync.SyncJob;
import pl.szczodrzynski.edziennik.sync.SyncService;

import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_CLASS_EVENT;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_DEFAULT;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_EXAM;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_EXCURSION;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_HOMEWORK;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_INFORMATION;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_PROJECT;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_PT_MEETING;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_READING;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.COLOR_SHORT_QUIZ;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_CLASS_EVENT;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_DEFAULT;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_ESSAY;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_EXAM;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_EXCURSION;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_HOMEWORK;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_INFORMATION;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_PROJECT;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_PT_MEETING;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_READING;
import static pl.szczodrzynski.edziennik.data.db.modules.events.Event.TYPE_SHORT_QUIZ;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.REGISTRATION_DISABLED;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.REGISTRATION_ENABLED;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.REGISTRATION_UNSPECIFIED;
import static pl.szczodrzynski.edziennik.utils.Utils.d;

public class LoginSyncFragment extends Fragment {

    private App app;
    private NavController nav;
    private FragmentLoginSyncBinding b;
    private static final String TAG = "LoginSyncFragment";
    private List<String> profileNameList = new ArrayList<>();
    private int profileIndex = 0;

    public LoginSyncFragment() { }

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
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_login_sync, container, false);
        return b.getRoot();
    }

    private void begin() {
        AsyncTask.execute(() -> {
            if (getActivity() == null) {
                return;
            }
            int profileId = app.profileLastId()+1;
            final int firstProfileId = profileId;
            int loginStoreId = profileId;
            // profileId contains the first ID free to use

            for (LoginProfileObject profileObject: LoginActivity.profileObjects) {
                int subIndex = 0;
                for (Profile profile: profileObject.profileList) {
                    if (profileObject.selectedList.get(subIndex)) {
                        saveProfile(
                                profile,
                                profileObject.loginStore,
                                profileId,
                                loginStoreId
                        );
                        profileNameList.add(profile.getName());
                        profileId++;
                    }
                    subIndex++;
                }
                loginStoreId = profileId;
            }

            for (Profile profile: app.db.profileDao().getAllNow()) {
                d(TAG, profile.toString());
            }
            for (LoginStore loginStore: app.db.loginStoreDao().getAllNow()) {
                d(TAG, loginStore.toString());
            }

            if (app.appConfig.loginFinished) {
                LoginFinishFragment.firstRun = false;
            }
            else {
                LoginFinishFragment.firstRun = true;
                app.appConfig.loginFinished = true;
                app.saveConfig("loginFinished");
            }
            LoginFinishFragment.firstProfileId = firstProfileId;

            getActivity().runOnUiThread(() -> {
                profileIndex = 0;
                b.loginSyncSubtitle1.setText(Html.fromHtml(getString(R.string.login_sync_subtitle_1_format, profileNameList.size() > profileIndex ? profileNameList.get(profileIndex) : " ")));
            });
            SyncJob.run(app, firstProfileId, -1);
        });
    }

    private void saveProfile(Profile profile, LoginStore loginStore, int profileId, int loginStoreId) {
        profile.setRegistration(REGISTRATION_UNSPECIFIED);
        if (getArguments() != null) {
            if (getArguments().getBoolean("registrationAllowed", false)) {
                profile.setRegistration(REGISTRATION_ENABLED);
            }
            else {
                profile.setRegistration(REGISTRATION_DISABLED);
            }
        }
        profile.setId(profileId);
        profile.setLoginStoreId(loginStoreId);
        loginStore.id = loginStoreId;
        List<EventType> typeList = new ArrayList<>();
        typeList.add(new EventType(profileId, TYPE_HOMEWORK, getString(R.string.event_type_homework), COLOR_HOMEWORK));
        typeList.add(new EventType(profileId, TYPE_DEFAULT, getString(R.string.event_other), COLOR_DEFAULT));
        typeList.add(new EventType(profileId, TYPE_EXAM, getString(R.string.event_exam), COLOR_EXAM));
        typeList.add(new EventType(profileId, TYPE_SHORT_QUIZ, getString(R.string.event_short_quiz), COLOR_SHORT_QUIZ));
        typeList.add(new EventType(profileId, TYPE_ESSAY, getString(R.string.event_essay), COLOR_SHORT_QUIZ));
        typeList.add(new EventType(profileId, TYPE_PROJECT, getString(R.string.event_project), COLOR_PROJECT));
        typeList.add(new EventType(profileId, TYPE_PT_MEETING, getString(R.string.event_pt_meeting), COLOR_PT_MEETING));
        typeList.add(new EventType(profileId, TYPE_EXCURSION, getString(R.string.event_excursion), COLOR_EXCURSION));
        typeList.add(new EventType(profileId, TYPE_READING, getString(R.string.event_reading), COLOR_READING));
        typeList.add(new EventType(profileId, TYPE_CLASS_EVENT, getString(R.string.event_class_event), COLOR_CLASS_EVENT));
        typeList.add(new EventType(profileId, TYPE_INFORMATION, getString(R.string.event_information), COLOR_INFORMATION));
        app.db.eventTypeDao().addAll(typeList);
        app.profileSaveFull(profile, loginStore);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        assert getContext() != null;
        assert getActivity() != null;

        LoginActivity.error = null;

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
                        nav.navigate(R.id.loginFinishFragment, null , LoginActivity.navOptions);
                    }
                });
            }

            @Override
            public void onError(Context activityContext, AppError error) {
                LoginActivity.error = error;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        nav.navigate(R.id.loginSyncErrorFragment, null, LoginActivity.navOptions);
                    });
                }
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
