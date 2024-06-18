package pl.szczodrzynski.navlib

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import com.google.android.material.appbar.MaterialToolbar

class NavToolbar : MaterialToolbar {

    constructor(context: Context) : super(context) {
        create(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        create(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        create(attrs, defStyle)
    }

    var toolbarImage: ImageView? = null
        set(value) {
            field = value
            toolbarImage?.setOnClickListener {
                profileImageClickListener?.invoke()
            }
        }

    override fun setSubtitle(subtitle: CharSequence?) {
        if(subtitle.isNullOrEmpty()) {
            setPadding(0, 0, 0, 0)
            toolbarImage?.translationY = 0f
        } else {
            setPadding(0, -1, 0, 5)
            toolbarImage?.translationY = 6f
        }
        super.setSubtitle(subtitle)
    }

    private fun create(attrs: AttributeSet?, defStyle: Int) {

    }

    var subtitleFormat: Int? = null
    var subtitleFormatWithUnread: Int? = null

    var profileImageClickListener: (() -> Unit)? = null

    var profileImage
        get() = toolbarImage?.drawable
        set(value) {
            toolbarImage?.setImageDrawable(value)
        }
}