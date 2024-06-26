/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-6.
 */

package pl.szczodrzynski.edziennik.ui.widgets;

import static pl.szczodrzynski.edziennik.ext.DataExtensionsKt.filterOutArchived;

import android.app.Activity;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.SeekBar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;

import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.db.entity.Profile;
import pl.szczodrzynski.edziennik.data.enums.LoginType;
import pl.szczodrzynski.edziennik.databinding.DialogWidgetConfigBinding;
import pl.szczodrzynski.edziennik.databinding.WidgetProfileDialogItemBinding;
import pl.szczodrzynski.edziennik.ui.widgets.luckynumber.WidgetLuckyNumberProvider;
import pl.szczodrzynski.edziennik.ui.widgets.notifications.WidgetNotificationsProvider;
import pl.szczodrzynski.edziennik.ui.widgets.timetable.WidgetTimetableProvider;

public class WidgetConfigActivity extends Activity {

    public static final String TAG = "WidgetConfigActivity";
    private App app;
    private int mAppWidgetId;
    private List<Profile> profileList;
    private DialogWidgetConfigBinding b;
    public static final int WIDGET_TIMETABLE = 0;
    public static final int WIDGET_NOTIFICATIONS = 1;
    public static final int WIDGET_LUCKY_NUMBER = 2;
    private int widgetType = -1;
    private int profileId = -1;
    private String profileName = null;
    private boolean darkTheme = false;
    private boolean bigStyle;
    private float opacity = 0.8f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new ColorDrawable(0));

        app = (App) getApplication();
        if (app == null)
            return;

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        setResult(RESULT_CANCELED);
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

            String className = AppWidgetManager.getInstance(app).getAppWidgetInfo(mAppWidgetId).provider.getClassName();
            if (className.contains("WidgetTimetable"))
                widgetType = WIDGET_TIMETABLE;
            else if (className.contains("WidgetNotifications"))
                widgetType = WIDGET_NOTIFICATIONS;
            else if (className.contains("WidgetLuckyNumber"))
                widgetType = WIDGET_LUCKY_NUMBER;

            if (widgetType == -1)
                finish();

            if (widgetType == WIDGET_LUCKY_NUMBER)
                opacity = 0.6f;
            else
                opacity = 0.8f;

            AsyncTask.execute(() -> {
                profileList = App.Companion.getDb().profileDao().getAllNow();
                profileList = filterOutArchived(profileList);

                if (widgetType == WIDGET_NOTIFICATIONS)
                    this.runOnUiThread(this::configure);
                else
                    this.runOnUiThread(this::selectProfile);
            });
        } else {
            finish();
        }
    }

    private void selectProfile() {
        if (profileList.size() > 1 && widgetType != WIDGET_LUCKY_NUMBER) {
            profileList.add(
                    new Profile(-1,
                            0,
                            LoginType.TEMPLATE,
                            getString(R.string.widget_config_all_profiles),
                            null,
                            "",
                            "",
                            null,
                            new JsonObject()
                    )
            );
        }

        ListAdapter adapter = new ListAdapter() {
            @Override public boolean areAllItemsEnabled() { return true; }
            @Override public boolean isEnabled(int position) { return true; }
            @Override public void registerDataSetObserver(DataSetObserver observer) { }
            @Override public void unregisterDataSetObserver(DataSetObserver observer) { }
            @Override public boolean hasStableIds() { return true; }
            @Override public int getItemViewType(int position) { return 0; }
            @Override public int getViewTypeCount() { return 1; }
            @Override public boolean isEmpty() { return false; }

            @Override public int getCount() { return profileList.size(); }
            @Override public Object getItem(int position) { return profileList.get(position); }

            @Override
            public long getItemId(int position) {
                return profileList.get(position).getId();
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                WidgetProfileDialogItemBinding b;
                if (convertView == null) {
                    b = WidgetProfileDialogItemBinding.inflate(getLayoutInflater(), null, false);
                }
                else {
                    b = WidgetProfileDialogItemBinding.bind(convertView);
                }
                Profile profile = profileList.get(position);

                b.name.setText(profile.getName());
                b.subname.setText(profile.getSubname());
                b.subname.setVisibility(profile.getSubname() == null ? View.GONE : View.VISIBLE);
                b.image.setVisibility(profile.getId() == -1 ? View.GONE : View.VISIBLE);
                if (profile.getId() == -1)
                    b.image.setImageDrawable(null);
                else
                    b.image.setImageDrawable(profile.getImageDrawable(WidgetConfigActivity.this));

                b.getRoot().setOnClickListener(v -> {
                    profileId = profile.getId();
                    profileName = profile.getName();
                    configure();
                });

                return b.getRoot();
            }
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.choose_profile)
                .setAdapter(adapter, null)
                .setOnDismissListener(dialog -> finish())
                .show();
    }

    private void configure() {
        b = DialogWidgetConfigBinding.inflate(getLayoutInflater(), null, false);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.widget_config_activity_customize)
                .setView(b.getRoot())
                .setOnDismissListener(dialog -> finish())
                .setPositiveButton(R.string.ok, ((dialog, which) -> {
                    WidgetConfig config = new WidgetConfig(profileId, bigStyle, darkTheme, opacity);
                    JsonObject configs = app.getConfig().getWidgetConfigs();
                    configs.add(Integer.toString(mAppWidgetId), app.getGson().toJsonTree(config));
                    app.getConfig().setWidgetConfigs(configs);

                    Intent refreshIntent;
                    switch (widgetType) {
                        default:
                        case WIDGET_TIMETABLE:
                            refreshIntent = new Intent(app, WidgetTimetableProvider.class);
                            break;
                        case WIDGET_NOTIFICATIONS:
                            refreshIntent = new Intent(app, WidgetNotificationsProvider.class);
                            break;
                        case WIDGET_LUCKY_NUMBER:
                            refreshIntent = new Intent(app, WidgetLuckyNumberProvider.class);
                            break;
                    }
                    refreshIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    // TODO: 2019-05-11 updating only one widget does not seem to work
                    //int[] ids = AppWidgetManager.getInstance(app).getAppWidgetIds(new ComponentName(app, WidgetTimetable.class));
                    //refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                    refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetId});
                    app.sendBroadcast(refreshIntent);

                    Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                    setResult(RESULT_OK, resultValue);
                    finish();
                }))
                .setNegativeButton(R.string.cancel, null)
                .show();

        b.setProfileName(profileName);

        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        try {
            b.wallpaper.setImageDrawable(wallpaperManager.getDrawable());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        b.theme.check(R.id.themeLight);
        b.theme.setOnCheckedChangeListener((group, checkedId) -> {
            darkTheme = checkedId == R.id.themeDark;
            updatePreview();
        });

        b.bigStyle.setChecked(bigStyle);
        b.bigStyle.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            bigStyle = isChecked;
            updatePreview();
        }));

        b.opacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser)
                    return;
                b.opacityText.setText(getString(R.string.widget_config_activity_opacity_format, progress));
                opacity = (float) progress / 100.0f;
                b.widgetPreview.setColorFilter(new PorterDuffColorFilter((int)(0x01000000L * (long)(255f * opacity)), PorterDuff.Mode.DST_IN));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        int progress = (int)(opacity * 100.0f);
        b.opacity.setProgress(progress);
        b.opacityText.setText(getString(R.string.widget_config_activity_opacity_format, progress));
        b.widgetPreview.setColorFilter(new PorterDuffColorFilter((int)(0x01000000L * (long)(255f * opacity)), PorterDuff.Mode.DST_IN));

        updatePreview();
    }

    private void updatePreview() {
        int resource;
        switch (widgetType) {
            default:
            case WIDGET_TIMETABLE:
                if (darkTheme) {
                    if (bigStyle) resource = R.drawable.widget_timetable_dark_big_preview;
                    else resource = R.drawable.widget_timetable_dark_preview;
                } else {
                    if (bigStyle) resource = R.drawable.widget_timetable_big_preview;
                    else resource = R.drawable.widget_timetable_preview;
                }
                break;
            case WIDGET_NOTIFICATIONS:
                if (darkTheme) {
                    if (bigStyle) resource = R.drawable.widget_notifications_dark_big_preview;
                    else resource = R.drawable.widget_notifications_dark_preview;
                } else {
                    if (bigStyle) resource = R.drawable.widget_notifications_big_preview;
                    else resource = R.drawable.widget_notifications_preview;
                }
                break;
            case WIDGET_LUCKY_NUMBER:
                if (darkTheme) {
                    if (bigStyle) resource = R.drawable.widget_lucky_number_dark_big_preview;
                    else resource = R.drawable.widget_lucky_number_dark_preview;
                } else {
                    if (bigStyle) resource = R.drawable.widget_lucky_number_big_preview;
                    else resource = R.drawable.widget_lucky_number_preview;
                }
                break;
        }
        b.widgetPreview.setImageResource(resource);
    }
}
