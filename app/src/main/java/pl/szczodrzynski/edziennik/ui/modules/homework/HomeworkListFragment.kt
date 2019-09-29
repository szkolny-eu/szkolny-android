package pl.szczodrzynski.edziennik.ui.modules.homework

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.HomeworkListBinding
import pl.szczodrzynski.edziennik.data.db.modules.events.Event
import pl.szczodrzynski.edziennik.getInt
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.Themes

class HomeworkListFragment : Fragment() {

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: HomeworkListBinding

    private var homeworkDate = HomeworkDate.CURRENT

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // TODO check if app, activity, b can be null
        if (app.profile == null || !isAdded)
            return

        if (arguments != null) {
            homeworkDate = arguments.getInt("homeworkDate", HomeworkDate.CURRENT)
        }

        val layoutManager = LinearLayoutManager(context)
        layoutManager.reverseLayout = false
        layoutManager.stackFromEnd = false

        b.homeworkView.setHasFixedSize(true)
        b.homeworkView.layoutManager = layoutManager

        val filter = when(homeworkDate) {
            HomeworkDate.CURRENT -> "eventDate > '" + Date.getToday().stringY_m_d + "'"
            else -> "eventDate <= '" + Date.getToday().stringY_m_d + "'"
        }

        app.db.eventDao()
                .getAllByType(App.profileId, Event.TYPE_HOMEWORK, filter)
                .observe(this, Observer { homeworkList ->
                    if (app.profile == null || !isAdded) return@Observer

                    if (homeworkList != null && homeworkList.size > 0) {
                        val adapter = HomeworkAdapter(context, homeworkList.reversed())
                        b.homeworkView.adapter = adapter
                        b.homeworkView.visibility = View.VISIBLE
                        b.homeworkNoData.visibility = View.GONE
                    } else {
                        b.homeworkView.visibility = View.GONE
                        b.homeworkNoData.visibility = View.VISIBLE
                    }
                })
    }
}
