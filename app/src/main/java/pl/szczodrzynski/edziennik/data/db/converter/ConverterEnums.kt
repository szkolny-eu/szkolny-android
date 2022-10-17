/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-17.
 */

package pl.szczodrzynski.edziennik.data.db.converter

import androidx.room.TypeConverter
import pl.szczodrzynski.edziennik.data.db.enum.FeatureType
import pl.szczodrzynski.edziennik.data.db.enum.LoginMode
import pl.szczodrzynski.edziennik.data.db.enum.LoginType
import pl.szczodrzynski.edziennik.data.db.enum.MetadataType

class ConverterEnums {

    @TypeConverter
    fun fromFeatureType(value: FeatureType) = value.id

    @TypeConverter
    fun toFeatureType(value: Int) = FeatureType.values().first { it.id == value }

    @TypeConverter
    fun fromLoginMode(value: LoginMode) = value.id

    @TypeConverter
    fun toLoginMode(value: Int) = LoginMode.values().first { it.id == value }

    @TypeConverter
    fun fromLoginType(value: LoginType) = value.id

    @TypeConverter
    fun toLoginType(value: Int) = LoginType.values().first { it.id == value }

    @TypeConverter
    fun fromMetadataType(value: MetadataType) = value.id

    @TypeConverter
    fun toMetadataType(value: Int) = MetadataType.values().first { it.id == value }
}
