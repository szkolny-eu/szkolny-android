/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-4
 */

package pl.szczodrzynski.edziennik.ui.modules.homework

import android.os.AsyncTask
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.data.db.modules.metadata.MetadataDao
import pl.szczodrzynski.edziennik.ui.base.BasePresenter
import javax.inject.Inject

class HomeworkPresenter @Inject constructor(
        private val metadataDao: MetadataDao
) : BasePresenter<HomeworkView>() {

    override fun onAttachView(view: HomeworkView) {
        super.onAttachView(view)
        view.initView()
    }

    fun onPageSelected(position: Int) {
        view?.setPageSelection(position)
    }

    fun onAddEventClick() {
        view?.apply {
            closeBottomSheet()
            showAddHomeworkDialog()
        }
    }

    fun onMarkAsReadClick() {
        view?.apply {
            closeBottomSheet()
            AsyncTask.execute {
                metadataDao.setAllSeen(App.profileId, Metadata.TYPE_HOMEWORK, true)
            }
            showMessage(markAsReadSuccessString)
        }
    }

    fun onHomeworkAddFabClick() {
        view?.showAddHomeworkDialog()
    }
}
