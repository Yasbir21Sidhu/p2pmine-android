package com.hashcore.pool.data.remote.websocket

import com.hashcore.pool.BuildConfig
import com.hashcore.pool.data.local.prefs.SecureTokenStore
import com.hashcore.pool.data.remote.DemoData
import com.hashcore.pool.data.remote.api.HashCoreApi
import com.hashcore.pool.data.remote.dto.LiveUpdateDto
import com.hashcore.pool.data.remote.dto.toDomain
import com.hashcore.pool.domain.model.LiveUpdate
import com.hashcore.pool.domain.model.WorkersSummary
import com.hashcore.pool.domain.repository.LiveConnectionState
import com.hashcore.pool.domain.repository.LiveDataRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.math.min
import kotlin.random.Random

/**
 * P2PMine live feed: wss://p2pmine.com/ws?token=...
 * Frames: { "type": "miner_dashboard", "data": { ...dashboard fields } }
 * Fallback: REST dashboard poll every 7s.
 */
@Singleton
class LiveDataRepositoryImpl @Inject constructor(
    @Named("wsClient") private val wsClient: OkHttpClient,
    private val api: HashCoreApi,
    private val tokenStore: SecureTokenStore,
    private val moshi: Moshi,
) : LiveDataRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = AtomicBoolean(false)

    private val _updates = MutableSharedFlow<LiveUpdate>(extraBufferCapacity = 16)
    override val updates: SharedFlow<LiveUpdate> = _updates.asSharedFlow()

    private val _state = MutableStateFlow(LiveConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<LiveConnectionState> = _state.asStateFlow()

    private var webSocket: WebSocket? = null
    private var pollJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0

    override fun start() {
        if (!running.compareAndSet(false, true)) return
        if (BuildConfig.DEMO_MODE) {
            startDemoStream()
            return
        }
        connectWebSocket()
    }

    override fun stop() {
        running.set(false)
        reconnectJob?.cancel()
        pollJob?.cancel()
        pollJob = null
        webSocket?.close(1000, "app background")
        webSocket = null
        _state.value = LiveConnectionState.DISCONNECTED
        reconnectAttempt = 0
    }

    private fun connectWebSocket() {
        if (!running.get()) return
        _state.value = LiveConnectionState.CONNECTING
        val token = tokenStore.getAccessToken().orEmpty()
        if (token.isBlank()) {
            startPollingFallback()
            return
        }
        val base = BuildConfig.WS_BASE_URL.trimEnd('/')
        val url = "$base?token=${java.net.URLEncoder.encode(token, "UTF-8")}"
        val request = Request.Builder().url(url).build()
        webSocket?.cancel()
        webSocket = wsClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempt = 0
                pollJob?.cancel()
                _state.value = LiveConnectionState.CONNECTED
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val dto = moshi.adapter(LiveUpdateDto::class.java).fromJson(text) ?: return
                    if (dto.type == "miner_dashboard" && dto.data != null) {
                        val dash = dto.data.toDomain(rangeHint = "24h")
                        _updates.tryEmit(
                            LiveUpdate(
                                currentHashrateHs = dash.currentHashrateHs,
                                avg24hHashrateHs = dash.avg24hHashrateHs,
                                workersSummary = dash.workers,
                                timestampEpochMs = System.currentTimeMillis(),
                            ),
                        )
                    } else if (dto.hashrateHs != null) {
                        _updates.tryEmit(
                            LiveUpdate(
                                currentHashrateHs = dto.hashrateHs,
                                workersSummary = WorkersSummary(
                                    online = dto.onlineWorkers ?: 0,
                                    offline = dto.offlineWorkers ?: 0,
                                    problem = 0,
                                ),
                            ),
                        )
                    }
                } catch (_: Exception) {
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                scheduleReconnectOrPoll()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                scheduleReconnectOrPoll()
            }
        })
    }

    private fun scheduleReconnectOrPoll() {
        if (!running.get()) return
        if (reconnectAttempt >= 3) startPollingFallback()
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delayMs = min(30_000L, (1_000L * (1 shl reconnectAttempt.coerceAtMost(4))))
            reconnectAttempt++
            delay(delayMs)
            if (running.get()) connectWebSocket()
        }
    }

    private fun startPollingFallback() {
        if (pollJob?.isActive == true) return
        _state.value = LiveConnectionState.POLLING_FALLBACK
        pollJob = scope.launch {
            while (isActive && running.get()) {
                try {
                    val dash = api.dashboard("24h").toDomain(rangeHint = "24h")
                    _updates.emit(
                        LiveUpdate(
                            currentHashrateHs = dash.currentHashrateHs,
                            avg24hHashrateHs = dash.avg24hHashrateHs,
                            workersSummary = dash.workers,
                        ),
                    )
                } catch (_: Exception) {
                }
                delay(7_000)
            }
        }
    }

    private fun startDemoStream() {
        _state.value = LiveConnectionState.CONNECTED
        pollJob = scope.launch {
            while (isActive && running.get()) {
                val jitter = 1.0 + Random.nextDouble(-0.02, 0.02)
                val dash = DemoData.dashboard(jitter)
                _updates.emit(
                    LiveUpdate(
                        currentHashrateHs = dash.currentHashrateHs,
                        avg24hHashrateHs = dash.avg24hHashrateHs,
                        workers = DemoData.workers(),
                        workersSummary = dash.workers,
                    ),
                )
                delay(5_000)
            }
        }
    }
}
