/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-4
 */

package pl.szczodrzynski.edziennik.ui.modules.homework.list

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.modules.events.Event
import pl.szczodrzynski.edziennik.data.db.modules.events.EventDao
import pl.szczodrzynski.edziennik.data.db.modules.events.EventFull
import pl.szczodrzynski.edziennik.ui.base.BasePresenter
import pl.szczodrzynski.edziennik.ui.modules.homework.HomeworkDate
import pl.szczodrzynski.edziennik.utils.models.Date
import javax.inject.Inject

class HomeworkListPresenter @Inject constructor(
        private val app: App,
        private val eventDao: EventDao
) : BasePresenter<HomeworkListView>() {

    fun onAttachView(view: HomeworkListView, homeworkDate: Int?) {
        super.onAttachView(view)
        view.initView()
        loadData(homeworkDate ?: HomeworkDate.CURRENT)
    }

    private fun loadData(homeworkDate: Int) {
        val today = Date.getToday().stringY_m_d

        val filter = when (homeworkDate) {
            HomeworkDate.CURRENT -> "eventDate > '$today'"
            else -> "eventDate <= '$today'"
        }

        view?.run {
            eventDao.getAllByType(App.profileId, Event.TYPE_HOMEWORK, filter)
                    .observe({ viewLifecycle }, { homeworkList ->
                        if (app.profile == null) return@observe

                        if (homeworkList != null && homeworkList.size > 0) {
                            updateData(homeworkList)
                            showContent(true)
                            showNoData(false)
                        } else {
                            showContent(false)
                            showNoData(true)
                        }
                    })
        }
    }

    fun onItemEditClick(homework: EventFull) {
        view?.showEditHomeworkDialog(homework)
    }
}
