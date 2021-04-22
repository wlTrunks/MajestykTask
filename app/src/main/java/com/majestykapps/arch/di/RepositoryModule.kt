package com.majestykapps.arch.di

import com.majestykapps.arch.data.repository.TasksRepositoryImpl
import com.majestykapps.arch.data.source.TasksDataSource
import com.majestykapps.arch.data.source.local.TasksLocalDataSource
import com.majestykapps.arch.data.source.remote.TasksRemoteDataSource
import com.majestykapps.arch.domain.repository.TasksRepository
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
class RepositoryModule {
    @Provides
    @Singleton
    fun provideRepository(repo: TasksRepositoryImpl): TasksRepository = repo

    @Provides
    @Named("local")
    @Singleton
    fun provideLocalSource(source: TasksLocalDataSource): TasksDataSource = source

    @Provides
    @Named("remote")
    @Singleton
    fun provideRemoteSource(source: TasksRemoteDataSource): TasksDataSource = source
}