package com.majestykapps.arch.presentation.taskdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.majestykapps.arch.data.common.Resource.Failure
import com.majestykapps.arch.data.common.Resource.Success
import com.majestykapps.arch.domain.entity.Task
import com.majestykapps.arch.domain.usecase.GetTaskUseCase
import com.majestykapps.arch.presentation.common.Error
import com.majestykapps.arch.presentation.common.Loading
import com.majestykapps.arch.presentation.common.ViewEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class TaskDetailViewModel @Inject constructor(
    private val getTaskUseCase: GetTaskUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TaskDetailState())
    val state = _state.asStateFlow()
    private val viewEventsChannel = Channel<ViewEvent?>(1)
    val viewEvents = viewEventsChannel.receiveAsFlow()

    fun getTask(id: String) {
        viewModelScope.launch {
            getTaskUseCase.getTask(id).onStart {
                viewEventsChannel.offer(Loading)
            }
                .catch {
                    viewEventsChannel.offer(Error(it))
                }.collect { resource ->
                    println("TASK resource $resource")
                    when (resource) {
                        is Failure -> viewEventsChannel.offer(Error(resource.error))
                        is Success -> _state.value = TaskDetailState(resource.data)
                    }
                }
        }
    }
}

data class TaskDetailState(
    val task: Task? = null
)