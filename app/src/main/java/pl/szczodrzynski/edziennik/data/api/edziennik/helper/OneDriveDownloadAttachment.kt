/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-7.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.helper

import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.FileCallbackHandler
import im.wangchao.mhttp.callback.TextCallbackHandler
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.ERROR_ONEDRIVE_DOWNLOAD
import pl.szczodrzynski.edziennik.data.api.ERROR_REQUEST_FAILURE
import pl.szczodrzynski.edziennik.data.api.SYSTEM_USER_AGENT
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.utils.Utils
import java.io.File

class OneDriveDownloadAttachment(
        app: App,
        fileUrl: String,
        val onSuccess: (file: File) -> Unit,
        val onProgress: (written: Long, total: Long) -> Unit,
        val onError: (apiError: ApiError) -> Unit
) {
    companion object {
        private const val TAG = "OneDriveDownloadAttachment"
    }

    init {
        Request.builder()
                .url(fileUrl)
                .userAgent(SYSTEM_USER_AGENT)
                .withClient(app.httpLazy)
                .callback(object : TextCallbackHandler() {
                    override fun onSuccess(text: String, response: Response) {
                        val location = response.headers().get("Location")
                        if (location?.contains("onedrive.live.com/redir?resid=") != true) {
                            onError(ApiError(TAG, ERROR_ONEDRIVE_DOWNLOAD)
                                    .withApiResponse(text)
                                    .withResponse(response))
                            return
                        }
                        val url = location
                                .replace("onedrive.live.com/redir?resid=", "storage.live.com/items/")
                                .replace("&", "?")
                        downloadFile(url)
                    }

                    override fun onFailure(response: Response, throwable: Throwable) {
                        onError(ApiError(TAG, ERROR_REQUEST_FAILURE)
                                .withResponse(response)
                                .withThrowable(throwable))
                    }
                })
                .build()
                .enqueue()
    }

    private fun downloadFile(url: String) {
        val targetFile = Utils.getStorageDir()

        val callback = object : FileCallbackHandler(targetFile) {
            override fun onSuccess(file: File?, response: Response?) {
                if (file == null) {
                    onError(ApiError(TAG, ERROR_ONEDRIVE_DOWNLOAD)
                            .withResponse(response))
                    return
                }

                try {
                    onSuccess(file)
                } catch (e: Exception) {
                    onError(ApiError(TAG, ERROR_ONEDRIVE_DOWNLOAD)
                            .withResponse(response)
                            .withThrowable(e))
                }
            }

            override fun onProgress(bytesWritten: Long, bytesTotal: Long) {
                try {
                    this@OneDriveDownloadAttachment.onProgress(bytesWritten, bytesTotal)
                } catch (e: Exception) {
                    onError(ApiError(TAG, ERROR_ONEDRIVE_DOWNLOAD)
                            .withThrowable(e))
                }
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                onError(ApiError(TAG, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        Request.builder()
                .url(url)
                .userAgent(SYSTEM_USER_AGENT)
                .callback(callback)
                .build()
                .enqueue()
    }
}
