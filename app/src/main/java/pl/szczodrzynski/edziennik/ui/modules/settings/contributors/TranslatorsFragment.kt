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
import pl.szczodrzynski.edziennik.ui.modules.settings.contributors.adapters.TranslatorsAdapter

class TranslatorsFragment(translators: List<ContributorItem>?) : Fragment() {

    private val mTranslators: List<ContributorItem>?

    init {
        mTranslators = translators
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_translators, container, false)
        val listView = view.findViewById<ListView>(R.id.translatorsListView)

        listView.adapter = TranslatorsAdapter(context, mTranslators)

        return view
    }
}
