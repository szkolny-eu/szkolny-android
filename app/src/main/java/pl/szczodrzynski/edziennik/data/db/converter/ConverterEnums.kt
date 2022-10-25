/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-17.
 */

package pl.szczodrzynski.edziennik.data.db.converter

import androidx.room.TypeConverter
import pl.szczodrzynski.edziennik.data.db.enums.FeatureType
import pl.szczodrzynski.edziennik.data.db.enums.LoginMethod
import pl.szczodrzynski.edziennik.data.db.enums.LoginMode
import pl.szczodrzynski.edziennik.data.db.enums.LoginType
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.data.db.enums.NotificationType
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.ui.base.enums.NavTarget

class ConverterEnums {

    @TypeConverter
    fun fromFeatureType(value: FeatureType?) = value?.id

    @TypeConverter
    fun fromLoginMethod(value: LoginMethod?) = value?.id

    @TypeConverter
    fun fromLoginMode(value: LoginMode?) = value?.id

    @TypeConverter
    fun fromLoginType(value: LoginType?) = value?.id

    @TypeConverter
    fun fromMetadataType(value: MetadataType?) = value?.id

    @TypeConverter
    fun fromNotificationType(value: NotificationType?) = value?.id

    @TypeConverter
    fun fromNavTarget(value: NavTarget?) = value?.id

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
