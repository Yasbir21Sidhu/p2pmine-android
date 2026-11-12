package com.hashcore.pool.data.remote.interceptor

import com.hashcore.pool.data.local.prefs.SecureTokenStore
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P2PMine uses a single JWT (no refresh endpoint).
 * On 401 we clear the session so the UI can return to login.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: SecureTokenStore,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        if (response.request.url.encodedPath.contains("/auth/")) return null
        // Session invalid — clear tokens; UI observes logout via failed me/dashboard
        tokenStore.clear()
        return null
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
