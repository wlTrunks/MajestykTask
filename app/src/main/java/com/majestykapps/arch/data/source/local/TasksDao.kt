package com.majestykapps.arch.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.majestykapps.arch.domain.entity.Task
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the tasks table.
 *
 * Using abstract class (instead of interface) to allow for @Transaction functions
 */
@Dao
interface TasksDao {

    /**
     * Select all tasks from the tasks table.
     */
    @Query("SELECT * FROM Tasks")
    fun getTasks(): Flow<List<Task>>

    /**
     * Select a task by id.
     *
     * @param taskId the task id.
     */
    @Query("SELECT * FROM Tasks WHERE id = :taskId")
    fun getTaskById(taskId: String): Flow<Task>

    /**
     * Insert a task in the database. If the task already exists, replace it.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskSync(task: Task)

    /**
     * Delete a task by id.
     *
     * @param taskId the task id.
     */
    @Query("DELETE FROM Tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    /**
     * Bulk insert in a transaction
     */
    @Transaction
    suspend fun insertTasks(tasks: List<Task>) {
        tasks.forEach { insertTaskSync(it) }
    }
}