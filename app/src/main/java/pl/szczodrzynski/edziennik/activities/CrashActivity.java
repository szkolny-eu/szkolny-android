package pl.szczodrzynski.edziennik.activities;

/*
 * Copyright 2014-2017 Eduard Ereza Martínez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;
import cat.ereza.customactivityoncrash.config.CaocConfig;
import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.BuildConfig;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.network.ServerRequest;
import pl.szczodrzynski.edziennik.utils.Themes;

import static pl.szczodrzynski.edziennik.App.APP_URL;
import static pl.szczodrzynski.edziennik.datamodels.Profile.REGISTRATION_ENABLED;

public final class CrashActivity extends AppCompatActivity {

    private App app;

    @SuppressLint("PrivateResource")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.app = (App)getApplication();
        setTheme(Themes.INSTANCE.getAppTheme());

        setContentView(R.layout.activity_crash);

        final CaocConfig config = CustomActivityOnCrash.getConfigFromIntent(getIntent());

        if (config == null) {
            //This should never happen - Just finish the activity to avoid a recursive crash.
            finish();
            return;
        }

        //Close/restart button logic:
        //If a class if set, use restart.
        //Else, use close and just finish the app.
        //It is recommended that you follow this logic if implementing a custom error activity.
        Button restartButton = findViewById(R.id.crash_restart_btn);
        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomActivityOnCrash.restartApplication(CrashActivity.this, config);
            }
        });


        Button devMessageButton = findViewById(R.id.crash_dev_message_btn);
        devMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(CrashActivity.this, CrashGtfoActivity.class);
                startActivity(i);
            }
        });

        final Button reportButton = findViewById(R.id.crash_report_btn);
        reportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!app.networkUtils.isOnline())
                {
                    new MaterialDialog.Builder(CrashActivity.this)
                            .title(R.string.network_you_are_offline_title)
                            .content(R.string.network_you_are_offline_text)
                            .positiveText(R.string.ok)
                            .show();
                }
                else
                {
                    //app.networkUtils.setSelfSignedSSL(CrashActivity.this, null);
                    new ServerRequest(app, app.requestScheme + APP_URL + "main.php?report", "CrashActivity")
                            .setBodyParameter("base64_encoded", Base64.encodeToString(getErrorString(getIntent(), true).getBytes(), Base64.DEFAULT))
                            .run((e, result) -> {
                                if (result != null)
                                {
                                    if (result.get("success").getAsBoolean()) {
                                        Toast.makeText(CrashActivity.this, getString(R.string.crash_report_sent), Toast.LENGTH_SHORT).show();
                                        reportButton.setEnabled(false);
                                        reportButton.setTextColor(getResources().getColor(android.R.color.darker_gray));
                                    }
                                    else {
                                        Toast.makeText(CrashActivity.this, getString(R.string.crash_report_cannot_send) + ": " + result.get("reason").getAsString(), Toast.LENGTH_LONG).show();
                                    }
                                }
                                else
                                {
                                    Toast.makeText(CrashActivity.this, getString(R.string.crash_report_cannot_send)+" JsonObject equals null", Toast.LENGTH_LONG).show();
                                }
                            });
                }
            }
        });

        Button moreInfoButton = findViewById(R.id.crash_details_btn);
        moreInfoButton.setOnClickListener(v -> new MaterialDialog.Builder(CrashActivity.this)
                .title(R.string.crash_details)
                .content(Html.fromHtml(getErrorString(getIntent(), false)))
                .typeface(null, "RobotoMono-Regular.ttf")
                .positiveText(R.string.close)
                .neutralText(R.string.copy_to_clipboard)
                .onNeutral((dialog, which) -> copyErrorToClipboard())
                .show());

        String errorInformation = CustomActivityOnCrash.getAllErrorDetailsFromIntent(CrashActivity.this, getIntent());
        if (errorInformation.contains("MANUAL CRASH"))
        {
            findViewById(R.id.crash_notice).setVisibility(View.GONE);
            findViewById(R.id.crash_report_btn).setVisibility(View.GONE);
            findViewById(R.id.crash_feature).setVisibility(View.VISIBLE);
        }
        else
        {
            findViewById(R.id.crash_notice).setVisibility(View.VISIBLE);
            findViewById(R.id.crash_report_btn).setVisibility(View.VISIBLE);
            findViewById(R.id.crash_feature).setVisibility(View.GONE);
        }
    }

    private String getErrorString(Intent intent, boolean plain) {
        // build a string containing the stack trace and the device name + user's registration data
        String contentPlain = "Crash report:\n\n"+CustomActivityOnCrash.getStackTraceFromIntent(intent);
        String content = "<small>"+contentPlain+"</small>";
        content = content.replaceAll(getPackageName(), "<font color='#4caf50'>"+getPackageName()+"</font>");
        content = content.replaceAll("\n", "<br>");

        contentPlain += "\n"+Build.MANUFACTURER+"\n"+Build.BRAND+"\n"+Build.MODEL+"\n"+Build.DEVICE+"\n";
        if (app.profile != null && app.profile.getRegistration() == REGISTRATION_ENABLED) {
            contentPlain += "U: "+app.profile.getUsernameId()+"\nS: "+ app.profile.getStudentNameLong() +"\n";
        }
        contentPlain += BuildConfig.VERSION_NAME+" "+BuildConfig.BUILD_TYPE;

        return plain ? contentPlain : content;
    }

    private void copyErrorToClipboard() {
        String errorInformation = CustomActivityOnCrash.getAllErrorDetailsFromIntent(CrashActivity.this, getIntent());

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        //Are there any devices without clipboard...?
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText(getString(R.string.customactivityoncrash_error_activity_error_details_clipboard_label), errorInformation);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(CrashActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
        }
    }
}