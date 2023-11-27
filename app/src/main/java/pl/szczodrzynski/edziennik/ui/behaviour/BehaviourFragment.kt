package pl.szczodrzynski.edziennik.ui.behaviour

import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.App.Companion.profileId
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Notice
import pl.szczodrzynski.edziennik.data.db.full.NoticeFull
import pl.szczodrzynski.edziennik.databinding.FragmentBehaviourBinding
import pl.szczodrzynski.edziennik.ui.behaviour.NoticesAdapter
import pl.szczodrzynski.edziennik.utils.Themes.getPrimaryTextColor
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import java.util.*
import kotlin.coroutines.CoroutineContext

class BehaviourFragment : Fragment() {

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: FragmentBehaviourBinding
    
    private var displayMode = MODE_YEAR
    private var noticeList: List<NoticeFull>? = null
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = DataBindingUtil.inflate(inflater, R.layout.fragment_behaviour, container, false)
        b.refreshLayout.setParent(activity.swipeRefreshLayout)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (app == null || activity == null || b == null || !isAdded) return
        activity.bottomSheet.prependItems(
            BottomSheetPrimaryItem(true)
                .withTitle(R.string.menu_mark_as_read)
                .withIcon(CommunityMaterial.Icon.cmd_eye_check_outline)
                .withOnClickListener { v3: View? ->
                    activity.bottomSheet.close()
                    AsyncTask.execute {
                        App.db.metadataDao().setAllSeen(profileId, Metadata.TYPE_NOTICE, true)
                    }
                    Toast.makeText(
                        activity,
                        R.string.main_menu_mark_as_read_success,
                        Toast.LENGTH_SHORT
                    ).show()
                }
        )
        b.toggleGroup.check(when (displayMode) {
            0 -> R.id.allYear
            1 -> R.id.semester1
            2 -> R.id.semester2
            else -> R.id.allYear
        })
        b.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked)
                return@addOnButtonCheckedListener
            displayMode = when (checkedId) {
                R.id.allYear -> 0
                R.id.semester1 -> 1
                R.id.semester2 -> 2
                else -> 0
            }
            updateList()
        }
        /*b.refreshLayout.setOnRefreshListener(() -> {
            activity.syncCurrentFeature(MainActivity.DRAWER_ITEM_BEHAVIOUR, b.refreshLayout);
        });*/
        val linearLayoutManager = LinearLayoutManager(context)
        b.noticesView.setHasFixedSize(true)
        b.noticesView.layoutManager = linearLayoutManager
        b.noticesView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (recyclerView.canScrollVertically(-1)) {
                    b.refreshLayout.isEnabled = false
                }
                if (!recyclerView.canScrollVertically(-1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    b.refreshLayout.isEnabled = true
                }
            }
        })
        App.db.noticeDao().getAll(profileId).observe(this) { notices: List<NoticeFull>? ->
            if (app == null || activity == null || b == null || !isAdded) return@observe
            if (notices == null) {
                b.noticesView.visibility = View.GONE
                b.noticesNoData.visibility = View.VISIBLE
                return@observe
            }
            noticeList = notices
            updateList()
        }
    }

    private fun updateList() {
        var praisesCount = 0
        var warningsCount = 0
        var otherCount = 0
        val filteredList: MutableList<NoticeFull> = ArrayList()
        for (notice in noticeList!!) {
            if (displayMode != MODE_YEAR && notice.semester != displayMode) continue
            filteredList.add(notice)
            when (notice.type) {
                Notice.TYPE_POSITIVE -> praisesCount++
                Notice.TYPE_NEGATIVE -> warningsCount++
                Notice.TYPE_NEUTRAL -> otherCount++
            }
        }
        if (filteredList.size > 0) {
            val adapter = NoticesAdapter(requireContext(), filteredList)
            b.noticesView.visibility = View.VISIBLE
            b.noticesNoData.visibility = View.GONE
            b.noticesView.adapter = adapter
            adapter.noticeList = filteredList
            adapter.notifyDataSetChanged()
        } else {
            b.noticesView.visibility = View.GONE
            b.noticesNoData.visibility = View.VISIBLE
        }
        b.noticesPraisesCount.text = String.format(Locale.getDefault(), "%d", praisesCount)
        b.noticesWarningsCount.text = String.format(Locale.getDefault(), "%d", warningsCount)
        b.noticesOtherCount.text = String.format(Locale.getDefault(), "%d", otherCount)
        if (warningsCount >= 3) {
            b.noticesWarningsCount.setTextColor(Color.RED)
        } else {
            b.noticesWarningsCount.setTextColor(getPrimaryTextColor(activity))
        }
    }

    companion object {
        private const val MODE_YEAR = 0
        private const val MODE_SEMESTER_1 = 1
        private const val MODE_SEMESTER_2 = 2
    }
}