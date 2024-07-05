package pl.szczodrzynski.edziennik.ui.behaviour

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.App.Companion.profileId
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Notice
import pl.szczodrzynski.edziennik.data.db.full.NoticeFull
import pl.szczodrzynski.edziennik.data.enums.MetadataType
import pl.szczodrzynski.edziennik.databinding.FragmentBehaviourBinding
import pl.szczodrzynski.edziennik.ext.resolveAttr
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import java.util.Locale

class BehaviourFragment : BaseFragment<FragmentBehaviourBinding, MainActivity>(
    inflater = FragmentBehaviourBinding::inflate,
) {

    override fun getMarkAsReadType() = MetadataType.NOTICE
    
    private var displayMode = MODE_YEAR
    private var noticeList: List<NoticeFull>? = null

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
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
        App.db.noticeDao().getAll(profileId).observe(viewLifecycleOwner) { notices: List<NoticeFull>? ->
            if (!isAdded) return@observe
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
            b.noticesWarningsCount.setTextColor(android.R.attr.textColorPrimary.resolveAttr(activity))
        }
    }

    companion object {
        private const val MODE_YEAR = 0
        private const val MODE_SEMESTER_1 = 1
        private const val MODE_SEMESTER_2 = 2
    }
}
