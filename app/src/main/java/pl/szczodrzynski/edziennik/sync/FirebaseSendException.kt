/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-11.
 */

package pl.szczodrzynski.edziennik.sync

class FirebaseSendException(reason: String?) : Exception(reason) {
    companion object {
        const val ERROR_UNKNOWN = 0
        const val ERROR_INVALID_PARAMETERS = 1
        const val ERROR_SIZE = 2
        const val ERROR_TTL_EXCEEDED = 3
        const val ERROR_TOO_MANY_MESSAGES = 4
    }

    val errorCode = when (reason) {
        "service_not_available" -> ERROR_TTL_EXCEEDED
        "toomanymessages" -> ERROR_TOO_MANY_MESSAGES
        "invalid_parameters" -> ERROR_INVALID_PARAMETERS
        "messagetoobig" -> ERROR_SIZE
        "missing_to" -> ERROR_INVALID_PARAMETERS
        else -> ERROR_UNKNOWN
    }
}
