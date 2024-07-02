package pl.szczodrzynski.edziennik.ui.settings.contributors

import androidx.recyclerview.widget.LinearLayoutManager
import pl.szczodrzynski.edziennik.data.api.szkolny.response.ContributorsResponse
import pl.szczodrzynski.edziennik.databinding.ContributorsListFragmentBinding
import pl.szczodrzynski.edziennik.ui.base.lazypager.LazyFragment
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration

class ContributorsFragment : LazyFragment<ContributorsListFragmentBinding, ContributorsActivity>(
    inflater = ContributorsListFragmentBinding::inflate,
) {

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
