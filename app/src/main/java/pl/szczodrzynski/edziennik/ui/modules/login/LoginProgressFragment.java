package pl.szczodrzynski.edziennik.ui.modules.login;

import androidx.databinding.DataBindingUtil;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.JsonObject;

import java.util.List;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.api.Edziennik;
import pl.szczodrzynski.edziennik.api.AppError;
import pl.szczodrzynski.edziennik.api.interfaces.SyncCallback;
import pl.szczodrzynski.edziennik.databinding.FragmentLoginProgressBinding;
import pl.szczodrzynski.edziennik.datamodels.LoginStore;
import pl.szczodrzynski.edziennik.datamodels.Profile;
import pl.szczodrzynski.edziennik.datamodels.ProfileFull;

import static pl.szczodrzynski.edziennik.api.AppError.CODE_OTHER;

public class LoginProgressFragment extends Fragment {

    private App app;
    private NavController nav;
    private FragmentLoginProgressBinding b;
    private static final String TAG = "LoginProgress";

    public LoginProgressFragment() { }

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
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_login_progress, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        assert getContext() != null;
        assert getActivity() != null;
        Bundle args = getArguments();

        LoginActivity.error = null;

        if (args == null) {
            LoginActivity.error = new AppError(TAG, 72, CODE_OTHER, getString(R.string.login_error_no_arguments));
            nav.navigateUp();
            return;
        }

        int loginType = args.getInt("loginType", -1);

        LoginStore loginStore = new LoginStore(-1, loginType, new JsonObject());
        loginStore.copyFrom(args);

        Edziennik.getApi(app, loginType).sync(getActivity(), new SyncCallback() {
            @Override
            public void onLoginFirst(List<Profile> profileList, LoginStore loginStore) {
                // because these callbacks are always on a worker thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        LoginActivity.profileObjects.add(new LoginProfileObject(
                                loginStore,
                                profileList));
                        nav.navigate(R.id.loginSummaryFragment, null, LoginActivity.navOptions);
                    });
                }
            }

            @Override
            public void onSuccess(Context activityContext, ProfileFull profileFull) {

            }

            @Override
            public void onError(Context activityContext, AppError error) {
                LoginActivity.error = error;
                // because these callbacks are always on a worker thread
                if (getActivity() == null)
                    return;
                getActivity().runOnUiThread(() -> {
                    nav.navigateUp();
                });
            }

            @Override
            public void onProgress(int progressStep) {

            }

            @Override
            public void onActionStarted(int stringResId) {

            }
        }, -1, null, loginStore);

        /*if (true)
            return;
        JsonObject loginData = new JsonObject();
        loginData.addProperty("serverName", b.loginServerAddress.getText().toString());
        loginData.addProperty("username", b.loginUsername.getText().toString());
        loginData.addProperty("password", b.loginPassword.getText().toString());
        getApi(app, LOGIN_TYPE_MOBIDZIENNIK).sync(getActivity(), new Edziennik.DataCallback() {
            @Override
            public void onLoginFirst(List<Profile> profileList, LoginStore loginStore) {
                int profileId = app.profileLastId()+1;
                if (profileList.size() == 1) {
                    Profile profile = profileList.get(0);
                    saveProfile(profile, loginStore, profileId, profileId);
                    finishSaving();
                    return;
                }
                List<String> profileNames = new ArrayList<>();
                for (Profile profile: profileList) {
                    profileNames.add(profile.name);
                }
                new MaterialDialog.Builder(getActivity())
                        .title(R.string.sync_multiaccount_select_students)
                        .items(profileNames)
                        .positiveText(R.string.ok)
                        .negativeText(R.string.cancel)
                        .neutralText(R.string.help)
                        .autoDismiss(false)
                        .canceledOnTouchOutside(false)
                        .onNeutral((dialog, which) ->
                                new MaterialDialog.Builder(getActivity())
                                        .title(R.string.help)
                                        .content(R.string.sync_multiaccount_select_students_text)
                                        .positiveText(R.string.ok)
                                        .show()
                        )
                        .onNegative(((dialog, which) -> dialog.dismiss()))
                        .itemsCallbackMultiChoice(null, (dialog, which, text) -> {
                            // create new profiles, then restart the application or sth
                            if (text.length < 1 || which.length < 1) {
                                Toast.makeText(app, R.string.sync_multiaccount_select_students_error, Toast.LENGTH_SHORT).show();
                                return false;
                            }
                            dialog.dismiss();
                            int pos = 0;
                            for (int index: which) {
                                Profile profile = profileList.get(index);
                                saveProfile(profile, loginStore, profileId+(pos++), profileId);
                            }
                            finishSaving();


                            String list = "";
                            for (ProfileFull profileFull: app.db.profileDao().getAllFullNow()) {
                                d(TAG, profileFull.toString());
                                list += profileFull.studentNameLong+" student ID "+profileFull.getStudentData("studentId", -1)+"\n";
                            }
                            d(TAG, loginStore.toString());
                            list += loginStore.getLoginData("username", "(NO USERNAME)")+"\n";
                            new MaterialDialog.Builder(getActivity())
                                    .title("Znaleziono profile")
                                    .content(list)
                                    .positiveText("OK")
                                    .show();


                            return false;
                        })
                        .show();
            }

            @Override
            public void onSuccess(Context activityContext, ProfileFull profile) {
                Toast.makeText(activityContext, "Zakończono", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Context activityContext, int errorCode, String errorText, Throwable throwable, String apiResponse) {

            }

            @Override
            public void onProgress(int progressStep) {

            }
        }, -1, null, new LoginStore(-1, LOGIN_TYPE_MOBIDZIENNIK, loginData));*/
    }
}
