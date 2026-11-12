package com.hashcore.pool.di

import com.hashcore.pool.BuildConfig
import com.hashcore.pool.data.remote.api.HashCoreApi
import com.hashcore.pool.data.remote.interceptor.AuthInterceptor
import com.hashcore.pool.data.remote.interceptor.TokenAuthenticator
import com.hashcore.pool.data.remote.websocket.LiveDataRepositoryImpl
import com.hashcore.pool.data.repository.AlertsRepositoryImpl
import com.hashcore.pool.data.repository.AuthRepositoryImpl
import com.hashcore.pool.data.repository.DashboardRepositoryImpl
import com.hashcore.pool.data.repository.EarningsRepositoryImpl
import com.hashcore.pool.data.repository.PayoutsRepositoryImpl
import com.hashcore.pool.data.repository.SecurityExtrasRepositoryImpl
import com.hashcore.pool.data.repository.SettingsRepositoryImpl
import com.hashcore.pool.data.repository.WorkersRepositoryImpl
import com.hashcore.pool.domain.repository.AlertsRepository
import com.hashcore.pool.domain.repository.AuthRepository
import com.hashcore.pool.domain.repository.DashboardRepository
import com.hashcore.pool.domain.repository.EarningsRepository
import com.hashcore.pool.domain.repository.LiveDataRepository
import com.hashcore.pool.domain.repository.PayoutsRepository
import com.hashcore.pool.domain.repository.SecurityExtrasRepository
import com.hashcore.pool.domain.repository.SettingsRepository
import com.hashcore.pool.domain.repository.WorkersRepository
import com.hashcore.pool.data.remote.dto.FlexibleEpochMsAdapter
import com.hashcore.pool.data.remote.dto.FlexibleTimeLabelAdapter
import com.hashcore.pool.data.remote.dto.HashratePointListAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun auth(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun dashboard(impl: DashboardRepositoryImpl): DashboardRepository
    @Binds @Singleton abstract fun workers(impl: WorkersRepositoryImpl): WorkersRepository
    @Binds @Singleton abstract fun earnings(impl: EarningsRepositoryImpl): EarningsRepository
    @Binds @Singleton abstract fun payouts(impl: PayoutsRepositoryImpl): PayoutsRepository
    @Binds @Singleton abstract fun settings(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds @Singleton abstract fun live(impl: LiveDataRepositoryImpl): LiveDataRepository
    @Binds @Singleton abstract fun alerts(impl: AlertsRepositoryImpl): AlertsRepository
    @Binds @Singleton abstract fun securityExtras(impl: SecurityExtrasRepositoryImpl): SecurityExtrasRepository
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        // Factories/adapters before codegen + Kotlin reflect fallback
        .add(HashratePointListAdapterFactory())
        .add(FlexibleEpochMsAdapter())
        .add(FlexibleTimeLabelAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttp(
        authInterceptor: AuthInterceptor,
        authenticator: TokenAuthenticator,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .authenticator(authenticator)
            .addInterceptor(authInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor { message ->
                    // Never print bearer tokens if log level is raised later
                    val redacted = message
                        .replace(Regex("(?i)authorization:\\s*bearer\\s+\\S+"), "Authorization: Bearer <redacted>")
                        .replace(Regex("(?i)token=([^&\\s]+)"), "token=<redacted>")
                    android.util.Log.d("P2PMineHttp", redacted)
                }.apply {
                    // BASIC: method/url/status only — no headers/body (safer for JWT)
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BASIC
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                },
            )

        // Certificate pinning when pins are configured (production)
        val pins = BuildConfig.CERT_PINS.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (pins.isNotEmpty() && BuildConfig.PIN_HOSTNAME.isNotBlank()) {
            val pinnerBuilder = CertificatePinner.Builder()
            pins.forEach { pin ->
                val formatted = if (pin.startsWith("sha256/")) pin else "sha256/$pin"
                pinnerBuilder.add(BuildConfig.PIN_HOSTNAME, formatted)
            }
            builder.certificatePinner(pinnerBuilder.build())
        }

        return builder.build()
    }

    @Provides
    @Singleton
    @Named("wsClient")
    fun provideWsClient(base: OkHttpClient): OkHttpClient =
        base.newBuilder()
            .pingInterval(20, TimeUnit.SECONDS)
            // WS auth is via query token; no HTTP authenticator recursion
            .authenticator(okhttp3.Authenticator.NONE)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit {
        val base = BuildConfig.API_BASE_URL.let {
            if (it.endsWith("/")) it else "$it/"
        }
        return Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideApi(retrofit: Retrofit): HashCoreApi = retrofit.create(HashCoreApi::class.java)
}
