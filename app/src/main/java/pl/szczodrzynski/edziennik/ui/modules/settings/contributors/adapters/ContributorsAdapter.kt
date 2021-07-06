package pl.szczodrzynski.edziennik.ui.modules.settings.contributors.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import coil.load
import coil.transform.CircleCropTransformation
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.szkolny.request.ContributorItem

class ContributorsAdapter(context: Context?, contributors: List<ContributorItem>?): BaseAdapter() {

    private val mContext: Context?
    private val mContributors: List<ContributorItem>?

    init {
        mContext = context
        mContributors = contributors
    }

    override fun getCount() = mContributors?.size ?: 0
    override fun getItemId(position: Int) = position.toLong()
    override fun getItem(position: Int) = mContributors?.get(position)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val layoutInflater = LayoutInflater.from(mContext)
        val item = layoutInflater.inflate(R.layout.contributors_list_item, parent, false)

        val username = item.findViewById<TextView>(R.id.contributorUsername)
        val name = item.findViewById<TextView>(R.id.contributorName)
        val profileImage = item.findViewById<ImageView>(R.id.contributorProfile)

        val contributor = getItem(position)
        var contributions = contributor?.contributions

        if (contributions == null) contributions = 0

        name.text = mContributors?.get(position)?.name
        username.text = "@${contributor?.login} - " + mContext?.resources?.getQuantityString(
            R.plurals.contributions_quantity,
            contributions
        )

        profileImage.load(contributor?.avatarUrl) {
            transformations(CircleCropTransformation())
        }

        item.setOnClickListener {
            val openURL = Intent(Intent.ACTION_VIEW)
            openURL.data = Uri.parse(contributor?.profileUrl)
            mContext?.startActivity(openURL)
        }

        return item
    }
}
