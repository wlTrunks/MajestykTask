package com.majestykapps.arch

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
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
import com.majestykapps.arch.presentation.util.NetworkConnectionChecker
import com.majestykapps.arch.presentation.util.NetworkConnectionLiveData
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
            networkConnectionLiveData.observe(this@MainActivity, {
                if (!it) {
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage(R.string.no_network)
                        .setPositiveButton(android.R.string.ok) { _, _ -> startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
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
    val factory = ViewModelFactory.getInstance(tasksRepository, object : NetworkConnectionLiveData {
        override fun provideLiveData(): LiveData<Boolean> = NetworkConnectionChecker(applicationContext)
    })
    return ViewModelProvider(this, factory).get(VM::class.java)
}