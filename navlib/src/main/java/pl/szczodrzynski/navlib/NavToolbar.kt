package pl.szczodrzynski.navlib

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import com.google.android.material.appbar.MaterialToolbar
import pl.szczodrzynski.navlib.bottomsheet.NavBottomSheet

class NavToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : MaterialToolbar(context, attrs, defStyle), NavMenuBarBase {

    internal lateinit var navView: NavView
    override lateinit var bottomSheet: NavBottomSheet

    /**
     * Shows the toolbar and sets the contentView's margin to be
     * below the toolbar.
     */
    var enable
        get() = isVisible
        set(value) {
            isVisible = value
            navView.setContentMargins()
        }

    var toolbarImage: ImageView? = null
        set(value) {
            field = value
            toolbarImage?.setOnClickListener {
                profileImageClickListener?.invoke()
            }
        }

    override var drawerClickListener: (() -> Unit)? = null
    override var menuClickListener: (() -> Unit)? = null
    var profileImageClickListener: (() -> Unit)? = null

    override fun setSubtitle(subtitle: CharSequence?) {
        if (subtitle.isNullOrEmpty()) {
            setPadding(0, 0, 0, 0)
            toolbarImage?.translationY = 0f
        } else {
            setPadding(0, -1, 0, 5)
            toolbarImage?.translationY = 6f
        }
        super.setSubtitle(subtitle)
    }

    var profileImage
        get() = toolbarImage?.drawable
        set(value) {
            toolbarImage?.setImageDrawable(value)
        }
}
