/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-17.
 */

package pl.szczodrzynski.edziennik.data.db.converter

import androidx.room.TypeConverter
import pl.szczodrzynski.edziennik.data.db.enums.FeatureType
import pl.szczodrzynski.edziennik.data.db.enums.LoginMode
import pl.szczodrzynski.edziennik.data.db.enums.LoginType
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType

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
