/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-18.
 */

package pl.szczodrzynski.edziennik.sync

import android.app.DownloadManager
import android.app.IntentService
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.szkolny.response.Update
import pl.szczodrzynski.edziennik.utils.Utils
import java.io.File

class UpdateDownloaderService : IntentService(UpdateDownloaderService::class.java.simpleName) {
    companion object {
        private const val TAG = "UpdateDownloaderService"
        private var downloadId = 0L
        private var downloadFilename = ""
    }

    private fun tryUpdateWithGooglePlay(update: Update): Boolean {
        if (!update.isOnGooglePlay)
            return false
        return try {
            Utils.openGooglePlay(this, application.packageName)
            true
        }
        catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    class DownloadProgressReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                return
            if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != downloadId)
                return
            val app = context.applicationContext as App

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !app.permissionChecker.canRequestApkInstall()) {
                app.permissionChecker.requestApkInstall()
                return
            }

            val file = File(app.getExternalFilesDir(null), downloadFilename)
            val installIntent = Intent(Intent.ACTION_VIEW)
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            installIntent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val apkUri = FileProvider.getUriForFile(app, "${app.packageName}.provider", file)
                installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive")

                val resInfoList = app.packageManager.queryIntentActivities(installIntent, PackageManager.MATCH_DEFAULT_ONLY)
                for (resolveInfo in resInfoList) {
                    val packageName = resolveInfo.activityInfo.packageName
                    app.grantUriPermission(packageName, apkUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            app.startActivity(installIntent)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        val app = application as App
        val update = App.config.update ?: return

        if (tryUpdateWithGooglePlay(update))
            return

        if (update.downloadUrl == null) {
            Toast.makeText(app, "Nie można pobrać tej aktualizacji. Pobierz ręcznie z Google Play.", Toast.LENGTH_LONG).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !app.permissionChecker.canRequestApkInstall()) {
            app.permissionChecker.requestApkInstall()
            return
        }

        (app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(app.notificationChannelsManager.updates.id)

        val dir: File? = app.getExternalFilesDir(null)
        if (dir?.isDirectory == true) {
            dir.listFiles()?.forEach {
                it.delete()
            }
        }

        val uri = Uri.parse(update.downloadUrl)
        downloadFilename = "${update.versionName}.apk"

        val downloadManager = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(uri)
        request.setTitle(app.getString(R.string.app_name)+" "+update.versionName)
        request.setDescription(app.getString(R.string.notification_downloading_update))
        try {
            request.setDestinationInExternalFilesDir(app, null, downloadFilename)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            Toast.makeText(app, "Nie można znaleźć katalogu docelowego. Pobierz aktualizację ręcznie z Google Play.", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(app, "Pobieranie aktualizacji Szkolny.eu ${update.versionName}", Toast.LENGTH_LONG).show()
        downloadId = downloadManager.enqueue(request)
    }
}
