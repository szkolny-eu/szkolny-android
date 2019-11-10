package pl.szczodrzynski.edziennik.ui.modules.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.gson.JsonObject;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.BuildConfig;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.api.v2.events.ApiTaskErrorEvent;
import pl.szczodrzynski.edziennik.api.v2.events.FirstLoginFinishedEvent;
import pl.szczodrzynski.edziennik.api.v2.events.task.EdziennikTask;
import pl.szczodrzynski.edziennik.data.api.AppError;
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore;
import pl.szczodrzynski.edziennik.databinding.FragmentLoginProgressBinding;

import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_OTHER;

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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFirstLoginFinishedEvent(FirstLoginFinishedEvent event) {
        LoginActivity.profileObjects.add(new LoginProfileObject(
                event.getLoginStore(),
                event.getProfileList()));
        nav.navigate(R.id.loginSummaryFragment, null, LoginActivity.navOptions);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSyncErrorEvent(ApiTaskErrorEvent event) {
        LoginActivity.error = event.getError().toAppError();
        if (getActivity() == null)
            return;
        nav.navigateUp();
    }

    // TODO add progress bar in login

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

        if (BuildConfig.DEBUG && LoginChooserFragment.fakeLogin) {
            loginStore.putLoginData("fakeLogin", true);
        }

        EdziennikTask.Companion.firstLogin(loginStore).enqueue(getContext());
    }

    @Override
    public void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }
}
