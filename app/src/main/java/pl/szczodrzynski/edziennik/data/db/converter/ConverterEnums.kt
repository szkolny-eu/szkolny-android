/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-17.
 */

package pl.szczodrzynski.edziennik.data.db.converter

import androidx.room.TypeConverter
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.ui.base.enums.NavTarget

class ConverterEnums {

    @TypeConverter
    fun fromEnum(value: Enum<*>?) = value?.toInt()

    @TypeConverter
    fun toFeatureType(value: Int?) = value.asFeatureTypeOrNull()

    @TypeConverter
    fun toLoginMethod(value: Int?) = value.asLoginMethodOrNull()

    @TypeConverter
    fun toLoginMode(value: Int?) = value.asLoginModeOrNull()

    @TypeConverter
    fun toLoginType(value: Int?) = value.asLoginTypeOrNull()

    @TypeConverter
    fun toMetadataType(value: Int?) = value.asMetadataTypeOrNull()

    @TypeConverter
    fun toNotificationType(value: Int?) = value.asNotificationTypeOrNull()

    @TypeConverter
    fun toNavTarget(value: Int?) = value.asNavTargetOrNull() ?: NavTarget.HOME
}
