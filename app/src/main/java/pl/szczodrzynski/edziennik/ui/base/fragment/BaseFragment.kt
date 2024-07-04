/*
 * Copyright (c) Kuba Szczodrzyński 2024-7-2.
 */

package pl.szczodrzynski.edziennik.ui.base.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.mikepenz.iconics.typeface.IIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.startCoroutineTimer
import pl.szczodrzynski.edziennik.ui.login.LoginActivity
import pl.szczodrzynski.navlib.bottomsheet.items.IBottomSheetItem
import kotlin.coroutines.CoroutineContext

@Suppress("UNCHECKED_CAST")
abstract class BaseFragment<B : ViewBinding, A : AppCompatActivity>(
    private val inflater: ((inflater: LayoutInflater, parent: ViewGroup?, attachToParent: Boolean) -> B)?,
) : Fragment(), CoroutineScope {

    internal lateinit var app: App
    internal lateinit var activity: A
    internal lateinit var b: B

    private var isViewReady: Boolean = false
    private var inState: Bundle? = null

    private var canRefreshSent = false
    open var canRefresh = false
        set(value) {
            if (field == value && canRefreshSent) // broadcast only if changed
                return
            field = value
            (activity as? MainActivity)?.swipeRefreshLayout?.isEnabled =
                !canRefreshDisabled && value
            canRefreshSent = true
        }
    protected var canRefreshDisabled = false
        set(value) {
            field = value
            canRefresh = canRefresh
        }

    private var job = Job()
    final override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        job.cancel()
        job = Job()
        activity = getActivity() as? A ?: return null
        context ?: return null
        app = activity.application as App
        // inflate using the constructor parameter or the body method
        b = this.inflater?.invoke(inflater, container, false)
            ?: inflate(inflater, container, false)
            ?: return null
        isViewReady = false // reinitialize the view in onResume()
        inState = savedInstanceState // save the instance state for onResume()
        return b.root
    }

    override fun onResume() {
        super.onResume()
        try {
            EventBus.getDefault().register(this)
        } catch (_: Exception) {
        }

        if (!isAdded || isViewReady)
            return
        isViewReady = true
        setupCanRefresh()
        (activity as? MainActivity)?.let(::setupMainActivity)
        (activity as? LoginActivity)?.let(::setupLoginActivity)
        // let the UI transition for a moment
        startCoroutineTimer(100L) {
            if (!isAdded)
                return@startCoroutineTimer
            onViewReady(inState)
            (activity as? MainActivity)?.gainAttention()
            (activity as? MainActivity)?.gainAttentionFAB()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            EventBus.getDefault().unregister(this)
        } catch (_: Exception) {
        }
    }

    final override fun onDestroyView() {
        super.onDestroyView()
        isViewReady = false
        job.cancel()
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) =
        super.onViewCreated(view, savedInstanceState)

    final override fun onViewStateRestored(savedInstanceState: Bundle?) =
        super.onViewStateRestored(savedInstanceState)

    final override fun onAttach(context: Context) = super.onAttach(context)
    final override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState)
    final override fun onStart() = super.onStart()
    final override fun onStop() = super.onStop()
    final override fun onDestroy() = super.onDestroy()
    final override fun onDetach() = super.onDetach()

    open fun inflate(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean,
    ): B? = null

    open fun getRefreshScrollingView(): View? = null
    open fun getFab(): Pair<Int, IIcon>? = null
    open fun getMarkAsReadType(): MetadataType? = null
    open fun getBottomSheetItems() = listOf<IBottomSheetItem<*>>()

    open suspend fun onViewReady(savedInstanceState: Bundle?) {}

    open suspend fun onFabClick() {}
}
