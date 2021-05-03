package pl.szczodrzynski.edziennik.ui.modules.settings.contributors

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.szkolny.request.ContributorItem
import pl.szczodrzynski.edziennik.ui.modules.settings.contributors.adapters.ContributorsAdapter

class ContributorsFragment(contributors: List<ContributorItem>?) : Fragment() {

    private val mContributors: List<ContributorItem>?

    init {
        mContributors = contributors
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_contributors, container, false)
        val listView = view.findViewById<ListView>(R.id.contributorsListView)

        listView.adapter = ContributorsAdapter(context, mContributors)

        return view
    }
}
