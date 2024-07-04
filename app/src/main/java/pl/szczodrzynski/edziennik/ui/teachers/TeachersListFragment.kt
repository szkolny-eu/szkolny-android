/*
 * Copyright (c) Antoni Czaplicki 2021-10-15.
 */

package pl.szczodrzynski.edziennik.ui.teachers

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.databinding.TeachersListFragmentBinding
import pl.szczodrzynski.edziennik.ext.isNotNullNorEmpty
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.ui.messages.MessagesUtils
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration

class TeachersListFragment : BaseFragment<TeachersListFragmentBinding, MainActivity>(
    inflater = TeachersListFragmentBinding::inflate,
) {

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        val adapter = TeachersAdapter(activity)

        adapter.subjectList = withContext(Dispatchers.IO) {
            app.db.subjectDao().getAllNow(App.profileId)
        }

        app.db.teacherDao().getAllTeachers(App.profileId).observe(viewLifecycleOwner, Observer { items ->
            if (!isAdded) return@Observer

            // load & configure the adapter
            adapter.items = items.sortedWith(compareBy(
                { it.subjects.isEmpty() },
                { it.type == 0 },
            ))
            adapter.items.forEach {
                it.image = it.image ?: MessagesUtils.getProfileImage(48, 24, 16, 12, 1, it.fullName)
            }
            if (items.isNotNullNorEmpty() && b.list.adapter == null) {
                b.list.adapter = adapter
                b.list.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                    addItemDecoration(SimpleDividerItemDecoration(context))
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
    }
}
