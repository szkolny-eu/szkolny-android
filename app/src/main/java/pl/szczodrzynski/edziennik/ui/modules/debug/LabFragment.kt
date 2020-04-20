/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-3.
 */

package pl.szczodrzynski.edziennik.ui.modules.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.databinding.LabFragmentBinding
import pl.szczodrzynski.edziennik.onClick
import kotlin.coroutines.CoroutineContext

class LabFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "LabFragment"
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: LabFragmentBinding

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = LabFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded) return

        b.last10unseen.onClick {
            launch(Dispatchers.Default) {
                val events = app.db.eventDao().getAllNow(App.profileId)
                val ids = events.sortedBy { it.date }.filter { it.type == Event.TYPE_HOMEWORK }.takeLast(10)
                ids.forEach {
                    app.db.metadataDao().setSeen(App.profileId, it, false)
                }
            }
        }

        b.rodo.onClick {
            app.db.teacherDao().query(SimpleSQLiteQuery("UPDATE teachers SET teacherSurname = \"\" WHERE profileId = ${App.profileId}"))
        }

        b.removeHomework.onClick {
            app.db.eventDao().getRawNow("UPDATE events SET homeworkBody = NULL WHERE profileId = ${App.profileId}")
        }
    }
}
