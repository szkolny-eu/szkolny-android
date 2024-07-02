/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-30.
 */

package pl.szczodrzynski.edziennik.ui.template

import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.databinding.TemplateListPageFragmentBinding
import pl.szczodrzynski.edziennik.ext.isNotNullNorEmpty
import pl.szczodrzynski.edziennik.ext.startCoroutineTimer
import pl.szczodrzynski.edziennik.ui.base.lazypager.LazyFragment
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration

class TemplateListPageFragment : LazyFragment<TemplateListPageFragmentBinding, MainActivity>(
    inflater = TemplateListPageFragmentBinding::inflate,
) {

    override fun onPageCreated(): Boolean { startCoroutineTimer(100L) {
        val adapter = TemplateAdapter(activity)

        app.db.notificationDao().getAll().observe(this@TemplateListPageFragment, Observer { items ->
            if (!isAdded) return@Observer

            // load & configure the adapter
            adapter.items = items
            if (items.isNotNullNorEmpty() && b.list.adapter == null) {
                b.list.adapter = adapter
                b.list.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                    addItemDecoration(SimpleDividerItemDecoration(context))
                    addOnScrollListener(onScrollListener)
                }
            }
            adapter.notifyDataSetChanged()
            setSwipeToRefresh(items.isNullOrEmpty())

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
