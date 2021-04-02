package com.majestykapps.arch.di

import com.majestykapps.arch.MainActivity
import com.majestykapps.arch.presentation.taskdetail.TaskDetailsFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class BindingModule {
    @ContributesAndroidInjector
    abstract fun contributeActivity(): MainActivity

    @ContributesAndroidInjector
    abstract fun taskDetailsFragment(): TaskDetailsFragment
}