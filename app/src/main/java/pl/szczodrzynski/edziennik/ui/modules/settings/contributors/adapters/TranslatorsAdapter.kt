package pl.szczodrzynski.edziennik.ui.modules.settings.contributors.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.szkolny.request.ContributorItem
import android.util.Log
import android.widget.ImageView
import coil.load
import coil.transform.CircleCropTransformation

class TranslatorsAdapter(context: Context?, translators: List<ContributorItem>?): BaseAdapter() {

    private val mContext: Context?
    private val mTranslators: List<ContributorItem>?

    init {
        mContext = context
        mTranslators = translators

    }

    override fun getCount() = mTranslators?.size ?: 0
    override fun getItemId(position: Int) = position.toLong()
    override fun getItem(position: Int) = mTranslators?.get(position)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val layoutInflater = LayoutInflater.from(mContext)
        val item = layoutInflater.inflate(R.layout.contributors_list_item, parent, false)

        val username = item.findViewById<TextView>(R.id.contributorUsername)
        val name = item.findViewById<TextView>(R.id.contributorName)
        val profileImage = item.findViewById<ImageView>(R.id.contributorProfile)

        val translator = getItem(position)
        var translations = translator?.contributions

        if (translations == null) translations = 0

        name.text = translator?.name
        username.text = "@${translator?.login} - $translations translations"
        profileImage.load(translator?.avatarUrl) {
            transformations(CircleCropTransformation())
        }

        item.setOnClickListener {
            val openURL = Intent(Intent.ACTION_VIEW)
            openURL.data = Uri.parse(translator?.profileUrl)
            mContext?.startActivity(openURL)
        }

        return item
    }
}
