package com.majestykapps.arch

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.majestykapps.arch.databinding.ActivityMainBinding
import com.majestykapps.arch.presentation.tasks.TasksFragment
import com.majestykapps.arch.presentation.tasks.TasksViewModel
import com.majestykapps.arch.presentation.util.visibleIf
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

class MainActivity : AppCompatActivity(), HasAndroidInjector {

    private val tasksViewModel: TasksViewModel by viewModels { viewModelFactory }

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun androidInjector(): AndroidInjector<Any> = dispatchingAndroidInjector

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setupView()
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