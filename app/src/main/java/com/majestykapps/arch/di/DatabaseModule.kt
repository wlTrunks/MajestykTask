package com.majestykapps.arch.di

import android.content.Context
import androidx.room.Room
import com.majestykapps.arch.data.source.local.TasksDao
import com.majestykapps.arch.data.source.local.ToDoDatabase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

private const val DB_VERSION: Int = 1
private const val DB_NAME: String = "tasks.db"

@Module
internal class DatabaseModule {

    /**
     * Database initialization.
     * Set database name and (optionally) provide a fallback strategy here.
     */
    @Singleton
    @Provides
    fun database(context: Context): ToDoDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            ToDoDatabase::class.java,
            DB_NAME
        ).build()
    }

    @Provides
    fun tasksDao(db: ToDoDatabase): TasksDao = db.taskDao()
}