package com.majestykapps.arch.domain.usecase

import com.majestykapps.arch.data.common.Resource
import com.majestykapps.arch.domain.entity.Task
import com.majestykapps.arch.domain.repository.TasksRepository
import io.reactivex.Observable
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface GetTaskUseCase {
    fun getTask(id: String): Flow<Resource<Task>>
    fun refresh()
}

class GetTask @Inject constructor(
    private val repository: TasksRepository
) : GetTaskUseCase {
    override fun getTask(id: String): Flow<Resource<Task>> = repository.getTask(id)
    override fun refresh() = repository.refresh()
}