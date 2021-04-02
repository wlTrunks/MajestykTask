package com.majestykapps.arch.presentation.taskdetail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.majestykapps.arch.data.common.Resource
import com.majestykapps.arch.domain.entity.Task
import com.majestykapps.arch.domain.usecase.GetTaskUseCase
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class TaskDetailViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    lateinit var useCase: GetTaskUseCase

    lateinit var viewModel: TaskDetailViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = TaskDetailViewModel(useCase)
    }

    @After
    fun clearMocks() {
        // Ensures inline Kotlin mocks do not leak
        Mockito.framework().clearInlineMocks()
    }

    @Test
    fun `success get task`() {
        val observerTitle: Observer<String> = mock()
        val observerDesc: Observer<String> = mock()
        val id = "id"
        val title = "test"
        val desc = "test_desk"
        val task = Task(id, title, desc)
        viewModel.description.observeForever(observerDesc)
        viewModel.title.observeForever(observerTitle)
        whenever(useCase.getTask(id)).thenReturn(Observable.just(Resource.Success(task)))
        viewModel.getTask(id)

        verify(observerTitle, times(1)).onChanged(title)
        verify(observerDesc, times(1)).onChanged(desc)
    }

    @Test
    fun `loading triggered`() {
        val observer: Observer<Boolean> = mock()
        viewModel.loadingEvent.observeForever(observer)
        val id = "id"
        whenever(useCase.getTask(id)).thenReturn(Observable.never())
        viewModel.getTask(id)
        verify(observer, times(1)).onChanged(true)
    }

    @Test
    fun `error triggered`() {
        val observer: Observer<Throwable> = mock()
        viewModel.errorEvent.observeForever(observer)
        val id = "id"
        val errorMessage = RuntimeException()
        whenever(useCase.getTask(id)).thenReturn(Observable.error(errorMessage))
        viewModel.getTask(id)
        verify(observer, times(1)).onChanged(errorMessage)
    }
}