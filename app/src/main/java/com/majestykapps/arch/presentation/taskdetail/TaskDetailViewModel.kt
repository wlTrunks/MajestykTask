package com.majestykapps.arch.presentation.taskdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.majestykapps.arch.data.common.Resource.Failure
import com.majestykapps.arch.data.common.Resource.Success
import com.majestykapps.arch.domain.entity.Task
import com.majestykapps.arch.domain.usecase.GetTaskUseCase
import com.majestykapps.arch.presentation.common.BaseViewModel
import com.majestykapps.arch.util.SingleLiveEvent

//todo ref this
class TaskDetailViewModel(
    private val getTaskUseCase: GetTaskUseCase
) : BaseViewModel() {

    val loadingEvent = SingleLiveEvent<Boolean>()
    val errorEvent = SingleLiveEvent<Throwable>()

    private val task = MutableLiveData<Task>()

    val title: LiveData<String> = Transformations.switchMap(task) {
        MutableLiveData<String>(it.title)
    }

    val description: LiveData<String> = Transformations.switchMap(task) {
        MutableLiveData<String>(it.description)
    }

    fun getTask(id: String) {
        val disposable = getTaskUseCase.getTask(id)
            .doOnSubscribe { loadingEvent.postValue(true) }
            .subscribe({ resource ->
                when (resource) {
                    is Failure -> errorEvent.postValue(resource.error)
                    is Success -> task.postValue(resource.data)
                }
            }, { throwable ->
                errorEvent.postValue(throwable)
            })
        disposables.add(disposable)
    }
}