/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-27.
 */

package pl.szczodrzynski.edziennik.utils.managers

import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import okhttp3.Request
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.szkolny.interceptor.Signing
import pl.szczodrzynski.edziennik.ui.modules.base.BuildInvalidActivity

class BuildManager(val app: App) : CoroutineScope {

    override val coroutineContext = Job() + Dispatchers.Main

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
    var gitRemote: String? = ""
    var gitAuthor: String? = ""

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

        val colorOnBackground = R.attr.colorOnBackground.resolveAttr(activity)
        val mtrlGreen = R.color.md_green_500.resolveColor(activity)
        val mtrlYellow = R.color.md_yellow_700.resolveColor(activity)
        val mtrlRed = R.color.md_red_500.resolveColor(activity)

        val fields = mapOf(
            R.string.build_version to BuildConfig.VERSION_BASE,
            R.string.build_official to if (isOfficial)
                yes.asColoredSpannable(mtrlGreen)
            else
                no.asColoredSpannable(mtrlRed),
            R.string.build_platform to when {
                isPlayRelease -> activity.getString(R.string.build_platform_play)
                isApkRelease -> activity.getString(R.string.build_platform_apk)
                else -> activity
                    .getString(R.string.build_platform_unofficial)
                    .asColoredSpannable(mtrlYellow)
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
            TextUtils.concat(
                activity
                    .getString(key)
                    .asBoldSpannable()
                    .asColoredSpannable(colorOnBackground),
                ":\n",
                value
            )
        }.concat("\n\n")

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.build_details)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    enum class InvalidBuildReason(
        val message: Int,
        val color: Int,
        val isCritical: Boolean = true
    ) {
        NO_REMOTE_REPO(R.string.build_invalid_no_remote_repo, R.color.md_orange_500),
        NO_COMMIT_HASH(R.string.build_invalid_no_commit_hash, R.color.md_orange_500),
        REMOTE_NO_COMMIT(R.string.build_invalid_remote_no_commit, R.color.md_red_500),
        OFFICIAL_UNSIGNED(R.string.build_invalid_official_unsigned, R.color.md_red_500),
        UNSTAGED_CHANGES(R.string.build_invalid_unstaged_changes, R.color.md_amber_800),
        DEBUG(R.string.build_invalid_debug, R.color.md_yellow_500, false),
        VALID(R.string.build_valid_unofficial, R.color.md_yellow_500, false)
    }

    private fun getRemoteRepo(): String? {
        if (gitRemotes == null)
            return null
        return gitRemotes.map {
            it.substringAfter("(").substringBefore(")")
        }.firstOrNull {
            it != "szkolny-eu/szkolny-android"
        }
    }

    private suspend fun validateRepo(
        repo: String,
        commitHash: String
    ) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repo/git/commits/$commitHash")
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        val call = app.http.newCall(request)

        val response = runCatching {
            call.execute()
        }.getOrNull() ?: return@withContext false

        if (response.code() != 200)
            return@withContext false

        val json = runCatching {
            response.body()?.string()?.toJsonObject()
        }.getOrNull() ?: return@withContext false

        val sha = json.getString("sha")
        if (sha != commitHash)
            return@withContext false

        val author = json.getJsonObject("author") ?: return@withContext false
        val name = author.getString("name")
        val email = author.getString("email")
        gitAuthor = "$name <$email>"

        return@withContext true
    }

    fun validateBuild(activity: AppCompatActivity) {
        launch {
            gitRemote = getRemoteRepo()
            if (gitRemote == null && !isDebug) {
                invalidateBuild(activity, null, InvalidBuildReason.NO_REMOTE_REPO)
                return@launch
            }
            if (gitHash == null) {
                invalidateBuild(activity, null, InvalidBuildReason.NO_COMMIT_HASH)
                return@launch
            }

            // official, signed package
            if (isOfficial)
                return@launch

            // seems official, but unsigned
            if (isPlayRelease || isApkRelease) {
                invalidateBuild(activity, null, InvalidBuildReason.OFFICIAL_UNSIGNED)
                return@launch
            }

            // debug build, invalidate once
            if (isDebug) {
                if (app.config.validation != "debug${Signing.appCertificate}".md5()) {
                    app.config.validation = "debug${Signing.appCertificate}".md5()
                    invalidateBuild(activity, null, InvalidBuildReason.DEBUG)
                }
                return@launch
            }

            // release version with unstaged changes
            if (gitIsDirty) {
                invalidateBuild(activity, null, InvalidBuildReason.UNSTAGED_CHANGES)
                return@launch
            }

            val validation = Signing.appCertificate + gitHash + gitRemotes?.join(";")

            // app already validated
            if (app.config.validation == validation.md5())
                return@launch

            val dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.please_wait)
                .setMessage(R.string.build_validate_progress)
                .setCancelable(false)
                .show()

            val isRepoValid = if (app.config.validation == "invalid$gitRemote$gitHash".md5())
                false
            else
                validateRepo(gitRemote!!, gitHash)

            // release build with no public repository or not published changes
            if (!isRepoValid) {
                app.config.validation = "invalid$gitRemote$gitHash".md5()
                invalidateBuild(activity, dialog, InvalidBuildReason.REMOTE_NO_COMMIT)
                return@launch
            }

            // release, unofficial, published build
            app.config.validation = validation.md5()
            invalidateBuild(activity, dialog, InvalidBuildReason.VALID)
        }
    }

    private fun invalidateBuild(
        activity: AppCompatActivity,
        progressDialog: AlertDialog?,
        reason: InvalidBuildReason
    ) {
        progressDialog?.dismiss()

        val message = activity.getString(
            reason.message,
            gitRemote,
            gitBranch,
            gitAuthor
        )

        val color = reason.color.resolveColor(activity)

        val intent = Intent(
            activity,
            BuildInvalidActivity::class.java,
            "message" to message,
            "color" to color,
            "isCritical" to reason.isCritical
        )

        activity.startActivity(intent)

        if (reason.isCritical)
            activity.finish()
    }
}
