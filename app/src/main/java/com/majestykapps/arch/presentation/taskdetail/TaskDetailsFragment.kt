package com.majestykapps.arch.presentation.taskdetail

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.majestykapps.arch.R
import com.majestykapps.arch.databinding.FragmentTaskDetailsBinding
import com.majestykapps.arch.initViewModel
import com.majestykapps.arch.presentation.util.bindingDelegate
import com.majestykapps.arch.presentation.util.showSnack
import com.majestykapps.arch.presentation.util.visibleIf

class TaskDetailsFragment : Fragment(R.layout.fragment_task_details) {

    private val binding by bindingDelegate(FragmentTaskDetailsBinding::bind)

    private val viewModel: TaskDetailViewModel by lazy { requireActivity().initViewModel() }

    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
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
        with(viewModel) {
            loadingEvent.observe(viewLifecycleOwner, {
                binding.progressBar.visibleIf(true)
            })
            errorEvent.observe(viewLifecycleOwner, {
                binding.progressBar.visibleIf(false)
                showSnack(it.message) { getTaskId() }.apply {
                    snackbar = this
                    show()
                }
            })
            title.observe(viewLifecycleOwner, {
                binding.progressBar.visibleIf(false)
                binding.titleTV.text = it
            })
            description.observe(viewLifecycleOwner, {
                binding.descTV.text = it
            })
        }
    }
}