/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.entity

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import com.google.gson.JsonObject
import pl.droidsonroids.gif.GifDrawable
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.LOGIN_TYPE_EDUDZIENNIK
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.navlib.ImageHolder
import pl.szczodrzynski.navlib.R
import pl.szczodrzynski.navlib.drawer.IDrawerProfile
import pl.szczodrzynski.navlib.getDrawableFromRes

@Entity(tableName = "profiles", primaryKeys = ["profileId"])
open class Profile(
        @ColumnInfo(name = "profileId")
        override val id: Int,
        val loginStoreId: Int,
        val loginStoreType: Int,

        override var name: String,
        override var subname: String?,

        /**
         * The name of the student.
         * This doesn't change, no matter if it's a parent or student account.
         */
        var studentNameLong: String,
        var studentNameShort: String,
        /**
         * A full name of the account owner.
         * If null, then it's a student account.
         * If not null, then it's a parent account with this name.
         */
        var accountName: String?,

        val studentData: JsonObject = JsonObject()

) : IDrawerProfile {
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

    override var image: String? = null

    var empty = true
    var archived = false

    var syncEnabled = true
    var enableSharedEvents = true
    var registration = REGISTRATION_UNSPECIFIED
    var userCode = ""

    /**
     * The student's number in the class register.
     */
    var studentNumber = -1
    var studentClassName: String? = null
    var studentSchoolYearStart = Date.getToday().let { if (it.month < 9) it.year - 1 else it.year }

    var dateSemester1Start = Date(studentSchoolYearStart, 9, 1)
    var dateSemester2Start = Date(studentSchoolYearStart + 1, 2, 1)
    var dateYearEnd = Date(studentSchoolYearStart + 1, 6, 30)
    fun getSemesterStart(semester: Int) = if (semester == 1) dateSemester1Start else dateSemester2Start
    fun getSemesterEnd(semester: Int) = if (semester == 1) dateSemester2Start.clone().stepForward(0, 0, -1) else dateYearEnd
    fun dateToSemester(date: Date) = if (date.value >= getSemesterStart(2).value) 2 else 1
    @delegate:Ignore
    val currentSemester by lazy { dateToSemester(Date.getToday()) }

    var disabledNotifications: List<Long>? = null

    var lastReceiversSync: Long = 0

    fun hasStudentData(key: String) = studentData.has(key)
    fun getStudentData(key: String, defaultValue: Boolean) = studentData.getBoolean(key) ?: defaultValue
    fun getStudentData(key: String, defaultValue: String?) = studentData.getString(key) ?: defaultValue
    fun getStudentData(key: String, defaultValue: Int) = studentData.getInt(key) ?: defaultValue
    fun getStudentData(key: String, defaultValue: Long) = studentData.getLong(key) ?: defaultValue
    fun getStudentData(key: String, defaultValue: Float) = studentData.getFloat(key) ?: defaultValue
    fun getStudentData(key: String, defaultValue: Char) = studentData.getChar(key) ?: defaultValue
    fun putStudentData(key: String, value: Boolean) { studentData[key] = value }
    fun putStudentData(key: String, value: String?) { studentData[key] = value }
    fun putStudentData(key: String, value: Number) { studentData[key] = value }
    fun putStudentData(key: String, value: Char) { studentData[key] = value }
    fun removeStudentData(key: String) { studentData.remove(key) }

    val isParent
        get() = accountName != null

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
            it.colorFilter = PorterDuffColorFilter(colorFromName(name), PorterDuff.Mode.DST_OVER)
        }

    }
    override fun getImageHolder(context: Context): ImageHolder {
        return if (!image.isNullOrEmpty()) {
            try {
                ImageHolder(image ?: "")
            } catch (_: Exception) {
                ImageHolder(R.drawable.profile, colorFromName(name))
            }
        }
        else {
            ImageHolder(R.drawable.profile, colorFromName(name))
        }
    }
    override fun applyImageTo(imageView: ImageView) {
        getImageHolder(imageView.context).applyTo(imageView)
    }

    val supportedFragments: List<Int>
        get() = when (loginStoreType) {
            LoginStore.LOGIN_TYPE_MOBIDZIENNIK,
            LoginStore.LOGIN_TYPE_DEMO,
            LoginStore.LOGIN_TYPE_VULCAN -> listOf(
                    MainActivity.DRAWER_ITEM_TIMETABLE,
                    MainActivity.DRAWER_ITEM_AGENDA,
                    MainActivity.DRAWER_ITEM_GRADES,
                    MainActivity.DRAWER_ITEM_MESSAGES,
                    MainActivity.DRAWER_ITEM_HOMEWORK,
                    MainActivity.DRAWER_ITEM_BEHAVIOUR,
                    MainActivity.DRAWER_ITEM_ATTENDANCE
            )
            LoginStore.LOGIN_TYPE_LIBRUS,
            LoginStore.LOGIN_TYPE_IDZIENNIK -> listOf(
                    MainActivity.DRAWER_ITEM_TIMETABLE,
                    MainActivity.DRAWER_ITEM_AGENDA,
                    MainActivity.DRAWER_ITEM_GRADES,
                    MainActivity.DRAWER_ITEM_MESSAGES,
                    MainActivity.DRAWER_ITEM_HOMEWORK,
                    MainActivity.DRAWER_ITEM_BEHAVIOUR,
                    MainActivity.DRAWER_ITEM_ATTENDANCE,
                    MainActivity.DRAWER_ITEM_ANNOUNCEMENTS
            )
            LOGIN_TYPE_EDUDZIENNIK -> listOf(
                    MainActivity.DRAWER_ITEM_TIMETABLE,
                    MainActivity.DRAWER_ITEM_AGENDA,
                    MainActivity.DRAWER_ITEM_GRADES,
                    MainActivity.DRAWER_ITEM_HOMEWORK,
                    MainActivity.DRAWER_ITEM_BEHAVIOUR,
                    MainActivity.DRAWER_ITEM_ATTENDANCE,
                    MainActivity.DRAWER_ITEM_ANNOUNCEMENTS
            )
            else -> listOf(
                    MainActivity.DRAWER_ITEM_TIMETABLE,
                    MainActivity.DRAWER_ITEM_AGENDA,
                    MainActivity.DRAWER_ITEM_GRADES
            )
        }
}
