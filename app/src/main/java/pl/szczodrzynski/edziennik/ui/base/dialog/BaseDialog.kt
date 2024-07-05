/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-18.
 */

package pl.szczodrzynski.edziennik.ui.base.dialog

import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE
import androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL
import androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.databinding.DialogEditTextBinding
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.registerSafe
import pl.szczodrzynski.edziennik.ext.resolveString
import pl.szczodrzynski.edziennik.ext.unregisterSafe
import pl.szczodrzynski.edziennik.utils.BetterLinkMovementMethod
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

abstract class BaseDialog<I : Any>(
    protected val activity: AppCompatActivity,
    protected val onShowListener: ((tag: String) -> Unit)? = null,
    protected val onDismissListener: ((tag: String) -> Unit)? = null,
) : CoroutineScope {
    companion object {
        const val DISMISS = true
        const val NO_DISMISS = false
    }

    @Suppress("PropertyName")
    abstract val TAG: String

    internal val app = activity.applicationContext as App
    protected lateinit var dialog: AlertDialog

    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main
    private var continuation: Continuation<BaseDialog<I>>? = null

    private var button: Int? = null
    private var items = emptyList<I>()
    private var itemSelected: I? = null
    private var itemStates = BooleanArray(0)
    private var input: TextInputEditText? = null

    protected open fun getTitle(): CharSequence? = null
    protected open fun getTitleRes(): Int? = null
    protected open fun getMessage(): CharSequence? = null
    protected open fun getMessageRes(): Int? = null
    protected open fun getMessageFormat(): Pair<Int, List<CharSequence>>? = null
    protected open fun getView(): View? = null
    protected open fun isCancelable() = true
    protected open fun getPositiveButtonText(): Int? = null
    protected open fun getNeutralButtonText(): Int? = null
    protected open fun getNegativeButtonText(): Int? = null

    protected open fun getItems(): Map<CharSequence, I>? = null
    protected open fun getSingleChoiceItems(): Map<CharSequence, I>? = null
    protected open fun getMultiChoiceItems(): Map<CharSequence, I>? = null
    protected open fun getDefaultSelectedItem(): I? = null
    protected open fun getDefaultSelectedItems(): Set<I> = emptySet()

    protected open fun getInputType(): Int? = null
    protected open fun getInputHint(): CharSequence? = null
    protected open fun getInputHintRes(): Int? = null
    protected open fun getInputValue(): CharSequence? = null

    protected open suspend fun onPositiveClick() = DISMISS
    protected open suspend fun onNeutralClick() = DISMISS
    protected open suspend fun onNegativeClick() = DISMISS
    protected open suspend fun onItemClick(item: I) = DISMISS
    protected open suspend fun onSingleSelectionChanged(item: I) = Unit
    protected open suspend fun onMultiSelectionChanged(item: I, isChecked: Boolean) = Unit
    protected open suspend fun onInputTextChanged(input: TextInputEditText, text: Editable?) = Unit

    protected open suspend fun onBeforeShow() = true
    protected open suspend fun onShow() = Unit
    protected open suspend fun onDismiss() = Unit

    fun show(): BaseDialog<I> {
        if (activity.isFinishing)
            return this
        job.cancel()
        job = Job()

        dialog = MaterialAlertDialogBuilder(activity)
            .also(this::configure)
            .setCancelable(isCancelable())
            .setOnDismissListener {
                launch {
                    dispatchOnDismiss()
                    job.cancel()
                }
            }
            .create()

        launch {
            if (activity.isFinishing)
                return@launch
            if (!onBeforeShow()) {
                dialog.dismiss()
                return@launch
            }
            if (activity.isFinishing)
                return@launch
            dialog.show()
            setButtons()
            dispatchOnShow()
        }
        return this
    }

    suspend fun showModal() = suspendCancellableCoroutine {
        it.invokeOnCancellation {
            dismiss()
        }
        continuation = it
        show()
    }

    private suspend fun dispatchOnShow() {
        if (activity.isFinishing)
            return
        onShowListener?.invoke(TAG)
        EventBus.getDefault().registerSafe(this)
        onShow()
    }

    private suspend fun dispatchOnDismiss() {
        if (activity.isFinishing)
            return
        onDismiss()
        EventBus.getDefault().unregisterSafe(this)
        onDismissListener?.invoke(TAG)
        continuation?.resume(this)
        continuation = null
    }

    private fun configure(md: MaterialAlertDialogBuilder) {
        getTitle()?.let {
            md.setTitle(it)
        }
        getTitleRes()?.let {
            md.setTitle(it)
        }
        getMessage()?.let {
            md.setMessage(it)
        }
        getMessageRes()?.let {
            md.setMessage(it)
        }
        getMessageFormat()?.let { (stringId, formatArgs) ->
            md.setMessage(activity.getString(stringId, *formatArgs.toTypedArray()))
        }
        getView()?.let {
            md.setView(it)
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

        getItems()?.let { map ->
            md.setItems(map.keys.toTypedArray()) { _, which ->
                button = null
                launch {
                    itemSelected = items[which]
                    if (onItemClick(items[which]))
                        dismiss()
                }
            }
            items = map.values.toList()
            itemSelected = null
            md.setMessage(null)
        }
        getSingleChoiceItems()?.let { map ->
            val default = getDefaultSelectedItem()
            val defaultIndex = map.values.indexOf(default)
            md.setSingleChoiceItems(
                map.keys.toTypedArray(),
                defaultIndex
            ) { _, which ->
                button = null
                launch {
                    itemSelected = items[which]
                    onSingleSelectionChanged(items[which])
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
            md.setMultiChoiceItems(
                map.keys.toTypedArray(),
                defaultStates
            ) { _, position, isChecked ->
                button = null
                launch {
                    itemStates[position] = isChecked
                    onMultiSelectionChanged(items[position], isChecked)
                }
            }
            items = map.values.toList()
            itemStates = defaultStates
            md.setMessage(null)
        }

        getInputType()?.let { inputType ->
            val b = DialogEditTextBinding.inflate(LayoutInflater.from(activity), null, false)
            b.text1.let {
                it.inputType = inputType
                it.hint = getInputHint() ?: getInputHintRes()?.resolveString(activity)
                it.setText(getInputValue() ?: "")
                it.addTextChangedListener {
                    launch {
                        onInputTextChanged(b.text1, it)
                    }
                }
            }
            input = b.text1
            md.setView(b.root)
        }
    }

    private fun setButtons() {
        dialog.getButton(BUTTON_POSITIVE)?.onClick {
            button = BUTTON_POSITIVE
            launch {
                if (onPositiveClick())
                    dismiss()
            }
        }

        dialog.getButton(BUTTON_NEUTRAL)?.onClick {
            button = BUTTON_NEUTRAL
            launch {
                if (onNeutralClick())
                    dismiss()
            }
        }

        dialog.getButton(BUTTON_NEGATIVE)?.onClick {
            button = BUTTON_NEGATIVE
            launch {
                if (onNegativeClick())
                    dismiss()
            }
        }

        dialog.findViewById<TextView>(android.R.id.message)?.movementMethod =
            BetterLinkMovementMethod.getInstance()
    }

    fun getButton() = button
    fun getItem() = itemSelected
    fun getSingleSelection() = itemSelected
    fun getMultiSelection(): Set<I> {
        return itemStates.mapIndexed { position, isChecked ->
            if (isChecked)
                items[position]
            else
                null
        }.filterNotNull().toSet()
    }

    fun getInput(): TextInputEditText? = input

    fun dismiss() {
        dialog.dismiss()
    }
}
