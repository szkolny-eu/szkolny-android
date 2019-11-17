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
import pl.szczodrzynski.edziennik.api.v2.models.ApiError;
import pl.szczodrzynski.edziennik.databinding.FragmentLoginLibrusBinding;
import pl.szczodrzynski.edziennik.ui.modules.error.ErrorSnackbar;

import static pl.szczodrzynski.edziennik.api.v2.ErrorsKt.ERROR_LOGIN_LIBRUS_PORTAL_INVALID_LOGIN;
import static pl.szczodrzynski.edziennik.api.v2.ErrorsKt.ERROR_LOGIN_LIBRUS_PORTAL_NOT_ACTIVATED;
import static pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore.LOGIN_TYPE_LIBRUS;

public class LoginLibrusFragment extends Fragment {

    private App app;
    private NavController nav;
    private FragmentLoginLibrusBinding b;
    private static final String TAG = "LoginLibrus";
    private ErrorSnackbar errorSnackbar;

    public LoginLibrusFragment() { }

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
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_login_librus, container, false);
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
                    case ERROR_LOGIN_LIBRUS_PORTAL_INVALID_LOGIN:
                        b.loginPasswordLayout.setError(getString(R.string.login_error_incorrect_login_or_password));
                        break;
                    case ERROR_LOGIN_LIBRUS_PORTAL_NOT_ACTIVATED:
                        b.loginEmailLayout.setError(getString(R.string.login_error_account_not_activated));
                        break;
                }
                errorSnackbar.addError(error).show();
                LoginActivity.error = null;
            }
        }, 100);

        b.helpButton.setOnClickListener((v) -> nav.navigate(R.id.loginLibrusHelpFragment, null, LoginActivity.navOptions));
        b.backButton.setOnClickListener((v) -> nav.navigateUp());

        b.loginButton.setOnClickListener((v) -> {
            boolean errors = false;

            b.loginEmailLayout.setError(null);
            b.loginPasswordLayout.setError(null);

            Editable emailEditable = b.loginEmail.getText();
            Editable passwordEditable = b.loginPassword.getText();
            if (emailEditable == null || emailEditable.length() == 0) {
                b.loginEmailLayout.setError(getString(R.string.login_error_no_email));
                errors = true;
            }
            if (passwordEditable == null || passwordEditable.length() == 0) {
                b.loginPasswordLayout.setError(getString(R.string.login_error_no_password));
                errors = true;
            }

            if (errors)
                return;
            errors = false;

            String email = emailEditable.toString().toLowerCase();
            String password = passwordEditable.toString();
            b.loginEmail.setText(email);
            if (!email.matches("([\\w.\\-_+]+)?\\w+@[\\w-_]+(\\.\\w+)+")) {
                b.loginEmailLayout.setError(getString(R.string.login_error_incorrect_email));
                errors = true;
            }

            if (errors)
                return;
            errors = false;

            Bundle args = new Bundle();
            args.putInt("loginType", LOGIN_TYPE_LIBRUS);
            args.putString("email", email);
            args.putString("password", password);
            nav.navigate(R.id.loginProgressFragment, args, LoginActivity.navOptions);
        });
    }
}
