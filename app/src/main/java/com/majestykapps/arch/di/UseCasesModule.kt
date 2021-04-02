package com.majestykapps.arch.di

import com.majestykapps.arch.domain.usecase.GetTask
import com.majestykapps.arch.domain.usecase.GetTaskUseCase
import com.majestykapps.arch.domain.usecase.SubscribeTasks
import com.majestykapps.arch.domain.usecase.SubscribeTasksUseCase
import dagger.Module
import dagger.Provides

@Module
class UseCasesModule {

    @Provides
    fun getTaskUseCase(useCase: GetTask): GetTaskUseCase = useCase


    @Provides
    fun getSubscribeTasksUseCase(useCase: SubscribeTasks): SubscribeTasksUseCase = useCase
}