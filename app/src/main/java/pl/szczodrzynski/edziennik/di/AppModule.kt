/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-4
 */

package pl.szczodrzynski.edziennik.di

import android.content.Context
import dagger.Module
import dagger.Provides
import pl.szczodrzynski.edziennik.App
import javax.inject.Singleton

@Module
internal class AppModule {

    @Singleton
    @Provides
    fun provideContext(app: App): Context = app
}
