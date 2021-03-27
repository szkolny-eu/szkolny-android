/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-27.
 */

package pl.szczodrzynski.edziennik.utils.managers

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.szkolny.interceptor.Signing

class BuildManager(val app: App) {

    val buildFlavor = BuildConfig.FLAVOR
    val buildType = BuildConfig.BUILD_TYPE
    val buildTimestamp = BuildConfig.BUILD_TIMESTAMP
    val isRelease = !BuildConfig.DEBUG
    val isDebug = BuildConfig.DEBUG
    val isNightly = BuildConfig.VERSION_NAME.contains("nightly")
    val isDaily = BuildConfig.VERSION_NAME.contains("daily")

    val gitHash = BuildConfig.GIT_INFO["hash"]
    val gitVersion = BuildConfig.GIT_INFO["version"]
    val gitBranch = BuildConfig.GIT_INFO["branch"]
    val gitUnstaged = BuildConfig.GIT_INFO["unstaged"]?.split("; ")
    val gitRevCount = BuildConfig.GIT_INFO["revCount"]
    val gitTag = BuildConfig.GIT_INFO["tag"]
    val gitIsDirty = BuildConfig.GIT_INFO["dirty"] !== "false"
    val gitRemotes = BuildConfig.GIT_INFO["remotes"]?.split("; ")

    val isSigned = Signing.appCertificate.md5() == "d8bab5259fda7d72121fe5db526a3d4d"

    val isPlayRelease = isRelease && buildFlavor == "play"
    val isApkRelease = isRelease && buildFlavor == "official"
    val isOfficial = isSigned && (isPlayRelease || isApkRelease)

    val versionName = when {
        isOfficial -> BuildConfig.VERSION_NAME + ", " + BuildConfig.BUILD_TYPE
        isRelease -> "$gitVersion\n$gitBranch"
        else -> BuildConfig.VERSION_NAME
    }

    val versionBadge = when {
        isOfficial && isNightly ->
            "Nightly\n" + BuildConfig.VERSION_NAME.substringAfterLast('.')
        isOfficial && isDaily ->
            "Daily\n" + BuildConfig.VERSION_NAME.substringAfterLast('.')
        isDebug ->
            "Debug\n" + BuildConfig.VERSION_BASE
        !isOfficial ->
            "Unofficial\n" + BuildConfig.VERSION_BASE
        else -> null
    }

    fun showVersionDialog(activity: AppCompatActivity) {
        val yes = activity.getString(R.string.yes)
        val no = activity.getString(R.string.no)

        val fields = mapOf(
            R.string.build_version to BuildConfig.VERSION_BASE,
            R.string.build_official to if (isOfficial) yes else no,
            R.string.build_platform to when {
                isPlayRelease -> activity.getString(R.string.build_platform_play)
                isApkRelease -> activity.getString(R.string.build_platform_apk)
                else -> activity.getString(R.string.build_platform_unofficial)
            },
            R.string.build_branch to gitBranch,
            R.string.build_commit to gitHash?.substring(0, 8),
            R.string.build_dirty to if (gitUnstaged?.isEmpty() == true)
                "-"
            else
                "\t" + gitUnstaged?.join("\n\t"),
            R.string.build_tag to gitTag,
            R.string.build_rev_count to gitRevCount,
            R.string.build_remote to gitRemotes?.join("\n")
        )

        val message = fields.map { (key, value) ->
            activity.getString(key) + ":\n" + value
        }.join("\n\n")

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.build_details)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }
}
