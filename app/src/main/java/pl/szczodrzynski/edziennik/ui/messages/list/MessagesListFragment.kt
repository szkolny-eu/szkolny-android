/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-4.
 */

package pl.szczodrzynski.edziennik.ui.messages.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.databinding.MessagesListFragmentBinding
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ext.getInt
import pl.szczodrzynski.edziennik.ext.startCoroutineTimer
import pl.szczodrzynski.edziennik.ui.base.enums.NavTarget
import pl.szczodrzynski.edziennik.ui.base.lazypager.LazyFragment
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import kotlin.coroutines.CoroutineContext

class MessagesListFragment : LazyFragment(), CoroutineScope {
    companion object {
        private const val TAG = "MessagesListFragment"
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: MessagesListFragmentBinding
    private lateinit var adapter: MessagesAdapter

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here
    private val manager
        get() = app.messageManager
    var teachers = listOf<Teacher>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = MessagesListFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onPageCreated(): Boolean { startCoroutineTimer(100L) {
        val messageType = arguments.getInt("messageType", Message.TYPE_RECEIVED)
        var recyclerViewState =
            arguments?.getParcelable<LinearLayoutManager.SavedState>("recyclerViewState")
        val searchText = arguments?.getString("searchText")

        teachers = withContext(Dispatchers.Default) {
            app.db.teacherDao().getAllNow(App.profileId)
        }

        adapter = MessagesAdapter(activity, teachers, onMessageClick = {
            val (target, args) =
                if (it.isDraft) {
                    NavTarget.MESSAGE_COMPOSE to Bundle("message" to app.gson.toJson(it))
                } else {
                    NavTarget.MESSAGE to Bundle("messageId" to it.id)
                }
            activity.navigate(navTarget = target, args = args)
        }, onStarClick = {
            this@MessagesListFragment.launch {
                manager.starMessage(it, !it.isStarred)
            }
        })

        app.db.messageDao().getAllByType(App.profileId, messageType).observe(this@MessagesListFragment, Observer { messages ->
            if (!isAdded || !this@MessagesListFragment::adapter.isInitialized)
                return@Observer

            messages.forEach { message ->
                message.filterNotes()

                // uh oh, so these are the workarounds ??
                message.recipients?.removeAll { it.profileId != message.profileId }
                message.recipients?.forEach { recipient ->
                    if (recipient.fullName == null) {
                        recipient.fullName = teachers.firstOrNull { it.id == recipient.id }?.fullName ?: ""
                    }
                }
            }

            // show/hide relevant views
            setSwipeToRefresh(messageType in Message.TYPE_RECEIVED..Message.TYPE_SENT)
            b.progressBar.isVisible = false
            b.list.isVisible = messages.isNotEmpty()
            b.noData.isVisible = messages.isEmpty()
            if (messages.isEmpty()) {
                return@Observer
            }

            // apply the new message list
            adapter.setAllItems(messages, searchText, addSearchField = true)

            // configure the adapter & recycler view
            if (b.list.adapter == null) {
                b.list.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                    addItemDecoration(SimpleDividerItemDecoration(context))
                    if (messageType in Message.TYPE_RECEIVED..Message.TYPE_SENT)
                        addOnScrollListener(onScrollListener)
                    this.adapter = this@MessagesListFragment.adapter
                }
            }

            val layoutManager = (b.list.layoutManager as? LinearLayoutManager) ?: return@Observer

            // reapply the filter
            adapter.getSearchField()?.applyTo(adapter) {
                // restore the previously saved scroll position
                recyclerViewState?.let {
                    layoutManager.onRestoreInstanceState(it)
                }
                recyclerViewState = null
            }
        })
    }; return true }

    override fun onDestroy() {
        super.onDestroy()
        if (!isAdded || !this::adapter.isInitialized)
            return
        val layoutManager = (b.list.layoutManager as? LinearLayoutManager)
        val searchField = adapter.getSearchField()

        onPageDestroy?.invoke(position, Bundle(
            "recyclerViewState" to layoutManager?.onSaveInstanceState(),
            "searchText" to searchField?.searchText?.toString()
        ))
    }
}
