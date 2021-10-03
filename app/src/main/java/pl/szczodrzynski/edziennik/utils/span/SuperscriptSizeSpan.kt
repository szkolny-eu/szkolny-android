/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-3.
 */

package pl.szczodrzynski.edziennik.utils.span

import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan

class SuperscriptSizeSpan(size: Int, dip: Boolean) : AbsoluteSizeSpan(size, dip) {

    override fun updateDrawState(textPaint: TextPaint) {
        super.updateDrawState(textPaint)
        textPaint.baselineShift += (textPaint.ascent() / 2).toInt()
    }

    override fun updateMeasureState(textPaint: TextPaint) {
        super.updateMeasureState(textPaint)
        textPaint.baselineShift += (textPaint.ascent() / 2).toInt()
    }
}
