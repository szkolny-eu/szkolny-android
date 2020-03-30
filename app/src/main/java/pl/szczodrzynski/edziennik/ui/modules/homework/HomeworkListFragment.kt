package pl.szczodrzynski.edziennik.ui.modules.homework

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.databinding.HomeworkListBinding
import pl.szczodrzynski.edziennik.getInt
import pl.szczodrzynski.edziennik.ui.modules.base.lazypager.LazyFragment
import pl.szczodrzynski.edziennik.utils.models.Date

class HomeworkListFragment : LazyFragment() {

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: HomeworkListBinding

    private var homeworkDate = HomeworkDate.CURRENT

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = HomeworkListBinding.inflate(inflater)
        return b.root
    }

    override fun onPageCreated(): Boolean {
        if (arguments != null) {
            homeworkDate = arguments.getInt("homeworkDate", HomeworkDate.CURRENT)
        }

        val layoutManager = LinearLayoutManager(context)
        layoutManager.reverseLayout = homeworkDate == HomeworkDate.PAST
        layoutManager.stackFromEnd = homeworkDate == HomeworkDate.PAST

        b.homeworkView.setHasFixedSize(true)
        b.homeworkView.layoutManager = layoutManager
        b.homeworkView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (recyclerView.canScrollVertically(-1)) {
                    setSwipeToRefresh(false)
                }
                if (!recyclerView.canScrollVertically(-1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    setSwipeToRefresh(true)
                }
            }
        })

        val filter = when(homeworkDate) {
            HomeworkDate.CURRENT -> "eventDate >= '" + Date.getToday().stringY_m_d + "'"
            else -> "eventDate < '" + Date.getToday().stringY_m_d + "'"
        }

        app.db.eventDao()
                .getAllByType(App.profileId, Event.TYPE_HOMEWORK, filter)
                .observe(this, Observer { homeworkList ->
                    if (!isAdded) return@Observer

                    if (homeworkList != null && homeworkList.size > 0) {
                        val adapter = HomeworkAdapter(context, homeworkList)
                        b.homeworkView.adapter = adapter
                        b.homeworkView.visibility = View.VISIBLE
                        b.homeworkNoData.visibility = View.GONE
                    } else {
                        b.homeworkView.visibility = View.GONE
                        b.homeworkNoData.visibility = View.VISIBLE
                    }
                })
        return true
    }
}
