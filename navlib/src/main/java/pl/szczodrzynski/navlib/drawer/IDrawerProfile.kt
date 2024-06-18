package pl.szczodrzynski.navlib.drawer

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import pl.szczodrzynski.navlib.ImageHolder

interface IDrawerProfile {
    val id: Int
    var name: String
    var subname: String?
    var image: String?

    fun getImageDrawable(context: Context): Drawable?
    fun getImageHolder(context: Context): ImageHolder?
    fun applyImageTo(imageView: ImageView)
}