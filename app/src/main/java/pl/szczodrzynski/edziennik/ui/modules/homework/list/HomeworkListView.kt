/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-4
 */

package pl.szczodrzynski.edziennik.ui.modules.homework.list

import androidx.lifecycle.Lifecycle
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.modules.events.EventFull
import pl.szczodrzynski.edziennik.ui.base.BaseView

interface HomeworkListView : BaseView {

    var app: App

    val viewLifecycle: Lifecycle

    fun initView()

    fun updateData(data: List<EventFull>)

    fun showContent(show: Boolean)

    fun showNoData(show: Boolean)

    fun showEditHomeworkDialog(homework: EventFull)
}
