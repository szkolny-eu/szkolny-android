/*
 * Copyright (c) Kuba Szczodrzyński 2024-6-20.
 */

package pl.szczodrzynski.edziennik.ui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import pl.szczodrzynski.edziennik.R

class IconicsMaterialButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = R.attr.materialButtonStyle,
) : MaterialButton(context, attrs, defStyle) {

    init {
        if (!isInEditMode) {
            val iconsBundle = Class.forName("com.mikepenz.iconics.internal.CompoundIconsBundle")
                .getDeclaredConstructor()
                .newInstance()
            val cls = Class.forName("com.mikepenz.iconics.internal.IconicsViewsAttrsApplier")
            val instance = cls.getField("INSTANCE").get(null)
            cls.getDeclaredMethod(
                "readIconicsTextView",
                Context::class.java,
                AttributeSet::class.java,
                iconsBundle::class.java,
            ).invoke(instance, context, attrs, iconsBundle)

            iconsBundle::class.java.getDeclaredMethod("setIcons", TextView::class.java)
                .invoke(iconsBundle, this)
        }
    }
}
