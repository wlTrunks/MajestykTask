package com.majestykapps.arch.domain.usecase

import com.majestykapps.arch.data.common.Resource
import com.majestykapps.arch.domain.entity.Task
import com.majestykapps.arch.domain.repository.TasksRepository
import io.reactivex.Observer
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Due to the simplicity of this example, this use case is somewhat redundant. Typically you'd
 * combine data from multiple repositories and transform them for the View Model in a Use Case
 */
interface SubscribeTasksUseCase {
    /**
     * Subscribes the [observer] to emissions of the full task list.
     */
    fun subscribe(observer: Observer<Resource<List<Task>>>)

    /**
     * Triggers a repository load that may emit cached results
     */
    suspend fun load(): Flow<Resource<List<Task>>>

    /**
     * Triggers a repository load that emits only fresh results; no cache
     */
    suspend fun refresh()

    fun search(text: String): Flow<Resource<List<Task>>>
}

class SubscribeTasks @Inject constructor(private val repository: TasksRepository) : SubscribeTasksUseCase {

    override fun subscribe(observer: Observer<Resource<List<Task>>>) =
        repository.subscribe(observer)

    override suspend fun load(): Flow<Resource<List<Task>>> = repository.loadTasks()

    override suspend fun refresh() {
        repository.run {
            refresh()
            loadTasks()
        }
    }

    override fun search(text: String): Flow<Resource<List<Task>>> {
       return repository.searchTask(text)
    }
}