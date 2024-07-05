/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-18.
 */

package pl.szczodrzynski.edziennik.ui.base.dialog

import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.setMessage
import kotlin.coroutines.CoroutineContext

abstract class BaseDialog<I : Any>(
    internal val activity: AppCompatActivity,
    protected val onShowListener: ((tag: String) -> Unit)? = null,
    protected val onDismissListener: ((tag: String) -> Unit)? = null,
) : CoroutineScope {
    companion object {
        const val DISMISS = true
        const val NO_DISMISS = false
    }

    @Suppress("PropertyName")
    abstract val TAG: String

    protected lateinit var app: App
    protected lateinit var dialog: AlertDialog

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private var items = emptyList<I>()
    private var itemSelected: I? = null
    private var itemStates = BooleanArray(0)

    protected open fun getTitle(): CharSequence? = null
    protected abstract fun getTitleRes(): Int?
    protected open fun getMessage(): CharSequence? = null
    protected open fun getMessageRes(): Int? = null
    protected open fun getMessageFormat(): Pair<Int, List<CharSequence>>? = null
    protected open fun getView(): View? = null
    open fun isCancelable() = true
    open fun getPositiveButtonText(): Int? = null
    open fun getNeutralButtonText(): Int? = null
    open fun getNegativeButtonText(): Int? = null

    protected open fun getSingleChoiceItems(): Map<CharSequence, I>? = null
    protected open fun getMultiChoiceItems(): Map<CharSequence, I>? = null
    protected open fun getDefaultSelectedItem(): I? = null
    protected open fun getDefaultSelectedItems(): Set<I> = emptySet()

    open suspend fun onPositiveClick() = true
    open suspend fun onNeutralClick() = true
    open suspend fun onNegativeClick() = true
    open suspend fun onSingleSelectionChanged(item: I?) = Unit
    open suspend fun onMultiSelectionChanged(items: Set<I>) = Unit

    protected open suspend fun onBeforeShow() = true
    protected abstract suspend fun onShow()
    protected open fun onDismiss() = Unit

    fun show() {
        if (activity.isFinishing)
            return
        onShowListener?.invoke(TAG)
        app = activity.applicationContext as App
        dialog = MaterialAlertDialogBuilder(activity)
            .also(this::configure)
            .setCancelable(isCancelable())
            .setOnDismissListener {
                onDismiss()
                onDismissListener?.invoke(TAG)
            }
            .create()

        reload()
    }

    protected fun reload() {
        launch {
            if (activity.isFinishing)
                return@launch
            if (!onBeforeShow()) {
                dialog.dismiss()
                return@launch
            }
            dialog.show()
            setButtons()
            onShow()
        }
    }

    private fun configure(md: MaterialAlertDialogBuilder) {
        getTitle()?.let {
            md.setTitle(it)
        }
        getTitleRes()?.let {
            md.setTitle(it)
        }
        getPositiveButtonText()?.let {
            md.setPositiveButton(it, null)
        }
        getNeutralButtonText()?.let {
            md.setNeutralButton(it, null)
        }
        getNegativeButtonText()?.let {
            md.setNegativeButton(it, null)
        }

        getMessage()?.let {
            md.setMessage(it)
        }
        getMessageRes()?.let {
            md.setMessage(it)
        }
        getMessageFormat()?.let { (stringId, formatArgs) ->
            md.setMessage(stringId, *formatArgs.toTypedArray())
        }
        getView()?.let {
            md.setView(it)
        }

        getSingleChoiceItems()?.let { map ->
            val default = getDefaultSelectedItem()
            val defaultIndex = map.values.indexOf(default)
            md.setSingleChoiceItems(map.keys.toTypedArray(), defaultIndex) { _, which ->
                launch {
                    itemSelected = items[which]
                    onSingleSelectionChanged(getSingleSelection())
                }
            }
            items = map.values.toList()
            itemSelected = default
            md.setMessage(null)
        }
        getMultiChoiceItems()?.let { map ->
            val default = getDefaultSelectedItems()
            val defaultStates = map.values.map {
                it in default
            }.toBooleanArray()
            md.setMultiChoiceItems(map.keys.toTypedArray(),
                defaultStates) { _, position, isChecked ->
                launch {
                    itemStates[position] = isChecked
                    onMultiSelectionChanged(getMultiSelection())
                }
            }
            items = map.values.toList()
            itemStates = defaultStates
            md.setMessage(null)
        }
    }

    private fun setButtons() {
        dialog.getButton(BUTTON_POSITIVE)?.onClick {
            launch {
                if (onPositiveClick())
                    dialog.dismiss()
            }
        }

        dialog.getButton(BUTTON_NEUTRAL)?.onClick {
            launch {
                if (onNeutralClick())
                    dialog.dismiss()
            }
        }

        dialog.getButton(BUTTON_NEGATIVE)?.onClick {
            launch {
                if (onNegativeClick())
                    dialog.dismiss()
            }
        }
    }

    protected fun getSingleSelection() = itemSelected
    protected fun getMultiSelection(): Set<I> {
        return itemStates.mapIndexed { position, isChecked ->
            if (isChecked)
                items[position]
            else
                null
        }.filterNotNull().toSet()
    }

    protected fun dismiss() {
        dialog.dismiss()
    }
}
