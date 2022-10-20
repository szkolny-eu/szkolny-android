/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ext

import android.util.LongSparseArray
import androidx.core.util.forEach
import com.google.android.material.datepicker.CalendarConstraints
import com.google.gson.JsonElement
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.entity.Team
import pl.szczodrzynski.edziennik.data.db.enums.FeatureType

fun List<Teacher>.byId(id: Long) = firstOrNull { it.id == id }
fun List<Teacher>.byNameFirstLast(nameFirstLast: String) = firstOrNull { it.name + " " + it.surname == nameFirstLast }
fun List<Teacher>.byNameLastFirst(nameLastFirst: String) = firstOrNull { it.surname + " " + it.name == nameLastFirst }
fun List<Teacher>.byNameFDotLast(nameFDotLast: String) = firstOrNull { it.name + "." + it.surname == nameFDotLast }
fun List<Teacher>.byNameFDotSpaceLast(nameFDotSpaceLast: String) = firstOrNull { it.name + ". " + it.surname == nameFDotSpaceLast }

fun List<Profile>.filterOutArchived() = this.filter { !it.archived }

fun List<Team>.getById(id: Long): Team? {
    return singleOrNull { it.id == id }
}

fun LongSparseArray<Team>.getById(id: Long): Team? {
    forEach { _, value ->
        if (value.id == id)
            return value
    }
    return null
}

operator fun Profile.set(key: String, value: JsonElement) = this.studentData.add(key, value)
operator fun Profile.set(key: String, value: Boolean) = this.studentData.addProperty(key, value)
operator fun Profile.set(key: String, value: String?) = this.studentData.addProperty(key, value)
operator fun Profile.set(key: String, value: Number) = this.studentData.addProperty(key, value)
operator fun Profile.set(key: String, value: Char) = this.studentData.addProperty(key, value)

fun Profile.getSchoolYearConstrains(): CalendarConstraints {
    return CalendarConstraints.Builder()
        .setStart(dateSemester1Start.inMillisUtc)
        .setEnd(dateYearEnd.inMillisUtc)
        .build()
}

fun Profile.hasFeature(featureType: FeatureType) = featureType in this.loginStoreType.features
fun Profile.hasUIFeature(featureType: FeatureType) = featureType.isUIAlwaysAvailable || hasFeature(featureType)
