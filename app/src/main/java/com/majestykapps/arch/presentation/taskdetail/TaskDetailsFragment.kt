package com.majestykapps.arch.presentation.taskdetail

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.majestykapps.arch.R
import com.majestykapps.arch.databinding.FragmentTaskDetailsBinding
import com.majestykapps.arch.presentation.common.Error
import com.majestykapps.arch.presentation.common.Loading
import com.majestykapps.arch.presentation.util.bindingDelegate
import com.majestykapps.arch.presentation.util.showSnack
import com.majestykapps.arch.presentation.util.visibleIf
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class TaskDetailsFragment : Fragment(R.layout.fragment_task_details) {

    private val binding by bindingDelegate(FragmentTaskDetailsBinding::bind)

    private val viewModel: TaskDetailViewModel by viewModels(factoryProducer = { viewModelFactory })

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
        getTaskId()
    }

    private fun getTaskId() {
        navArgs<TaskDetailsFragmentArgs>().value.taskId?.let {
            viewModel.getTask(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        snackbar?.dismiss()
        snackbar = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launchWhenResumed {
            viewModel.viewEvents
                .collect {
                    when (it) {
                        is Loading -> binding.progressBar.visibleIf(true)
                        is Error -> {
                            binding.progressBar.visibleIf(false)
                            showSnack(it.throwable?.message) { getTaskId() }.apply {
                                snackbar = this
                                show()
                            }
                        }
                    }
                }
        }
        lifecycleScope.launchWhenResumed {
            viewModel.state.collect {
                println("TASK $it")
                it.task?.let {
                    binding.progressBar.visibleIf(false)
                    binding.titleTV.text = it.title
                    binding.descTV.text = it.description
                }
            }
        }
    }
}