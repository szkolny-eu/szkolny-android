package pl.szczodrzynski.edziennik.ui.modules.settings

import android.content.Context
import android.net.Uri
import android.os.Bundle

import com.danielstone.materialaboutlibrary.ConvenienceBuilder
import com.danielstone.materialaboutlibrary.MaterialAboutActivity
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import com.danielstone.materialaboutlibrary.util.OpenSourceLicense
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.utils.Themes

class SettingsLicenseActivity : MaterialAboutActivity() {

    var foregroundColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        val app = application as App
        setTheme(Themes.appTheme)
        foregroundColor = Themes.getPrimaryTextColor(this)
        super.onCreate(savedInstanceState)
    }

    private fun createLicenseCard(
            context: Context,
            libraryTitle: CharSequence,
            copyrightYear: CharSequence,
            copyrightName: CharSequence,
            license: OpenSourceLicense,
            libraryUrl: String): MaterialAboutCard {
        val licenseItem = MaterialAboutActionItem.Builder()
                .icon(IconicsDrawable(this)
                        .icon(CommunityMaterial.Icon.cmd_book)
                        .colorInt(foregroundColor)
                        .sizeDp(18))
                .setIconGravity(MaterialAboutActionItem.GRAVITY_TOP)
                .text(libraryTitle)
                .subText(String.format(getString(license.resourceId), copyrightYear, copyrightName))
                .setOnClickAction(ConvenienceBuilder.createWebsiteOnClickAction(context, Uri.parse(libraryUrl)))
                .build()

        return MaterialAboutCard.Builder().addItem(licenseItem).build()
    }

    override fun getMaterialAboutList(context: Context): MaterialAboutList {

        return MaterialAboutList(
                createLicenseCard(this,
                        "OkHttp",
                        "",
                        "square",
                        OpenSourceLicense.APACHE_2,
                        "https://github.com/square/okhttp/"),
                createLicenseCard(this,
                        "MHttp",
                        "2018",
                        "Mot.",
                        OpenSourceLicense.APACHE_2,
                        "https://github.com/motcwang/MHttp/"),
                createLicenseCard(this,
                        "AgendaCalendarView",
                        "2015",
                        "Thibault Guégan",
                        OpenSourceLicense.APACHE_2,
                        "https://github.com/Tibolte/AgendaCalendarView/"),
                createLicenseCard(this,
                        "Material Calendar View",
                        "2017",
                        "Applandeo sp. z o.o.",
                        OpenSourceLicense.APACHE_2,
                        "https://github.com/Applandeo/Material-Calendar-View/"),
                createLicenseCard(this,
                        "Android-Job",
                        "2007-2017",
                        "Evernote Corporation",
                        OpenSourceLicense.APACHE_2,
                        "https://github.com/evernote/android-job/"),
                createLicenseCard(this,
                        "Custom Activity On Crash",
                        "",
                        "Eduard Ereza MartĂ­nez (Ereza)",
                        OpenSourceLicense.APACHE_2,
                        "https://github.com/Ereza/CustomActivityOnCrash/"),
                createLicenseCard(this,
                        "Android-Iconics",
                        "2018",
                        "Mike Penz",
                        OpenSourceLicense.APACHE_2,
                        "https://github.com/mikepenz/Android-Iconics/"),
                createLicenseCard(this,
                        "MaterialDrawer",
                        "2016",
                        "Mike Penz",
                        OpenSourceLicense.APACHE_2,
                        "https://github.com/mikepenz/MaterialDrawer/"),
                createLicenseCard(this,
                        "Material Dialogs",
                        "2014-2016",
                        "Aidan Michael Follestad",
                        OpenSourceLicense.MIT,
                        "https://github.com/afollestad/material-dialogs/"),
                createLicenseCard(this,
                        "MaterialDateTimePicker",
                        "2014",
                        "Wouter Dullaert",
                        OpenSourceLicense.APACHE_2,
                        "https://github.com/wdullaer/MaterialDateTimePicker/"),
                createLicenseCard(this,
                        "ColorPicker",
                        "2016",
                        "Jared Rummler, 2015 Daniel Nilsson",
                        OpenSourceLicense.APACHE_2,
                        "https://github.com/jaredrummler/ColorPicker/"),
                createLicenseCard(this,
                        "material-about-library",
                        "2016-2018",
                        "Daniel Stone",
                        OpenSourceLicense.APACHE_2,
                        "https://github.com/daniel-stoneuk/material-about-library/"),
                createLicenseCard(this,
                        "material-intro",
                        "2017",
                        "Jan Heinrich Reimer",
                        OpenSourceLicense.MIT,
                        "https://github.com/heinrichreimer/material-intro/"),
                createLicenseCard(this,
                        "JsonViewer",
                        "2017",
                        "smuyyh",
                        OpenSourceLicense.APACHE_2,
                        "https://github.com/smuyyh/JsonViewer/"),
                createLicenseCard(this,
                        "ShortcutBadger",
                        "2014",
                        "Leo Lin",
                        OpenSourceLicense.APACHE_2,
                        "https://github.com/leolin310148/ShortcutBadger/"),
                createLicenseCard(this,
                        "Android Image Cropper",
                        "2016",
                        "Arthur Teplitzki, 2013 Edmodo, Inc.",
                        OpenSourceLicense.APACHE_2,
                        "https://github.com/ArthurHub/Android-Image-Cropper/"),
                createLicenseCard(this,
                        "Material Tap Target Prompt",
                        "2016-2018",
                        "Samuel Wall",
                        OpenSourceLicense.APACHE_2,
                        "https://github.com/sjwall/MaterialTapTargetPrompt/"),
                createLicenseCard(this,
                        "Android Swipe Layout",
                        "2014",
                        "代码家 (daimajia)",
                        OpenSourceLicense.MIT,
                        "https://github.com/daimajia/AndroidSwipeLayout/"),
                createLicenseCard(this,
                        "barcodescanner (ZXing)",
                        "2014",
                        "Dushyanth Maguluru",
                        OpenSourceLicense.APACHE_2,
                        "https://github.com/dm77/barcodescanner/"),
                createLicenseCard(this,
                        "CircularProgressIndicator",
                        "2018",
                        "Anton Kozyriatskyi",
                        OpenSourceLicense.APACHE_2,
                        "https://github.com/antonKozyriatskyi/CircularProgressIndicator/")


                /*createLicenseCard(this,
                        "NoNonsense-FilePicker",
                        "",
                        "Jonas Kalderstam (spacecowboy)",
                        OpenSourceLicense.GNU_GPL_3,
                        "https://github.com/spacecowboy/NoNonsense-FilePicker/")*/

        )
    }

    override fun getActivityTitle(): CharSequence? {
        return getString(R.string.settings_about_licenses_text)
    }

}
