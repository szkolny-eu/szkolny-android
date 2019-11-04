/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-4
 */

package pl.szczodrzynski.edziennik.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.di.scopes.PerActivity
import pl.szczodrzynski.edziennik.di.scopes.PerFragment
import pl.szczodrzynski.edziennik.ui.modules.homework.HomeworkFragment
import pl.szczodrzynski.edziennik.ui.modules.homework.list.HomeworkListFragment

@Module
@Suppress("unused")
internal abstract class BindingModule {

    /**
     * ACTIVITIES
     */

    @PerActivity
    @ContributesAndroidInjector
    abstract fun bindMainActivity(): MainActivity

    /**
     * FRAGMENTS
     */

    @PerFragment
    @ContributesAndroidInjector
    abstract fun bindHomeworkFragment(): HomeworkFragment

    @PerFragment
    @ContributesAndroidInjector
    abstract fun bindHomeworkListFragment(): HomeworkListFragment
}
