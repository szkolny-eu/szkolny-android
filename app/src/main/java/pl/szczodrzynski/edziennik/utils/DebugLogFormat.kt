package pl.szczodrzynski.edziennik.utils

import android.content.Context
import com.hypertrack.hyperlog.LogFormat

class DebugLogFormat(context: Context) : LogFormat(context) {
    override fun getFormattedLogMessage(logLevelName: String?, tag: String?, message: String?, timeStamp: String?, senderName: String?, osVersion: String?, deviceUUID: String?): String {
        return "${timeStamp?.replace("[TZ]".toRegex(), " ")}D/$tag: $message"
    }
}