package pl.szczodrzynski.navlib

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.utils.actionBar
import com.mikepenz.iconics.utils.sizeDp
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import pl.droidsonroids.gif.GifDrawable
import java.io.FileNotFoundException

/**
 * Created by mikepenz on 13.07.15.
 */

open class ImageHolder : com.mikepenz.materialdrawer.holder.ImageHolder {

    constructor(@DrawableRes iconRes: Int, colorFilter: Int?) : super(iconRes) {
        this.colorFilter = colorFilter
    }
    constructor(iicon: IIcon) : super(null as Drawable?) {
        this.iicon = iicon
    }
    constructor() : super()
    constructor(url: String) : super(url)
    constructor(uri: Uri) : super(uri)
    constructor(icon: Drawable?) : super(icon)
    constructor(bitmap: Bitmap) : super(bitmap)
    constructor(iconRes: Int) : super(iconRes)

    var iicon: IIcon? = null
    @ColorInt
    var colorFilter: Int? = null
    var colorFilterMode: PorterDuff.Mode = PorterDuff.Mode.DST_OVER


    /**
     * sets an existing image to the imageView
     *
     * @param imageView
     * @param tag       used to identify imageViews and define different placeholders
     * @return true if an image was set
     */
    override fun applyTo(imageView: ImageView, tag: String?): Boolean {
        val ii = iicon

        if (uri != null) {
            if (uri.toString().endsWith(".gif", true)) {
                imageView.setImageDrawable(GifDrawable(uri.toString()))
            }
            else {
                val consumed = DrawerImageLoader.instance.setImage(imageView, uri!!, tag)
                if (!consumed) {
                    imageView.setImageURI(uri)
                }
            }
        } else if (icon != null) {
            imageView.setImageDrawable(icon)
        } else if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
        } else if (iconRes != -1) {
            imageView.setImageResource(iconRes)
        } else if (ii != null) {
            imageView.setImageDrawable(IconicsDrawable(imageView.context, ii).actionBar())
        } else {
            imageView.setImageBitmap(null)
            return false
        }

        if (colorFilter != null) {
            imageView.colorFilter = PorterDuffColorFilter(colorFilter!!, colorFilterMode)
        }

        return true
    }

    /**
     * this only handles Drawables
     *
     * @param ctx
     * @param iconColor
     * @param tint
     * @return
     */
    override fun decideIcon(ctx: Context, iconColor: ColorStateList, tint: Boolean, paddingDp: Int): Drawable? {
        var icon: Drawable? = icon
        val ii = iicon
        val uri = uri

        when {
            ii != null -> icon = IconicsDrawable(ctx).apply {
                this.icon = ii
                colorList = iconColor
                sizeDp = 24
            }
            iconRes != -1 -> icon = AppCompatResources.getDrawable(ctx, iconRes)
            uri != null -> try {
                val inputStream = ctx.contentResolver.openInputStream(uri)
                icon = Drawable.createFromStream(inputStream, uri.toString())
            } catch (e: FileNotFoundException) {
                //no need to handle this
            }
        }
        //if we got an icon AND we have auto tinting enabled AND it is no IIcon, tint it ;)
        if (icon != null && tint && iicon == null) {
            icon = icon.mutate()
            icon.setColorFilter(iconColor.defaultColor, PorterDuff.Mode.SRC_IN)
        }
        return icon
    }
}
