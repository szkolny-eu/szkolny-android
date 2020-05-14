/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-14
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.helper

import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.FileCallbackHandler
import pl.szczodrzynski.edziennik.data.api.ERROR_FILE_DOWNLOAD
import pl.szczodrzynski.edziennik.data.api.ERROR_REQUEST_FAILURE
import pl.szczodrzynski.edziennik.data.api.SYSTEM_USER_AGENT
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.utils.Utils
import java.io.File

class DownloadAttachment(
        fileUrl: String,
        val onSuccess: (file: File) -> Unit,
        val onProgress: (written: Long, total: Long) -> Unit,
        val onError: (apiError: ApiError) -> Unit
) {
    companion object {
        private const val TAG = "DownloadAttachment"
    }

    init {
        val targetFile = Utils.getStorageDir()

        val callback = object : FileCallbackHandler(targetFile) {
            override fun onSuccess(file: File?, response: Response?) {
                if (file == null) {
                    onError(ApiError(TAG, ERROR_FILE_DOWNLOAD)
                            .withResponse(response))
                    return
                }

                try {
                    onSuccess(file)
                } catch (e: Exception) {
                    onError(ApiError(TAG, ERROR_FILE_DOWNLOAD)
                            .withResponse(response)
                            .withThrowable(e))
                }
            }

            override fun onProgress(bytesWritten: Long, bytesTotal: Long) {
                try {
                    this@DownloadAttachment.onProgress(bytesWritten, bytesTotal)
                } catch (e: Exception) {
                    onError(ApiError(TAG, ERROR_FILE_DOWNLOAD)
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
                .url(fileUrl)
                .userAgent(SYSTEM_USER_AGENT)
                .callback(callback)
                .build()
                .enqueue()
    }
}
