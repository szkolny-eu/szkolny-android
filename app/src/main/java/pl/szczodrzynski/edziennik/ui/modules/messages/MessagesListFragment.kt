/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-4.
 */

package pl.szczodrzynski.edziennik.ui.modules.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.MainActivity.Companion.TARGET_MESSAGES_COMPOSE
import pl.szczodrzynski.edziennik.MainActivity.Companion.TARGET_MESSAGES_DETAILS
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.databinding.MessagesListFragmentBinding
import pl.szczodrzynski.edziennik.ui.modules.base.lazypager.LazyFragment
import pl.szczodrzynski.edziennik.ui.modules.messages.models.MessagesSearch
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

        adapter = MessagesAdapter(activity, teachers, onItemClick = {
            val (target, args) =
                if (it.type == Message.TYPE_DRAFT) {
                    TARGET_MESSAGES_COMPOSE to Bundle("message" to app.gson.toJson(it))
                } else {
                    TARGET_MESSAGES_DETAILS to Bundle("messageId" to it.id)
                }
            activity.loadTarget(target, args)
        }, onStarClick = {
            this@MessagesListFragment.launch {
                manager.starMessage(it, !it.isStarred)
            }
        })

        app.db.messageDao().getAllByType(App.profileId, messageType).observe(this@MessagesListFragment, Observer { messages ->
            if (!isAdded || !this@MessagesListFragment::adapter.isInitialized)
                return@Observer

            messages.forEach { message ->
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

            if (adapter.allItems.isEmpty()) {
                // items empty - add the search field
                adapter.allItems += MessagesSearch().also {
                    it.searchText = searchText ?: ""
                }
            } else {
                // items not empty - remove all messages
                adapter.allItems.removeAll { it is MessageFull }
            }
            // add all messages
            adapter.allItems.addAll(messages)

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
            val searchItem = adapter.items.firstOrNull { it is MessagesSearch } as? MessagesSearch
            adapter.filter.filter(searchText ?: searchItem?.searchText) {
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
        val searchItem = adapter.items.firstOrNull { it is MessagesSearch } as? MessagesSearch

        onPageDestroy?.invoke(position, Bundle(
            "recyclerViewState" to layoutManager?.onSaveInstanceState(),
            "searchText" to searchItem?.searchText?.toString()
        ))
    }
}
