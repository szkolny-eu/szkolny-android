/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-18.
 */

package pl.szczodrzynski.edziennik.ui.settings.cards

import android.content.Intent
import android.media.MediaPlayer
import android.widget.Toast
import com.danielstone.materialaboutlibrary.items.MaterialAboutItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.szkolny.font.SzkolnyFont
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.szkolny.response.Update
import pl.szczodrzynski.edziennik.ext.after
import pl.szczodrzynski.edziennik.ui.dialogs.ChangelogDialog
import pl.szczodrzynski.edziennik.ui.settings.SettingsCard
import pl.szczodrzynski.edziennik.ui.settings.SettingsLicenseActivity
import pl.szczodrzynski.edziennik.ui.settings.SettingsUtil
import pl.szczodrzynski.edziennik.ui.settings.contributors.ContributorsActivity
import pl.szczodrzynski.edziennik.utils.Utils
import kotlin.coroutines.CoroutineContext

class SettingsAboutCard(util: SettingsUtil) : SettingsCard(util), CoroutineScope {

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private var clickCounter = 0
    private val mediaPlayer by lazy {
        MediaPlayer.create(activity, R.raw.ogarnij_sie)
    }

    override fun buildCard(): MaterialAboutCard =
        util.createCard(
            null,
            items = listOf(),
            itemsMore = listOf(),
            backgroundColor = 0xff1976d2.toInt(),
            theme = R.style.AppTheme_Dark
        ).also {
            it.items.addAll(getItems(it))
        }

    override fun getItems() = listOf<MaterialAboutItem>()
    override fun getItemsMore() = listOf<MaterialAboutItem>()

    private val versionDetailsItem by lazy {
        util.createActionItem(
            text = R.string.settings_about_version_details_text,
            subText = R.string.settings_about_version_details_subtext,
            icon = CommunityMaterial.Icon.cmd_cellphone_information,
            onClick = { _ ->
                app.buildManager.showVersionDialog(activity)
            }
        )
    }

    private fun getItems(card: MaterialAboutCard) = listOf(
        util.createTitleItem(),

        util.createActionItem(
            text = R.string.settings_about_version_text,
            icon = CommunityMaterial.Icon2.cmd_information_outline,
            onClick = { item ->
                if (!card.items.contains(versionDetailsItem)) {
                    card.items.after(item, versionDetailsItem)
                    util.refresh()
                }

                clickCounter++
                if (clickCounter < 7)
                    Toast.makeText(activity, "\uD83D\uDE02", Toast.LENGTH_SHORT).show()
                item.subText =
                    BuildConfig.VERSION_NAME + ", " + BuildConfig.BUILD_TYPE + " \uD83D\uDCA3"
                util.refresh()
                if (clickCounter >= 7) {
                    mediaPlayer.start()
                    clickCounter = 0
                }
            }
        ).also {
            it.subText = BuildConfig.VERSION_NAME + ", " + BuildConfig.BUILD_TYPE
        },

        util.createActionItem(
            text = R.string.settings_about_contributors_text,
            subText = R.string.settings_about_contributors_subtext,
            icon = CommunityMaterial.Icon.cmd_account_group_outline
        ) {
            activity.startActivity(Intent(activity, ContributorsActivity::class.java))
        },

        util.createMoreItem(card, items = listOf(
            util.createActionItem(
                text = R.string.settings_about_changelog_text,
                icon = CommunityMaterial.Icon3.cmd_radar
            ) {
                ChangelogDialog(activity).show()
            },

            util.createActionItem(
                text = R.string.settings_about_update_text,
                subText = R.string.settings_about_update_subtext,
                icon = CommunityMaterial.Icon3.cmd_update
            ) {
                launch {
                    val channel = if (App.devMode)
                        Update.Type.BETA
                    else
                        Update.Type.RC
                    val result = app.updateManager.checkNow(channel, notify = false)
                    val update = result.getOrNull()
                    // the dialog is shown by MainActivity (EventBus)
                    when {
                        result.isFailure -> Toast.makeText(app, app.getString(R.string.notification_cant_check_update), Toast.LENGTH_SHORT).show()
                        update == null -> Toast.makeText(app, app.getString(R.string.notification_no_update), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )),

        util.createSectionItem(
            text = R.string.see_also
        ),

        util.createActionItem(
            text = R.string.settings_about_privacy_policy_text,
            icon = CommunityMaterial.Icon3.cmd_shield_outline
        ) {
            Utils.openUrl(activity, "https://szkolny.eu/privacy-policy")
        },

        util.createActionItem(
            text = R.string.settings_about_discord_text,
            subText = R.string.settings_about_discord_subtext,
            icon = SzkolnyFont.Icon.szf_discord_outline
        ) {
            Utils.openUrl(activity, "https://szkolny.eu/discord")
        },

        util.createActionItem(
            text = R.string.settings_about_github_text,
            subText = R.string.settings_about_github_subtext,
            icon = SzkolnyFont.Icon.szf_github_face
        ) {
            Utils.openUrl(activity, "https://szkolny.eu/github/android")
        },

        util.createMoreItem(card, items = listOfNotNull(
            util.createActionItem(
                text = R.string.settings_about_homepage_text,
                subText = R.string.settings_about_homepage_subtext,
                icon = CommunityMaterial.Icon.cmd_earth
            ) {
                Utils.openUrl(activity, "https://szkolny.eu/")
            },

            util.createActionItem(
                text = R.string.settings_about_licenses_text,
                icon = CommunityMaterial.Icon.cmd_code_braces
            ) {
                activity.startActivity(Intent(activity, SettingsLicenseActivity::class.java))
            },

            if (App.devMode)
                util.createActionItem(
                    text = R.string.settings_about_crash_text,
                    subText = R.string.settings_about_crash_subtext,
                    icon = CommunityMaterial.Icon.cmd_bug_outline
                ) {
                    throw RuntimeException("MANUAL CRASH")
                }
            else
                null
        ))
    )
}
