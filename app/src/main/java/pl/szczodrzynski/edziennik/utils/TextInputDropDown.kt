package pl.szczodrzynski.edziennik.utils

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.textfield.TextInputEditText
import pl.szczodrzynski.edziennik.R

open class TextInputDropDown : TextInputEditText {
    constructor(context: Context) : super(context) {
        create(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        create(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        create(context)
    }

    var items = mutableListOf<Item>()
    private var onChangeListener: ((item: Item) -> Boolean)? = null

    var selected: Item? = null
    val selectedId
        get() = selected?.id

    fun updateText() {
        setText(selected?.displayText ?: selected?.text)
    }

    open fun create(context: Context) {
        val drawable = context.resources.getDrawable(R.drawable.dropdown_arrow)
        val wrappedDrawable = DrawableCompat.wrap(drawable)
        DrawableCompat.setTint(wrappedDrawable, Themes.getPrimaryTextColor(context))

        setCompoundDrawablesWithIntrinsicBounds(null, null, wrappedDrawable, null)
        isFocusableInTouchMode = false
        isCursorVisible = false
        isLongClickable = false
        maxLines = 1
        inputType = 0
        keyListener = null
        setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                v.isFocusableInTouchMode = false
            }
        }

        setOnClickListener {
            isFocusableInTouchMode = true
            requestFocus()
            val popup = PopupMenu(context, this)

            items.forEachIndexed { index, item ->
                popup.menu.add(0, item.id.toInt(), index, item.text)
            }

            popup.setOnMenuItemClickListener { menuItem ->
                val item = items[menuItem.order]
                if (onChangeListener?.invoke(item) != false) {
                    select(item)
                }
                clearFocus()
                true
            }

            popup.setOnDismissListener {
                clearFocus()
            }

            popup.show()
        }
    }

    fun select(item: Item): Item? {
        selected = item
        updateText()
        error = null
        return item
    }

    fun select(id: Long?): Item? {
        return items.singleOrNull { it.id == id }?.let { select(it) }
    }

    fun select(tag: Any?): Item? {
        return items.singleOrNull { it.tag == tag }?.let { select(it) }
    }

    fun select(index: Int): Item? {
        return items.getOrNull(index)?.let { select(it) }
    }

    fun deselect(): TextInputDropDown {
        selected = null
        text = null
        return this
    }

    fun clear(): TextInputDropDown {
        items.clear()
        return this
    }

    fun append(items: List<Item>): TextInputDropDown {
        this.items.addAll(items)
        return this
    }

    fun prepend(items: List<Item>): TextInputDropDown{
        this.items.addAll(0, items)
        return this
    }

    operator fun plusAssign(items: Item) {
        this.items.add(items)
    }
    operator fun plusAssign(items: List<Item>) {
        this.items.addAll(items)
    }

    /**
     * Set the listener called when other item is selected.
     *
     * The listener should return true to allow the item to be selected, false otherwise.
     */
    fun setOnChangeListener(onChangeListener: ((item: Item) -> Boolean)? = null): TextInputDropDown {
        this.onChangeListener = onChangeListener
        return this
    }

    override fun setOnClickListener(onClickListener: OnClickListener?) {
        super.setOnClickListener { v ->
            isFocusableInTouchMode = true
            requestFocus()
            onClickListener!!.onClick(v)
        }
    }

    class Item(val id: Long, val text: CharSequence, val displayText: CharSequence? = null, val tag: Any? = null)
}
