package com.majestykapps.arch.presentation.tasks

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import com.majestykapps.arch.data.common.Resource
import com.majestykapps.arch.data.common.Resource.Failure
import com.majestykapps.arch.data.common.Resource.Loading
import com.majestykapps.arch.data.common.Resource.Success
import com.majestykapps.arch.domain.entity.Task
import com.majestykapps.arch.domain.usecase.SubscribeTasksUseCase
import com.majestykapps.arch.presentation.util.NetworkConnectionChecker
import com.majestykapps.arch.util.SingleLiveEvent
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class TasksViewModel @Inject constructor(
    context: Context,
    private val subscribeTasksUseCase: SubscribeTasksUseCase
) : ViewModel() {

    val loadingEvent = SingleLiveEvent<Boolean>()
    val errorEvent = SingleLiveEvent<Throwable?>()
    val launchEvent = SingleLiveEvent<String>()

    private val _tasks = MutableLiveData<List<Task>?>()
    val tasks: LiveData<List<Task>?> get() = _tasks.distinctUntilChanged()

    val networkConnectionLiveData: LiveData<Boolean> = NetworkConnectionChecker(context)

    @VisibleForTesting
    val tasksObserver = object : Observer<Resource<List<Task>>> {
        override fun onSubscribe(d: Disposable) {
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
        viewModelScope.launch {
            val resource = subscribeTasksUseCase.load().onStart {
                loadingEvent.postValue(true)
            }.catch {
                errorEvent.postValue(it)
            }.collect { resource ->
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
        }
    }

    fun onTaskClick(id: String) {
        launchEvent.value = id
    }

    fun refresh() {
        viewModelScope.launch {
            subscribeTasksUseCase.refresh()
        }
    }

    //make it simple
    fun searchTask(text: String) {
        viewModelScope.launch {
            val res = subscribeTasksUseCase.search(text).first()
            println("res $res")
            _tasks.postValue(res.data)
        }
    }

    companion object {
        private const val TAG = "TasksViewModel"
    }
}