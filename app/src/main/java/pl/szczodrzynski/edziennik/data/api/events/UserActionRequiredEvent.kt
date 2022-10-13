/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-15.
 */

package pl.szczodrzynski.edziennik.data.api.events

import android.os.Bundle

data class UserActionRequiredEvent(val profileId: Int, val type: Int, val params: Bundle?) {
    companion object {
        const val LOGIN_DATA_MOBIDZIENNIK = 101
        const val LOGIN_DATA_LIBRUS = 102
        const val LOGIN_DATA_VULCAN = 104
        const val CAPTCHA_LIBRUS = 202
        const val OAUTH_USOS = 701
    }
}
