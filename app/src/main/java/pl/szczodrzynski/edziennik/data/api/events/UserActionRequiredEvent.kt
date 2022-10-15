/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-15.
 */

package pl.szczodrzynski.edziennik.data.api.events

import android.os.Bundle

data class UserActionRequiredEvent(
    val profileId: Int?,
    val type: Type,
    val params: Bundle,
    val errorText: Int,
) {
    enum class Type {
        RECAPTCHA,
        OAUTH,
    }
}
