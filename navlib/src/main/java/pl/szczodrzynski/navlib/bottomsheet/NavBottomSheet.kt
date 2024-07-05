package pl.szczodrzynski.navlib.bottomsheet

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import pl.szczodrzynski.navlib.Anim
import pl.szczodrzynski.navlib.bottomsheet.items.IBottomSheetItem
import pl.szczodrzynski.navlib.databinding.NavBottomSheetBinding
import pl.szczodrzynski.navlib.elevateSurface

class NavBottomSheet @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : CoordinatorLayout(context, attrs, defStyle) {

    private val b = NavBottomSheetBinding.inflate(LayoutInflater.from(context), this, false)

    private val scrimView
        get() = b.bsScrim
    private val bottomSheet
        get() = b.bsView
    private val content
        get() = b.bsContent
    private val dragBar
        get() = b.bsDragBar
    private val list
        get() = b.bsList

    private var bottomSheetBehavior = BottomSheetBehavior.from<View>(bottomSheet)
    private var bottomSheetVisible = false

    private val items = ArrayList<IBottomSheetItem<*>>()
    private val adapter = BottomSheetAdapter(items)

    init {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        ViewCompat.setOnApplyWindowInsetsListener(list) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view. Here the system is setting
            // only the bottom, left, and right dimensions, but apply whichever insets are
            // appropriate to your layout. You can also update the view padding
            // if that's more appropriate.
            view.setPadding(insets.left, 0, insets.right, insets.bottom)
            // Return CONSUMED if you don't want want the window insets to keep being
            // passed down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        scrimView.setOnClickListener {
            isOpen = false
        }

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(v: View, p1: Float) {}
            override fun onStateChanged(v: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN && bottomSheetVisible) {
                    bottomSheetVisible = false
                    bottomSheet.scrollTo(0, 0)
                    Anim.fadeOut(scrimView, 300, null)
                    // steal the focus from any EditTexts
                    dragBar.requestFocus()
                    hideKeyboard()
                    onCloseListener?.invoke()
                } else if (!bottomSheetVisible) {
                    bottomSheetVisible = true
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
    }

    fun hideKeyboard() = context.getSystemService<InputMethodManager>()
        ?.hideSoftInputFromWindow(rootView.windowToken, 0)

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

    fun dispatchBottomBarEvent(view: View, event: MotionEvent): Boolean {
        val location = IntArray(2)
        bottomSheet.getLocationOnScreen(location)
        event.setLocation(event.rawX - location[0], event.rawY - location[1])
        bottomSheet.dispatchTouchEvent(event)
        return true
    }

    var isOpen
        get() = bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN
        set(value) {
            bottomSheetBehavior.state =
                if (value)
                    BottomSheetBehavior.STATE_EXPANDED
                else
                    BottomSheetBehavior.STATE_HIDDEN
        }

    fun open() {
        isOpen = true
    }

    fun close() {
        isOpen = false
    }

    fun toggle() {
        isOpen = !isOpen
    }
}
