package pl.szczodrzynski.edziennik.ui.modules.webpush;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import java.util.ArrayList;
import java.util.List;

import me.dm7.barcodescanner.zxing.ZXingScannerView;
import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.databinding.ActivityWebPushConfigBinding;
import pl.szczodrzynski.edziennik.network.ServerRequest;
import pl.szczodrzynski.edziennik.utils.Anim;
import pl.szczodrzynski.edziennik.utils.Themes;
import pl.szczodrzynski.edziennik.utils.Utils;

import static pl.szczodrzynski.edziennik.App.APP_URL;
import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.REGISTRATION_ENABLED;

public class WebPushConfigActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private static final String TAG = "WebPushConfigActivity";
    private ZXingScannerView mScannerView;

    ActivityWebPushConfigBinding b;

    boolean cameraRunning = false;

    private void showCamera() {
        cameraRunning = true;
        Anim.fadeIn(b.qrCodeScanner, 500, null);
        b.webPushConfig.setVisibility(View.GONE);
        b.qrCodeScanner.startCamera();
        b.qrCodeScanner.setResultHandler(this);
    }

    private App app;

    private void hideCamera() {
        cameraRunning = false;
        Anim.fadeOut(b.qrCodeScanner, 500, null);
        b.webPushConfig.setVisibility(View.VISIBLE);
        b.qrCodeScanner.stopCamera();
    }

    private void getPairedBrowsers(@NonNull String newFcm, int removeId) {
        Anim.fadeIn(b.browserListProgressBar, 500, null);
        Anim.fadeOut(b.browserList, 500, null);
        Anim.fadeOut(b.browserListErrorText, 500, null);
        new ServerRequest(app, app.requestScheme + APP_URL + "main.php?web_push_list"+(!newFcm.equals("") ? "&web_push_pair" : "") + (removeId != -1 ? "&web_push_unpair" : ""), "WebPushConfigActivity", app.profile)
                .setBodyParameter((removeId != -1 ? "id" : "browser_fcm"), (removeId != -1 ? Integer.toString(removeId) : newFcm))
                .run(((e, result) -> {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Anim.fadeOut(b.browserListProgressBar, 500, null);
                        if (result == null || result.get("browser_count") == null) {
                            b.browserListErrorText.setText(R.string.web_push_connection_error);
                            Anim.fadeIn(b.browserListErrorText, 500, null);
                            return;
                        }
                        if (result.get("browser_count").getAsInt() == 0) {
                            b.browserListErrorText.setText(R.string.web_push_no_browsers);
                            Anim.fadeIn(b.browserListErrorText, 500, null);
                            if (app.appConfig.webPushEnabled) {
                                app.appConfig.webPushEnabled = false;
                                app.appConfig.savePending = true;
                            }
                            return;
                        }

                        b.browserList.removeAllViews();

                        LinearLayout.LayoutParams textViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        textViewParams.setMargins(0, 0, Utils.dpToPx(8), 0);
                        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        LinearLayout.LayoutParams tableRowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

                        JsonArray browsers = result.get("browsers").getAsJsonArray();
                        for (JsonElement browserEl: browsers) {
                            JsonObject browser = browserEl.getAsJsonObject();
                            if (browser != null) {
                                //Log.d(TAG, browser.toString());
                                String browserDescription = "(error)";
                                if (browser.get("description") != null) {
                                    browserDescription = browser.get("description").getAsString();
                                }
                                int browserId = -1;
                                if (browser.get("id") != null) {
                                    browserId = browser.get("id").getAsInt();
                                }

                                TableRow browserRow = new TableRow(this);
                                browserRow.setLayoutParams(tableRowParams);

                                TextView browserDescriptionText = new TextView(this);
                                //browserDescriptionText.setLayoutParams(textViewParams);
                                browserDescriptionText.setText(browserDescription);
                                browserDescriptionText.setGravity(Gravity.CENTER_VERTICAL);
                                browserRow.addView(browserDescriptionText);

                                Button browserRemoveButton = new Button(this, null, android.R.attr.buttonStyleSmall);
                                browserRemoveButton.setMinHeight(0);
                                browserRemoveButton.setText(R.string.remove);
                                int finalBrowserId = browserId;
                                browserRemoveButton.setOnClickListener((v -> {
                                    new MaterialDialog.Builder(this)
                                            .title(R.string.are_you_sure)
                                            .content(R.string.web_push_really_remove)
                                            .positiveText(R.string.yes)
                                            .negativeText(R.string.no)
                                            .onPositive(((dialog, which) -> getPairedBrowsers("", finalBrowserId)))
                                            .show();
                                }));
                                browserRow.addView(browserRemoveButton/*, buttonParams*/);

                                b.browserList.addView(browserRow);
                            }
                        }
                        if (!app.appConfig.webPushEnabled) {
                            app.appConfig.webPushEnabled = true;
                            app.appConfig.savePending = true;
                        }
                        Anim.fadeIn(b.browserList, 500, null);
                    });
                }));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showCamera();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, R.string.no_permissions, Toast.LENGTH_SHORT).show();
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        app = (App) getApplicationContext();

        getTheme().applyStyle(Themes.INSTANCE.getAppTheme(), true);

        b = DataBindingUtil.inflate(getLayoutInflater(), R.layout.activity_web_push_config, null, false);
        setContentView(b.getRoot());

        Toolbar toolbar = b.toolbar;
        toolbar.setTitle(R.string.settings_notification_web_push);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);

        mScannerView = b.qrCodeScanner;
        List<BarcodeFormat> formats = new ArrayList<>();
        formats.add(BarcodeFormat.QR_CODE);
        mScannerView.setFormats(formats);
        mScannerView.setAspectTolerance(0.5f);

        b.webPushScanNewButton.setOnClickListener((v -> {
            int result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            if (result == PackageManager.PERMISSION_GRANTED) {
                showCamera();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            }
        }));

        if (app.profile.getRegistration() != REGISTRATION_ENABLED) {
            new MaterialDialog.Builder(this)
                    .title(R.string.web_push_unavailable)
                    .content(R.string.web_push_you_need_to_register)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.what_is_this)
                    .onPositive(((dialog, which) -> {
                        dialog.dismiss();
                        finish();
                    }))
                    .onNegative(((dialog, which) -> {
                        new MaterialDialog.Builder(this)
                                .title(R.string.help)
                                .content(R.string.help_notification_web_push)
                                .positiveText(R.string.ok)
                                .show();
                    }))
                    .dismissListener((dialog -> finish()))
                    .autoDismiss(false)
                    .canceledOnTouchOutside(false)
                    .show();
            b.webPushScanNewButton.setEnabled(false);
        }
        else {
            getPairedBrowsers("", -1);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register ourselves as a handler for scan results.
        //mScannerView.startCamera();          // Start camera on resume
        //showCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        hideCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        // Do something with the result here
        //Log.v(TAG, rawResult.getText()); // Prints scan results
        //Log.v(TAG, rawResult.getBarcodeFormat().toString()); // Prints the scan format (qrcode, pdf417 etc.)

        //Toast.makeText(this, rawResult.getText(), Toast.LENGTH_SHORT).show();

        getPairedBrowsers(rawResult.getText(), -1);

        /*Ion.with(app.getContext())
                .load(app.requestScheme + APP_URL + "main.php?web_push_pair")
                .setBodyParameter("username", (app.profile.autoRegistrationAllowed ? app.profile.registrationUsername : app.appConfig.deviceId))
                .setBodyParameter("app_version_build_type", BuildConfig.BUILD_TYPE)
                .setBodyParameter("app_version_code", Integer.toString(BuildConfig.VERSION_CODE))
                .setBodyParameter("app_version", BuildConfig.VERSION_NAME + " " + BuildConfig.BUILD_TYPE + " (" + BuildConfig.VERSION_CODE + ")")
                .setBodyParameter("device_id", Settings.Secure.getString(app.getContext().getContentResolver(), Settings.Secure.ANDROID_ID))
                .setBodyParameter("device_model", Build.MANUFACTURER+" "+Build.MODEL)
                .setBodyParameter("device_os_version", Build.VERSION.RELEASE)
                .setBodyParameter("fcm_token", app.appConfig.fcmToken)
                .setBodyParameter("browser_fcm", rawResult.getText())
                .asJsonObject()
                .setCallback((e, result) -> {
                    getPairedBrowsers(-1);
                });*/

        hideCamera();

        // If you would like to resume scanning, call this method below:
        //mScannerView.resumeCameraPreview(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (cameraRunning) {
                hideCamera();
                return super.onOptionsItemSelected(item);
            }
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (cameraRunning) {
            hideCamera();
            return;
        }
        super.onBackPressed();
    }
}
