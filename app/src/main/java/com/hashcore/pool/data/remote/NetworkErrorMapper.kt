package com.hashcore.pool.data.remote

import com.hashcore.pool.data.remote.dto.ApiErrorDto
import com.hashcore.pool.domain.model.AppResult
import com.squareup.moshi.Moshi
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object NetworkErrorMapper {
    fun map(throwable: Throwable, moshi: Moshi? = null): AppResult.Error {
        return when (throwable) {
            is HttpException -> {
                val code = throwable.code()
                val body = try {
                    throwable.response()?.errorBody()?.string()
                } catch (_: Exception) {
                    null
                }
                val msg = parseMessage(body, moshi)
                    ?: when (code) {
                        401, 403 -> "Session expired. Please sign in again."
                        404 -> "Not found."
                        429 -> "Too many requests. Try again shortly."
                        in 500..599 -> "Server error. Please try again later."
                        else -> "Request failed ($code)."
                    }
                AppResult.Error(
                    message = msg,
                    cause = throwable,
                    isAuthError = code == 401 || code == 403,
                    isNetworkError = false,
                )
            }
            is UnknownHostException, is SocketTimeoutException, is IOException ->
                AppResult.Error(
                    message = "Network unavailable. Check your connection.",
                    cause = throwable,
                    isNetworkError = true,
                )
            else -> AppResult.Error(
                message = throwable.message ?: "Something went wrong.",
                cause = throwable,
            )
        }
    }

    private fun parseMessage(body: String?, moshi: Moshi?): String? {
        if (body.isNullOrBlank() || moshi == null) return null
        return try {
            val err = moshi.adapter(ApiErrorDto::class.java).fromJson(body)
            err?.error ?: err?.message
                ?: Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.getOrNull(1)
        } catch (_: Exception) {
            Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.getOrNull(1)
                ?: Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.getOrNull(1)
        }
    }
}
