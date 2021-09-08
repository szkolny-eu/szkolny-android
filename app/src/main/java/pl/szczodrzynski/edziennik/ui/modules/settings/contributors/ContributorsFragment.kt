package pl.szczodrzynski.edziennik.ui.modules.settings.contributors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.szkolny.response.ContributorsResponse
import pl.szczodrzynski.edziennik.databinding.ContributorsListFragmentBinding
import pl.szczodrzynski.edziennik.ui.modules.base.lazypager.LazyFragment
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration

class ContributorsFragment : LazyFragment() {
    companion object {
        private const val TAG = "ContributorsFragment"
    }

    private lateinit var app: App
    private lateinit var activity: ContributorsActivity
    private lateinit var b: ContributorsListFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as ContributorsActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = ContributorsListFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onPageCreated(): Boolean {
        val contributorsArray = requireArguments().getParcelableArray("items") as Array<ContributorsResponse.Item>
        val contributors = contributorsArray.toList()
        val quantityPluralRes = requireArguments().getInt("quantityPluralRes")

        val adapter = ContributorsAdapter(activity, contributors, quantityPluralRes)
        b.list.adapter = adapter
        b.list.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(SimpleDividerItemDecoration(context))
            addOnScrollListener(onScrollListener)
        }

        return true
    }
}
