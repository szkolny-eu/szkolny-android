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
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.utils.ProfileImageHolder
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.navlib.ImageHolder
import pl.szczodrzynski.navlib.R
import pl.szczodrzynski.navlib.drawer.IDrawerProfile
import pl.szczodrzynski.navlib.getDrawableFromRes

@Entity(tableName = "profiles", primaryKeys = ["profileId"])
open class Profile(
        @ColumnInfo(name = "profileId")
        override var id: Int, /* needs to be var for ProfileArchiver */
        val loginStoreId: Int,
        val loginStoreType: Int,

        override var name: String = "",
        override var subname: String? = null,

        /**
         * The name of the student.
         * This doesn't change, no matter if it's a parent or student account.
         */
        var studentNameLong: String = "",
        var studentNameShort: String = "",
        /**
         * A full name of the account owner.
         * If null, then it's a student account.
         * If not null, then it's a parent account with this name.
         */
        var accountName: String? = null,

        val studentData: JsonObject = JsonObject()

) : IDrawerProfile {
    companion object {
        const val REGISTRATION_UNSPECIFIED = 0
        const val REGISTRATION_DISABLED = 1
        const val REGISTRATION_ENABLED = 2
        const val AGENDA_DEFAULT = 0
        const val AGENDA_CALENDAR = 1
    }

    override var image: String? = null

    var empty = true
    var archived = false

    /**
     * A unique ID matching [archived] profiles with current ones
     * and vice-versa.
     */
    var archiveId: Int? = null

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
    fun dateToSemester(date: Date) = if (date >= dateSemester2Start) 2 else 1
    @delegate:Ignore
    val currentSemester by lazy { dateToSemester(Date.getToday()) }

    fun shouldArchive(): Boolean {
        // vulcan hotfix
        if (dateYearEnd.month > 6) {
            dateYearEnd.month = 6
            dateYearEnd.day = 30
        }
        // fix for when versions <4.3 synced 2020/2021 year dates to older profiles during 2020 Jun-Aug
        if (dateSemester1Start.year > studentSchoolYearStart) {
            val diff = dateSemester1Start.year - studentSchoolYearStart
            dateSemester1Start.year -= diff
            dateSemester2Start.year -= diff
            dateYearEnd.year -= diff
        }
        return App.config.archiverEnabled
                && Date.getToday() >= dateYearEnd
                && Date.getToday().year > studentSchoolYearStart
    }
    fun isBeforeYear() = false && Date.getToday() < dateSemester1Start

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

    val accountOwnerName
        get() = accountName ?: studentNameLong

    val registerName
        get() = when (loginStoreType) {
            LOGIN_TYPE_LIBRUS -> "librus"
            LOGIN_TYPE_VULCAN -> "vulcan"
            LOGIN_TYPE_IDZIENNIK -> "idziennik"
            LOGIN_TYPE_MOBIDZIENNIK -> "mobidziennik"
            LOGIN_TYPE_PODLASIE -> "podlasie"
            LOGIN_TYPE_EDUDZIENNIK -> "edudziennik"
            else -> "unknown"
        }

    val canShare
        get() = registration == REGISTRATION_ENABLED && !archived

    override fun getImageDrawable(context: Context): Drawable {
        if (archived) {
            return context.getDrawableFromRes(pl.szczodrzynski.edziennik.R.drawable.profile_archived).also {
                it.colorFilter = PorterDuffColorFilter(colorFromName(name), PorterDuff.Mode.DST_OVER)
            }
        }

        if (!image.isNullOrEmpty()) {
            try {
                return if (image?.endsWith(".gif", true) == true) {
                    GifDrawable(image ?: "")
                } else {
                    RoundedBitmapDrawableFactory.create(context.resources, image ?: "")
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
        if (archived) {
            return ImageHolder(pl.szczodrzynski.edziennik.R.drawable.profile_archived, colorFromName(name))
        }

        return if (!image.isNullOrEmpty()) {
            try {
                ProfileImageHolder(image ?: "")
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
            LOGIN_TYPE_PODLASIE -> listOf(
                    MainActivity.DRAWER_ITEM_TIMETABLE,
                    MainActivity.DRAWER_ITEM_AGENDA,
                    MainActivity.DRAWER_ITEM_GRADES,
                    MainActivity.DRAWER_ITEM_HOMEWORK
            )
            else -> listOf(
                    MainActivity.DRAWER_ITEM_TIMETABLE,
                    MainActivity.DRAWER_ITEM_AGENDA,
                    MainActivity.DRAWER_ITEM_GRADES
            )
        }
}
