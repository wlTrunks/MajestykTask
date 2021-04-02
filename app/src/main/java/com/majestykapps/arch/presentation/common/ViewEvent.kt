package com.majestykapps.arch.presentation.common

/***
 * Common interface of view event
 */
interface ViewEvent

object Loading : ViewEvent
data class Error(val throwable: Throwable?) : ViewEvent