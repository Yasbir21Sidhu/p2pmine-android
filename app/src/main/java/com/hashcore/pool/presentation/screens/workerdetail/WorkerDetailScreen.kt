package com.hashcore.pool.presentation.screens.workerdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hashcore.pool.presentation.common.UiState
import com.hashcore.pool.presentation.components.ErrorState
import com.hashcore.pool.presentation.components.HashCoreCard
import com.hashcore.pool.presentation.components.HashrateChart
import com.hashcore.pool.presentation.components.LoadingState
import com.hashcore.pool.presentation.components.MetricBlock
import com.hashcore.pool.presentation.components.SectionTitle
import com.hashcore.pool.presentation.components.StatusChip
import com.hashcore.pool.presentation.theme.HashCoreBlack
import com.hashcore.pool.presentation.theme.HashCoreTextMuted
import com.hashcore.pool.presentation.theme.HashCoreTextSecondary
import com.hashcore.pool.util.HashrateFormatter
import com.hashcore.pool.util.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDetailScreen(
    onBack: () -> Unit,
    viewModel: WorkerDetailViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val chartNote by viewModel.chartNote.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Worker detail") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = HashCoreBlack,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
        when (val state = ui) {
            UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(state.message, onRetry = viewModel::load)
            is UiState.Success -> {
                val w = state.data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Row {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                w.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            StatusChip(w.status)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HashCoreCard {
                        MetricBlock(
                            label = "Current hashrate",
                            value = HashrateFormatter.format(w.currentHashrateHs),
                            subtitle = "24h avg ${HashrateFormatter.format(w.avg24hHashrateHs)}",
                            emphasize = true,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HashCoreCard {
                        SectionTitle("Hashrate history")
                        if (!chartNote.isNullOrBlank()) {
                            Text(
                                chartNote!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = HashCoreTextMuted,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (w.hashrateHistory.size >= 2) {
                            HashrateChart(points = w.hashrateHistory)
                        } else {
                            Text(
                                "No hashrate history for “${w.name}” yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HashCoreTextSecondary,
                            )
                            Text(
                                "This chart is per-worker (not account-wide). It fills when the pool has sample points for this worker.",
                                style = MaterialTheme.typography.bodySmall,
                                color = HashCoreTextMuted,
                            )
                        }
                    }
                    if (!w.stratumUser.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HashCoreCard {
                            SectionTitle("Stratum username")
                            Text(
                                w.stratumUser!!,
                                style = MaterialTheme.typography.bodyLarge,
                                color = HashCoreTextSecondary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HashCoreCard {
                        MetricBlock(label = "Accepted shares", value = "%,d".format(w.acceptedShares))
                        Spacer(modifier = Modifier.height(12.dp))
                        MetricBlock(label = "Rejected shares", value = "%,d".format(w.rejectedShares))
                        Spacer(modifier = Modifier.height(12.dp))
                        MetricBlock(label = "Uptime", value = TimeFormatter.duration(w.uptimeSeconds))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Last share ${TimeFormatter.relative(w.lastShareEpochMs)} · ${TimeFormatter.dateTime(w.lastShareEpochMs)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HashCoreTextSecondary,
                        )
                    }
                }
            }
        }
    }
}
