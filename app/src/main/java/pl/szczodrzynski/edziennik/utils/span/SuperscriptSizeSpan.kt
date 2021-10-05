/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-3.
 */

package pl.szczodrzynski.edziennik.utils.span

import android.text.TextPaint
import android.text.style.SuperscriptSpan

class SuperscriptSizeSpan(
    private val size: Int,
    private val dip: Boolean,
) : SuperscriptSpan() {

    override fun updateDrawState(textPaint: TextPaint) {
        super.updateDrawState(textPaint)
        if (dip) {
            textPaint.textSize = size * textPaint.density
        } else {
            textPaint.textSize = size.toFloat()
        }
    }

    override fun updateMeasureState(textPaint: TextPaint) {
        super.updateMeasureState(textPaint)
        if (dip) {
            textPaint.textSize = size * textPaint.density
        } else {
            textPaint.textSize = size.toFloat()
        }
    }
}
