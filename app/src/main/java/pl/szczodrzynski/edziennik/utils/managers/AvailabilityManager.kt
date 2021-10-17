/*
 * Copyright (c) Kuba Szczodrzyński 2021-9-18.
 */

package pl.szczodrzynski.edziennik.utils.managers

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.data.api.szkolny.response.RegisterAvailabilityStatus
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.ext.currentTimeUnix
import pl.szczodrzynski.edziennik.ext.toApiError

class AvailabilityManager(val app: App) {
    companion object {
        private const val TAG = "AvailabilityManager"
    }

    private val api = SzkolnyApi(app)

    data class Error(
        val type: Type,
        val status: RegisterAvailabilityStatus?,
        val apiError: ApiError?
    ) {
        companion object {
            fun notAvailable(status: RegisterAvailabilityStatus) =
                Error(Type.NOT_AVAILABLE, status, null)

            fun apiError(apiError: ApiError) =
                Error(Type.API_ERROR, null, apiError)

            fun noApiAccess() =
                Error(Type.NO_API_ACCESS, null, null)
        }

        enum class Type {
            NOT_AVAILABLE,
            API_ERROR,
            NO_API_ACCESS,
        }
    }

    fun check(profile: Profile, cacheOnly: Boolean = false): Error? {
        return check(profile.registerName, cacheOnly)
    }

    fun check(loginType: Int, cacheOnly: Boolean = false): Error? {
        val registerName = when (loginType) {
            LOGIN_TYPE_LIBRUS -> "librus"
            LOGIN_TYPE_VULCAN -> "vulcan"
            LOGIN_TYPE_IDZIENNIK -> "idziennik"
            LOGIN_TYPE_MOBIDZIENNIK -> "mobidziennik"
            LOGIN_TYPE_PODLASIE -> "podlasie"
            LOGIN_TYPE_EDUDZIENNIK -> "edudziennik"
            else -> "unknown"
        }
        return check(registerName, cacheOnly)
    }

    fun check(registerName: String, cacheOnly: Boolean = false): Error? {
        if (!app.config.apiAvailabilityCheck)
            return null
        val status = app.config.sync.registerAvailability[registerName]
        if (status != null && status.nextCheckAt > currentTimeUnix()) {
            return reportStatus(status)
        }
        if (cacheOnly) {
            return reportStatus(status)
        }

        return try {
            val availability = api.getRegisterAvailability()
            app.config.sync.registerAvailability = availability
            reportStatus(availability[registerName])
        } catch (e: Throwable) {
            reportApiError(e)
        }
    }

    private fun reportStatus(status: RegisterAvailabilityStatus?): Error? {
        if (status == null)
            return null
        if (!status.available || status.minVersionCode > BuildConfig.VERSION_CODE)
            return Error.notAvailable(status)
        return null
    }

    private fun reportApiError(throwable: Throwable): Error {
        val apiError = throwable.toApiError(TAG)
        if (apiError.errorCode == ERROR_API_INVALID_SIGNATURE) {
            app.config.sync.registerAvailability = mapOf()
            return Error.noApiAccess()
        }
        return Error.apiError(apiError)
    }
}
