package pl.szczodrzynski.edziennik.ui.modules.login;

import android.content.Intent;
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
import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.ui.modules.feedback.FeedbackActivity;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.databinding.FragmentLoginChooserBinding;

import static android.app.Activity.RESULT_CANCELED;

public class LoginChooserFragment extends Fragment {

    private App app;
    private NavController nav;
    private FragmentLoginChooserBinding b;
    private static final String TAG = "LoginTemplate";

    public LoginChooserFragment() { }

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
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_login_chooser, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        assert getContext() != null;
        assert getActivity() != null;

        b.loginMobidziennikLogo.setOnClickListener((v) -> nav.navigate(R.id.loginMobidziennikFragment, null, LoginActivity.navOptions));
        b.loginLibrusLogo.setOnClickListener((v) -> nav.navigate(R.id.loginLibrusFragment, null, LoginActivity.navOptions));
        b.loginVulcanLogo.setOnClickListener((v) -> nav.navigate(R.id.loginVulcanFragment, null, LoginActivity.navOptions));
        b.loginIuczniowieLogo.setOnClickListener((v) -> nav.navigate(R.id.loginIuczniowieFragment, null, LoginActivity.navOptions));

        if (LoginActivity.firstCompleted) {
            // we are navigated here from LoginSummary
            b.cancelButton.setVisibility(View.VISIBLE);
            b.cancelButton.setOnClickListener((v -> nav.navigateUp()));
        }
        else if (app.appConfig.lastAppVersion < 1991) {
            nav.navigate(R.id.loginMigrationFragment, null, LoginActivity.navOptions);
        }
        else if (app.appConfig.loginFinished) {
            // we are navigated here from AppDrawer
            b.cancelButton.setVisibility(View.VISIBLE);
            b.cancelButton.setOnClickListener((v -> {
                getActivity().setResult(RESULT_CANCELED);
                getActivity().finish();
            }));
        }
        else {
            // there is no profiles
            b.cancelButton.setVisibility(View.GONE);
        }

        b.helpButton.setOnClickListener((v -> {
            startActivity(new Intent(getActivity(), FeedbackActivity.class));
        }));
    }
}
