/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-22.
 */

package pl.szczodrzynski.edziennik.utils

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import androidx.annotation.NonNull
import androidx.appcompat.widget.AppCompatEditText

class TextInputKeyboardEdit : AppCompatEditText {

    /**
     * Keyboard Listener
     */
    internal var listener: KeyboardListener? = null
    private var selectionListener: ((Int, Int) -> Unit)? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        listener?.onStateChanged(this, true)
    }

    override fun onKeyPreIme(keyCode: Int, @NonNull event: KeyEvent): Boolean {
        if (event.keyCode == KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            listener?.onStateChanged(this, false)

            // Hide cursor
            isFocusable = false

            // Set EditText to be focusable again
            isFocusable = true
            isFocusableInTouchMode = true
        }
        return super.onKeyPreIme(keyCode, event)
    }

    fun setOnKeyboardListener(listener: KeyboardListener) {
        this.listener = listener
    }

    fun setSelectionChangedListener(listener: ((selectionStart: Int, selectionEnd: Int) -> Unit)?) {
        this.selectionListener = listener
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        selectionListener?.invoke(selStart, selEnd)
    }

    interface KeyboardListener {
        fun onStateChanged(keyboardEditText: TextInputKeyboardEdit, showing: Boolean)
    }
}
