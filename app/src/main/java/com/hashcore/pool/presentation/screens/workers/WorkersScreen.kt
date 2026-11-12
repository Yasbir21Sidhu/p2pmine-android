package com.hashcore.pool.presentation.screens.workers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hashcore.pool.domain.model.Worker
import com.hashcore.pool.domain.model.WorkerStatus
import com.hashcore.pool.presentation.common.UiState
import com.hashcore.pool.presentation.components.EmptyState
import com.hashcore.pool.presentation.components.ErrorState
import com.hashcore.pool.presentation.components.HashCoreCard
import com.hashcore.pool.presentation.components.LoadingState
import com.hashcore.pool.presentation.components.StatusChip
import com.hashcore.pool.presentation.theme.HashCoreAccent
import com.hashcore.pool.presentation.theme.HashCoreBlack
import com.hashcore.pool.presentation.theme.HashCoreError
import com.hashcore.pool.presentation.theme.HashCoreTextMuted
import com.hashcore.pool.presentation.theme.HashCoreTextSecondary
import com.hashcore.pool.util.HashrateFormatter
import com.hashcore.pool.util.TimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkersScreen(
    onWorkerClick: (String) -> Unit,
    viewModel: WorkersViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var showCreate by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Worker?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                is WorkersEvent.Copied -> {
                    Toast.makeText(context, "${event.label} copied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = HashCoreBlack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreate = true },
                containerColor = HashCoreAccent,
                contentColor = HashCoreBlack,
                modifier = Modifier.semantics { contentDescription = "Add worker" },
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Text(
                "Workers",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            )

            when (val state = ui) {
                UiState.Loading -> LoadingState()
                is UiState.Error -> ErrorState(state.message, onRetry = viewModel::load)
                is UiState.Success -> {
                    val data = state.data
                    LaunchedEffect(data.actionMessage, data.actionError) {
                        data.actionMessage?.let {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            viewModel.clearActionFeedback()
                        }
                        data.actionError?.let {
                            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                            viewModel.clearActionFeedback()
                        }
                    }

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
                        if (data.workers.isEmpty()) {
                            Column {
                                StratumHelpCard(
                                    host = viewModel.stratumHost(data),
                                    ports = viewModel.stratumPorts(data),
                                    onCopyUrl = {
                                        copyText(
                                            context,
                                            viewModel.stratumUrl(data),
                                            "Stratum URL",
                                            viewModel::notifyCopied,
                                        )
                                    },
                                )
                                EmptyState(
                                    title = "No workers yet",
                                    body = "Tap + to create a worker name, then point your ASIC using the stratum details above.",
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                item {
                                    StratumHelpCard(
                                        host = viewModel.stratumHost(data),
                                        ports = viewModel.stratumPorts(data),
                                        onCopyUrl = {
                                            copyText(
                                                context,
                                                viewModel.stratumUrl(data),
                                                "Stratum URL",
                                                viewModel::notifyCopied,
                                            )
                                        },
                                    )
                                }
                                items(data.workers, key = { it.id }) { worker ->
                                    WorkerRow(
                                        worker = worker,
                                        stratumUser = viewModel.stratumUsername(worker, data),
                                        stratumUrl = viewModel.stratumUrl(data),
                                        fullConfig = viewModel.fullConfigBlock(worker, data),
                                        deleting = data.deletingId == worker.id,
                                        onClick = { onWorkerClick(worker.id) },
                                        onCopy = { text, label ->
                                            copyText(context, text, label, viewModel::notifyCopied)
                                        },
                                        onDelete = { deleteTarget = worker },
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(72.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateWorkerDialog(
            busy = (ui as? UiState.Success)?.data?.creating == true,
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                viewModel.createWorker(name)
                showCreate = false
            },
        )
    }

    deleteTarget?.let { worker ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Disable worker?") },
            text = {
                Text(
                    "\"${worker.name}\" will be disabled on the pool (same as the website). " +
                        "It stays in your list as Disabled. Point a miner at this name again after re-creating if you need it active.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.disableWorker(worker.id)
                        deleteTarget = null
                    },
                ) {
                    Text("Disable", color = HashCoreError)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = HashCoreTextSecondary)
                }
            },
        )
    }
}

@Composable
private fun StratumHelpCard(
    host: String,
    ports: List<String>,
    onCopyUrl: () -> Unit,
) {
    HashCoreCard {
        Text(
            "Stratum endpoint",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "stratum+tcp://$host:${ports.firstOrNull() ?: "3333"}",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = HashCoreAccent,
        )
        Text(
            "Ports: ${ports.joinToString(", ")}  ·  Password: x",
            style = MaterialTheme.typography.bodySmall,
            color = HashCoreTextMuted,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onCopyUrl,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.semantics { contentDescription = "Copy stratum URL" },
        ) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy URL", color = HashCoreAccent)
        }
    }
}

@Composable
private fun WorkerRow(
    worker: Worker,
    stratumUser: String,
    stratumUrl: String,
    fullConfig: String,
    deleting: Boolean,
    onClick: () -> Unit,
    onCopy: (text: String, label: String) -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember(worker.id) { mutableStateOf(false) }

    HashCoreCard(
        modifier = Modifier.semantics {
            contentDescription = "Worker ${worker.name}, ${worker.status.name.lowercase()}"
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    worker.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    HashrateFormatter.format(worker.currentHashrateHs),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "24h avg ${HashrateFormatter.format(worker.avg24hHashrateHs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HashCoreTextSecondary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Last share ${TimeFormatter.relative(worker.lastShareEpochMs)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = HashCoreTextMuted,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(worker.status)
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Open details",
                    tint = HashCoreTextMuted,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.semantics {
                    contentDescription = if (expanded) "Hide stratum config" else "Show stratum config"
                },
            ) {
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = HashCoreAccent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (expanded) "Hide setup" else "Stratum setup",
                    color = HashCoreAccent,
                )
            }
            if (worker.status != WorkerStatus.DISABLED && worker.isActive) {
                IconButton(
                    onClick = onDelete,
                    enabled = !deleting,
                    modifier = Modifier.semantics {
                        contentDescription = "Disable worker ${worker.name}"
                    },
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = HashCoreError.copy(alpha = 0.85f),
                    )
                }
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("URL", style = MaterialTheme.typography.labelMedium, color = HashCoreTextMuted)
            CopyableLine(stratumUrl) { onCopy(stratumUrl, "URL") }
            Spacer(modifier = Modifier.height(6.dp))
            Text("Username", style = MaterialTheme.typography.labelMedium, color = HashCoreTextMuted)
            CopyableLine(stratumUser) { onCopy(stratumUser, "Username") }
            Spacer(modifier = Modifier.height(6.dp))
            Text("Password", style = MaterialTheme.typography.labelMedium, color = HashCoreTextMuted)
            CopyableLine("x") { onCopy("x", "Password") }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { onCopy(fullConfig, "Full config") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HashCoreAccent,
                    contentColor = HashCoreBlack,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Copy full stratum config" },
            ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy all for ASIC", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CopyableLine(value: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = HashCoreAccent,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Outlined.ContentCopy,
            contentDescription = "Copy",
            tint = HashCoreTextMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun CreateWorkerDialog(
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Create worker") },
        text = {
            Column {
                Text(
                    "Choose a short name for this machine (letters, numbers, dashes).",
                    style = MaterialTheme.typography.bodySmall,
                    color = HashCoreTextMuted,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.filter { c -> c.isLetterOrDigit() || c == '-' || c == '_' }.take(32) },
                    singleLine = true,
                    label = { Text("Worker name") },
                    placeholder = { Text("rig-01") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HashCoreAccent,
                        cursorColor = HashCoreAccent,
                        focusedLabelColor = HashCoreAccent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.trim().length >= 2 && !busy,
            ) {
                Text(if (busy) "Creating…" else "Create", color = HashCoreAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("Cancel", color = HashCoreTextSecondary)
            }
        },
    )
}

private fun copyText(
    context: Context,
    text: String,
    label: String,
    onCopied: (String) -> Unit,
) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    onCopied(label)
}
