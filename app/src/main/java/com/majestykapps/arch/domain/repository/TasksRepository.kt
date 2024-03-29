package com.majestykapps.arch.domain.repository

import com.majestykapps.arch.common.Repository
import com.majestykapps.arch.data.common.Resource
import com.majestykapps.arch.domain.entity.Task
import io.reactivex.Observable
import io.reactivex.Observer
import kotlinx.coroutines.flow.Flow

interface TasksRepository : Repository {

    /**
     * Observe emissions of the entire task collection
     */
    fun subscribe(observer: Observer<Resource<List<Task>>>)

    /**
     * Trigger an update from the data sources. Observers added with [subscribe] will receive
     * emissions when the backing data changes.
     *
     * @see [subscribe]
     */
    fun loadTasks() : Flow<Resource<List<Task>>>

    /**
     * Returns an [Observable] that will emit the resource when the backing data changes
     */
    fun getTask(id: String): Flow<Resource<Task>>

    /**
     * Search task by text
     */
    fun searchTask(text: String): Flow<Resource<List<Task>>>
}