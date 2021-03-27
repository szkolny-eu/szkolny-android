/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-27.
 */

package pl.szczodrzynski.edziennik.utils.managers

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.data.api.szkolny.interceptor.Signing
import pl.szczodrzynski.edziennik.md5

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
}
