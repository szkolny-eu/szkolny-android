/*
 * Copyright (c) Kuba Szczodrzyński 2024-7-5.
 */

package pl.szczodrzynski.edziennik.ui.base.dialog

import android.text.Editable
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import pl.szczodrzynski.edziennik.ext.resolveString

class SimpleDialog<I : Any>(
    activity: AppCompatActivity,
    config: SimpleDialog<I>.() -> Unit,
) : BaseDialog<I>(activity) {

    init {
        config()
    }

    private var title: CharSequence? = null
    private var titleRes: Int? = null
    private var message: CharSequence? = null
    private var messageRes: Int? = null
    private var messageFormat: Pair<Int, List<CharSequence>>? = null
    private var isCancelable = true
    private var positiveButtonText: Int? = null
    private var neutralButtonText: Int? = null
    private var negativeButtonText: Int? = null

    private var items: Map<CharSequence, I>? = null
    private var singleChoiceItems: Map<CharSequence, I>? = null
    private var multiChoiceItems: Map<CharSequence, I>? = null
    private var defaultSelectedItem: I? = null
    private var defaultSelectedItems: Set<I>? = null

    private var inputType: Int? = null
    private var inputHint: CharSequence? = null
    private var inputHintRes: Int? = null
    private var inputValue: CharSequence? = null

    private var onPositiveClick: (suspend () -> Unit)? = null
    private var onNeutralClick: (suspend () -> Unit)? = null
    private var onNegativeClick: (suspend () -> Unit)? = null
    private var onItemClick: (suspend (item: I) -> Unit)? = null
    private var onSingleSelectionChanged: (suspend (item: I) -> Unit)? = null
    private var onMultiSelectionChanged: (suspend (item: I, isChecked: Boolean) -> Unit)? = null
    private var onInputTextChanged: (suspend TextInputEditText.(text: Editable?) -> Unit)? = null

    fun title(value: CharSequence) {
        title = value
    }

    fun title(value: Int) {
        titleRes = value
    }

    fun message(value: CharSequence) {
        message = value
    }

    fun message(value: Int, vararg args: Any) {
        if (args.isEmpty()) {
            messageRes = value
        } else {
            messageFormat = value to args.map {
                if (it is CharSequence) it else it.toString()
            }
        }
    }

    fun cancelable(value: Boolean) {
        isCancelable = value
    }

    fun positive(text: Int, block: (suspend () -> Unit)? = null) {
        positiveButtonText = text
        onPositiveClick = block
    }

    fun neutral(text: Int, block: (suspend () -> Unit)? = null) {
        neutralButtonText = text
        onNeutralClick = block
    }

    fun negative(text: Int, block: (suspend () -> Unit)? = null) {
        negativeButtonText = text
        onNegativeClick = block
    }

    fun items(
        items: Map<CharSequence, I>,
        block: (suspend (item: I) -> Unit)? = null,
    ) {
        this.items = items
        onItemClick = block
    }

    fun itemsRes(
        items: Map<Int, I>,
        block: (suspend (item: I) -> Unit)? = null,
    ) {
        this.items = items.mapKeys { (k, _) -> k.resolveString(activity) }
        onItemClick = block
    }

    fun single(
        items: Map<CharSequence, I>,
        default: I? = null,
        block: (suspend (item: I) -> Unit)? = null,
    ) {
        singleChoiceItems = items
        defaultSelectedItem = default
        onSingleSelectionChanged = block
    }

    fun singleRes(
        items: Map<Int, I>,
        default: I? = null,
        block: (suspend (item: I) -> Unit)? = null,
    ) {
        singleChoiceItems = items.mapKeys { (k, _) -> k.resolveString(activity) }
        defaultSelectedItem = default
        onSingleSelectionChanged = block
    }

    fun multi(
        items: Map<CharSequence, I>,
        default: Set<I> = emptySet(),
        block: (suspend (item: I, isChecked: Boolean) -> Unit)? = null,
    ) {
        multiChoiceItems = items
        defaultSelectedItems = default
        onMultiSelectionChanged = block
    }

    fun multiRes(
        items: Map<Int, I>,
        default: Set<I> = emptySet(),
        block: (suspend (item: I, isChecked: Boolean) -> Unit)? = null,
    ) {
        singleChoiceItems = items.mapKeys { (k, _) -> k.resolveString(activity) }
        defaultSelectedItems = default
        onMultiSelectionChanged = block
    }

    fun items(
        vararg items: Pair<CharSequence, I>,
        block: (suspend (item: I) -> Unit)? = null,
    ) = items(items.toMap(), block)

    fun itemsRes(
        vararg items: Pair<Int, I>,
        block: (suspend (item: I) -> Unit)? = null,
    ) = itemsRes(items.toMap(), block)

    fun single(
        vararg items: Pair<CharSequence, I>,
        default: I? = null,
        block: (suspend (item: I) -> Unit)? = null,
    ) = single(items.toMap(), default, block)

    fun singleRes(
        vararg items: Pair<Int, I>,
        default: I? = null,
        block: (suspend (item: I) -> Unit)? = null,
    ) = singleRes(items.toMap(), default, block)

    fun multi(
        vararg items: Pair<CharSequence, I>,
        default: Set<I> = emptySet(),
        block: (suspend (item: I, isChecked: Boolean) -> Unit)? = null,
    ) = multi(items.toMap(), default, block)

    fun multiRes(
        vararg items: Pair<Int, I>,
        default: Set<I> = emptySet(),
        block: (suspend (item: I, isChecked: Boolean) -> Unit)? = null,
    ) = multiRes(items.toMap(), default, block)

    fun input(
        type: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
        hint: CharSequence? = null,
        value: CharSequence? = null,
        block: (suspend TextInputEditText.(text: Editable?) -> Unit)? = null,
    ) {
        inputType = type
        inputHint = hint
        inputValue = value
        onInputTextChanged = block
    }

    fun inputRes(
        type: Int,
        hint: Int? = null,
        value: CharSequence? = null,
        block: (suspend TextInputEditText.(text: Editable?) -> Unit)? = null,
    ) {
        inputType = type
        inputHintRes = hint
        inputValue = value
        onInputTextChanged = block
    }

    override fun getTitle() = title
    override fun getTitleRes() = titleRes
    override fun getMessage() = message
    override fun getMessageRes() = messageRes
    override fun getMessageFormat() = messageFormat
    override fun isCancelable() = isCancelable
    override fun getPositiveButtonText() = positiveButtonText
    override fun getNeutralButtonText() = neutralButtonText
    override fun getNegativeButtonText() = negativeButtonText

    override fun getItems() = items
    override fun getSingleChoiceItems() = singleChoiceItems
    override fun getMultiChoiceItems() = multiChoiceItems
    override fun getDefaultSelectedItem() = defaultSelectedItem
    override fun getDefaultSelectedItems() = defaultSelectedItems ?: setOf()

    override fun getInputType() = inputType
    override fun getInputHint() = inputHint
    override fun getInputHintRes() = inputHintRes
    override fun getInputValue() = inputValue

    override suspend fun onPositiveClick(): Boolean {
        onPositiveClick?.invoke()
        return DISMISS
    }

    override suspend fun onNeutralClick(): Boolean {
        onNeutralClick?.invoke()
        return DISMISS
    }

    override suspend fun onNegativeClick(): Boolean {
        onNegativeClick?.invoke()
        return DISMISS
    }

    override suspend fun onItemClick(item: I): Boolean {
        onItemClick?.invoke(item)
        return DISMISS
    }

    override suspend fun onSingleSelectionChanged(item: I) {
        onSingleSelectionChanged?.invoke(item)
    }

    override suspend fun onMultiSelectionChanged(item: I, isChecked: Boolean) {
        onMultiSelectionChanged?.invoke(item, isChecked)
    }

    override suspend fun onInputTextChanged(input: TextInputEditText, text: Editable?) {
        onInputTextChanged?.let { input.it(text) }
    }
}
