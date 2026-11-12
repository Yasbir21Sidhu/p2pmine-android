package com.hashcore.pool.presentation.screens.earnings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hashcore.pool.domain.model.EarningsPeriod
import com.hashcore.pool.presentation.common.UiState
import com.hashcore.pool.presentation.components.EmptyState
import com.hashcore.pool.presentation.components.ErrorState
import com.hashcore.pool.presentation.components.HashCoreCard
import com.hashcore.pool.presentation.components.LoadingState
import com.hashcore.pool.presentation.components.MetricBlock
import com.hashcore.pool.presentation.theme.HashCoreAccent
import com.hashcore.pool.presentation.theme.HashCoreBlack
import com.hashcore.pool.presentation.theme.HashCoreBorder
import com.hashcore.pool.presentation.theme.HashCoreCard
import com.hashcore.pool.presentation.theme.HashCoreTextSecondary
import com.hashcore.pool.util.BtcFormatter
import com.hashcore.pool.util.FeeFormatter
import com.hashcore.pool.util.TimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsScreen(viewModel: EarningsViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    val preferFiat by viewModel.displayFiat.collectAsStateWithLifecycle()
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Earnings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Row {
                val success = ui as? UiState.Success
                IconButton(
                    onClick = {
                        val data = success?.data ?: return@IconButton
                        scope.launch {
                            try {
                                val csv = buildString {
                                    appendLine("date,label,amount_btc")
                                    data.buckets.forEach { b ->
                                        appendLine(
                                            "${TimeFormatter.date(b.startEpochMs)},\"${b.label}\",${b.amountBtc}",
                                        )
                                    }
                                }
                                val file = withContext(Dispatchers.IO) {
                                    val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
                                    File(dir, "p2pmine_earnings_${data.period.name.lowercase()}.csv").apply {
                                        writeText(csv)
                                    }
                                }
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file,
                                )
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "P2PMine earnings export")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(share, "Export earnings"))
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Export failed: ${e.message}",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    },
                    enabled = success != null && success.data.buckets.isNotEmpty(),
                    modifier = Modifier.semantics { contentDescription = "Export earnings CSV" },
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, tint = HashCoreAccent)
                }
                IconButton(onClick = {
                    scope.launch {
                        refreshing = true
                        viewModel.load()
                        refreshing = false
                    }
                }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = HashCoreAccent)
                }
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PeriodChip("Daily", period == EarningsPeriod.DAILY) {
                viewModel.setPeriod(EarningsPeriod.DAILY)
            }
            PeriodChip("Weekly", period == EarningsPeriod.WEEKLY) {
                viewModel.setPeriod(EarningsPeriod.WEEKLY)
            }
            PeriodChip("Monthly", period == EarningsPeriod.MONTHLY) {
                viewModel.setPeriod(EarningsPeriod.MONTHLY)
            }
        }

        when (val state = ui) {
            UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(state.message, onRetry = viewModel::load)
            is UiState.Success -> {
                val data = state.data
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = {
                        scope.launch {
                            refreshing = true
                            viewModel.load()
                            refreshing = false
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (data.buckets.isEmpty()) {
                        EmptyState(
                            title = "No earnings history yet",
                            body = "Once your miners submit shares, earnings will appear here after the pool fee.",
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            item {
                                HashCoreCard {
                                    MetricBlock(
                                        label = "Period total",
                                        value = BtcFormatter.dual(data.totalBtc, data.btcUsdRate, preferFiat),
                                        emphasize = true,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "After ${FeeFormatter.percent(data.visibleFeePercent)}% pool fee",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = HashCoreAccent,
                                    )
                                }
                            }
                            items(data.buckets.reversed()) { bucket ->
                                HashCoreCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column {
                                            Text(
                                                TimeFormatter.date(bucket.startEpochMs),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                bucket.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = HashCoreTextSecondary,
                                            )
                                        }
                                        Text(
                                            BtcFormatter.dual(bucket.amountBtc, data.btcUsdRate, preferFiat),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
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
