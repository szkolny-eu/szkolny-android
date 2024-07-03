/*
 * Copyright (c) Kuba Szczodrzyński 2024-7-2.
 */

package pl.szczodrzynski.edziennik.ui.base.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.startCoroutineTimer
import pl.szczodrzynski.edziennik.ui.login.LoginActivity
import pl.szczodrzynski.edziennik.utils.SwipeRefreshLayoutNoIndicator
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem
import pl.szczodrzynski.navlib.bottomsheet.items.IBottomSheetItem
import kotlin.coroutines.CoroutineContext

abstract class BaseFragment<B : ViewBinding, A : AppCompatActivity>(
    private val inflater: ((inflater: LayoutInflater, parent: ViewGroup?, attachToParent: Boolean) -> B)?,
) : Fragment(), CoroutineScope {

    protected lateinit var app: App
    protected lateinit var activity: A
    protected lateinit var b: B

    private val job: Job = Job()
    final override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    @Suppress("UNCHECKED_CAST")
    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        activity = getActivity() as? A ?: return null
        context ?: return null
        app = activity.application as App
        b = this.inflater?.invoke(inflater, container, false)
            ?: inflate(inflater, container, false)
            ?: return null
        return b.root
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded)
            return
        (activity as? MainActivity)?.let(::setupMainActivity)
        (activity as? LoginActivity)?.let(::setupLoginActivity)
        // let the UI transition for a moment
        startCoroutineTimer(100L) {
            if (!isAdded)
                return@startCoroutineTimer
            onViewCreated(savedInstanceState)
            (activity as? MainActivity)?.gainAttention()
            (activity as? MainActivity)?.gainAttentionFAB()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun setupMainActivity(activity: MainActivity) {
        getRefreshLayout()?.setParent(activity.swipeRefreshLayout)

        val items = getBottomSheetItems().toMutableList()
        getMarkAsReadType()?.let { metadataType ->
            if (items.isNotEmpty())
                items += BottomSheetSeparatorItem(true)
            items += BottomSheetPrimaryItem(true)
                .withTitle(R.string.menu_mark_as_read)
                .withIcon(CommunityMaterial.Icon.cmd_eye_check_outline)
                .withOnClickListener {
                    activity.bottomSheet.close()
                    launch(Dispatchers.IO) {
                        app.db.metadataDao().setAllSeen(app.profileId, metadataType, true)
                    }
                    Toast.makeText(
                        activity,
                        R.string.main_menu_mark_as_read_success,
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
        if (items.isNotEmpty()) {
            activity.navView.bottomSheet.prependItems(*items.toTypedArray())
        }

        getFab()?.let { (text, icon) ->
            activity.navView.bottomBar.apply {
                fabEnable = true
                fabExtendedText = app.getString(text)
                fabIcon = icon
                setFabOnClickListener {
                    launch {
                        onFabClick()
                    }
                }
            }
        }
    }

    private fun setupLoginActivity(activity: LoginActivity) {
        getRefreshLayout()?.setParent(activity.swipeRefreshLayout)
    }

    open fun inflate(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean,
    ): B? = null

    open fun getRefreshLayout(): SwipeRefreshLayoutNoIndicator? = null
    open fun getFab(): Pair<Int, IIcon>? = null
    open fun getMarkAsReadType(): MetadataType? = null
    open fun getBottomSheetItems() = listOf<IBottomSheetItem<*>>()

    open suspend fun onViewCreated(savedInstanceState: Bundle?) {}

    open suspend fun onFabClick() {}
}
