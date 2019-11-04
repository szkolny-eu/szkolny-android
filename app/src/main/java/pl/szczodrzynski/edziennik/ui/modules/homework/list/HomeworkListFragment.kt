/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-4
 */

package pl.szczodrzynski.edziennik.ui.modules.homework.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.modules.events.EventFull
import pl.szczodrzynski.edziennik.databinding.HomeworkListBinding
import pl.szczodrzynski.edziennik.ui.base.BaseFragment
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventManualDialog
import pl.szczodrzynski.edziennik.utils.Themes

class HomeworkListFragment : BaseFragment<HomeworkListPresenter>(), HomeworkListView {

    override lateinit var app: App

    private lateinit var activity: MainActivity
    private lateinit var b: HomeworkListBinding

    private lateinit var homeworkAdapter: HomeworkListAdapter

    override val presenter: HomeworkListPresenter = HomeworkListPresenter()

    override val viewLifecycle: Lifecycle
        get() = lifecycle

    companion object {
        private const val ARGUMENT_KEY = "homeworkDate"

        fun newInstance(homeworkDate: Int) = HomeworkListFragment().apply {
            arguments = Bundle().apply { putInt(ARGUMENT_KEY, homeworkDate) }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        presenter.onAttachView(this, arguments?.getInt(ARGUMENT_KEY))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        if (context == null)
            return null
        app = activity.application as App
        context!!.theme.applyStyle(Themes.appTheme, true)
        if (app.profile == null)
            return inflater.inflate(R.layout.fragment_loading, container, false)
        // activity, context and profile is valid
        b = HomeworkListBinding.inflate(inflater)
        return b.root
    }

    override fun initView() {
        // TODO check if app, activity, b can be null
        if (app.profile == null || !isAdded)
            return

        val layoutManager = LinearLayoutManager(context)
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true

        homeworkAdapter = HomeworkListAdapter()
        homeworkAdapter.onItemEditClick = presenter::onItemEditClick

        b.homeworkView.apply {
            setHasFixedSize(true)
            this.layoutManager = layoutManager
            adapter = homeworkAdapter
        }
    }

    override fun updateData(data: List<EventFull>) {
        homeworkAdapter.apply {
            homeworkList.apply {
                clear()
                addAll(data)
            }
            notifyDataSetChanged()
        }
    }

    override fun showContent(show: Boolean) {
        b.homeworkView.visibility = if (show) VISIBLE else GONE
    }

    override fun showNoData(show: Boolean) {
        b.homeworkNoData.visibility = if (show) VISIBLE else GONE
    }

    override fun showEditHomeworkDialog(homework: EventFull) {
        EventManualDialog(context).show(app, homework, null, null, EventManualDialog.DIALOG_HOMEWORK)
    }
}
