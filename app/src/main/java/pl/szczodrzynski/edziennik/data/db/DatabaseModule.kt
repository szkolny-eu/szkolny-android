/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-4
 */

package pl.szczodrzynski.edziennik.data.db

import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal class DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(context: Context) = AppDb.getDatabase(context)

    @Singleton
    @Provides
    fun provideEventDao(database: AppDb) = database.eventDao()

    @Singleton
    @Provides
    fun provideMetadataDao(database: AppDb) = database.metadataDao()
}
