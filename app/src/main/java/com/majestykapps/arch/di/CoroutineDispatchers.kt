package com.majestykapps.arch.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Provides different types of [CoroutineDispatcher].
 */

interface CoroutineDispatcherProvider {

    val default: CoroutineDispatcher

    val ui: CoroutineDispatcher

    val io: CoroutineDispatcher

    val unconfined: CoroutineDispatcher
}

class CoroutineDispatcherProviderImpl @JvmOverloads constructor(
    override val default: CoroutineDispatcher = Dispatchers.Default,
    override val ui: CoroutineDispatcher = Dispatchers.Main,
    override val io: CoroutineDispatcher = Dispatchers.IO,
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
) : CoroutineDispatcherProvider