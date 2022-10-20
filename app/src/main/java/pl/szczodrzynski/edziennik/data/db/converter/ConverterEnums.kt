/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-17.
 */

package pl.szczodrzynski.edziennik.data.db.converter

import androidx.room.TypeConverter
import pl.szczodrzynski.edziennik.data.db.enums.*
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.ui.base.enums.NavTarget

class ConverterEnums {

    @TypeConverter
    fun fromEnum(value: Enum<*>) = value.toInt()

    @TypeConverter
    fun toFeatureType(value: Int) = value.asFeatureType()

    @TypeConverter
    fun toLoginMethod(value: Int) = value.asLoginMethod()

    @TypeConverter
    fun toLoginMode(value: Int) = value.asLoginMode()

    @TypeConverter
    fun toLoginType(value: Int) = value.asLoginType()

    @TypeConverter
    fun toMetadataType(value: Int) = value.asMetadataType()

    @TypeConverter
    fun toNotificationType(value: Int) = value.asNotificationType()

    @TypeConverter
    fun toNavTarget(value: Int) = value.asNavTargetOrNull() ?: NavTarget.HOME
}
