/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-23.
 */

package pl.szczodrzynski.edziennik

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageView
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.ui.modules.login.LoginActivity
import java.io.File
import java.io.FileOutputStream

class MainActivityRequestHandler(val activity: MainActivity) {
    companion object {
        private const val REQUEST_LOGIN_ACTIVITY = 2000
        private const val REQUEST_FILE_HEADER_BACKGROUND = 3000
        private const val REQUEST_FILE_APP_BACKGROUND = 4000
        private const val REQUEST_FILE_PROFILE_IMAGE = 5000
        private const val REQUEST_CROP_HEADER_BACKGROUND = 3100
        private const val REQUEST_CROP_APP_BACKGROUND = 4100
        private const val REQUEST_CROP_PROFILE_IMAGE = 5100
    }

    private val app = activity.app
    private val requestData = mutableMapOf<Int, Any?>()
    private val listeners = mutableMapOf<Int, (data: Any?) -> Unit>()

    fun requestLogin() = activity.startActivityForResult(
        Intent(activity, LoginActivity::class.java),
        REQUEST_LOGIN_ACTIVITY
    )

    fun requestHeaderBackground(listener: (Any?) -> Unit) {
        listeners[REQUEST_FILE_HEADER_BACKGROUND] = listener
        activity.startActivityForResult(
            CropImage.getPickImageChooserIntent(
                activity,
                activity.getString(R.string.pick_image_intent_chooser_title),
                true,
                true
            ),
            REQUEST_FILE_HEADER_BACKGROUND
        )
    }

    fun requestAppBackground(listener: (Any?) -> Unit) {
        listeners[REQUEST_FILE_APP_BACKGROUND] = listener
        activity.startActivityForResult(
            CropImage.getPickImageChooserIntent(
                activity,
                activity.getString(R.string.pick_image_intent_chooser_title),
                true,
                true
            ),
            REQUEST_FILE_APP_BACKGROUND
        )
    }

    fun requestProfileImage(profile: Profile, listener: (Any?) -> Unit) {
        listeners[REQUEST_FILE_PROFILE_IMAGE] = listener
        requestData[REQUEST_FILE_PROFILE_IMAGE] = profile
        activity.startActivityForResult(
            CropImage.getPickImageChooserIntent(
                activity,
                activity.getString(R.string.pick_image_intent_chooser_title),
                true,
                true
            ),
            REQUEST_FILE_PROFILE_IMAGE
        )
    }

    private fun getFileInfo(uri: Uri): Pair<String, String?> {
        if (uri.scheme == "file") {
            return (uri.lastPathSegment ?: "unknown") to null
        }
        val cursor = activity.contentResolver.query(
            uri,
            null,
            null,
            null,
            null,
            null
        )

        return cursor?.use {
            if (it.moveToFirst()) {
                val name = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                val mimeIndex = it.getColumnIndex("mime_type")
                val mimeType = if (mimeIndex != -1) it.getString(mimeIndex) else null

                name to mimeType
            }
            else
                null
        } ?: "unknown" to null
    }

    private fun shouldCrop(uri: Uri): Boolean {
        val (filename, mimeType) = getFileInfo(uri)
        return !filename.endsWith(".gif") && mimeType?.endsWith("/gif") != true
    }

    private fun saveFile(uri: Uri, name: String): String {
        val (filename, _) = getFileInfo(uri)
        val extension = filename.substringAfterLast('.')
        val file = File(activity.filesDir, "$name.$extension")
        activity.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }

    fun handleResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK)
            return
        var uri = CropImage.getPickImageResultUri(activity, data)
        when (requestCode) {
            REQUEST_LOGIN_ACTIVITY -> {
                if (!app.config.loginFinished)
                    activity.finish()
                else {
                    activity.handleIntent(data?.extras)
                }
            }
            REQUEST_FILE_HEADER_BACKGROUND -> {
                if (uri == null)
                    return // TODO: 2021-03-24 if the app returns no data 
                if (shouldCrop(uri)) {
                    val intent = CropImage.activity(uri)
                        .setAspectRatio(512, 288)
                        .setGuidelines(CropImageView.Guidelines.ON_TOUCH)
                        .setAllowFlipping(true)
                        .setAllowRotation(true)
                        .setRequestedSize(512, 288)
                        .getIntent(activity)
                    activity.startActivityForResult(intent, REQUEST_CROP_HEADER_BACKGROUND)
                } else {
                    val path = saveFile(uri, "header")
                    app.config.ui.headerBackground = path
                    listeners.remove(REQUEST_FILE_HEADER_BACKGROUND)?.invoke(path)
                }
            }
            REQUEST_FILE_APP_BACKGROUND -> {
                if (uri == null)
                    return
                if (shouldCrop(uri)) {
                    val intent = CropImage.activity(uri)
                        .setGuidelines(CropImageView.Guidelines.ON_TOUCH)
                        .setAllowFlipping(true)
                        .setAllowRotation(true)
                        .getIntent(activity)
                    activity.startActivityForResult(intent, REQUEST_CROP_APP_BACKGROUND)
                } else {
                    val path = saveFile(uri, "background")
                    app.config.ui.appBackground = path
                    listeners.remove(REQUEST_FILE_APP_BACKGROUND)?.invoke(path)
                }
            }
            REQUEST_FILE_PROFILE_IMAGE -> {
                if (uri == null)
                    return
                if (shouldCrop(uri)) {
                    val intent = CropImage.activity(uri)
                        .setAspectRatio(1, 1)
                        .setCropShape(CropImageView.CropShape.OVAL)
                        .setGuidelines(CropImageView.Guidelines.ON_TOUCH)
                        .setAllowFlipping(true)
                        .setAllowRotation(true)
                        .setRequestedSize(512, 512)
                        .getIntent(activity)
                    activity.startActivityForResult(intent, REQUEST_CROP_PROFILE_IMAGE)
                } else {
                    val profile = requestData.remove(REQUEST_FILE_PROFILE_IMAGE) as? Profile ?: return
                    val path = saveFile(uri, "profile${profile.id}")
                    profile.image = path
                    listeners.remove(REQUEST_FILE_PROFILE_IMAGE)?.invoke(profile)
                }
            }
            REQUEST_CROP_HEADER_BACKGROUND -> {
                uri = CropImage.getActivityResult(data)?.uri ?: return
                val path = saveFile(uri, "header")
                app.config.ui.headerBackground = path
                listeners.remove(REQUEST_FILE_HEADER_BACKGROUND)?.invoke(path)
            }
            REQUEST_CROP_APP_BACKGROUND -> {
                uri = CropImage.getActivityResult(data)?.uri ?: return
                val path = saveFile(uri, "background")
                app.config.ui.appBackground = path
                listeners.remove(REQUEST_FILE_APP_BACKGROUND)?.invoke(path)
            }
            REQUEST_CROP_PROFILE_IMAGE -> {
                uri = CropImage.getActivityResult(data)?.uri ?: return
                val profile = requestData.remove(REQUEST_FILE_PROFILE_IMAGE) as? Profile ?: return
                val path = saveFile(uri, "profile${profile.id}")
                profile.image = path
                listeners.remove(REQUEST_FILE_PROFILE_IMAGE)?.invoke(profile)
            }
        }
    }
}
