package com.majestykapps.arch.data.repository

import androidx.annotation.VisibleForTesting
import com.majestykapps.arch.common.SchedulerProvider
import com.majestykapps.arch.common.ToDoSchedulerProvider
import com.majestykapps.arch.data.common.Resource
import com.majestykapps.arch.data.source.TasksDataSource
import com.majestykapps.arch.data.source.remote.TasksRemoteDataSource
import com.majestykapps.arch.domain.entity.Task
import com.majestykapps.arch.domain.repository.TasksRepository
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Concrete implementation to load tasks from the data sources into a cache.
 *
 * For simplicity, this implements a dumb synchronisation between locally persisted data and data
 * obtained from the server, by using the remote data source only if the local database doesn't
 * exist or is empty.
 */
class TasksRepositoryImpl private constructor(
    private val tasksLocalDataSource: TasksDataSource,
    private val tasksRemoteDataSource: TasksDataSource,
    private val schedulerProvider: SchedulerProvider
) : TasksRepository {

    init {
        RxJavaPlugins.setErrorHandler { e ->
            Timber.tag(TAG).e(e)
        }
    }

    @VisibleForTesting
    var tasksSubject: Subject<Resource<List<Task>>> = BehaviorSubject.create()
    private var disposableLoad: Disposable? = null
    private var disposableSearch: Disposable? = null

    /**
     * Map of cached tasks using their id as the key
     */
    @VisibleForTesting
    var cachedTasks: LinkedHashMap<String, Task> = LinkedHashMap()

    /**
     * When true indicates cached data should not be used
     */
    @VisibleForTesting
    var isCacheDirty = true

    override fun subscribe(observer: Observer<Resource<List<Task>>>) {
        tasksSubject.subscribe(observer)
    }

    override fun loadTasks() {
        Timber.tag(TAG).i("loadTasks: isCacheDirty = $isCacheDirty")
        val listRequest = mutableListOf<Observable<Resource<List<Task>>>>()
        // First check to see if there are cached tasks
        if (!isCacheDirty && cachedTasks.isNotEmpty()) {
            listRequest.add(Observable.just(Resource.Success(ArrayList(cachedTasks.values))))
        } else {
            listRequest.add(getAndCacheLocalTasks())
        }
        listRequest.add(getAndCacheRemoteTasks().delay(100, TimeUnit.MILLISECONDS)
            .onErrorResumeNext { error: Throwable ->
                tasksSubject.onNext(Resource.Failure(error))
                Observable.error(error)
            })
        disposableLoad?.dispose()
        disposableLoad = Observable.mergeDelayError(listRequest)
            .subscribeOn(schedulerProvider.io)
            .subscribe({
                tasksSubject.onNext(it)
            }, {
                Timber.tag(TAG).e(it)
                tasksSubject.onNext(Resource.Failure(it))
            })
    }

    override fun getTask(id: String): Observable<Resource<Task>> {
        Timber.tag(TAG).i("getTask: id = $id, isCacheDirty = $isCacheDirty")
        val listRequest = mutableListOf<Observable<Resource<Task>>>()
        if (!isCacheDirty && cachedTasks.isNotEmpty() && cachedTasks.containsKey(id)) {
            listRequest.add(Observable.just(Resource.Success(cachedTasks[id])))
        } else {
            listRequest.add(tasksLocalDataSource.getTask(id).doOnNext {
                it.data?.let { cache(it) }
            })
        }
        listRequest.add(tasksRemoteDataSource.getTask(id)
            .delay(100, TimeUnit.MILLISECONDS)
            .doOnNext {
                it.data?.let {
                    cache(it)
                    tasksLocalDataSource.saveTask(it)
                }
            }
            .onErrorResumeNext { error: Throwable ->
                Observable.error(error)
            })
        return Observable.mergeDelayError(listRequest).subscribeOn(schedulerProvider.io)
    }

    override fun searchTask(text: String) {
        val observer = if (cachedTasks.isNotEmpty()) Observable.just(cachedTasks.values)
        else getAndCacheLocalTasks().map { it.data }
        disposableSearch?.dispose()
        disposableSearch = observer
            .map { list ->
                list.filter {
                    it.title.startsWith(text, true) // add any specific constraint to search
                            || it.description.startsWith(text, true)
                }
            }.subscribeOn(schedulerProvider.io)
            .subscribe { tasksSubject.onNext(Resource.Success(it)) }
    }

    override fun refresh() {
        Timber.tag(TAG).i("refresh() called")
        cachedTasks.clear()
        isCacheDirty = true
    }

    private fun getAndCacheRemoteTasks(): Observable<Resource<List<Task>>> =
        tasksRemoteDataSource.getTasks()
            .doOnNext { resource ->
                Timber.tag(TAG).d("getAndCacheRemoteTasks: emitted $resource")
                resource.data?.let { tasks ->
                    cache(tasks)
                    saveToDb(tasks)
                    isCacheDirty = false
                }
            }

    private fun getAndCacheLocalTasks(): Observable<Resource<List<Task>>> =
        tasksLocalDataSource.getTasks()
            .doOnNext { resource ->
                Timber.tag(TAG).d("getAndCacheLocalTasks: emitted $resource")
                resource.data?.let { cache(it) }
            }

    private fun cache(tasks: List<Task>?) {
        Timber.tag(TAG).d("cache: $tasks")
        tasks?.apply {
            cachedTasks.clear()
            forEach { cache(it) }
        }
    }

    private fun cache(task: Task?) {
        Timber.tag(TAG).d("cache: $task")
        task?.id?.let { id ->
            cachedTasks[id] = task
        }
    }

    private fun saveToDb(tasks: List<Task>?) {
        Timber.tag(TAG).d("saveToDb: $tasks")
        tasks?.let {
            tasksLocalDataSource.saveTasks(it)
                .subscribeOn(schedulerProvider.io)
                .observeOn(schedulerProvider.main)
                .subscribe()
        }
    }

    companion object {
        private const val TAG = "TasksRepository"

        private var INSTANCE: TasksRepositoryImpl? = null

        fun getInstance(
            tasksLocalDataSource: TasksDataSource,
            tasksRemoteDataSource: TasksDataSource = TasksRemoteDataSource.getInstance(),
            schedulerProvider: SchedulerProvider = ToDoSchedulerProvider()
        ): TasksRepositoryImpl = INSTANCE ?: synchronized(this) {
            INSTANCE ?: TasksRepositoryImpl(
                tasksLocalDataSource,
                tasksRemoteDataSource,
                schedulerProvider
            ).also { INSTANCE = it }
        }

        @VisibleForTesting
        fun destroy() {
            INSTANCE = null
        }
    }
}