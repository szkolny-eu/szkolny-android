package pl.szczodrzynski.edziennik.login;

import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.danimahardhika.cafebar.CafeBar;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.api.AppError;
import pl.szczodrzynski.edziennik.databinding.FragmentLoginMobidziennikBinding;

import static pl.szczodrzynski.edziennik.api.AppError.CODE_ARCHIVED;
import static pl.szczodrzynski.edziennik.api.AppError.CODE_INVALID_LOGIN;
import static pl.szczodrzynski.edziennik.api.AppError.CODE_INVALID_SERVER_ADDRESS;
import static pl.szczodrzynski.edziennik.api.AppError.CODE_OLD_PASSWORD;
import static pl.szczodrzynski.edziennik.datamodels.LoginStore.LOGIN_TYPE_MOBIDZIENNIK;

public class LoginMobidziennikFragment extends Fragment {

    private App app;
    private NavController nav;
    private FragmentLoginMobidziennikBinding b;
    private static final String TAG = "LoginMobidziennik";

    public LoginMobidziennikFragment() { }

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
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_login_mobidziennik, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        assert getContext() != null;
        assert getActivity() != null;

        view.postDelayed(() -> {
            AppError error = LoginActivity.error;
            if (error != null) {
                switch (error.errorCode) {
                    case CODE_INVALID_LOGIN:
                        b.loginPasswordLayout.setError(getString(R.string.login_error_incorrect_login_or_password));
                        break;
                    case CODE_OLD_PASSWORD:
                        b.loginPasswordLayout.setError(getString(R.string.login_error_old_password));
                        break;
                    case CODE_ARCHIVED:
                        b.loginUsernameLayout.setError(getString(R.string.sync_error_archived));
                        break;
                    case CODE_INVALID_SERVER_ADDRESS:
                        b.loginServerAddressLayout.setError(getString(R.string.login_error_incorrect_address));
                        break;
                    default:
                        CafeBar.builder(getActivity())
                                .to(b.root)
                                .content(getString(R.string.login_error, error.asReadableString(getActivity())))
                                .autoDismiss(false)
                                .positiveText(R.string.ok)
                                .onPositive(CafeBar::dismiss)
                                .floating(true)
                                .swipeToDismiss(true)
                                .neutralText(R.string.more)
                                .onNeutral(cafeBar -> app.apiEdziennik.guiShowErrorDialog(getActivity(), error, R.string.error_details))
                                .negativeText(R.string.report)
                                .onNegative((cafeBar -> app.apiEdziennik.guiReportError(getActivity(), error, null)))
                                .show();
                        break;
                }
                LoginActivity.error = null;
            }
        }, 100);

        b.helpButton.setOnClickListener((v) -> nav.navigate(R.id.loginMobidziennikHelpFragment, null, LoginActivity.navOptions));
        b.backButton.setOnClickListener((v) -> nav.navigateUp());

        b.loginButton.setOnClickListener((v) -> {
            boolean errors = false;

            b.loginServerAddressLayout.setError(null);
            b.loginUsernameLayout.setError(null);
            b.loginPasswordLayout.setError(null);

            Editable serverNameEditable = b.loginServerAddress.getText();
            Editable usernameEditable = b.loginUsername.getText();
            Editable passwordEditable = b.loginPassword.getText();
            if (serverNameEditable == null || serverNameEditable.length() == 0) {
                b.loginServerAddressLayout.setError(getString(R.string.login_error_no_address));
                errors = true;
            }
            if (usernameEditable == null || usernameEditable.length() == 0) {
                b.loginUsernameLayout.setError(getString(R.string.login_error_no_login));
                errors = true;
            }
            if (passwordEditable == null || passwordEditable.length() == 0) {
                b.loginPasswordLayout.setError(getString(R.string.login_error_no_password));
                errors = true;
            }

            if (errors)
                return;
            errors = false;

            String serverName = serverNameEditable.toString().toLowerCase().replaceAll("(?:http://|www.|mobidziennik\\.pl|wizja\\.net|\\.)", "");
            String username = usernameEditable.toString().toLowerCase();
            String password = passwordEditable.toString();
            b.loginServerAddress.setText(serverName);
            b.loginUsername.setText(username);
            if (!serverName.matches("^[a-z0-9_\\-]+$")) {
                b.loginServerAddressLayout.setError(getString(R.string.login_error_incorrect_address));
                errors = true;
            }
            if (!username.matches("^[a-z0-9_\\-@+.]+$")) {
                b.loginUsernameLayout.setError(getString(R.string.login_error_incorrect_login));
                errors = true;
            }

            if (errors)
                return;
            errors = false;

            Bundle args = new Bundle();
            args.putInt("loginType", LOGIN_TYPE_MOBIDZIENNIK);
            args.putString("serverName", serverName);
            args.putString("username", username);
            args.putString("password", password);
            nav.navigate(R.id.loginProgressFragment, args, LoginActivity.navOptions);
        });
    }
}
