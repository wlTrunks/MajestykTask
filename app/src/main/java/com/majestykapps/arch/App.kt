package com.majestykapps.arch

import android.app.Application
import android.os.StrictMode
import com.majestykapps.arch.di.ContextModule
import com.majestykapps.arch.di.DaggerTaskComponent
import com.majestykapps.arch.di.TaskComponent
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import timber.log.Timber
import timber.log.Timber.DebugTree
import javax.inject.Inject

class App : Application(), HasAndroidInjector {
    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = dispatchingAndroidInjector

    override fun onCreate() {
        super.onCreate()
        component = DaggerTaskComponent.builder()
            .contextModule(ContextModule(this))
            .build().apply {
                inject(this@App)
            }
        if (BuildConfig.DEBUG) {
            initStrictMode()
            Timber.plant(DebugTree())
        }
    }

    private fun initStrictMode() {
        val threadPolicy = StrictMode.ThreadPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build()
        StrictMode.setThreadPolicy(threadPolicy)

        val vmPolicy = StrictMode.VmPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build()
        StrictMode.setVmPolicy(vmPolicy)
    }

    companion object {
        internal lateinit var component: TaskComponent
    }
}