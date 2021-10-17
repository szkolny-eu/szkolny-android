/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-2.
 */

package pl.szczodrzynski.edziennik.ui.home

import java.io.Serializable

data class HomeCardModel(
        val profileId: Int,
        val cardId: Int
) : Serializable
