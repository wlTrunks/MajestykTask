package com.majestykapps.arch.presentation.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.majestykapps.arch.R
import com.majestykapps.arch.databinding.FragmentTasksBinding
import com.majestykapps.arch.databinding.ItemTaskBinding
import com.majestykapps.arch.domain.entity.Task
import com.majestykapps.arch.presentation.util.bindingDelegate
import com.majestykapps.arch.presentation.util.showSnack
import timber.log.Timber

class TasksFragment : Fragment(R.layout.fragment_tasks) {

    private val binding by bindingDelegate(FragmentTasksBinding::bind)

    private val viewModel: TasksViewModel by activityViewModels()

    private var snackbar: Snackbar? = null

    private val taskAdapter by lazy {
        TaskAdapter {
            findNavController().navigate(TasksFragmentDirections.taskFragmentToTaskDetailsFragment(it.id))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        initViewModelObservers()
    }

    private fun setupView() {
        with(binding) {
            swipeRefresh.setOnRefreshListener {
                viewModel.refresh()
            }
            listRV.apply {
                adapter = taskAdapter
                addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.swipeRefresh.isEnabled = false
    }

    override fun onResume() {
        super.onResume()
        binding.swipeRefresh.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        snackbar?.dismiss()
        snackbar = null
    }

    private fun initViewModelObservers() {
        with(binding) {
            viewModel.apply {
                loadingEvent.observe(viewLifecycleOwner, Observer { isRefreshing ->
                    swipeRefresh.isRefreshing = isRefreshing
                })

                errorEvent.observe(viewLifecycleOwner, Observer { throwable ->
                    Timber.tag(TAG).e(throwable)
                    showSnack(throwable.message) { viewModel.refresh() }.apply {
                        snackbar = this
                        show()
                    }
                })

                tasks.observe(viewLifecycleOwner, Observer { tasks ->
                    taskAdapter.setData(tasks)
                })
            }
        }
    }

    companion object {
        private const val TAG = "TasksFragment"

        fun newInstance() = TasksFragment()
    }
}

private class TaskAdapter(private val listener: (Task) -> Unit) : RecyclerView.Adapter<TaskAdapter.VH>() {

    private val listItems = mutableListOf<Task>()

    fun setData(list: List<Task>) {
        val diff = DiffUtil.calculateDiff(TaskDiffUtil(this.listItems, list))
        this.listItems.clear()
        this.listItems.addAll(list)
        diff.dispatchUpdatesTo(this)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(listItems[holder.adapterPosition])
    }

    override fun getItemCount(): Int = listItems.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = VH(
        ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    inner class VH(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Task) {
            itemView.setOnClickListener { listener.invoke(item) }
            with(binding) {
                titleTV.text = item.title
                descTV.text = item.description
            }
        }
    }
}

private class TaskDiffUtil(
    private val oldItems: List<Task>,
    private val newItems: List<Task>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = newItems.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldTask = oldItems[oldItemPosition]
        val newTask = newItems[newItemPosition]
        return oldTask == newTask
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldTask = oldItems[oldItemPosition]
        val newTask = newItems[newItemPosition]
        return oldTask.id == newTask.id && oldTask.title == newTask.title && oldTask.description == newTask.description
    }
}