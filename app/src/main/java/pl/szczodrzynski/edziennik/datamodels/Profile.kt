package pl.szczodrzynski.edziennik.datamodels

import androidx.room.ColumnInfo
import androidx.room.Entity
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.widget.ImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory

import com.google.gson.JsonElement
import com.google.gson.JsonObject

import androidx.room.Ignore
import pl.droidsonroids.gif.GifDrawable
import pl.szczodrzynski.edziennik.colorFromName

import pl.szczodrzynski.edziennik.models.Date
import pl.szczodrzynski.navlib.ImageHolder
import pl.szczodrzynski.navlib.R
import pl.szczodrzynski.navlib.drawer.IDrawerProfile
import pl.szczodrzynski.navlib.getDrawableFromRes

@Entity(tableName = "profiles", primaryKeys = ["profileId"])
open class Profile : IDrawerProfile {

    @ColumnInfo(name = "profileId")
    override var id = -1
    override var name: String? = ""
    override var subname: String? = null
    override var image: String? = null
    /*public String name = "";
    public String subname = null;
    public String image = null;*/

    var syncEnabled = true
    var syncNotifications = true
    var enableSharedEvents = true
    var countInSeconds = false
    var loggedIn = false
    var empty = true
    var archived = false

    var studentNameLong: String? = null
    var studentNameShort: String? = null
    var studentNumber = -1
    var studentData: JsonObject? = null

    var registration = REGISTRATION_UNSPECIFIED

    var gradeColorMode = COLOR_MODE_WEIGHTED
    var agendaViewType = AGENDA_DEFAULT

    var yearAverageMode = YEAR_ALL_GRADES

    var currentSemester = 1

    var attendancePercentage: Float = 0.toFloat()

    var dateSemester1Start: Date? = null
    var dateSemester2Start: Date? = null
    var dateYearEnd: Date? = null

    var luckyNumberEnabled = true
    var luckyNumber = -1
    var luckyNumberDate: Date? = null

    //public Map<Integer, Pair<String, Integer>> eventTypes;

    var loginStoreId = id

    var changedEndpoints: List<String>? = null

    var lastFullSync: Long = 0
    var lastReceiversSync: Long = 0

    fun shouldFullSync(context: Context): Boolean {
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        // if time since last full sync is > 7 days and wifi is connected
        // or
        // if time since last full sync is > 14 days, regardless of wifi state
        return System.currentTimeMillis() - lastFullSync > 7 * 24 * 60 * 60 * 1000 && mWifi.isConnected || System.currentTimeMillis() - lastFullSync > 14 * 24 * 60 * 60 * 1000
    }

    @Ignore
    constructor(id: Int, name: String, subname: String, loginStoreId: Int) : this() {
        this.id = id
        this.name = name
        this.subname = subname
        this.loginStoreId = loginStoreId
    }

    constructor() {
        //eventTypes = new HashMap<>();
        val today = Date.getToday()
        val schoolYearStart = if (today.month < 9) today.year - 1 else today.year
        dateSemester1Start = Date(schoolYearStart, 9, 1)
        dateSemester2Start = Date(schoolYearStart + 1, 2, 1)
        dateYearEnd = Date(schoolYearStart + 1, 6, 30)
    }

    fun getSemesterStart(semester: Int): Date {
        if (dateSemester1Start == null || dateSemester2Start == null || dateYearEnd == null) {
            val today = Date.getToday()
            val schoolYearStart = if (today.month < 9) today.year - 1 else today.year
            dateSemester1Start = Date(schoolYearStart, 9, 1)
            dateSemester2Start = Date(schoolYearStart + 1, 2, 1)
            dateYearEnd = Date(schoolYearStart + 1, 6, 30)
        }

        return if (semester == 1)
            dateSemester1Start!!
        else
            dateSemester2Start!!
    }

    fun getSemesterEnd(semester: Int): Date {
        if (dateSemester1Start == null || dateSemester2Start == null || dateYearEnd == null) {
            val today = Date.getToday()
            val schoolYearStart = if (today.month < 9) today.year - 1 else today.year
            dateSemester1Start = Date(schoolYearStart, 9, 1)
            dateSemester2Start = Date(schoolYearStart + 1, 2, 1)
            dateYearEnd = Date(schoolYearStart + 1, 6, 30)
        }

        return if (semester == 1)
            dateSemester2Start!!.clone().stepForward(0, 0, -1)
        else
            dateYearEnd!!
    }

    fun dateToSemester(date: Date?): Int {
        if (date == null)
            return 1
        return if (date.value >= getSemesterStart(2).value) 2 else 1
    }

    @Ignore
    constructor(context: Context) {
        //RegisterEvent.checkPredefinedEventTypes(context, eventTypes);
    }

    override fun getImageDrawable(context: Context): Drawable {

        if (!image.isNullOrEmpty()) {
            try {
                if (image?.endsWith(".gif", true) == true) {
                    return GifDrawable(image ?: "")
                }
                else {
                    return RoundedBitmapDrawableFactory.create(context.resources, image ?: "")
                    //return Drawable.createFromPath(image ?: "") ?: throw Exception()
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return context.getDrawableFromRes(R.drawable.profile).also {
            it.colorFilter = PorterDuffColorFilter(colorFromName(context, name), PorterDuff.Mode.DST_OVER)
        }

        /*if (profileImage == null) {
            profileImage = BitmapFactory.decodeResource(getResources(), pl.szczodrzynski.edziennik.R.drawable.profile);
        }
        profileImage = ThumbnailUtils.extractThumbnail(profileImage, Math.min(profileImage.getWidth(), profileImage.getHeight()), Math.min(profileImage.getWidth(), profileImage.getHeight()));
        RoundedBitmapDrawable roundDrawable = RoundedBitmapDrawableFactory.create(getResources(), profileImage);
        roundDrawable.setCircular(true);
        return roundDrawable;*/
    }
    override fun getImageHolder(context: Context): ImageHolder {
        return if (!image.isNullOrEmpty()) {
            try {
                ImageHolder(image ?: "")
            } catch (_: Exception) {
                ImageHolder(R.drawable.profile, colorFromName(context, name))
            }
        }
        else {
            ImageHolder(R.drawable.profile, colorFromName(context, name))
        }
    }
    override fun applyImageTo(imageView: ImageView) {
        getImageHolder(imageView.context).applyTo(imageView)
    }

    fun getStudentData(key: String, defaultValue: String?): String? {
        if (studentData == null)
            return defaultValue
        val element = studentData!!.get(key)
        return if (element != null) {
            element.asString
        } else defaultValue
    }

    fun getStudentData(key: String, defaultValue: Int): Int {
        if (studentData == null)
            return defaultValue
        val element = studentData!!.get(key)
        return element?.asInt ?: defaultValue
    }

    fun getStudentData(key: String, defaultValue: Long): Long {
        if (studentData == null)
            return defaultValue
        val element = studentData!!.get(key)
        return element?.asLong ?: defaultValue
    }

    fun getStudentData(key: String, defaultValue: Float): Float {
        if (studentData == null)
            return defaultValue
        val element = studentData!!.get(key)
        return element?.asFloat ?: defaultValue
    }

    fun getStudentData(key: String, defaultValue: Boolean): Boolean {
        if (studentData == null)
            return defaultValue
        val element = studentData!!.get(key)
        return element?.asBoolean ?: defaultValue
    }

    fun putStudentData(key: String, value: String) {
        if (studentData == null)
            studentData = JsonObject()
        studentData!!.addProperty(key, value)
    }

    fun putStudentData(key: String, value: Int) {
        if (studentData == null)
            studentData = JsonObject()
        studentData!!.addProperty(key, value)
    }

    fun putStudentData(key: String, value: Long) {
        if (studentData == null)
            studentData = JsonObject()
        studentData!!.addProperty(key, value)
    }

    fun putStudentData(key: String, value: Float) {
        if (studentData == null)
            studentData = JsonObject()
        studentData!!.addProperty(key, value)
    }

    fun putStudentData(key: String, value: Boolean) {
        if (studentData == null)
            studentData = JsonObject()
        studentData!!.addProperty(key, value)
    }

    fun removeStudentData(key: String) {
        if (studentData == null)
            studentData = JsonObject()
        studentData!!.remove(key)
    }

    fun clearStudentStore() {
        studentData = JsonObject()
    }

    override fun toString(): String {
        return "Profile{" +
                "id=" + id +
                ", name='" + name + '\''.toString() +
                ", subname='" + subname + '\''.toString() +
                ", image='" + image + '\''.toString() +
                ", syncEnabled=" + syncEnabled +
                ", syncNotifications=" + syncNotifications +
                ", enableSharedEvents=" + enableSharedEvents +
                ", loggedIn=" + loggedIn +
                ", empty=" + empty +
                ", studentNameLong='" + studentNameLong + '\''.toString() +
                ", studentNameShort='" + studentNameShort + '\''.toString() +
                ", studentNumber=" + studentNumber +
                ", studentData=" + studentData.toString() +
                ", registration=" + registration +
                ", gradeColorMode=" + gradeColorMode +
                ", agendaViewType=" + agendaViewType +
                ", currentSemester=" + currentSemester +
                ", attendancePercentage=" + attendancePercentage +
                ", dateSemester1Start=" + dateSemester1Start +
                ", dateSemester2Start=" + dateSemester2Start +
                ", dateYearEnd=" + dateYearEnd +
                ", luckyNumberEnabled=" + luckyNumberEnabled +
                ", luckyNumber=" + luckyNumber +
                ", luckyNumberDate=" + luckyNumberDate +
                ", loginStoreId=" + loginStoreId +
                '}'.toString()
    }

    companion object {
        const val REGISTRATION_UNSPECIFIED = 0
        const val REGISTRATION_DISABLED = 1
        const val REGISTRATION_ENABLED = 2
        const val COLOR_MODE_DEFAULT = 0
        const val COLOR_MODE_WEIGHTED = 1
        const val AGENDA_DEFAULT = 0
        const val AGENDA_CALENDAR = 1
        const val YEAR_1_AVG_2_AVG = 0
        const val YEAR_1_SEM_2_AVG = 1
        const val YEAR_1_AVG_2_SEM = 2
        const val YEAR_1_SEM_2_SEM = 3
        const val YEAR_ALL_GRADES = 4
    }
}
