package pl.szczodrzynski.edziennik.ui.modules.homework

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.databinding.HomeworkListFragmentBinding
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventDetailsDialog
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventListAdapter
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventManualDialog
import pl.szczodrzynski.edziennik.ui.modules.base.lazypager.LazyFragment
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import pl.szczodrzynski.edziennik.utils.models.Date
import kotlin.coroutines.CoroutineContext

class HomeworkListFragment : LazyFragment(), CoroutineScope {
    companion object {
        private const val TAG = "HomeworkListFragment"
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: HomeworkListFragmentBinding

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = HomeworkListFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onPageCreated(): Boolean { startCoroutineTimer(100L) {
        val homeworkDate = arguments.getInt("homeworkDate", HomeworkDate.CURRENT)

        val today = Date.getToday()
        val filter = when(homeworkDate) {
            HomeworkDate.CURRENT -> "eventDate >= '${today.stringY_m_d}'"
            else -> "eventDate < '${today.stringY_m_d}'"
        }

        val adapter = EventListAdapter(
                activity,
                showWeekDay = true,
                showDate = true,
                showType = false,
                showTime = true,
                showSubject = true,
                onItemClick = {
                    EventDetailsDialog(
                            activity,
                            it
                    )
                },
                onEventEditClick = {
                    EventManualDialog(
                            activity,
                            it.profileId,
                            editingEvent = it
                    )
                }
        )

        app.db.eventDao().getAllByType(App.profileId, Event.TYPE_HOMEWORK, filter).observe(this@HomeworkListFragment, Observer { items ->
            if (!isAdded) return@Observer

            // load & configure the adapter
            adapter.items = items
            if (items.isNotNullNorEmpty() && b.list.adapter == null) {
                b.list.adapter = adapter
                b.list.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context).apply {
                        reverseLayout = homeworkDate == HomeworkDate.PAST
                        stackFromEnd = homeworkDate == HomeworkDate.PAST
                    }
                    addItemDecoration(SimpleDividerItemDecoration(context))
                    addOnScrollListener(onScrollListener)
                }
            }
            adapter.notifyDataSetChanged()

            // show/hide relevant views
            b.progressBar.isVisible = false
            if (items.isNullOrEmpty()) {
                b.list.isVisible = false
                b.noData.isVisible = true
            } else {
                b.list.isVisible = true
                b.noData.isVisible = false
            }
        })
    }; return true }
}
