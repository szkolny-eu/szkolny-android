/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-18.
 */

package pl.szczodrzynski.edziennik.ui.settings.cards

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.provider.Settings
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ext.after
import pl.szczodrzynski.edziennik.ext.getSyncInterval
import pl.szczodrzynski.edziennik.core.work.SyncWorker
import pl.szczodrzynski.edziennik.core.work.UpdateWorker
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.ui.dialogs.settings.NotificationFilterDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.QuietHoursConfigDialog
import pl.szczodrzynski.edziennik.ui.dialogs.settings.SyncIntervalDialog
import pl.szczodrzynski.edziennik.ui.settings.SettingsCard
import pl.szczodrzynski.edziennik.ui.settings.SettingsUtil
import pl.szczodrzynski.edziennik.utils.models.Time

class SettingsSyncCard(util: SettingsUtil) : SettingsCard(util) {

    override fun buildCard() = util.createCard(
        R.string.settings_card_sync_title,
        items = ::getItems,
        itemsMore = ::getItemsMore,
    )

    private fun getQuietHours(): String {
        if (configGlobal.sync.quietHoursStart == null) {
            configGlobal.sync.quietHoursStart = Time(22, 30, 0)
        }
        if (configGlobal.sync.quietHoursEnd == null) {
            configGlobal.sync.quietHoursEnd = Time(6, 30, 0)
        }

        return activity.getString(
            if (configGlobal.sync.quietHoursStart!! > configGlobal.sync.quietHoursEnd!!)
                R.string.settings_sync_quiet_hours_subtext_next_day_format
            else
                R.string.settings_sync_quiet_hours_subtext_format,
            configGlobal.sync.quietHoursStart?.stringHM,
            configGlobal.sync.quietHoursEnd?.stringHM
        )
    }

    private val syncWifiItem by lazy {
        util.createPropertyItem(
            text = R.string.settings_sync_wifi_text,
            subText = R.string.settings_sync_wifi_subtext,
            icon = CommunityMaterial.Icon3.cmd_wifi_strength_2,
            value = configGlobal.sync.onlyWifi
        ) { _, it ->
            configGlobal.sync.onlyWifi = it
            SyncWorker.rescheduleNext(app)
        }
    }

    override fun getItems(card: MaterialAboutCard) = listOfNotNull(
        util.createPropertyActionItem(
            text = R.string.settings_sync_sync_interval_text,
            subText = R.string.settings_sync_sync_interval_subtext_disabled,
            icon = CommunityMaterial.Icon.cmd_download_outline,
            value = configGlobal.sync.enabled,
            onChange = { item, value ->
                // When calling onChange from the onClick listener below
                // a list refresh is requested, the adapter refreshes
                // all view holders, changing the state of the switch
                // view, thus calling onChange again, causing it to
                // try to recursively refresh the list, therefore
                // crashing the app. To avoid this, the method will
                // continue only if the checked state is different
                // from the saved value, which should only happen
                // when clicking the switch manually or when called
                // by onClick, **once** (because onClick doesn't
                // update the config value - we let the switch
                // listener do it). Then there comes a different problem,
                // when onClick changes the subText and the onChange
                // listener returns because the boolean value
                // is unchanged, leaving the list not refreshed.
                // To solve this, a list refresh is also requested
                // in onClick, when the config value is the same
                // as the new switch value, which would normally
                // cause the onChange method to exit here.
                if (value == configGlobal.sync.enabled)
                    return@createPropertyActionItem

                if (value) {
                    card.items.after(item, syncWifiItem)
                } else {
                    card.items.remove(syncWifiItem)
                }
                util.refresh()

                configGlobal.sync.enabled = value
                SyncWorker.rescheduleNext(app)
            },
            onClick = { item ->
                SyncIntervalDialog(activity, onChangeListener = {
                    item.subTextChecked = activity.getSyncInterval(configGlobal.sync.interval)
                    item.isChecked = true
                    item.onCheckedChangedAction.onCheckedChanged(item, true)
                    if (configGlobal.sync.enabled)
                        util.refresh()
                }).show()
            }
        ).also {
            it.subTextChecked = activity.getSyncInterval(configGlobal.sync.interval)
        },

        if (configGlobal.sync.enabled)
            syncWifiItem
        else
            null,

        util.createActionItem(
            text = R.string.settings_profile_notifications_text,
            subText = R.string.settings_profile_notifications_subtext,
            icon = CommunityMaterial.Icon2.cmd_filter_outline
        ) {
            NotificationFilterDialog(activity).show()
        },

        util.createPropertyActionItem(
            text = R.string.settings_sync_quiet_hours_text,
            subText = R.string.settings_sync_quiet_hours_subtext_disabled,
            icon = CommunityMaterial.Icon.cmd_bell_sleep_outline,
            value = configGlobal.sync.quietHoursEnabled,
            onChange = { _, value ->
                configGlobal.sync.quietHoursEnabled = value
            },
            onClick = { item ->
                QuietHoursConfigDialog(activity, onChangeListener = {
                    item.subTextChecked = getQuietHours()
                    item.isChecked = configGlobal.sync.quietHoursEnabled
                    util.refresh()
                })
            }
        ).also {
            it.subTextChecked = getQuietHours()
        },

        util.createActionItem(
            text = R.string.settings_sync_web_push_text,
            subText = R.string.settings_sync_web_push_subtext,
            icon = CommunityMaterial.Icon2.cmd_laptop
        ) {
            activity.navigate(navTarget = NavTarget.WEB_PUSH)
        }
    )

    override fun getItemsMore(card: MaterialAboutCard) = listOfNotNull(
        util.createPropertyItem(
            text = R.string.settings_sync_updates_text,
            icon = CommunityMaterial.Icon.cmd_cellphone_arrow_down,
            value = configGlobal.sync.notifyAboutUpdates
        ) { _, it ->
            configGlobal.sync.notifyAboutUpdates = it
            UpdateWorker.rescheduleNext(app)
        },

        if (SDK_INT >= VERSION_CODES.KITKAT)
            util.createActionItem(
                text = R.string.settings_sync_notifications_settings_text,
                subText = R.string.settings_sync_notifications_settings_subtext,
                icon = CommunityMaterial.Icon.cmd_cog_outline
            ) {
                val channel = app.notificationManager.data.key
                val intent = Intent().apply {
                    when {
                        SDK_INT >= VERSION_CODES.O -> {
                            action = Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                            putExtra(Settings.EXTRA_APP_PACKAGE, app.packageName)
                            putExtra(Settings.EXTRA_CHANNEL_ID, channel)
                            addFlags(FLAG_ACTIVITY_NEW_TASK)
                        }
                        SDK_INT >= VERSION_CODES.LOLLIPOP -> {
                            action = "android.settings.APP_NOTIFICATION_SETTINGS"
                            putExtra("app_package", app.packageName)
                            putExtra("app_uid", app.applicationInfo.uid)
                        }
                        else -> {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            addCategory(Intent.CATEGORY_DEFAULT)
                            data = Uri.parse("package:" + app.packageName)
                        }
                    }
                }
                activity.startActivity(intent)
            }
        else
            null
    )
}
