package pl.szczodrzynski.edziennik.ui.modules.login;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.mikepenz.iconics.IconicsColor;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSize;
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.api.v2.models.ApiError;
import pl.szczodrzynski.edziennik.databinding.FragmentLoginVulcanBinding;
import pl.szczodrzynski.edziennik.ui.modules.error.ErrorSnackbar;
import pl.szczodrzynski.edziennik.ui.modules.webpush.QrScannerActivity;
import pl.szczodrzynski.edziennik.utils.Utils;

import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_EXPIRED_TOKEN;
import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_INVALID_PIN;
import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_INVALID_SYMBOL;
import static pl.szczodrzynski.edziennik.data.api.AppError.CODE_INVALID_TOKEN;
import static pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore.LOGIN_TYPE_VULCAN;

public class LoginVulcanFragment extends Fragment {

    private App app;
    private NavController nav;
    private FragmentLoginVulcanBinding b;
    private static final String TAG = "LoginVulcan";
    private ErrorSnackbar errorSnackbar;

    public LoginVulcanFragment() { }

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
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_login_vulcan, container, false);
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
                    case CODE_INVALID_TOKEN:
                        b.loginTokenLayout.setError(getString(R.string.login_error_incorrect_token));
                        break;
                    case CODE_EXPIRED_TOKEN:
                        b.loginTokenLayout.setError(getString(R.string.login_error_expired_token));
                        break;
                    case CODE_INVALID_SYMBOL:
                        b.loginSymbolLayout.setError(getString(R.string.login_error_incorrect_symbol));
                        break;
                    case CODE_INVALID_PIN:
                        /*if (!"?".equals(error.errorText)) {
                            b.loginPinLayout.setError(getString(R.string.login_error_incorrect_pin_format, error.errorText));
                            break;
                        }*/
                        b.loginPinLayout.setError(getString(R.string.login_error_incorrect_pin));
                        break;
                }
                errorSnackbar.addError(error).show();
                LoginActivity.error = null;
            }
        }, 100);

        b.helpButton.setOnClickListener((v) -> nav.navigate(R.id.loginVulcanHelpFragment, null, LoginActivity.navOptions));
        b.backButton.setOnClickListener((v) -> nav.navigateUp());

        b.loginQrScan.setImageDrawable(new IconicsDrawable(getActivity()).icon(CommunityMaterial.Icon2.cmd_qrcode_scan).color(IconicsColor.colorInt(Color.BLACK)).size(IconicsSize.dp(72)));
        b.loginQrScan.setOnClickListener((v -> {
            QrScannerActivity.resultHandler = result -> {
                try {
                    String qr = result.getText();
                    String data = Utils.VulcanQrEncryptionUtils.decode(qr);
                    Matcher matcher = Pattern.compile("CERT#https?://.+?/([A-z]+)/mobile-api#([A-z0-9]+)#ENDCERT").matcher(data);
                    if (matcher.find()) {
                        b.loginToken.setText(matcher.group(2));
                        b.loginSymbol.setText(matcher.group(1));
                        if(b.loginPin.requestFocus()) {
                            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        }
                    }
                    else {

                    }
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (ShortBufferException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
            };
            startActivity(new Intent(getContext(), QrScannerActivity.class));
        }));

        b.loginButton.setOnClickListener((v) -> {
            boolean errors = false;

            b.loginTokenLayout.setError(null);
            b.loginSymbolLayout.setError(null);
            b.loginPinLayout.setError(null);

            Editable tokenEditable = b.loginToken.getText();
            Editable symbolEditable = b.loginSymbol.getText();
            Editable pinEditable = b.loginPin.getText();
            if (tokenEditable == null || tokenEditable.length() == 0) {
                b.loginTokenLayout.setError(getString(R.string.login_error_no_token));
                errors = true;
            }
            if (symbolEditable == null || symbolEditable.length() == 0) {
                b.loginSymbolLayout.setError(getString(R.string.login_error_no_symbol));
                errors = true;
            }
            if (pinEditable == null || pinEditable.length() == 0) {
                b.loginPinLayout.setError(getString(R.string.login_error_no_pin));
                errors = true;
            }

            if (errors)
                return;
            errors = false;

            String token = tokenEditable.toString().toUpperCase();
            String symbol = symbolEditable.toString().toLowerCase();
            String pin = pinEditable.toString();
            b.loginToken.setText(token);
            b.loginSymbol.setText(symbol);
            b.loginPin.setText(pin);
            if (!token.matches("[A-Z0-9]{5,12}")) {
                b.loginTokenLayout.setError(getString(R.string.login_error_incorrect_token));
                errors = true;
            }
            if (!symbol.matches("[a-z0-9]+")) {
                b.loginSymbolLayout.setError(getString(R.string.login_error_incorrect_symbol));
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
            args.putInt("loginType", LOGIN_TYPE_VULCAN);
            args.putString("deviceToken", token);
            args.putString("deviceSymbol", symbol);
            args.putString("devicePin", pin);
            nav.navigate(R.id.loginProgressFragment, args, LoginActivity.navOptions);
        });
    }// narysowac raz dwa trzy cztery wyresy funkcji ktore sa tak dzielone
    // nire wnikac w szkczegoly jak dzialaja
    // takie same sa funckje sinus comisinus
    //
}
