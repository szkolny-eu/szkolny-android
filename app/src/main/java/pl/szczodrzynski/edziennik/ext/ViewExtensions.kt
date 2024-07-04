/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ext

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.button.MaterialButton

fun TextView.setText(@StringRes resid: Int, vararg formatArgs: Any) {
    text = context.getString(resid, *formatArgs)
}

@Suppress("UNCHECKED_CAST")
inline fun <T : View> T.onClick(crossinline onClickListener: (v: T) -> Unit) {
    setOnClickListener { v: View ->
        onClickListener(v as T)
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <T : View> T.onLongClick(crossinline onLongClickListener: (v: T) -> Boolean) {
    setOnLongClickListener { v: View ->
        onLongClickListener(v as T)
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <T : CompoundButton> T.onChange(crossinline onChangeListener: (v: T, isChecked: Boolean) -> Unit) {
    setOnCheckedChangeListener { buttonView, isChecked ->
        onChangeListener(buttonView as T, isChecked)
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <T : MaterialButton> T.onChange(crossinline onChangeListener: (v: T, isChecked: Boolean) -> Unit) {
    clearOnCheckedChangeListeners()
    addOnCheckedChangeListener { buttonView, isChecked ->
        onChangeListener(buttonView as T, isChecked)
    }
}

fun View.attachToastHint(stringRes: Int) = onLongClick {
    Toast.makeText(it.context, stringRes, Toast.LENGTH_SHORT).show()
    true
}

fun View.detachToastHint() = setOnLongClickListener(null)

/**
 * Convert a value in dp to pixels.
 */
val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()
/**
 * Convert a value in pixels to dp.
 */
val Int.px: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

fun View.findParentById(targetId: Int): View? {
    if (id == targetId) {
        return this
    }
    val viewParent = this.parent ?: return null
    if (viewParent is View) {
        return viewParent.findParentById(targetId)
    }
    return null
}

fun CheckBox.trigger() { isChecked = !isChecked }

inline fun RadioButton.setOnSelectedListener(crossinline listener: (buttonView: CompoundButton) -> Unit)
        = setOnCheckedChangeListener { buttonView, isChecked -> if (isChecked) listener(buttonView) }

fun TextView.getTextPosition(range: IntRange): Rect {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // Initialize global value
    var parentTextViewRect = Rect()

    // Initialize values for the computing of clickedText position
    //val completeText = parentTextView.text as SpannableString
    val textViewLayout = this.layout

    val startOffsetOfClickedText = range.first//completeText.getSpanStart(clickedText)
    val endOffsetOfClickedText = range.last//completeText.getSpanEnd(clickedText)
    var startXCoordinatesOfClickedText = textViewLayout.getPrimaryHorizontal(startOffsetOfClickedText)
    var endXCoordinatesOfClickedText = textViewLayout.getPrimaryHorizontal(endOffsetOfClickedText)

    // Get the rectangle of the clicked text
    val currentLineStartOffset = textViewLayout.getLineForOffset(startOffsetOfClickedText)
    val currentLineEndOffset = textViewLayout.getLineForOffset(endOffsetOfClickedText)
    val keywordIsInMultiLine = currentLineStartOffset != currentLineEndOffset
    textViewLayout.getLineBounds(currentLineStartOffset, parentTextViewRect)

    // Update the rectangle position to his real position on screen
    val parentTextViewLocation = intArrayOf(0, 0)
    this.getLocationOnScreen(parentTextViewLocation)

    val parentTextViewTopAndBottomOffset = (parentTextViewLocation[1] - this.scrollY + this.compoundPaddingTop)
    parentTextViewRect.top += parentTextViewTopAndBottomOffset
    parentTextViewRect.bottom += parentTextViewTopAndBottomOffset

    // In the case of multi line text, we have to choose what rectangle take
    if (keywordIsInMultiLine) {
        val screenHeight = windowManager.defaultDisplay.height
        val dyTop = parentTextViewRect.top
        val dyBottom = screenHeight - parentTextViewRect.bottom
        val onTop = dyTop > dyBottom

        if (onTop) {
            endXCoordinatesOfClickedText = textViewLayout.getLineRight(currentLineStartOffset);
        } else {
            parentTextViewRect = Rect()
            textViewLayout.getLineBounds(currentLineEndOffset, parentTextViewRect);
            parentTextViewRect.top += parentTextViewTopAndBottomOffset;
            parentTextViewRect.bottom += parentTextViewTopAndBottomOffset;
            startXCoordinatesOfClickedText = textViewLayout.getLineLeft(currentLineEndOffset);
        }
    }

    parentTextViewRect.left += (
            parentTextViewLocation[0] +
                    startXCoordinatesOfClickedText +
                    this.compoundPaddingLeft -
                    this.scrollX
            ).toInt()
    parentTextViewRect.right = (
            parentTextViewRect.left +
                    endXCoordinatesOfClickedText -
                    startXCoordinatesOfClickedText
            ).toInt()

    return parentTextViewRect
}

inline fun ViewPager.addOnPageSelectedListener(crossinline block: (position: Int) -> Unit) = addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
    override fun onPageScrollStateChanged(state: Int) {}
    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
    override fun onPageSelected(position: Int) { block(position) }
})

fun View.removeFromParent() {
    (parent as? ViewGroup)?.removeView(this)
}

fun View.appendView(child: View) {
    val parent = parent as? ViewGroup ?: return
    val index = parent.indexOfChild(this)
    parent.addView(child, index + 1)
}
