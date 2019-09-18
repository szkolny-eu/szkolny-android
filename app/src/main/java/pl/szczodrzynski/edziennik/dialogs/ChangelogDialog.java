package pl.szczodrzynski.edziennik.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.utils.Themes;

import static pl.szczodrzynski.edziennik.utils.Utils.hexFromColorInt;
import static pl.szczodrzynski.navlib.UtilsKt.getColorFromAttr;

public class ChangelogDialog extends DialogFragment {

    private String charsetName = "UTF-8";

    private int getColor(int resId)
    {
        return getContext().getResources().getColor(resId);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        assert getContext() != null;
        assert getActivity() != null;
        App app = (App)getContext().getApplicationContext();
        final View customView;
        try {
            customView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_changelog, null);
        } catch (InflateException e) {
            Toast.makeText(getContext(), "This device does not support WebViews.", Toast.LENGTH_SHORT).show();
            return null;
        }
        boolean darkTheme = Themes.INSTANCE.isDark();

        MaterialDialog dialog =
                new MaterialDialog.Builder(getActivity())
                        .theme(darkTheme ? Theme.DARK : Theme.LIGHT)
                        .title(R.string.whats_new)
                        .customView(customView, true)
                        .positiveText(android.R.string.ok)
                        .build();

        WebView webView = customView.findViewById(R.id.webview);
        try {

            // Load from changelog.html in the assets folder
            StringBuilder buf = new StringBuilder();
            InputStream json = getActivity().getAssets().open(getContext().getString(R.string.prefix)+"-changelog.html");
            BufferedReader in = new BufferedReader(new InputStreamReader(json, charsetName));
            String str;
            while ((str = in.readLine()) != null) {
                buf.append(str);
            }
            in.close();

            // Inject color values for WebView body background and links
            webView.loadDataWithBaseURL(null,
                    buf.toString()
                            .replace("{bg-color}", hexFromColorInt(darkTheme ? 0x424242 : 0xffffff))
                            .replace("{text-color}", colorToHex(getColorFromAttr(getContext(), android.R.attr.textColorPrimary)))
                            .replace("{secondary-color}", colorToHex(getColorFromAttr(getContext(), android.R.attr.textColorSecondary)))
                            .replace("{link-color}", colorToHex(getColorFromAttr(getContext(), R.attr.colorAccent)))
                            .replace("{link-color-active}", colorToHex(getColorFromAttr(getContext(), R.attr.colorPrimaryDark))),
                    "text/html",
                    charsetName, null);
        } catch (Throwable e) {
            webView.loadData(
                    "<h1>Unable to load</h1><p>" + e.getLocalizedMessage() + "</p>", "text/html", charsetName);
        }
        return dialog;
    }

    private String colorToHex(int color) {
        return Integer.toHexString(color).substring(2);
    }
}
