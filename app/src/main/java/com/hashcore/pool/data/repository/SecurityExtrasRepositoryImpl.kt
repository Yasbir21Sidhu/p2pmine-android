package com.hashcore.pool.data.repository

import com.hashcore.pool.BuildConfig
import com.hashcore.pool.data.remote.NetworkErrorMapper
import com.hashcore.pool.data.remote.api.HashCoreApi
import com.hashcore.pool.domain.model.AppResult
import com.hashcore.pool.domain.repository.ObserverLink
import com.hashcore.pool.domain.repository.SecurityExtrasRepository
import com.hashcore.pool.domain.repository.TwoFaSetup
import com.hashcore.pool.domain.repository.TwoFaStatus
import com.squareup.moshi.Moshi
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityExtrasRepositoryImpl @Inject constructor(
    private val api: HashCoreApi,
    private val moshi: Moshi,
) : SecurityExtrasRepository {

    override suspend fun twoFaStatus(): AppResult<TwoFaStatus> {
        if (BuildConfig.DEMO_MODE) return AppResult.Success(TwoFaStatus(false))
        return try {
            val map = api.twoFaStatus()
            val enabled = (map["enabled"] as? Boolean)
                ?: (map["isEnabled"] as? Boolean)
                ?: false
            AppResult.Success(TwoFaStatus(enabled))
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }

    override suspend fun twoFaSetup(): AppResult<TwoFaSetup> {
        if (BuildConfig.DEMO_MODE) {
            delay(300)
            return AppResult.Success(TwoFaSetup("DEMOSECRETBASE32", null))
        }
        return try {
            val map = api.twoFaSetup()
            val secret = map["secret"]?.toString().orEmpty().trim()
            // Website uses qrDataUrl (data:image/... base64). Accept aliases.
            val qr = sequenceOf(
                map["qrDataUrl"],
                map["qr_data_url"],
                map["qrCode"],
                map["qr_code"],
                map["qr"],
                map["otpauthUrl"],
                map["otpauth_url"],
            ).mapNotNull { it?.toString()?.takeIf { s -> s.isNotBlank() } }.firstOrNull()

            if (secret.isBlank()) AppResult.Error("Could not start 2FA setup.")
            else AppResult.Success(TwoFaSetup(secret, qr))
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }

    override suspend fun twoFaEnable(secret: String, code: String): AppResult<Unit> {
        if (BuildConfig.DEMO_MODE) {
            delay(300)
            return AppResult.Success(Unit)
        }
        return try {
            api.twoFaEnable(mapOf("secret" to secret, "code" to code))
            AppResult.Success(Unit)
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }

    override suspend fun twoFaDisable(code: String, password: String): AppResult<Unit> {
        if (BuildConfig.DEMO_MODE) {
            delay(300)
            return AppResult.Success(Unit)
        }
        return try {
            api.twoFaDisable(mapOf("code" to code, "password" to password))
            AppResult.Success(Unit)
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun listObserverLinks(): AppResult<List<ObserverLink>> {
        if (BuildConfig.DEMO_MODE) {
            return AppResult.Success(emptyList())
        }
        return try {
            val map = api.observerLinks()
            val raw = (map["links"] as? List<*>).orEmpty()
            val links = raw.mapNotNull { item ->
                val m = item as? Map<*, *> ?: return@mapNotNull null
                ObserverLink(
                    id = m["id"]?.toString() ?: return@mapNotNull null,
                    label = m["label"]?.toString(),
                    url = m["url"]?.toString() ?: m["link"]?.toString(),
                    createdAt = m["createdAt"]?.toString(),
                )
            }
            AppResult.Success(links)
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }

    override suspend fun createObserverLink(label: String?): AppResult<ObserverLink> {
        if (BuildConfig.DEMO_MODE) {
            delay(300)
            return AppResult.Success(
                ObserverLink("demo", label, "https://p2pmine.com/observer/demo", null),
            )
        }
        return try {
            val body = if (label.isNullOrBlank()) emptyMap() else mapOf("label" to label)
            val map = api.createObserverLink(body)
            @Suppress("UNCHECKED_CAST")
            val linkMap = (map["link"] as? Map<*, *>) ?: map
            val id = linkMap["id"]?.toString() ?: System.currentTimeMillis().toString()
            AppResult.Success(
                ObserverLink(
                    id = id,
                    label = linkMap["label"]?.toString() ?: label,
                    url = linkMap["url"]?.toString() ?: linkMap["link"]?.toString(),
                    createdAt = linkMap["createdAt"]?.toString(),
                ),
            )
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }

    override suspend fun deleteObserverLink(id: String): AppResult<Unit> {
        if (BuildConfig.DEMO_MODE) {
            delay(200)
            return AppResult.Success(Unit)
        }
        return try {
            api.deleteObserverLink(id)
            AppResult.Success(Unit)
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }
}
