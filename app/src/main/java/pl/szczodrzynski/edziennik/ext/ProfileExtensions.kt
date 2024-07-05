/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-25.
 */

package pl.szczodrzynski.edziennik.ext

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.google.android.material.datepicker.CalendarConstraints
import com.google.gson.JsonElement
import pl.droidsonroids.gif.GifDrawable
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.config.AppData
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.enums.FeatureType
import pl.szczodrzynski.edziennik.utils.ProfileImageHolder
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.navlib.ImageHolder
import timber.log.Timber

// TODO refactor Data* fields and make the receiver non-nullable
operator fun Profile?.set(key: String, value: JsonElement) = this?.studentData?.add(key, value)
operator fun Profile?.set(key: String, value: Boolean) = this?.studentData?.addProperty(key, value)
operator fun Profile?.set(key: String, value: String?) = this?.studentData?.addProperty(key, value)
operator fun Profile?.set(key: String, value: Number) = this?.studentData?.addProperty(key, value)
operator fun Profile?.set(key: String, value: Char) = this?.studentData?.addProperty(key, value)

fun Profile.getStudentData(key: String, defaultValue: Boolean) =
    studentData.getBoolean(key) ?: defaultValue

fun Profile.getStudentData(key: String, defaultValue: String?) =
    studentData.getString(key) ?: defaultValue

fun Profile.getStudentData(key: String, defaultValue: Int) =
    studentData.getInt(key) ?: defaultValue

fun Profile.getStudentData(key: String, defaultValue: Long) =
    studentData.getLong(key) ?: defaultValue

fun Profile.getStudentData(key: String, defaultValue: Float) =
    studentData.getFloat(key) ?: defaultValue

fun Profile.getStudentData(key: String, defaultValue: Char) =
    studentData.getChar(key) ?: defaultValue

fun Profile.getSemesterStart(semester: Int) =
    if (semester == 1) dateSemester1Start else dateSemester2Start

fun Profile.getSemesterEnd(semester: Int) =
    if (semester == 1) dateSemester2Start.clone().stepForward(0, 0, -1) else dateYearEnd

fun Profile.dateToSemester(date: Date) = if (date >= dateSemester2Start) 2 else 1
fun Profile.isBeforeYear() = false && Date.getToday() < dateSemester1Start

fun Profile.getSchoolYearConstrains(): CalendarConstraints {
    return CalendarConstraints.Builder()
        .setStart(dateSemester1Start.inMillisUtc)
        .setEnd(dateYearEnd.inMillisUtc)
        .build()
}

fun Profile.hasFeature(featureType: FeatureType) = featureType in this.loginStoreType.features
fun Profile.hasUIFeature(featureType: FeatureType) =
    featureType.isUIAlwaysAvailable || hasFeature(featureType)

fun Profile.getAppData() =
    if (App.profileId == this.id) App.data else AppData.get(this.loginStoreType)

fun Profile.shouldArchive(): Boolean {
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
    return App.config.archiverEnabled && Date.getToday() >= dateYearEnd && Date.getToday().year > studentSchoolYearStart
}

fun Profile.getDrawable(context: Context): Drawable {
    if (archived) {
        return R.drawable.profile_archived.resolveDrawable(context).also {
            it.colorFilter = PorterDuffColorFilter(colorFromName(name), PorterDuff.Mode.DST_OVER)
        }
    }

    if (!image.isNullOrEmpty()) {
        try {
            return if (image?.endsWith(".gif", true) == true) {
                GifDrawable(image ?: "")
            } else {
                RoundedBitmapDrawableFactory.create(context.resources, image ?: "")
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    return R.drawable.profile.resolveDrawable(context).also {
        it.colorFilter = PorterDuffColorFilter(colorFromName(name), PorterDuff.Mode.DST_OVER)
    }
}

fun Profile.getHolder(): ImageHolder {
    if (archived) {
        return ImageHolder(R.drawable.profile_archived, colorFromName(name))
    }

    return if (!image.isNullOrEmpty()) {
        try {
            ProfileImageHolder(image ?: "")
        } catch (_: Exception) {
            ImageHolder(R.drawable.profile, colorFromName(name))
        }
    } else {
        ImageHolder(R.drawable.profile, colorFromName(name))
    }
}
