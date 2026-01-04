package com.example.make.data.model

/**
 * Represents the state of an API call
 */
sealed class ApiState<out T> {
    object Idle : ApiState<Nothing>()
    object Loading : ApiState<Nothing>()
    data class Success<T>(val data: T) : ApiState<T>()
    data class Error(val message: String, val code: ErrorCode) : ApiState<Nothing>()
}

/**
 * Error codes for API failures
 */
enum class ErrorCode {
    RATE_LIMIT_EXCEEDED,    // API quota exceeded
    NETWORK_ERROR,          // Network connectivity issue
    INVALID_SYMBOL,         // Symbol not found or invalid
    API_KEY_INVALID,        // API key is missing or invalid
    PARSE_ERROR,            // JSON parsing failed
    UNKNOWN                 // Unknown error
}

/**
 * Result wrapper for API responses
 */
data class ApiResult<T>(
    val data: T? = null,
    val error: ApiError? = null,
    val isSuccess: Boolean = data != null && error == null
)

/**
 * Detailed error information
 */
data class ApiError(
    val code: ErrorCode,
    val message: String,
    val details: String? = null
)
