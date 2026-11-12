package com.hashcore.pool.presentation.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hashcore.pool.domain.repository.LiveConnectionState
import com.hashcore.pool.presentation.common.UiState
import com.hashcore.pool.presentation.components.DashboardSkeleton
import com.hashcore.pool.presentation.components.ErrorState
import com.hashcore.pool.presentation.components.HashCoreCard
import com.hashcore.pool.presentation.components.HashrateChart
import com.hashcore.pool.presentation.components.LivePulseDot
import com.hashcore.pool.presentation.components.MetricBlock
import com.hashcore.pool.presentation.components.SectionTitle
import com.hashcore.pool.presentation.components.SetupStepRow
import com.hashcore.pool.presentation.theme.HashCoreAccent
import com.hashcore.pool.presentation.theme.HashCoreBlack
import com.hashcore.pool.presentation.theme.HashCoreBorder
import com.hashcore.pool.presentation.theme.HashCoreCard
import com.hashcore.pool.presentation.theme.HashCoreError
import com.hashcore.pool.presentation.theme.HashCoreSuccess
import com.hashcore.pool.presentation.theme.HashCoreTextMuted
import com.hashcore.pool.presentation.theme.HashCoreTextSecondary
import com.hashcore.pool.presentation.theme.HashCoreWarning
import com.hashcore.pool.presentation.widget.PoolStatsWidgetProvider
import com.hashcore.pool.util.BtcFormatter
import com.hashcore.pool.util.FeeFormatter
import com.hashcore.pool.util.HashrateFormatter
import com.hashcore.pool.util.TimeFormatter
import kotlinx.coroutines.delay

private val RANGES = listOf("1h", "24h", "7d", "30d")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val range by viewModel.range.collectAsStateWithLifecycle()
    val connection by viewModel.connectionState.collectAsStateWithLifecycle()
    val preferFiat by viewModel.displayFiat.collectAsStateWithLifecycle()
    val alertGroups by viewModel.alertGroups.collectAsStateWithLifecycle()
    val setup by viewModel.setup.collectAsStateWithLifecycle()
    val lastUpdated by viewModel.lastUpdatedEpochMs.collectAsStateWithLifecycle()
    val flashToken by viewModel.hashrateFlashToken.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val mutedUntil by viewModel.mutedUntil.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var showFirstShare by remember { mutableStateOf(false) }
    var flashActive by remember { mutableStateOf(false) }
    var notifAsked by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* preference already stored; system grant is enough */ }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onScreenVisible(true)
                Lifecycle.Event.ON_STOP -> viewModel.onScreenVisible(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onScreenVisible(false)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                HomeEvent.FirstShare -> showFirstShare = true
            }
        }
    }

    LaunchedEffect(flashToken) {
        if (flashToken > 0) {
            flashActive = true
            delay(700)
            flashActive = false
        }
    }

    // Request POST_NOTIFICATIONS once when dashboard is ready (Android 13+)
    LaunchedEffect(ui) {
        if (ui is UiState.Success && !notifAsked && Build.VERSION.SDK_INT >= 33) {
            notifAsked = true
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(ui) {
        if (ui is UiState.Success) {
            PoolStatsWidgetProvider.refreshAll(context)
        }
    }

    when (val state = ui) {
        UiState.Loading -> DashboardSkeleton()
        is UiState.Error -> ErrorState(state.message, onRetry = { viewModel.load() })
        is UiState.Success -> {
            val d = state.data
            val hrColor by animateColorAsState(
                targetValue = if (flashActive) HashCoreAccent else Color.Unspecified,
                animationSpec = tween(280),
                label = "hrFlash",
            )
            val isMuted = mutedUntil > System.currentTimeMillis()

            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                "Dashboard",
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                                fontWeight = FontWeight.Bold,
                            )
                            if (lastUpdated > 0) {
                                Text(
                                    "Updated ${TimeFormatter.relative(lastUpdated)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HashCoreTextMuted,
                                    modifier = Modifier.semantics {
                                        contentDescription = "Last updated ${TimeFormatter.relative(lastUpdated)}"
                                    },
                                )
                            }
                        }
                        LiveBadge(connection)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (!setup.allDone) {
                        HashCoreCard {
                            SectionTitle("Get mining")
                            Text(
                                "Four steps to your first share",
                                style = MaterialTheme.typography.bodySmall,
                                color = HashCoreTextMuted,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SetupStepRow(
                                1,
                                "Register wallet / account",
                                setup.registered,
                                "You're signed in to P2PMine",
                            )
                            SetupStepRow(
                                2,
                                "Create a worker",
                                setup.hasWorker,
                                "Add a worker name on the Workers tab",
                            )
                            SetupStepRow(
                                3,
                                "Point your ASIC",
                                setup.hasShare,
                                "stratum+tcp://p2pmine.com:3333 (or 2525 / 9999)",
                            )
                            SetupStepRow(
                                4,
                                "Set payout address",
                                setup.hasPayoutAddress,
                                "Save your BTC address on the Payouts tab",
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (alertGroups.isNotEmpty()) {
                        HashCoreCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SectionTitle("Alerts")
                                if (isMuted) {
                                    TextButton(onClick = {
                                        viewModel.unmuteAlerts()
                                        Toast.makeText(context, "Alerts unmuted", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Text("Unmute", color = HashCoreAccent)
                                    }
                                } else {
                                    TextButton(onClick = {
                                        viewModel.muteAlerts24h()
                                        Toast.makeText(context, "Muted for 24 hours", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Text("Mute 24h", color = HashCoreTextSecondary)
                                    }
                                }
                            }
                            if (isMuted) {
                                Text(
                                    "Notifications muted until ${TimeFormatter.dateTime(mutedUntil)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HashCoreWarning,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            alertGroups.forEach { group ->
                                Text(
                                    group.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HashCoreTextSecondary,
                                )
                                group.alerts.take(4).forEach { alert ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                alert.message,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = when {
                                                    alert.type.contains("offline", true) -> HashCoreError
                                                    alert.type.contains("reject", true) -> HashCoreWarning
                                                    alert.type.contains("payout", true) -> HashCoreAccent
                                                    else -> HashCoreTextSecondary
                                                },
                                            )
                                            if (alert.createdAtEpochMs != null) {
                                                Text(
                                                    TimeFormatter.relative(alert.createdAtEpochMs),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = HashCoreTextMuted,
                                                )
                                            }
                                        }
                                        TextButton(onClick = { viewModel.dismissAlert(alert.id) }) {
                                            Text("Dismiss", color = HashCoreAccent)
                                        }
                                    }
                                }
                                if (group.alerts.size > 1) {
                                    TextButton(onClick = { viewModel.dismissGroup(group.typeKey) }) {
                                        Text("Dismiss all ${group.typeKey}", color = HashCoreTextMuted)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    HashCoreCard(
                        modifier = Modifier.semantics {
                            contentDescription =
                                "Current hashrate ${HashrateFormatter.format(d.currentHashrateHs)}"
                        },
                    ) {
                        val valueColor = if (flashActive) hrColor else MaterialTheme.colorScheme.onSurface
                        Column {
                            Text(
                                text = "CURRENT HASHRATE",
                                style = MaterialTheme.typography.labelMedium,
                                color = HashCoreTextMuted,
                                letterSpacing = 0.8.sp,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = HashrateFormatter.format(d.currentHashrateHs),
                                style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp),
                                color = valueColor,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Rolling avg ${HashrateFormatter.format(d.avg24hHashrateHs)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HashCoreTextSecondary,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    HashCoreCard {
                        MetricBlock(
                            label = "Unpaid balance",
                            value = BtcFormatter.dual(d.unpaidBalanceBtc, d.btcUsdRate, preferFiat),
                            subtitle = "Min payout ${BtcFormatter.btc(d.minPayoutBtc, compact = true)}",
                            emphasize = true,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "After ${FeeFormatter.percent(d.visibleFeePercent)}% pool fee",
                            style = MaterialTheme.typography.labelLarge,
                            color = HashCoreAccent,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HashCoreCard(modifier = Modifier.weight(1f)) {
                            MetricBlock(
                                label = "Est. today",
                                value = BtcFormatter.btc(d.estEarningsTodayBtc, compact = true),
                                subtitle = BtcFormatter.fiat(d.estEarningsTodayBtc, d.btcUsdRate),
                            )
                        }
                        HashCoreCard(modifier = Modifier.weight(1f)) {
                            MetricBlock(
                                label = "Yesterday",
                                value = BtcFormatter.btc(d.estEarningsYesterdayBtc, compact = true),
                                subtitle = BtcFormatter.fiat(d.estEarningsYesterdayBtc, d.btcUsdRate),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    HashCoreCard {
                        SectionTitle("Hashrate")
                        Text(
                            "Your workers only · long-press for exact values",
                            style = MaterialTheme.typography.bodySmall,
                            color = HashCoreTextMuted,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RANGES.forEach { r ->
                                val selected = range == r
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setRange(r) },
                                    label = { Text(r) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = HashCoreAccent,
                                        selectedLabelColor = HashCoreBlack,
                                        containerColor = HashCoreCard,
                                        labelColor = HashCoreTextSecondary,
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = HashCoreBorder,
                                        selectedBorderColor = HashCoreAccent,
                                    ),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HashrateChart(points = d.hashrateHistory24h)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    HashCoreCard {
                        SectionTitle("Workers")
                        Text(
                            "${d.workers.online} Online  •  ${d.workers.offline} Offline" +
                                if (d.workers.problem > 0) "  •  ${d.workers.problem} Problem" else "",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    HashCoreCard {
                        SectionTitle("Last payout")
                        val p = d.lastPayout
                        if (p == null) {
                            Text("No payouts yet", color = HashCoreTextSecondary)
                        } else {
                            Text(
                                BtcFormatter.dual(p.amountBtc, d.btcUsdRate, preferFiat),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                TimeFormatter.dateTime(p.createdAtEpochMs),
                                color = HashCoreTextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Pool fees: ${FeeFormatter.percent(d.visibleFeePercent)}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = HashCoreAccent,
                        )
                        if (d.payoutSchedule.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(d.payoutSchedule, style = MaterialTheme.typography.bodySmall, color = HashCoreTextMuted)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showFirstShare) {
        AlertDialog(
            onDismissRequest = { showFirstShare = false },
            title = { Text("First share received!") },
            text = {
                Text(
                    "Your miner is submitting work to P2PMine. Watch hashrate update live on this dashboard.",
                )
            },
            confirmButton = {
                TextButton(onClick = { showFirstShare = false }) {
                    Text("Let's go", color = HashCoreAccent)
                }
            },
        )
    }
}

@Composable
private fun LiveBadge(state: LiveConnectionState) {
    val (text, color, pulse) = when (state) {
        LiveConnectionState.CONNECTED -> Triple("LIVE", HashCoreSuccess, true)
        LiveConnectionState.CONNECTING -> Triple("…", HashCoreTextMuted, false)
        LiveConnectionState.POLLING_FALLBACK -> Triple("POLL", HashCoreWarning, true)
        LiveConnectionState.DISCONNECTED -> Triple("OFF", HashCoreTextMuted, false)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { contentDescription = "Connection $text" },
    ) {
        LivePulseDot(active = pulse, color = color)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}
