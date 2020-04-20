/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-7.
 */

package pl.szczodrzynski.edziennik.utils.managers

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
import kotlin.coroutines.CoroutineContext

class PermissionManager(val app: App) : CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private fun isStoragePermissionGranted() = if (Build.VERSION.SDK_INT >= 23) {
        app.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    fun requestStoragePermission(
            activity: AppCompatActivity,
            @StringRes permissionMessage: Int,
            onSuccess: suspend CoroutineScope.() -> Unit
    ) {
        launch {
            if (isStoragePermissionGranted()) {
                onSuccess()
                return@launch
            }
            val result = activity.awaitAskPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            when {
                result.hasAllGranted() -> onSuccess()
                result.hasRational() -> {
                    MaterialAlertDialogBuilder(activity)
                            .setTitle(R.string.permissions_required)
                            .setMessage(permissionMessage)
                            .setPositiveButton(R.string.ok) { _, _ ->
                                requestStoragePermission(activity, permissionMessage, onSuccess)
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                }
                result.hasPermanentDenied() -> {
                    MaterialAlertDialogBuilder(activity)
                            .setTitle(R.string.permissions_required)
                            .setMessage(R.string.permissions_denied)
                            .setPositiveButton(R.string.ok) { _, _ ->
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = Uri.fromParts("package", app.packageName, null)
                                intent.data = uri
                                activity.startActivity(intent)
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                }
            }
        }
    }
}
