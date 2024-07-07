/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-9.
 */

package pl.szczodrzynski.edziennik.utils

import android.os.Bundle
import pl.szczodrzynski.edziennik.data.enums.NavTarget

data class PausedNavigationData(
    val profileId: Int?,
    val navTarget: NavTarget?,
    val args: Bundle?,
)
