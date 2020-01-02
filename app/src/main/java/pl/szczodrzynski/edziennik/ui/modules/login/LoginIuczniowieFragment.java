package pl.szczodrzynski.edziennik.ui.modules.login;

import android.os.Bundle;
import android.text.Editable;
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
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.api.models.ApiError;
import pl.szczodrzynski.edziennik.databinding.FragmentLoginIuczniowieBinding;
import pl.szczodrzynski.edziennik.ui.modules.error.ErrorSnackbar;

import static pl.szczodrzynski.edziennik.data.api.ErrorsKt.ERROR_LOGIN_IDZIENNIK_WEB_INVALID_LOGIN;
import static pl.szczodrzynski.edziennik.data.api.ErrorsKt.ERROR_LOGIN_IDZIENNIK_WEB_INVALID_SCHOOL_NAME;
import static pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore.LOGIN_TYPE_IUCZNIOWIE;

public class LoginIuczniowieFragment extends Fragment {

    private App app;
    private NavController nav;
    private FragmentLoginIuczniowieBinding b;
    private static final String TAG = "LoginIuczniowie";
    private ErrorSnackbar errorSnackbar;

    public LoginIuczniowieFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (getActivity() != null) {
            app = (App) getActivity().getApplicationContext();
            nav = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
            errorSnackbar = ((LoginActivity) getActivity()).errorSnackbar;
        }
        else {
            return null;
        }
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_login_iuczniowie, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        assert getContext() != null;
        assert getActivity() != null;

        view.postDelayed(() -> {
            ApiError error = LoginActivity.error;
            if (error != null) {
                switch (error.getErrorCode()) {
                    case ERROR_LOGIN_IDZIENNIK_WEB_INVALID_SCHOOL_NAME:
                        b.loginSchoolNameLayout.setError(getString(R.string.login_error_incorrect_school_name));
                        break;
                    case ERROR_LOGIN_IDZIENNIK_WEB_INVALID_LOGIN:
                        b.loginPasswordLayout.setError(getString(R.string.login_error_incorrect_login_or_password));
                        break;
                }
                errorSnackbar.addError(error).show();
                LoginActivity.error = null;
            }
        }, 100);

        b.helpButton.setOnClickListener((v) -> nav.navigate(R.id.loginIuczniowieHelpFragment, null, LoginActivity.navOptions));
        b.backButton.setOnClickListener((v) -> nav.navigateUp());

        b.loginButton.setOnClickListener((v) -> {
            boolean errors = false;

            b.loginSchoolNameLayout.setError(null);
            b.loginUsernameLayout.setError(null);
            b.loginPasswordLayout.setError(null);

            Editable schoolNameEditable = b.loginSchoolName.getText();
            Editable usernameEditable = b.loginUsername.getText();
            Editable passwordEditable = b.loginPassword.getText();
            if (schoolNameEditable == null || schoolNameEditable.length() == 0) {
                b.loginSchoolNameLayout.setError(getString(R.string.login_error_no_school_name));
                errors = true;
            }
            if (usernameEditable == null || usernameEditable.length() == 0) {
                b.loginUsernameLayout.setError(getString(R.string.login_error_no_username));
                errors = true;
            }
            if (passwordEditable == null || passwordEditable.length() == 0) {
                b.loginPasswordLayout.setError(getString(R.string.login_error_no_password));
                errors = true;
            }

            if (errors)
                return;
            errors = false;

            String schoolName = schoolNameEditable.toString().toLowerCase();
            String username = usernameEditable.toString().toLowerCase();
            String password = passwordEditable.toString();
            b.loginSchoolName.setText(schoolName);
            b.loginUsername.setText(username);
            if (!schoolName.matches("[a-z0-9_\\-]+")) {
                b.loginSchoolNameLayout.setError(getString(R.string.login_error_incorrect_school_name));
                errors = true;
            }
            if (!username.matches("[a-z0-9_\\-]+")) {
                b.loginUsernameLayout.setError(getString(R.string.login_error_incorrect_username));
                errors = true;
            }

            if (errors)
                return;
            errors = false;

            Bundle args = new Bundle();
            args.putInt("loginType", LOGIN_TYPE_IUCZNIOWIE);
            args.putString("schoolName", schoolName);
            args.putString("username", username);
            args.putString("password", password);
            nav.navigate(R.id.loginProgressFragment, args, LoginActivity.navOptions);
        });
    }
}
