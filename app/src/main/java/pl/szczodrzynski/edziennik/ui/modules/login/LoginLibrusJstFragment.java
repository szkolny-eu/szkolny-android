/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-13.
 */

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
import pl.szczodrzynski.edziennik.databinding.FragmentLoginLibrusJstBinding;
import pl.szczodrzynski.edziennik.ui.modules.error.ErrorSnackbar;

import static pl.szczodrzynski.edziennik.api.v2.ErrorsKt.ERROR_LOGIN_LIBRUS_API_INVALID_LOGIN;
import static pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore.LOGIN_MODE_LIBRUS_JST;
import static pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore.LOGIN_TYPE_LIBRUS;

public class LoginLibrusJstFragment extends Fragment {

    private App app;
    private NavController nav;
    private FragmentLoginLibrusJstBinding b;
    private static final String TAG = "LoginLibrus";
    private ErrorSnackbar errorSnackbar;

    public LoginLibrusJstFragment() { }

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
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_login_librus_jst, container, false);
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
                    case ERROR_LOGIN_LIBRUS_API_INVALID_LOGIN:
                        b.loginCodeLayout.setError(getString(R.string.login_error_incorrect_code_or_pin));
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

            b.loginCodeLayout.setError(null);
            b.loginPinLayout.setError(null);

            Editable codeEditable = b.loginCode.getText();
            Editable pinEditable = b.loginPin.getText();
            if (codeEditable == null || codeEditable.length() == 0) {
                b.loginCodeLayout.setError(getString(R.string.login_error_no_code));
                errors = true;
            }
            if (pinEditable == null || pinEditable.length() == 0) {
                b.loginPinLayout.setError(getString(R.string.login_error_no_pin));
                errors = true;
            }

            if (errors)
                return;
            errors = false;

            String code = codeEditable.toString().toUpperCase();
            String pin = pinEditable.toString();
            b.loginCode.setText(code);
            if (!code.matches("[A-Z0-9_]+")) {
                b.loginCodeLayout.setError(getString(R.string.login_error_incorrect_code));
                errors = true;
            }
            if (!pin.matches("[a-z0-9_]+")) {
                b.loginPinLayout.setError(getString(R.string.login_error_incorrect_pin));
                errors = true;
            }

            if (errors)
                return;
            errors = false;

            Bundle args = new Bundle();
            args.putInt("loginType", LOGIN_TYPE_LIBRUS);
            args.putInt("loginMode", LOGIN_MODE_LIBRUS_JST);
            args.putString("accountCode", code);
            args.putString("accountPin", pin);
            nav.navigate(R.id.loginProgressFragment, args, LoginActivity.navOptions);
        });
    }
}
