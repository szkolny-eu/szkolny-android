package pl.szczodrzynski.edziennik.ui.settings.contributors

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import pl.szczodrzynski.edziennik.data.api.szkolny.response.ContributorsResponse
import pl.szczodrzynski.edziennik.databinding.ContributorsListFragmentBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration

class ContributorsFragment : BaseFragment<ContributorsListFragmentBinding, ContributorsActivity>(
    inflater = ContributorsListFragmentBinding::inflate,
) {

    override fun getRefreshScrollingView() = b.list

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        val contributorsArray = requireArguments().getParcelableArray("items") as Array<ContributorsResponse.Item>
        val contributors = contributorsArray.toList()
        val quantityPluralRes = requireArguments().getInt("quantityPluralRes")

        val adapter = ContributorsAdapter(activity, contributors, quantityPluralRes)
        b.list.adapter = adapter
        b.list.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(SimpleDividerItemDecoration(context))
        }
    }
}
