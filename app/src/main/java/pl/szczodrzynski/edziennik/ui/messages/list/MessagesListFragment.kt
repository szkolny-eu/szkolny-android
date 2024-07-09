/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-4.
 */

package pl.szczodrzynski.edziennik.ui.messages.list

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.databinding.MessagesListFragmentBinding
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ext.getInt
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration

class MessagesListFragment : BaseFragment<MessagesListFragmentBinding, MainActivity>(
    inflater = MessagesListFragmentBinding::inflate,
) {

    override fun getScrollingView() = b.list

    private lateinit var adapter: MessagesAdapter
    private val manager
        get() = app.messageManager
    var teachers = listOf<Teacher>()

    @SuppressLint("RestrictedApi")
    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        val messageType = arguments.getInt("messageType", Message.TYPE_RECEIVED)
        var recyclerViewState =
            savedInstanceState?.getParcelable<LinearLayoutManager.SavedState>("recyclerViewState")
        val searchText = savedInstanceState?.getString("searchText")

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

            if (messageType != Message.TYPE_RECEIVED && messageType != Message.TYPE_SENT)
                canRefreshDisabled = true

            // show/hide relevant views
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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!isAdded || !this::adapter.isInitialized)
            return
        val layoutManager = (b.list.layoutManager as? LinearLayoutManager)
        val searchField = adapter.getSearchField()
        outState.putParcelable("recyclerViewState", layoutManager?.onSaveInstanceState())
        outState.putString("searchText", searchField?.searchText?.toString())
    }
}
