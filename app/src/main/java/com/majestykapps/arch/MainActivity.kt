package com.majestykapps.arch

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.majestykapps.arch.data.repository.TasksRepositoryImpl
import com.majestykapps.arch.data.source.local.TasksLocalDataSource
import com.majestykapps.arch.data.source.local.ToDoDatabase
import com.majestykapps.arch.databinding.ActivityMainBinding
import com.majestykapps.arch.presentation.common.ViewModelFactory
import com.majestykapps.arch.presentation.tasks.TasksFragment
import com.majestykapps.arch.presentation.tasks.TasksViewModel
import com.majestykapps.arch.presentation.util.visibleIf

class MainActivity : AppCompatActivity() {

    private lateinit var tasksViewModel: TasksViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupView()
        tasksViewModel = initViewModel()
        initViewModelObservers()
    }

    private fun setupView() {
        ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)
            toolbar.apply {
                setSupportActionBar(this)
                backIV.setOnClickListener {
                    navHostFragment.findNavController().popBackStack()
                }
            }
            navHostFragment.post {
                navHostFragment.findNavController()
                    .addOnDestinationChangedListener { _, destination, _ ->
                        searchIV.visibleIf(destination.id == R.id.taskFragment)
                        backIV.visibleIf(destination.id == R.id.taskDetailsFragment)
                    }
            }
            searchIV.setOnClickListener {
                val tasksFragment = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
                    .childFragmentManager.primaryNavigationFragment
                if (tasksFragment is TasksFragment) tasksFragment.showSearchView()
            }
        }
    }

    private fun initViewModelObservers() {
        tasksViewModel.apply {
            launchEvent.observe(this@MainActivity, Observer { id ->
                Log.d(TAG, "launchTask: launching task with id = $id")
                // TODO add task detail fragment
            })
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

inline fun <reified VM : ViewModel> FragmentActivity.initViewModel(): VM {
    val tasksDao = ToDoDatabase.getInstance(applicationContext).taskDao()
    val localDataSource = TasksLocalDataSource.getInstance(tasksDao)
    val tasksRepository = TasksRepositoryImpl.getInstance(localDataSource)
    val factory = ViewModelFactory.getInstance(tasksRepository)
    return ViewModelProvider(this, factory).get(VM::class.java)
}