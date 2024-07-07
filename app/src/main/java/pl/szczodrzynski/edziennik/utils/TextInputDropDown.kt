package pl.szczodrzynski.edziennik.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.textfield.TextInputEditText
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.ext.toDrawable

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

    private fun updateText() {
        setText(selected?.displayText ?: selected?.text)
    }

    @SuppressLint("RestrictedApi")
    open fun create(context: Context) {
        val drawable = CommunityMaterial.Icon.cmd_chevron_down.toDrawable()

        setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
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
                popup.menu.add(0, item.id.toInt(), index, item.text).also {
                    it.icon = item.icon
                }
            }

            popup.setOnMenuItemClickListener { menuItem ->
                val item = items[menuItem.order]
                if (onChangeListener?.invoke(item) != false) {
                    select(item)
                }
                clearFocus()
                true
            }

            val helper = MenuPopupHelper(context, popup.menu as MenuBuilder, this)
            helper.setForceShowIcon(true)
            helper.setOnDismissListener {
                clearFocus()
            }
            helper.show()
        }
    }

    /**
     * Select an arbitrary [item]. Allows to select an item not present
     * in the original list.
     */
    fun select(item: Item): Item {
        selected = item
        updateText()
        error = null
        return item
    }

    /**
     * Select an item by its ID. Returns the selected item
     * if found.
     */
    fun select(id: Long?): Item? {
        return items.singleOrNull { it.id == id }?.let { select(it) }
    }

    /**
     * Select an item by its tag. Returns the selected item
     * if found.
     */
    fun select(tag: Any?): Item? {
        return items.singleOrNull { it.tag == tag }?.let { select(it) }
    }

    /**
     * Select an item by its index. Returns the selected item
     * if the index exists.
     */
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

    class Item(
        val id: Long,
        val text: CharSequence,
        val displayText: CharSequence? = null,
        val tag: Any? = null,
        val icon: Drawable? = null
    )
}
