package pl.szczodrzynski.navlib.bottomsheet

import android.app.Activity
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.navlibfont.NavLibFont
import com.mikepenz.iconics.utils.paddingDp
import com.mikepenz.iconics.utils.sizeDp
import pl.szczodrzynski.navlib.*
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem
import pl.szczodrzynski.navlib.bottomsheet.items.IBottomSheetItem


class NavBottomSheet : CoordinatorLayout {
    companion object {
        const val TOGGLE_GROUP_SINGLE_SELECTION = 0
        const val TOGGLE_GROUP_MULTIPLE_SELECTION = 1
        const val TOGGLE_GROUP_SORTING_ORDER = 2

        const val SORT_MODE_ASCENDING = 0
        const val SORT_MODE_DESCENDING = 1
    }

    constructor(context: Context) : super(context) {
        create(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        create(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        create(attrs, defStyle)
    }

    private lateinit var scrimView: View
    private lateinit var bottomSheet: NestedScrollView
    private lateinit var content: LinearLayout
    private lateinit var dragBar: View
    private lateinit var textInputLayout: TextInputLayout
    private lateinit var textInputEditText: TextInputEditText
    private lateinit var toggleGroupContainer: LinearLayout
    private lateinit var toggleGroup: MaterialButtonToggleGroup
    private lateinit var toggleGroupTitleView: TextView
    private lateinit var list: RecyclerView

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private var bottomSheetVisible = false

    private val items = ArrayList<IBottomSheetItem<*>>()
    private val adapter = BottomSheetAdapter(items)

    /**
     * Enable the bottom sheet.
     * This value is mostly relevant to the [pl.szczodrzynski.navlib.NavBottomBar].
     */
    var enable = true
        set(value) {
            field = value
            if (!value && bottomSheetVisible)
                close()
        }
    /**
     * Whether the [pl.szczodrzynski.navlib.NavBottomBar] should open this BottomSheet
     * when the user drags the bottom bar.
     */
    var enableDragToOpen = true

    /**
     * Control the scrim view visibility, shown when BottomSheet
     * is expanded.
     */
    var scrimViewEnabled = true
        set(value) {
            scrimView.visibility = if (value) View.INVISIBLE else View.GONE // INVISIBLE
            field = value
        }
    /**
     * Whether tapping the Scrim view should hide the BottomSheet.
     */
    var scrimViewTapToClose = true


    fun hideKeyboard() {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(rootView.windowToken, 0)
    }


    private fun create(attrs: AttributeSet?, defStyle: Int) {
        val layoutInflater = LayoutInflater.from(context)
        layoutInflater.inflate(R.layout.nav_bottom_sheet, this)

        scrimView = findViewById(R.id.bs_scrim)
        bottomSheet = findViewById(R.id.bs_view)
        content = findViewById(R.id.bs_content)
        dragBar = findViewById(R.id.bs_dragBar)
        textInputLayout = findViewById(R.id.bs_textInputLayout)
        textInputEditText = findViewById(R.id.bs_textInputEditText)
        toggleGroupContainer = findViewById(R.id.bs_toggleGroupContainer)
        toggleGroup = findViewById(R.id.bs_toggleGroup)
        toggleGroupTitleView = findViewById(R.id.bs_toggleGroupTitle)
        list = findViewById(R.id.bs_list)

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        scrimView.setOnTouchListener { _, event ->
            if (!scrimViewTapToClose)
                return@setOnTouchListener true
            if (event.action == MotionEvent.ACTION_UP && bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
            true
        }

        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(v: View, p1: Float) {}
            override fun onStateChanged(v: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN && bottomSheetVisible) {
                    bottomSheetVisible = false
                    bottomSheet.scrollTo(0, 0)
                    if (scrimViewEnabled)
                        Anim.fadeOut(scrimView, 300, null)
                    // steal the focus from any EditTexts
                    dragBar.requestFocus()
                    hideKeyboard()
                    onCloseListener?.invoke()
                }
                else if (!bottomSheetVisible) {
                    bottomSheetVisible = true
                    if (scrimViewEnabled)
                        Anim.fadeIn(scrimView, 300, null)
                }
            }
        })

        content.background.colorFilter = PorterDuffColorFilter(
            elevateSurface(context, dp = 8),
            PorterDuff.Mode.SRC_ATOP
        )

        // steal the focus from any EditTexts
        dragBar.requestFocus()

        list.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = this@NavBottomSheet.adapter
        }

        toggleGroup.addOnButtonCheckedListener(toggleGroupCheckedListener)
        textInputEditText.addTextChangedListener(textInputWatcher)
    }

    var onCloseListener: (() -> Unit)? = null

    /*    _____ _
         |_   _| |
           | | | |_ ___ _ __ ___  ___
           | | | __/ _ \ '_ ` _ \/ __|
          _| |_| ||  __/ | | | | \__ \
         |_____|\__\___|_| |_| |_|__*/
    operator fun plusAssign(item: IBottomSheetItem<*>) {
        appendItem(item)
    }
    fun appendItem(item: IBottomSheetItem<*>) {
        items.add(item)
        adapter.notifyDataSetChanged()
        //adapter.notifyItemInserted(items.size - 1)
    }
    fun appendItems(vararg items: IBottomSheetItem<*>) {
        this.items.addAll(items)
        adapter.notifyDataSetChanged()
        //adapter.notifyItemRangeInserted(this.items.size - items.size, items.size)
    }
    fun prependItem(item: IBottomSheetItem<*>) {
        items.add(0, item)
        adapter.notifyDataSetChanged()
        //adapter.notifyItemInserted(0)
    }
    fun prependItems(vararg items: IBottomSheetItem<*>) {
        this.items.addAll(0, items.toList())
        adapter.notifyDataSetChanged()
        //adapter.notifyItemRangeInserted(0, items.size)
    }
    fun addItemAt(index: Int, item: IBottomSheetItem<*>) {
        items.add(index, item)
        adapter.notifyDataSetChanged()
        //adapter.notifyItemInserted(index)
    }
    fun removeItemById(id: Int) {
        items.filterNot { it.id == id }
    }
    fun removeItemAt(index: Int) {
        items.removeAt(index)
        adapter.notifyDataSetChanged()
        //adapter.notifyItemRemoved(index)
    }
    fun removeAllItems() {
        items.clear()
        adapter.notifyDataSetChanged()
    }
    fun removeAllStatic() {
        items.removeAll { !it.isContextual }
        adapter.notifyDataSetChanged()
    }
    fun removeAllContextual() {
        items.removeAll { it.isContextual }
        adapter.notifyDataSetChanged()
    }
    fun removeSeparators() {
        items.removeAll { it is BottomSheetSeparatorItem }
        adapter.notifyDataSetChanged()
    }

    fun getItemById(id: Int, run: (it: IBottomSheetItem<*>?) -> Unit) {
        items.singleOrNull { it.id == id }.also {
            run(it)
            if (it != null)
                adapter.notifyItemChanged(items.indexOf(it))
        }
    }
    fun getItemByIndex(index: Int, run: (it: IBottomSheetItem<*>?) -> Unit) {
        items.getOrNull(index).also {
            run(it)
            if (it != null)
                adapter.notifyItemChanged(index)
        }
    }


    /*    _______                _
         |__   __|              | |
            | | ___   __ _  __ _| | ___    __ _ _ __ ___  _   _ _ __
            | |/ _ \ / _` |/ _` | |/ _ \  / _` | '__/ _ \| | | | '_ \
            | | (_) | (_| | (_| | |  __/ | (_| | | | (_) | |_| | |_) |
            |_|\___/ \__, |\__, |_|\___|  \__, |_|  \___/ \__,_| .__/
                      __/ | __/ |          __/ |               | |
                     |___/ |___/          |___/                |*/
    var toggleGroupEnabled
        get() = toggleGroupContainer.visibility == View.VISIBLE
        set(value) { toggleGroupContainer.visibility = if (value) View.VISIBLE else View.GONE }
    var toggleGroupTitle
        get() = toggleGroupTitleView.text.toString()
        set(value) { toggleGroupTitleView.text = value }
    var toggleGroupSelectionMode: Int = TOGGLE_GROUP_SORTING_ORDER
        set(value) {
            field = value
            toggleGroup.isSingleSelection = value != TOGGLE_GROUP_MULTIPLE_SELECTION
        }

    private fun toggleGroupGetIconicsDrawable(context: Context, icon: IIcon?): Drawable? {
        if (icon == null)
            return null
        return IconicsDrawable(context, icon).apply {
            sizeDp = 24
        }
    }

    fun toggleGroupAddItem(id: Int, text: String, @DrawableRes icon: Int, defaultSortOrder: Int = SORT_MODE_ASCENDING) {
        toggleGroupAddItem(id, text, context.getDrawableFromRes(icon), defaultSortOrder)
    }
    fun toggleGroupAddItem(id: Int, text: String, icon: IIcon, defaultSortOrder: Int = SORT_MODE_ASCENDING) {
        toggleGroupAddItem(id, text, toggleGroupGetIconicsDrawable(context, icon), defaultSortOrder)
    }
    fun toggleGroupAddItem(id: Int, text: String, icon: Drawable?, defaultSortOrder: Int = SORT_MODE_ASCENDING) {
        if (id < 0)
            throw IllegalArgumentException("ID cannot be less than 0")
        toggleGroup.addView(MaterialButton(context, null, R.attr.materialButtonOutlinedStyle).apply {
            this.id = id + 1
            this.tag = defaultSortOrder
            this.text = text
            this.icon = icon
        }, WRAP_CONTENT, WRAP_CONTENT)
    }
    fun toggleGroupCheck(id: Int) {
        toggleGroup.check(id)
    }
    fun toggleGroupRemoveItems() {
        toggleGroup.removeAllViews()
    }

    private val toggleGroupCheckedListener = MaterialButtonToggleGroup.OnButtonCheckedListener { group, checkedId, isChecked ->
        if (group.checkedButtonId == View.NO_ID) {
            group.check(checkedId)
            return@OnButtonCheckedListener
        }
        /* TAG bit order
         * bit 0 = default sorting mode
         * bit 1 = is checked
         * bit 2 = current sorting mode
         */
        if (toggleGroupSelectionMode == TOGGLE_GROUP_SORTING_ORDER) {
            val button = group.findViewById<MaterialButton>(checkedId) ?: return@OnButtonCheckedListener
            var tag = button.tag as Int
            var sortingMode: Int? = null
            if (isChecked) {
                sortingMode = if (tag and 0b010 == 1 shl 1) {
                    /* the view is checked and clicked once again */
                    if (tag and 0b100 == SORT_MODE_ASCENDING shl 2) SORT_MODE_DESCENDING else SORT_MODE_ASCENDING
                } else {
                    /* the view is first clicked so use the default sorting mode */
                    if (tag and 0b001 == SORT_MODE_ASCENDING) SORT_MODE_ASCENDING else SORT_MODE_DESCENDING
                }
                tag = tag and 0b001 /* retain only default sorting mode */
                tag = tag or 0b010 /* set as checked */
                tag = tag or (sortingMode shl 2) /* set new sorting mode */
            }
            else {
                tag = tag and 0b001 /* retain only default sorting mode */
            }
            button.tag = tag
            button.icon = toggleGroupGetIconicsDrawable(context, when (sortingMode) {
                SORT_MODE_ASCENDING -> NavLibFont.Icon.nav_sort_ascending
                SORT_MODE_DESCENDING -> NavLibFont.Icon.nav_sort_descending
                else -> null
            })
            if (sortingMode != null) {
                toggleGroupSortingOrderListener?.invoke(checkedId, sortingMode)
            }
        }
        else if (toggleGroup.isSingleSelection && isChecked) {
            toggleGroupSingleSelectionListener?.invoke(checkedId - 1)
        }
        else {
            toggleGroupMultipleSelectionListener?.invoke(checkedId - 1, isChecked)
        }
    }

    var toggleGroupSingleSelectionListener: ((id: Int) -> Unit)? = null
    var toggleGroupMultipleSelectionListener: ((id: Int, checked: Boolean) -> Unit)? = null
    var toggleGroupSortingOrderListener: ((id: Int, sortMode: Int) -> Unit)? = null


    /*    _______        _     _                   _
         |__   __|      | |   (_)                 | |
            | | _____  _| |_   _ _ __  _ __  _   _| |_
            | |/ _ \ \/ / __| | | '_ \| '_ \| | | | __|
            | |  __/>  <| |_  | | | | | |_) | |_| | |_
            |_|\___/_/\_\\__| |_|_| |_| .__/ \__,_|\__|
                                      | |
                                      |*/
    var textInputEnabled
        get() = textInputLayout.visibility == View.VISIBLE
        set(value) { textInputLayout.visibility = if (value) View.VISIBLE else View.GONE }
    var textInputText
        get() = textInputEditText.text.toString()
        set(value) { textInputEditText.setText(value) }
    var textInputHint
        get() = textInputLayout.hint.toString()
        set(value) { textInputLayout.hint = value }
    var textInputHelperText
        get() = textInputLayout.helperText.toString()
        set(value) { textInputLayout.helperText = value }
    var textInputError
        get() = textInputLayout.error
        set(value) { textInputLayout.error = value }
    var textInputIcon: Any?
        get() = textInputLayout.startIconDrawable
        set(value) {
            textInputLayout.startIconDrawable = when (value) {
                is Drawable -> value
                is IIcon -> IconicsDrawable(context).apply {
                    icon = value
                    sizeDp = 24
                    // colorInt = Color.BLACK
                }
                is Int -> context.getDrawableFromRes(value)
                else -> null
            }
        }

    private var textInputWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            textInputChangedListener?.onTextChanged(s?.toString() ?: "", start, before, count)
        }
    }

    interface OnTextInputChangedListener {
        fun onTextChanged(s: String, start: Int, before: Int, count: Int)
    }
    var textInputChangedListener: OnTextInputChangedListener? = null



    fun dispatchBottomBarEvent(event: MotionEvent) {
        val location = IntArray(2)
        bottomSheet.getLocationOnScreen(location)
        event.setLocation(event.rawX - location[0], event.rawY - location[1])
        bottomSheet.dispatchTouchEvent(event)
    }

    fun setContentPadding(left: Int, top: Int, right: Int, bottom: Int) {
        content.setPadding(left, top, right, bottom)
    }
    fun getContentView() = content

    var isOpen
        get() = bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN
        set(value) {
            bottomSheetBehavior.state = if (value) BottomSheetBehavior.STATE_EXPANDED else BottomSheetBehavior.STATE_HIDDEN
        }
    fun open() { isOpen = true }
    fun close() { isOpen = false }
    fun toggle() {
        if (!enable)
            return
        isOpen = !isOpen
    }
}
