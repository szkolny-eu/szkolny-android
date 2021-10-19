/*
 * Copyright (c) Antoni Czaplicki 2021-10-15.
 */

package pl.szczodrzynski.edziennik.ui.teachers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.databinding.TeachersListFragmentBinding
import pl.szczodrzynski.edziennik.ext.isNotNullNorEmpty
import pl.szczodrzynski.edziennik.ext.startCoroutineTimer
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import kotlin.coroutines.CoroutineContext

class TeachersListFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "TeachersListFragment"
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: TeachersListFragmentBinding

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = TeachersListFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { startCoroutineTimer(100L) {
        if (!isAdded) return@startCoroutineTimer

        val adapter = TeachersAdapter(activity)

        app.db.teacherDao().getAllTeachers(App.profileId).observe(this@TeachersListFragment, Observer { items ->
            if (!isAdded) return@Observer

            // load & configure the adapter
            adapter.items = items
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
    }}
}
