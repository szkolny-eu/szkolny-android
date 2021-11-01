/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-9.
 */

package pl.szczodrzynski.edziennik.utils

import android.os.Bundle

open class PausedNavigationData {

    data class LoadProfile(
        val id: Int,
        val drawerSelection: Int,
        val arguments: Bundle?,
    ) : PausedNavigationData()

    data class LoadTarget(
        val id: Int,
        val arguments: Bundle?,
    ) : PausedNavigationData()
}
