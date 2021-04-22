package com.majestykapps.arch.data.source.remote

import androidx.annotation.VisibleForTesting
import com.majestykapps.arch.data.api.TasksApi
import com.majestykapps.arch.data.api.TasksApiService
import com.majestykapps.arch.data.common.Resource
import com.majestykapps.arch.data.source.TasksDataSource
import com.majestykapps.arch.di.CoroutineDispatcherProvider
import com.majestykapps.arch.di.CoroutineDispatcherProviderImpl
import com.majestykapps.arch.domain.entity.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TasksRemoteDataSource @Inject constructor(
    private val api: TasksApiService,
    private val dispatcherProvider: CoroutineDispatcherProvider
) : TasksDataSource {

    override fun getTasks(): Flow<Resource<List<Task>>> = flow {
        emit(call { api.getTasks().records })
    }.flowOn(dispatcherProvider.io)

    override fun getTask(taskId: String): Flow<Resource<Task>> = flow {
        emit(call { api.getTask(taskId) })
    }.flowOn(dispatcherProvider.io)

    override suspend fun saveTask(task: Task) {
    }

    override suspend fun saveTasks(tasks: List<Task>) {
    }

    override suspend fun deleteTask(taskId: String) {
    }

    internal inline fun <R> call(requestBlock: () -> R): Resource<R> = try {
        Resource.Success(requestBlock.invoke())
    } catch (e: Exception) {
        Resource.Failure(e)
    }

    companion object {
        private var INSTANCE: TasksRemoteDataSource? = null

        fun getInstance(
            api: TasksApiService = TasksApi.getInstance().service
        ): TasksRemoteDataSource = INSTANCE ?: synchronized(this) {
            INSTANCE ?: TasksRemoteDataSource(api, CoroutineDispatcherProviderImpl()).also { INSTANCE = it }
        }

        @VisibleForTesting
        fun destroy() {
            INSTANCE = null
        }
    }
}