package pl.szczodrzynski.edziennik.widgets;

import android.app.Activity;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.SeekBar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListAdapter;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListItem;

import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.WidgetTimetable;
import pl.szczodrzynski.edziennik.data.db.entity.Profile;
import pl.szczodrzynski.edziennik.databinding.DialogWidgetConfigBinding;
import pl.szczodrzynski.edziennik.widgets.luckynumber.WidgetLuckyNumber;
import pl.szczodrzynski.edziennik.widgets.notifications.WidgetNotifications;

import static pl.szczodrzynski.edziennik.ExtensionsKt.filterOutArchived;

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
                profileList = app.db.profileDao().getAllNow();
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
        MaterialSimpleListAdapter adapter =
                new MaterialSimpleListAdapter((dialog, index1, item) -> {
                    profileId = (int) item.getId();
                    profileName = item.toString();
                    configure();
                });

        for (Profile profile : profileList) {
            adapter.add(
                    new MaterialSimpleListItem.Builder(this)
                            .id(profile.getId())
                            .content(profile.getName())
                            .icon(profile.getImageDrawable(this))
                            .backgroundColor(Color.WHITE)
                            .build());
        }
        if (profileList.size() > 1) {
            adapter.add(
                    new MaterialSimpleListItem.Builder(this)
                            .id(-1)
                            .content(R.string.widget_config_all_profiles)
                            .backgroundColor(Color.WHITE)
                            .build());
        }
        new MaterialDialog.Builder(this)
                .title(R.string.choose_profile)
                .adapter(adapter, null)
                .dismissListener(dialog -> finish())
                .show();
    }

    private void configure() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(R.string.widget_config_activity_customize)
                .customView(R.layout.dialog_widget_config, true)
                .dismissListener(dialog1 -> finish())
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .onPositive(((dialog1, which) -> {
                    app.appConfig.widgetTimetableConfigs.put(mAppWidgetId, new WidgetConfig(profileId, bigStyle, darkTheme, opacity));
                    app.saveConfig("widgetTimetableConfigs");

                    Intent refreshIntent;
                    switch (widgetType) {
                        default:
                        case WIDGET_TIMETABLE:
                            refreshIntent = new Intent(app.getContext(), WidgetTimetable.class);
                            break;
                        case WIDGET_NOTIFICATIONS:
                            refreshIntent = new Intent(app.getContext(), WidgetNotifications.class);
                            break;
                        case WIDGET_LUCKY_NUMBER:
                            refreshIntent = new Intent(app.getContext(), WidgetLuckyNumber.class);
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
                .show();

        b = DialogWidgetConfigBinding.bind(dialog.getCustomView());

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
