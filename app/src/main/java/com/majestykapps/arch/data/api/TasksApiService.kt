package com.majestykapps.arch.data.api

import com.majestykapps.arch.BuildConfig
import com.majestykapps.arch.domain.entity.Task
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface TasksApiService {

    object Config {
        const val BASE_URL = "https://api.jsonbin.io/"
    }

    /**
     * Example of a response that encapsulates results in a JSON payload
     */
    @GET("b/5d66b3a2a42e3b278d17b1a3/latest")
    suspend fun getTasks(
        @Header("secret-key")
        apiKey: String = BuildConfig.API_SECRET
    ): ApiResponse<Task>

    /**
     * Example of a response that contains a JSON object
     */
    @GET("b/{id}/latest")
    suspend fun getTask(@Path("id") id: String): Task
}