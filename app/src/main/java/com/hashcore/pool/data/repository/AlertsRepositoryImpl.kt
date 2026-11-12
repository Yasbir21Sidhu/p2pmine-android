package com.hashcore.pool.data.repository

import com.hashcore.pool.BuildConfig
import com.hashcore.pool.data.remote.NetworkErrorMapper
import com.hashcore.pool.data.remote.api.HashCoreApi
import com.hashcore.pool.domain.repository.AlertsRepository
import com.hashcore.pool.domain.repository.MinerAlert
import com.hashcore.pool.domain.model.AppResult
import com.squareup.moshi.Moshi
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertsRepositoryImpl @Inject constructor(
    private val api: HashCoreApi,
    private val moshi: Moshi,
) : AlertsRepository {

    override suspend fun getAlerts(): AppResult<List<MinerAlert>> {
        if (BuildConfig.DEMO_MODE) {
            delay(200)
            return AppResult.Success(
                listOf(
                    MinerAlert(
                        id = "demo-1",
                        type = "worker_offline",
                        message = "Worker garage-m50 is offline",
                        createdAtEpochMs = System.currentTimeMillis() - 3_600_000,
                        dismissed = false,
                    ),
                ),
            )
        }
        return try {
            val list = api.alerts().alerts.map { a ->
                MinerAlert(
                    id = a.id ?: a.hashCode().toString(),
                    type = a.type ?: "alert",
                    message = a.message ?: a.title ?: "Pool alert",
                    createdAtEpochMs = a.createdAt?.value ?: a.createdAtSnake?.value,
                    dismissed = a.dismissedAt != null || a.dismissedAtSnake != null,
                )
            }
            AppResult.Success(list)
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }

    override suspend fun dismissAlert(id: String): AppResult<Unit> {
        if (BuildConfig.DEMO_MODE) {
            delay(150)
            return AppResult.Success(Unit)
        }
        return try {
            api.dismissAlert(id)
            AppResult.Success(Unit)
        } catch (t: Throwable) {
            NetworkErrorMapper.map(t, moshi)
        }
    }
}
