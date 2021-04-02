package com.majestykapps.arch.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.majestykapps.arch.common.TestSchedulerProvider
import com.majestykapps.arch.data.common.Resource
import com.majestykapps.arch.data.repository.TasksRepositoryImpl
import com.majestykapps.arch.data.source.local.TasksLocalDataSource
import com.majestykapps.arch.data.source.remote.TasksRemoteDataSource
import com.majestykapps.arch.domain.entity.Task
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

class TasksRepositoryTest {

    /**
     * Runs Arch Components on a synchronous executor
     */
    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var localDataSource: TasksLocalDataSource

    @Mock
    private lateinit var remoteDataSource: TasksRemoteDataSource

    /**
     * Runs RxJava synchronously
     */
    private val schedulerProvider = TestSchedulerProvider()
    private val testScheduler = TestScheduler()

    /**
     * Receives emissions from [TasksRepositoryImpl.tasksSubject]
     */
    private val tasksObserver: Observer<Resource<List<Task>>> = mock()

    private lateinit var repository: TasksRepositoryImpl

    @Captor
    private lateinit var resourceDataCaptor: ArgumentCaptor<Resource<List<Task>>>

    @Before
    fun setup() {
        // Allows us to use @Mock annotations
        MockitoAnnotations.openMocks(this)

        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }
        repository = TasksRepositoryImpl.getInstance(
            localDataSource,
            remoteDataSource,
            schedulerProvider
        ).apply {
            subscribe(tasksObserver)
        }
    }

    @After
    fun clearMocks() {
        // Ensures inline Kotlin mocks do not leak
        Mockito.framework().clearInlineMocks()
        TasksRepositoryImpl.destroy()
        RxJavaPlugins.reset()
    }

    @Test
    fun `task subject subscribed`() {
        verify(tasksObserver, times(1)).onSubscribe(any())
    }

    @Test
    fun `task subscription observes task emission`() {
        val resource: Resource<List<Task>> = mock()
        repository.tasksSubject.onNext(resource)
        verify(tasksObserver, times(1)).onNext(resource)
    }

    @Test
    fun `cache is cleared and marked dirty on refresh`() {
        repository.apply {
            cachedTasks["a"] = mock()
            isCacheDirty = false
        }

        repository.refresh()

        assertTrue(repository.cachedTasks.isEmpty())
        assertTrue(repository.isCacheDirty)
    }

    @Test
    fun `cached tasks returned when cache is not empty or dirty`() {
        val task = Task("a", "test", "task")
        repository.apply {
            isCacheDirty = false
            cachedTasks["a"] = task
        }
        whenever(remoteDataSource.getTasks()).thenReturn(Observable.never<Resource<List<Task>>>())
        repository.loadTasks()
        delay()
        verify(tasksObserver, times(1)).onNext(resourceDataCaptor.capture())
        assertTrue(resourceDataCaptor.value is Resource.Success)
        assertEquals(ArrayList(repository.cachedTasks.values), resourceDataCaptor.value.data)
    }

    @Test
    fun `cached tasks not returned when cache is dirty`() {
        whenever(localDataSource.getTasks()).thenReturn(Observable.never<Resource<List<Task>>>())
        whenever(remoteDataSource.getTasks()).thenReturn(Observable.never<Resource<List<Task>>>())

        repository.loadTasks()

        verify(tasksObserver, never()).onNext(any())
    }

    @Test
    fun `remote tasks returned when cache is dirty`() {
        whenever(localDataSource.getTasks()).thenReturn(Observable.never<Resource<List<Task>>>())
        val resource: Resource<List<Task>> = mock()
        whenever(remoteDataSource.getTasks()).thenReturn(Observable.just(resource))

        repository.loadTasks()
        delay()
        verify(tasksObserver, times(1)).onNext(resource)
    }

    @Test
    fun `local tasks returned when cache is dirty and remote call fails`() {
        whenever(remoteDataSource.getTasks()).thenReturn(Observable.error(RuntimeException()))
        val resource: Resource<List<Task>> = mock()
        whenever(localDataSource.getTasks()).thenReturn(Observable.just(resource))

        repository.loadTasks()
        delay()
        verify(tasksObserver, times(1)).onNext(resource)
    }

    @Test
    fun `local tasks returned when cache is not dirty but is empty`() {
        repository.isCacheDirty = false
        val resource: Resource<List<Task>> = mock()
        whenever(localDataSource.getTasks()).thenReturn(Observable.just(resource))
        whenever(remoteDataSource.getTasks()).thenReturn(Observable.just(resource))
        repository.loadTasks()
        delay()
        verify(tasksObserver, times(2)).onNext(resource)
    }

    @Test
    fun `remote tasks are cached when successfully retrieved`() {
        whenever(localDataSource.getTasks()).thenReturn(Observable.never<Resource<List<Task>>>())
        val task = mock<Task> {
            on { id } doReturn "a"
        }
        val data = listOf(task)
        val resource = Resource.Success(data)
        whenever(remoteDataSource.getTasks()).thenReturn(Observable.just(resource))

        repository.loadTasks()
        delay()
        assertEquals(repository.cachedTasks["a"], task)
    }

    @Test
    fun `remote tasks are saved to db when successfully retrieved`() {
        whenever(localDataSource.getTasks()).thenReturn(Observable.never<Resource<List<Task>>>())
        val data = listOf(mock<Task>())
        val resource = Resource.Success(data)
        whenever(remoteDataSource.getTasks()).thenReturn(Observable.just(resource))

        repository.loadTasks()
        delay()
        verify(localDataSource, times(1)).saveTasks(data)
    }

    @Test
    fun `cache is marked clean when remote tasks are cached`() {
        whenever(localDataSource.getTasks()).thenReturn(Observable.never<Resource<List<Task>>>())
        whenever(localDataSource.saveTasks(any())).thenReturn(Completable.never())
        val data = listOf(mock<Task>())
        val resource = Resource.Success(data)
        whenever(remoteDataSource.getTasks()).thenReturn(Observable.just(resource))

        repository.loadTasks()
        delay()
        assertFalse(repository.isCacheDirty)
    }

    @Test
    fun `local tasks are cached when successfully retrieved`() {
        repository.isCacheDirty = false
        val task = mock<Task> {
            on { id } doReturn "a"
        }
        val data = listOf(task)
        val resource = Resource.Success(data)
        whenever(localDataSource.getTasks()).thenReturn(Observable.just(resource))
        whenever(remoteDataSource.getTasks()).thenReturn(Observable.just(resource))
        repository.loadTasks()
        delay()
        assertEquals(repository.cachedTasks["a"], task)
    }

    @Test
    fun `get task by id local and remote cached filled`() {
        val task = mock<Task> {
            on { id } doReturn "a"
        }
        val resource = Resource.Success(task)
        whenever(localDataSource.getTask("a")).thenReturn(Observable.just(resource))
        whenever(remoteDataSource.getTask("a")).thenReturn(Observable.just(resource))
        assert(repository.cachedTasks.isEmpty())
        val testObserver = TestObserver<Resource<Task>>()
        val observe = repository.getTask("a")
        observe.subscribe(testObserver)
        delay()
        testObserver.assertValues(resource, resource)
            .assertSubscribed()
            .assertNoErrors()
            .assertValueCount(2)
        assert(repository.cachedTasks.isNotEmpty())
        assertEquals(repository.cachedTasks["a"], task)
    }

    @Test
    fun `get task by id local and cache empty`() {
        val task = mock<Task> {
            on { id } doReturn "a"
        }
        val resource = Resource.Success(task)
        whenever(localDataSource.getTask("a")).thenReturn(Observable.never<Resource<Task>>())
        whenever(remoteDataSource.getTask("a")).thenReturn(Observable.just(resource))
        assert(repository.cachedTasks.isEmpty())
        val testObserver = TestObserver<Resource<Task>>()
        val observe = repository.getTask("a")
        observe.subscribe(testObserver)
        delay()
        testObserver.assertValues(resource)
            .assertSubscribed()
            .assertNoErrors()
            .assertValueCount(1)
        verify(localDataSource, times(1)).saveTask(task)
        assert(repository.cachedTasks.isNotEmpty())
        assertEquals(repository.cachedTasks["a"], task)
    }

    @Test
    fun `get task by id remote error local empty`() {
        whenever(localDataSource.getTask("a")).thenReturn(Observable.never<Resource<Task>>())
        whenever(remoteDataSource.getTask("a")).thenReturn(Observable.error(RuntimeException()))
        val testObserver = TestObserver<Resource<Task>>()
        val observe = repository.getTask("a")
        observe.subscribe(testObserver)
        delay()
        testObserver
            .assertSubscribed()
            .assertNoValues()
            .assertEmpty()
            .assertNotTerminated()
    }

    @Test
    fun `get task by id remote error local has data`() {
        val task = mock<Task> {
            on { id } doReturn "a"
        }
        val resource = Resource.Success(task)
        whenever(localDataSource.getTask("a")).thenReturn(Observable.just(resource))
        whenever(remoteDataSource.getTask("a")).thenReturn(Observable.error(RuntimeException()))
        val testObserver = TestObserver<Resource<Task>>()
        val observe = repository.getTask("a")
        observe.subscribe(testObserver)
        delay()
        testObserver
            .assertValueCount(1)
            .assertError(RuntimeException::class.java)
            .assertSubscribed()
            .assertTerminated()
    }

    @Test
    fun `get task by id from cache`() {
        val task = Task("a", "test", "task")
        repository.apply {
            isCacheDirty = false
            cachedTasks["a"] = task
        }
        val resource = Resource.Success(task)
        val testObserver = TestObserver<Resource<Task>>()
        whenever(remoteDataSource.getTask("a")).thenReturn(Observable.just(resource))
        val observe = repository.getTask("a")
        observe.subscribe(testObserver)
        delay()
        testObserver
            .assertValueCount(2)
            .assertValueAt(0) { it.data == task }
            .assertValueAt(1) { it.data == task }
            .assertSubscribed()
            .assertNoErrors()
            .assertComplete()
        verify(localDataSource, times(0)).getTask("a")
        verify(localDataSource, times(1)).saveTask(task)
    }

    @Test
    fun `search task from db`() {
        val task1 = Task("1", "test")
        val tasks = listOf(task1, Task("2", "some"))
        val resource = Resource.Success(tasks)
        whenever(localDataSource.getTasks()).thenReturn(Observable.just(resource))
        val testObserver = TestObserver<Resource<List<Task>>>()
        repository.searchTask("test")
        verify(localDataSource, times(1)).getTasks()
        repository.subscribe(testObserver)
        testObserver.assertValueAt(0) { it.data?.get(0) == task1 }
            .assertValueAt(0) { it.data!!.size == 1 }
    }

    @Test
    fun `search task from cache`() {
        val task1 = Task("1", "test")
        val task2 = Task("2", "some")
        repository.apply {
            cachedTasks[task1.id!!] = task1
            cachedTasks[task2.id!!] = task2
        }
        val testObserver = TestObserver<Resource<List<Task>>>()
        repository.searchTask("test")
        verify(localDataSource, times(0)).getTasks()
        repository.subscribe(testObserver)
        testObserver.assertValueAt(0) { it.data?.get(0) == task1 }
            .assertValueAt(0) { it.data!!.size == 1 }
    }

    @Test
    fun `search task failed`() {
        val task1 = Task("1", "test")
        val tasks = listOf(task1, Task("2", "some"))
        val resource = Resource.Success(tasks)
        whenever(localDataSource.getTasks()).thenReturn(Observable.just(resource))
        val testObserver = TestObserver<Resource<List<Task>>>()
        repository.searchTask("new")
        verify(localDataSource, times(1)).getTasks()
        repository.subscribe(testObserver)
        testObserver.assertValueAt(0) { it.data!!.isEmpty() }
    }

    private fun delay() {
        testScheduler.advanceTimeBy(150, TimeUnit.MILLISECONDS)
    }
}
