package com.getaltair.kairos.domain.common

/**
 * Generic wrapper for operation results that can succeed or fail.
 * Use for repository methods instead of throwing exceptions.
 *
 * @param T The type of the success value
 */
sealed class Result<out T> {
    /**
     * Represents a successful operation with a value.
     */
    data class Success<T>(val value: T) : Result<T>()

    /**
     * Represents a failed operation with an error message and optional cause.
     */
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()

    /**
     * Checks if the result is successful.
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Checks if the result is an error.
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Gets the success value or throws if result is an error.
     */
    fun getOrThrow(): T = when (this) {
        is Success<T> -> value
        is Error -> throw IllegalStateException(message, cause)
    }

    /**
     * Performs an action on success, or does nothing on error.
     */
    inline fun onSuccess(action: (T) -> Unit) {
        if (this is Success) {
            action(value)
        }
    }

    /**
     * Performs an action on error, or does nothing on success.
     */
    inline fun onError(action: (String, Throwable?) -> Unit) {
        if (this is Error) {
            action(message, cause)
        }
    }
}
