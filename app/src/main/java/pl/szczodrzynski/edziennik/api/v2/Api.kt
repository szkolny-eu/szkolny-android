/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-21.
 */

package pl.szczodrzynski.edziennik.api.v2

import com.crashlytics.android.Crashlytics
import pl.szczodrzynski.edziennik.api.AppError
import pl.szczodrzynski.edziennik.api.v2.models.Data

open class Api(open val data: Data) {
    fun finishWithError(error: AppError) {
        try {
            data.saveData()
        } catch (e: Exception) {
            Crashlytics.logException(e)
        }

    }
}