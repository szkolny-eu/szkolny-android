/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-12.
 */

package pl.szczodrzynski.edziennik.ui.modules.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.databinding.TemplateListPageFragmentBinding
import pl.szczodrzynski.edziennik.startCoroutineTimer
import pl.szczodrzynski.edziennik.ui.modules.base.lazypager.LazyFragment
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import kotlin.coroutines.CoroutineContext

class LabProfileFragment : LazyFragment(), CoroutineScope {
    companion object {
        private const val TAG = "LabProfileFragment"
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: TemplateListPageFragmentBinding

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = TemplateListPageFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onPageCreated(): Boolean { startCoroutineTimer(100L) {
        val adapter = LabJsonAdapter(activity)
        val json = JsonObject().also { json ->
            json.add("app.profile", app.profile.studentData)
            json.add("app.config", JsonParser().parse(app.gson.toJson(app.config.values)))
            EdziennikTask.profile?.let {
                json.add("API.profile", it.studentData)
            } ?: {
                json.addProperty("API.profile", "null")
            }()
            EdziennikTask.loginStore?.let {
                json.add("API.loginStore", it.data)
            } ?: {
                json.addProperty("API.loginStore", "null")
            }()
        }
        adapter.items = LabJsonAdapter.expand(json, 0)

        b.list.adapter = adapter
        b.list.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(SimpleDividerItemDecoration(context))
            addOnScrollListener(onScrollListener)
        }

        // show/hide relevant views
        b.progressBar.isVisible = false
        b.list.isVisible = true
        b.noData.isVisible = false

    }; return true }
}
