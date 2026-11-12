package com.hashcore.pool.presentation.screens.payouts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hashcore.pool.domain.model.Payout
import com.hashcore.pool.domain.model.PayoutStatus
import com.hashcore.pool.presentation.common.UiState
import com.hashcore.pool.presentation.components.EmptyState
import com.hashcore.pool.presentation.components.ErrorState
import com.hashcore.pool.presentation.components.HashCoreCard
import com.hashcore.pool.presentation.components.LoadingState
import com.hashcore.pool.presentation.components.MetricBlock
import com.hashcore.pool.presentation.components.SectionTitle
import com.hashcore.pool.presentation.theme.HashCoreAccent
import com.hashcore.pool.presentation.theme.HashCoreBlack
import com.hashcore.pool.presentation.theme.HashCoreBorder
import com.hashcore.pool.presentation.theme.HashCoreCard
import com.hashcore.pool.presentation.theme.HashCoreError
import com.hashcore.pool.presentation.theme.HashCoreSuccess
import com.hashcore.pool.presentation.theme.HashCoreTextMuted
import com.hashcore.pool.presentation.theme.HashCoreTextSecondary
import com.hashcore.pool.presentation.theme.HashCoreWarning
import com.hashcore.pool.util.BtcFormatter
import com.hashcore.pool.util.TimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayoutsScreen(viewModel: PayoutsViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val request by viewModel.request.collectAsStateWithLifecycle()
    val preferFiat by viewModel.displayFiat.collectAsStateWithLifecycle()
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Payouts",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )
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
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            HashCoreCard {
                                MetricBlock(
                                    label = "Unpaid balance",
                                    value = BtcFormatter.dual(data.unpaidBalanceBtc, data.btcUsdRate, preferFiat),
                                    emphasize = true,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Minimum payout ${BtcFormatter.btc(data.minPayoutBtc, compact = true)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HashCoreTextSecondary,
                                )
                                if (data.payoutSchedule.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        data.payoutSchedule,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = HashCoreTextMuted,
                                    )
                                }
                                if (!data.btcAddress.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Payout address", style = MaterialTheme.typography.labelMedium, color = HashCoreTextMuted)
                                    Text(
                                        data.btcAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = HashCoreTextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        item {
                            HashCoreCard {
                                SectionTitle("Payout address")
                                Text(
                                    "BTC address where withdrawals are sent",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HashCoreTextMuted,
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                val hasSavedAddress = !data.btcAddress.isNullOrBlank()
                                val showLocked = hasSavedAddress && !request.addressEditing

                                if (showLocked) {
                                    // Stored / locked view — feels “punched in”
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                HashCoreSuccess.copy(alpha = 0.10f),
                                                RoundedCornerShape(12.dp),
                                            )
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = HashCoreSuccess,
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Saved",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = HashCoreSuccess,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                data.btcAddress!!,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = HashCoreTextSecondary,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                    if (request.message != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            request.message!!,
                                            color = HashCoreSuccess,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    if (request.error != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            request.error!!,
                                            color = HashCoreError,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedButton(
                                        onClick = viewModel::startEditAddress,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = HashCoreAccent,
                                        ),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = null,
                                            tint = HashCoreAccent,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Change address", fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    // Edit / first-time entry — blank field forces full re-type (no soft edit)
                                    if (hasSavedAddress) {
                                        Text(
                                            "Type the full new address. The previous one is not pre-filled so a one-letter mistake is harder.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = HashCoreTextMuted,
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                    OutlinedTextField(
                                        value = request.addressText,
                                        onValueChange = viewModel::onAddressChange,
                                        label = { Text("BTC receiving address") },
                                        placeholder = {
                                            Text(
                                                if (hasSavedAddress) "Paste or type the full new address" else "bc1… or 1… / 3…",
                                            )
                                        },
                                        singleLine = true,
                                        enabled = !request.addressSaving,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = HashCoreAccent,
                                            unfocusedBorderColor = HashCoreBorder,
                                            focusedContainerColor = HashCoreCard,
                                            unfocusedContainerColor = HashCoreCard,
                                            cursorColor = HashCoreAccent,
                                            disabledBorderColor = HashCoreBorder,
                                            disabledTextColor = HashCoreTextSecondary,
                                        ),
                                    )
                                    if (request.error != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            request.error!!,
                                            color = HashCoreError,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = viewModel::savePayoutAddress,
                                        enabled = !request.addressSaving,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = HashCoreAccent.copy(alpha = 0.85f),
                                            contentColor = HashCoreBlack,
                                        ),
                                    ) {
                                        if (request.addressSaving) {
                                            CircularProgressIndicator(
                                                color = HashCoreBlack,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.height(22.dp),
                                            )
                                        } else {
                                            Text(
                                                if (hasSavedAddress) "Update address" else "Save payout address",
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                    if (hasSavedAddress) {
                                        TextButton(
                                            onClick = viewModel::cancelEditAddress,
                                            enabled = !request.addressSaving,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text("Cancel", color = HashCoreTextMuted)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            HashCoreCard {
                                SectionTitle("Request payout")
                                Text(
                                    "Leave amount empty to withdraw your full unpaid balance.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HashCoreTextMuted,
                                )
                                if (!data.btcAddress.isNullOrBlank() && !request.addressEditing) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        "Payouts are sent only to your saved address:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = HashCoreTextMuted,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        data.btcAddress,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = HashCoreTextSecondary,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium,
                                    )
                                } else if (request.addressEditing) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Save or cancel the address change before requesting a payout.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = HashCoreWarning,
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = request.amountText,
                                    onValueChange = viewModel::onAmountChange,
                                    label = { Text("Amount BTC (optional)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = HashCoreAccent,
                                        unfocusedBorderColor = HashCoreBorder,
                                        focusedContainerColor = HashCoreCard,
                                        unfocusedContainerColor = HashCoreCard,
                                        cursorColor = HashCoreAccent,
                                    ),
                                )
                                if (request.error != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(request.error!!, color = HashCoreError, style = MaterialTheme.typography.bodySmall)
                                }
                                if (request.message != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(request.message!!, color = HashCoreSuccess, style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = viewModel::requestPayout,
                                    enabled = !request.loading && !request.addressEditing,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HashCoreAccent,
                                        contentColor = HashCoreBlack,
                                    ),
                                ) {
                                    if (request.loading) {
                                        CircularProgressIndicator(
                                            color = HashCoreBlack,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.height(22.dp),
                                        )
                                    } else {
                                        Text("Request payout", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                "Transaction history",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        if (data.payouts.isEmpty()) {
                            item {
                                EmptyState(
                                    title = "No payouts yet",
                                    body = "Requested and completed payouts appear here.",
                                )
                            }
                        } else {
                            items(data.payouts, key = { it.id }) { payout ->
                                PayoutRow(payout, data.btcUsdRate, preferFiat) {
                                    copyTx(context, it)
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }

                if (request.showPayoutConfirm) {
                    val dest = data.btcAddress.orEmpty()
                    val amountLabel = request.confirmAmountBtc?.let {
                        BtcFormatter.dual(it, data.btcUsdRate, preferFiat)
                    } ?: "Full unpaid balance (${BtcFormatter.dual(data.unpaidBalanceBtc, data.btcUsdRate, preferFiat)})"
                    AlertDialog(
                        onDismissRequest = viewModel::dismissPayoutConfirm,
                        title = {
                            Text("Confirm payout", fontWeight = FontWeight.Bold)
                        },
                        text = {
                            Column {
                                Text(
                                    "This payout will be sent to the address below. Double-check every character.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HashCoreTextSecondary,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Destination",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HashCoreTextMuted,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    dest,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HashCoreAccent,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Amount",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HashCoreTextMuted,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    amountLabel,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = viewModel::confirmRequestPayout,
                                enabled = !request.loading,
                            ) {
                                Text(
                                    "Request payout to this address",
                                    color = HashCoreAccent,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = viewModel::dismissPayoutConfirm) {
                                Text("Cancel", color = HashCoreTextMuted)
                            }
                        },
                        containerColor = HashCoreCard,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        textContentColor = HashCoreTextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PayoutRow(
    payout: Payout,
    btcUsd: Double?,
    preferFiat: Boolean,
    onCopy: (String) -> Unit,
) {
    HashCoreCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    BtcFormatter.dual(payout.amountBtc, btcUsd, preferFiat),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    TimeFormatter.dateTime(payout.createdAtEpochMs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = HashCoreTextSecondary,
                )
            }
            Text(
                statusLabel(payout.status),
                style = MaterialTheme.typography.labelLarge,
                color = statusColor(payout.status),
                fontWeight = FontWeight.Bold,
            )
        }
        if (!payout.txId.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("TxID", style = MaterialTheme.typography.labelMedium, color = HashCoreTextMuted)
            Text(
                payout.txId,
                style = MaterialTheme.typography.bodySmall,
                color = HashCoreTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = { onCopy(payout.txId) }) {
                Text("Copy TxID", color = HashCoreAccent)
            }
        }
    }
}

private fun statusLabel(s: PayoutStatus) = when (s) {
    PayoutStatus.PENDING -> "Pending"
    PayoutStatus.SENT -> "Sent"
    PayoutStatus.CONFIRMED -> "Confirmed"
    PayoutStatus.FAILED -> "Failed"
}

@Composable
private fun statusColor(s: PayoutStatus) = when (s) {
    PayoutStatus.PENDING -> HashCoreWarning
    PayoutStatus.SENT -> HashCoreAccent
    PayoutStatus.CONFIRMED -> HashCoreSuccess
    PayoutStatus.FAILED -> HashCoreError
}

private fun copyTx(context: Context, tx: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("txid", tx))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}
