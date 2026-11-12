package com.hashcore.pool.data.remote.interceptor

import com.hashcore.pool.data.local.prefs.SecureTokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches Bearer access token. Skips auth endpoints.
 * Token refresh is handled by [TokenAuthenticator] on 401.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: SecureTokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        // Never attach stale tokens to unauthenticated endpoints
        if (path.contains("/auth/login") ||
            path.contains("/auth/register") ||
            path.contains("/auth/refresh") ||
            path.contains("/auth/2fa") ||
            path.contains("/public/")
        ) {
            return chain.proceed(request)
        }
        val token = tokenStore.getAccessToken()
        if (token.isNullOrBlank()) {
            return chain.proceed(request)
        }
        val authed = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()
        return chain.proceed(authed)
    }
}
