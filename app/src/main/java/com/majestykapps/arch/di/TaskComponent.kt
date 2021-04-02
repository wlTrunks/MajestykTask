package com.majestykapps.arch.di

import android.content.Context
import com.majestykapps.arch.App
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidInjectionModule::class,
        RepositoryModule::class,
        NetworkModule::class,
        BindingModule::class,
        ViewModelModule::class,
        DatabaseModule::class,
        UseCasesModule::class,
        ContextModule::class
    ]
)
interface TaskComponent : AndroidInjector<App>

@Module
internal class ContextModule(private val context: Context) {

    @Provides
    @Singleton
    fun context(): Context {
        return context
    }
}
