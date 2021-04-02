package com.majestykapps.arch.data.source.local

import androidx.annotation.VisibleForTesting
import com.majestykapps.arch.data.common.Resource
import com.majestykapps.arch.data.source.TasksDataSource
import com.majestykapps.arch.domain.entity.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TasksLocalDataSource @Inject constructor(
    private val tasksDao: TasksDao
) : TasksDataSource {

    override fun getTasks(): Flow<Resource<List<Task>>> = tasksDao.getTasks()
        .flatMapConcat { flowOf(Resource.Success(it)) }

    override fun getTask(taskId: String): Flow<Resource<Task>> = tasksDao.getTaskById(taskId)
        .flatMapConcat { flowOf(Resource.Success(it)) }

    override suspend fun saveTask(task: Task) {
        tasksDao.insertTask(task)
    }

    override suspend fun saveTasks(tasks: List<Task>) {
        tasksDao.insertTasks(tasks)
    }

    override suspend fun deleteTask(taskId: String) {
        tasksDao.deleteTaskById(taskId)
    }

    companion object {
        private var INSTANCE: TasksLocalDataSource? = null

        fun getInstance(
            tasksDao: TasksDao
        ): TasksLocalDataSource = INSTANCE ?: synchronized(this) {
            INSTANCE ?: TasksLocalDataSource(tasksDao).also { INSTANCE = it }
        }

        @VisibleForTesting
        fun destroy() {
            INSTANCE = null
        }
    }
}