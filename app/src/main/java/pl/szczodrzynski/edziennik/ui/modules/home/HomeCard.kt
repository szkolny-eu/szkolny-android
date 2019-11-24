/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-23.
 */

package pl.szczodrzynski.edziennik.ui.modules.home

interface HomeCard {
    fun bind(position: Int, holder: HomeCardAdapter.ViewHolder)
    fun unbind(position: Int, holder: HomeCardAdapter.ViewHolder)
}