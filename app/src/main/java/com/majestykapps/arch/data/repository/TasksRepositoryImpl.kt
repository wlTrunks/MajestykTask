package com.majestykapps.arch.data.repository

import androidx.annotation.VisibleForTesting
import com.majestykapps.arch.data.common.Resource
import com.majestykapps.arch.data.source.TasksDataSource
import com.majestykapps.arch.data.source.remote.TasksRemoteDataSource
import com.majestykapps.arch.di.CoroutineDispatcherProvider
import com.majestykapps.arch.di.CoroutineDispatcherProviderImpl
import com.majestykapps.arch.domain.entity.Task
import com.majestykapps.arch.domain.repository.TasksRepository
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

/**
 * Concrete implementation to load tasks from the data sources into a cache.
 *
 * For simplicity, this implements a dumb synchronisation between locally persisted data and data
 * obtained from the server, by using the remote data source only if the local database doesn't
 * exist or is empty.
 */
class TasksRepositoryImpl @Inject constructor(
    @Named("local") private val tasksLocalDataSource: TasksDataSource,
    @Named("remote") private val tasksRemoteDataSource: TasksDataSource,
    private val dispatcherProvider: CoroutineDispatcherProvider
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

    override fun loadTasks(): Flow<Resource<List<Task>>> {
        Timber.tag(TAG).i("loadTasks: isCacheDirty = $isCacheDirty")
        // First check to see if there are cached tasks
        return flow {
            if (!isCacheDirty && cachedTasks.isNotEmpty()) {
                emit(Resource.Success(ArrayList(cachedTasks.values)))
            } else {
                emit(getAndCacheLocalTasks().first())
            }
            emit(getAndCacheRemoteTasks()
                .onStart { delay(100) }
                .catch { error: Throwable ->
                    flowOf(error)
                }.first()
            )
        }.flowOn(dispatcherProvider.io)
    }

    override fun getTask(id: String): Flow<Resource<Task>> {
        Timber.tag(TAG).i("getTask: id = $id, isCacheDirty = $isCacheDirty")
        return flow {
            if (!isCacheDirty && cachedTasks.isNotEmpty() && cachedTasks.containsKey(id)) {
                emit(Resource.Success(cachedTasks[id]))
            } else {
                emit(tasksLocalDataSource.getTask(id).onEach {
                    it.data?.let { cache(it) }
                }.first())
            }
            emit(tasksRemoteDataSource.getTask(id)
                .onStart { delay(100) }
                .onEach {
                    it.data?.let {
                        cache(it)
                        tasksLocalDataSource.saveTask(it)
                    }
                }
                .catch { flowOf(it) }.first()
            )
        }.flowOn(dispatcherProvider.io)
    }

    override fun searchTask(text: String): Flow<Resource<List<Task>>> {
        return flow {
            if (cachedTasks.isNotEmpty()) emit(Resource.Success(cachedTasks.values.toList()))
            else emit(getAndCacheLocalTasks().first())
        }.map {
            Resource.Success(
                it.data!!.filter {
                    it.title.startsWith(text, true) // add any specific constraint to search
                            || it.description.startsWith(text, true)
                })
        }
            .flowOn(dispatcherProvider.io)
    }

    override fun refresh() {
        Timber.tag(TAG).i("refresh() called")
        cachedTasks.clear()
        isCacheDirty = true
    }

    private fun getAndCacheRemoteTasks(): Flow<Resource<List<Task>>> =
        tasksRemoteDataSource.getTasks()
            .onEach { resource ->
                Timber.tag(TAG).d("getAndCacheRemoteTasks: emitted $resource")
                resource.data?.let { tasks ->
                    cache(tasks)
                    saveToDb(tasks)
                    isCacheDirty = false
                }
            }

    private fun getAndCacheLocalTasks(): Flow<Resource<List<Task>>> =
        tasksLocalDataSource.getTasks()
            .onEach { resource ->
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

    private suspend fun saveToDb(tasks: List<Task>?) {
        Timber.tag(TAG).d("saveToDb: $tasks")
        tasks?.let {
            tasksLocalDataSource.saveTasks(it)
        }
    }

    companion object {
        private const val TAG = "TasksRepository"

        private var INSTANCE: TasksRepositoryImpl? = null

        fun getInstance(
            tasksLocalDataSource: TasksDataSource,
            tasksRemoteDataSource: TasksDataSource = TasksRemoteDataSource.getInstance(),
            schedulerProvider: CoroutineDispatcherProvider = CoroutineDispatcherProviderImpl()
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