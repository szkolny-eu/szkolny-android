/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-17.
 */

package pl.szczodrzynski.edziennik.data.db.converter

import androidx.room.TypeConverter
import pl.szczodrzynski.edziennik.data.enums.FeatureType
import pl.szczodrzynski.edziennik.data.enums.LoginMode
import pl.szczodrzynski.edziennik.data.enums.LoginType
import pl.szczodrzynski.edziennik.data.enums.MetadataType
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.data.enums.NotificationType

class ConverterEnums {

    @TypeConverter
    fun fromFeatureType(value: FeatureType?) = value?.id

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
    fun toFeatureType(value: Int?) = FeatureType.entries.firstOrNull { it.id == value }

    @TypeConverter
    fun toLoginMode(value: Int?) = LoginMode.entries.firstOrNull { it.id == value }

    @TypeConverter
    fun toLoginType(value: Int?) = LoginType.entries.firstOrNull { it.id == value }

    @TypeConverter
    fun toMetadataType(value: Int?) = MetadataType.entries.firstOrNull { it.id == value }

    @TypeConverter
    fun toNotificationType(value: Int?) = NotificationType.entries.firstOrNull { it.id == value }

    @TypeConverter
    fun toNavTarget(value: Int?) = NavTarget.entries.firstOrNull { it.id == value }
        ?: NavTarget.HOME
}
