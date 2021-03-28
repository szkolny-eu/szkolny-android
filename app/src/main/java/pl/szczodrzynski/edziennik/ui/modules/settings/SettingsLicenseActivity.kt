package pl.szczodrzynski.edziennik.ui.modules.settings

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.danielstone.materialaboutlibrary.ConvenienceBuilder
import com.danielstone.materialaboutlibrary.ConvenienceBuilder.createLicenseCard
import com.danielstone.materialaboutlibrary.MaterialAboutActivity
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import com.danielstone.materialaboutlibrary.util.OpenSourceLicense
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.resolveColor
import pl.szczodrzynski.edziennik.utils.Themes

class SettingsLicenseActivity : MaterialAboutActivity() {

    var foregroundColor: Int = 0

    private val icon
        get() = IconicsDrawable(this).apply {
            icon = CommunityMaterial.Icon.cmd_book_outline
            colorInt = foregroundColor
            sizeDp = 18
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(
            if (Themes.isDark)
                R.style.Theme_MaterialComponents
            else
                R.style.Theme_MaterialComponents_Light
        )
        foregroundColor = if (Themes.isDark)
            R.color.primaryTextDark.resolveColor(this)
        else
            R.color.primaryTextLight.resolveColor(this)
        super.onCreate(savedInstanceState)
    }

    private fun license(
        title: String,
        year: String,
        copyright: String,
        license: OpenSourceLicense,
        url: String
    ): MaterialAboutCard {
        return createLicenseCard(this, icon, title, year, copyright, license).also {
            (it.items[0] as MaterialAboutActionItem).onClickAction =
                ConvenienceBuilder.createWebsiteOnClickAction(
                    this,
                    Uri.parse(url)
                )
        }
    }

    override fun getMaterialAboutList(context: Context) = MaterialAboutList(
        license(
            "Kotlin",
            "2000-2020",
            "JetBrains s.r.o. and Kotlin Programming Language contributors.",
            OpenSourceLicense.APACHE_2,
            "https://github.com/JetBrains/kotlin"
        ),

        license(
            "Android Jetpack",
            "",
            "The Android Open Source Project",
            OpenSourceLicense.APACHE_2,
            "https://github.com/androidx/androidx"
        ),

        license(
            "Material Components for Android",
            "2014-2020",
            "Google, Inc.",
            OpenSourceLicense.APACHE_2,
            "https://github.com/material-components/material-components-android"
        ),

        license(
            "OkHttp",
            "2019",
            "Square, Inc.",
            OpenSourceLicense.APACHE_2,
            "https://github.com/square/okhttp"
        ),

        license(
            "Retrofit",
            "2013",
            "Square, Inc.",
            OpenSourceLicense.APACHE_2,
            "https://github.com/square/retrofit"
        ),

        license(
            "Gson",
            "2008",
            "Google Inc.",
            OpenSourceLicense.APACHE_2,
            "https://github.com/google/gson"
        ),

        license(
            "jsoup",
            "2009-2021",
            "Jonathan Hedley",
            OpenSourceLicense.MIT,
            "https://github.com/jhy/jsoup"
        ),

        license(
            "jspoon",
            "2017",
            "Droids On Roids",
            OpenSourceLicense.MIT,
            "https://github.com/DroidsOnRoids/jspoon"
        ),

        license(
            "AgendaCalendarView",
            "2015",
            "Thibault Guégan",
            OpenSourceLicense.APACHE_2,
            "https://github.com/szkolny-eu/agendacalendarview"
        ),

        license(
            "CafeBar",
            "2017",
            "Dani Mahardhika",
            OpenSourceLicense.APACHE_2,
            "https://github.com/szkolny-eu/cafebar"
        ),

        license(
            "FSLogin",
            "2021",
            "kuba2k2",
            OpenSourceLicense.MIT,
            "https://github.com/szkolny-eu/FSLogin"
        ),

        license(
            "material-about-library",
            "2016-2020",
            "Daniel Stone",
            OpenSourceLicense.APACHE_2,
            "https://github.com/szkolny-eu/material-about-library"
        ),

        license(
            "MHttp",
            "2018",
            "Mot.",
            OpenSourceLicense.APACHE_2,
            "https://github.com/szkolny-eu/mhttp"
        ),

        license(
            "Nachos for Android",
            "2016",
            "Hootsuite Media, Inc.",
            OpenSourceLicense.APACHE_2,
            "https://github.com/szkolny-eu/nachos"
        ),

        license(
            "Material Number Sliding Picker",
            "2019",
            "Alessandro Crugnola",
            OpenSourceLicense.MIT,
            "https://github.com/kuba2k2/NumberSlidingPicker"
        ),

        license(
            "RecyclerTabLayout",
            "2017",
            "nshmura",
            OpenSourceLicense.APACHE_2,
            "https://github.com/kuba2k2/RecyclerTabLayout"
        ),

        license(
            "Tachyon",
            "2019",
            "LinkedIn Corporation",
            OpenSourceLicense.BSD,
            "https://github.com/kuba2k2/Tachyon"
        ),

        license(
            "Android-Iconics",
            "2021",
            "Mike Penz",
            OpenSourceLicense.APACHE_2,
            "https://github.com/mikepenz/Android-Iconics"
        ),

        license(
            "Custom Activity On Crash library",
            "2020",
            "Eduard Ereza Martínez",
            OpenSourceLicense.APACHE_2,
            "https://github.com/Ereza/CustomActivityOnCrash"
        ),

        license(
            "Material-Calendar-View",
            "2017",
            "Applandeo sp. z o.o.",
            OpenSourceLicense.APACHE_2,
            "https://github.com/Applandeo/Material-Calendar-View"
        ),

        license(
            "Android Swipe Layout",
            "2014",
            "代码家",
            OpenSourceLicense.MIT,
            "https://github.com/daimajia/AndroidSwipeLayout"
        ),

        license(
            "CircularProgressIndicator",
            "2018",
            "Anton Kozyriatskyi",
            OpenSourceLicense.APACHE_2,
            "https://github.com/antonKozyriatskyi/CircularProgressIndicator"
        ),

        license(
            "ChatMessageView",
            "2019",
            "Tsubasa Nakayama",
            OpenSourceLicense.APACHE_2,
            "https://github.com/bassaer/ChatMessageView"
        ),

        license(
            "Android Image Cropper",
            "2016 Arthur Teplitzki,",
            "2013 Edmodo, Inc.",
            OpenSourceLicense.APACHE_2,
            "https://github.com/CanHub/Android-Image-Cropper"
        ),

        license(
            "Chucker",
            "2018-2020 Chucker Team,",
            "2017 Jeff Gilfelt",
            OpenSourceLicense.APACHE_2,
            "https://github.com/ChuckerTeam/chucker"
        ),

        license(
            "Android-Snowfall",
            "2016",
            "JetRadar",
            OpenSourceLicense.APACHE_2,
            "https://github.com/JetradarMobile/android-snowfall"
        ),

        license(
            "UONET+ Request Signer",
            "2019",
            "Wulkanowy",
            OpenSourceLicense.MIT,
            "https://github.com/wulkanowy/uonet-request-signer"
        ),

        license(
            "material-intro",
            "2017",
            "Jan Heinrich Reimer",
            OpenSourceLicense.MIT,
            "https://github.com/heinrichreimer/material-intro"
        ),

        license(
            "HyperLog Android",
            "2018",
            "HyperTrack",
            OpenSourceLicense.MIT,
            "https://github.com/hypertrack/hyperlog-android"
        ),

        license(
            "Color Picker",
            "2016 Jared Rummler,",
            "2015 Daniel Nilsson",
            OpenSourceLicense.APACHE_2,
            "https://github.com/jaredrummler/ColorPicker"
        ),

        license(
            "PowerPermission",
            "2020",
            "Qifan Yang",
            OpenSourceLicense.APACHE_2,
            "https://github.com/underwindfall/PowerPermission"
        ),

        license(
            "Material DateTime Picker",
            "2015",
            "Wouter Dullaert",
            OpenSourceLicense.APACHE_2,
            "https://github.com/wdullaer/MaterialDateTimePicker"
        ),

        license(
            "JsonViewer",
            "2017",
            "smuyyh",
            OpenSourceLicense.APACHE_2,
            "https://github.com/smuyyh/JsonViewer"
        ),

        license(
            "Coil",
            "2021",
            "Coil Contributors",
            OpenSourceLicense.APACHE_2,
            "https://github.com/coil-kt/coil"
        ),

        license(
            "Barcode Scanner (ZXing)",
            "2014",
            "Dushyanth Maguluru",
            OpenSourceLicense.APACHE_2,
            "https://github.com/dm77/barcodescanner"
        ),

        license(
            "AutoFitTextView",
            "2014",
            "Grantland Chew",
            OpenSourceLicense.APACHE_2,
            "https://github.com/grantland/android-autofittextview"
        ),

        license(
            "ShortcutBadger",
            "2014",
            "Leo Lin",
            OpenSourceLicense.APACHE_2,
            "https://github.com/leolin310148/ShortcutBadger"
        ),

        license(
            "EventBus",
            "2012-2020",
            "Markus Junginger, greenrobot",
            OpenSourceLicense.APACHE_2,
            "https://github.com/greenrobot/EventBus"
        ),

        license(
            "android-gif-drawable",
            "2013 - present,",
            "Karol Wrótniak, Droids on Roids LLC\n",
            OpenSourceLicense.MIT,
            "https://github.com/koral--/android-gif-drawable"
        ),

        license(
            "Android Debug Database",
            "2019 Amit Shekhar,",
            "2011 Android Open Source Project",
            OpenSourceLicense.APACHE_2,
            "https://github.com/amitshekhariitbhu/Android-Debug-Database"
        )
    )

    override fun getActivityTitle(): CharSequence {
        return getString(R.string.settings_about_licenses_text)
    }
}
