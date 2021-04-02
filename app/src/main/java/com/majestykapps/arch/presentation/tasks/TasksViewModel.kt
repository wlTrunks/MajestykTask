package com.majestykapps.arch.presentation.tasks

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import com.majestykapps.arch.data.common.Resource
import com.majestykapps.arch.data.common.Resource.Failure
import com.majestykapps.arch.data.common.Resource.Loading
import com.majestykapps.arch.data.common.Resource.Success
import com.majestykapps.arch.domain.entity.Task
import com.majestykapps.arch.domain.usecase.SubscribeTasksUseCase
import com.majestykapps.arch.presentation.common.BaseViewModel
import com.majestykapps.arch.util.SingleLiveEvent
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import timber.log.Timber

class TasksViewModel(
    private val subscribeTasksUseCase: SubscribeTasksUseCase
) : BaseViewModel() {

    val loadingEvent = SingleLiveEvent<Boolean>()
    val errorEvent = SingleLiveEvent<Throwable>()
    val launchEvent = SingleLiveEvent<String>()

    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> get() = _tasks.distinctUntilChanged()

    @VisibleForTesting
    val tasksObserver = object : Observer<Resource<List<Task>>> {
        override fun onSubscribe(d: Disposable) {
            disposables.add(d)
        }

        override fun onNext(resource: Resource<List<Task>>) {
            Timber.tag(TAG).d("tasksObserver onNext: resource = $resource")
            when (resource) {
                is Loading -> {
                    loadingEvent.postValue(true)
                }
                is Failure -> {
                    loadingEvent.postValue(false)
                    errorEvent.postValue(resource.error)
                }
                is Success -> {
                    loadingEvent.postValue(false)
                    _tasks.postValue(resource.data)
                }
            }
        }

        override fun onError(e: Throwable) {
            // Uncaught errors will land here
            loadingEvent.postValue(false)
            errorEvent.postValue(e)
        }

        override fun onComplete() {
            loadingEvent.postValue(false)
        }
    }

    init {
        subscribeTasksUseCase.subscribe(tasksObserver)
        subscribeTasksUseCase.load()
    }

    fun onTaskClick(id: String) {
        launchEvent.value = id
    }

    fun refresh() {
        subscribeTasksUseCase.refresh()
    }

    //make it simple
    fun searchTask(text: String) {
        subscribeTasksUseCase.search(text)
    }

    companion object {
        private const val TAG = "TasksViewModel"
    }
}