package com.majestykapps.arch.data.source

import com.majestykapps.arch.data.common.Resource
import com.majestykapps.arch.domain.entity.Task
import io.reactivex.Completable
import kotlinx.coroutines.flow.Flow

/**
 * Main entry point for accessing tasks data.
 */
interface TasksDataSource {

    fun getTasks(): Flow<Resource<List<Task>>>

    fun getTask(taskId: String): Flow<Resource<Task>>

    suspend fun saveTask(task: Task)

    suspend fun saveTasks(tasks: List<Task>)

    suspend fun deleteTask(taskId: String)
}