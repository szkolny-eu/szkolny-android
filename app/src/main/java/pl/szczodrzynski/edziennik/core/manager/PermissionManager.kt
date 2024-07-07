/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-7.
 */

package pl.szczodrzynski.edziennik.core.manager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.qifan.powerpermission.coroutines.awaitAskPermissions
import com.qifan.powerpermission.data.hasAllGranted
import com.qifan.powerpermission.data.hasPermanentDenied
import com.qifan.powerpermission.data.hasRational
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog
import kotlin.coroutines.CoroutineContext

class PermissionManager(val app: App) : CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private fun isPermissionGranted(name: String) =
        if (Build.VERSION.SDK_INT >= 23)
            app.checkSelfPermission(name) == PackageManager.PERMISSION_GRANTED
        else
            true
    val isNotificationPermissionGranted by lazy {
        if (Build.VERSION.SDK_INT >= 33) {
            app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    private fun openPermissionSettings(activity: AppCompatActivity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", app.packageName, null)
        intent.data = uri
        activity.startActivity(intent)
    }

    private fun requestPermission(
        activity: AppCompatActivity,
        @StringRes permissionMessage: Int,
        isRequired: Boolean = true,
        permissionName: String,
        onSuccess: suspend CoroutineScope.() -> Unit
    ) {
        launch {
            if (isPermissionGranted(permissionName)) {
                onSuccess()
                return@launch
            }
            val result = activity.awaitAskPermissions(permissionName)
            when {
                result.hasAllGranted() -> onSuccess()
                result.hasRational() -> {
                    if (!isRequired) {
                        onSuccess()
                        return@launch
                    }
                    SimpleDialog<Unit>(activity) {
                        title(R.string.permissions_required)
                        message(permissionMessage)
                        positive(R.string.ok) {
                            requestPermission(
                                activity,
                                permissionMessage,
                                isRequired,
                                permissionName,
                                onSuccess
                            )
                        }
                        negative(R.string.cancel)
                    }.show()
                }
                result.hasPermanentDenied() -> {
                    if (!isRequired) {
                        onSuccess()
                        return@launch
                    }
                    SimpleDialog<Unit>(activity) {
                        title(R.string.permissions_required)
                        message(R.string.permissions_denied)
                        positive(R.string.ok) {
                            openPermissionSettings(activity)
                        }
                        negative(R.string.cancel)
                    }.show()
                }
            }
        }
    }
    fun requestNotificationsPermission(
        activity: AppCompatActivity,
        @StringRes permissionMessage: Int,
        isRequired: Boolean = false,
        onSuccess: suspend CoroutineScope.() -> Unit
    ) = requestPermission(
        activity,
        permissionMessage,
        isRequired,
        Manifest.permission.POST_NOTIFICATIONS,
        onSuccess
    )

    fun requestStoragePermission(
        activity: AppCompatActivity,
        @StringRes permissionMessage: Int,
        isRequired: Boolean = true,
        onSuccess: suspend CoroutineScope.() -> Unit
    ) = requestPermission(
        activity,
        permissionMessage,
        isRequired,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        onSuccess
    )

    fun requestCameraPermission(
        activity: AppCompatActivity,
        @StringRes permissionMessage: Int,
        isRequired: Boolean = true,
        onSuccess: suspend CoroutineScope.() -> Unit
    ) = requestPermission(
        activity,
        permissionMessage,
        isRequired,
        Manifest.permission.CAMERA,
        onSuccess
    )
}
