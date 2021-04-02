package com.majestykapps.arch.presentation.common

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.majestykapps.arch.domain.repository.TasksRepository
import com.majestykapps.arch.domain.usecase.GetTask
import com.majestykapps.arch.domain.usecase.SubscribeTasks
import com.majestykapps.arch.presentation.taskdetail.TaskDetailViewModel
import com.majestykapps.arch.presentation.tasks.TasksViewModel
import com.majestykapps.arch.presentation.util.NetworkConnectionLiveData

class ViewModelFactory private constructor(
    private val ld: NetworkConnectionLiveData,
    private val tasksRepository: TasksRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>) = with(modelClass) {
        when {
            isAssignableFrom(TasksViewModel::class.java) -> {
                TasksViewModel(ld, SubscribeTasks(tasksRepository))
            }
            isAssignableFrom(TaskDetailViewModel::class.java) -> {
                TaskDetailViewModel(GetTask(tasksRepository))
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    } as T

    companion object {
        @Volatile
        private var INSTANCE: ViewModelFactory? = null

        fun getInstance(tasksRepository: TasksRepository, ld: NetworkConnectionLiveData) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ViewModelFactory(ld, tasksRepository).also { INSTANCE = it }
            }

        @VisibleForTesting
        fun destroyInstance() {
            INSTANCE = null
        }
    }
}