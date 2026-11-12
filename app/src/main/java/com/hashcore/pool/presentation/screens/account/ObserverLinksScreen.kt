package com.hashcore.pool.presentation.screens.account

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hashcore.pool.domain.repository.ObserverLink
import com.hashcore.pool.presentation.components.EmptyState
import com.hashcore.pool.presentation.components.ErrorState
import com.hashcore.pool.presentation.components.HashCoreCard
import com.hashcore.pool.presentation.components.LoadingState
import com.hashcore.pool.presentation.common.UiState
import com.hashcore.pool.presentation.theme.HashCoreAccent
import com.hashcore.pool.presentation.theme.HashCoreBlack
import com.hashcore.pool.presentation.theme.HashCoreError
import com.hashcore.pool.presentation.theme.HashCoreTextMuted
import com.hashcore.pool.presentation.theme.HashCoreTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObserverLinksScreen(
    onBack: () -> Unit,
    viewModel: ObserverLinksViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val creating by viewModel.creating.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var label by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Observer links") },
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

        feedback?.let {
            Text(
                it,
                color = if (it.startsWith("Error")) HashCoreError else HashCoreAccent,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        when (val state = ui) {
            UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(state.message, onRetry = viewModel::load)
            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        HashCoreCard {
                            Text(
                                "Share read-only dashboard access without your password.",
                                style = MaterialTheme.typography.bodySmall,
                                color = HashCoreTextMuted,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = label,
                                onValueChange = { label = it.take(40) },
                                label = { Text("Label (optional)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HashCoreAccent,
                                    cursorColor = HashCoreAccent,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    viewModel.create(label.ifBlank { null })
                                    label = ""
                                },
                                enabled = !creating,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HashCoreAccent,
                                    contentColor = HashCoreBlack,
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (creating) "Creating…" else "Create link")
                            }
                        }
                    }
                    if (state.data.isEmpty()) {
                        item {
                            EmptyState(
                                title = "No observer links",
                                body = "Create a link to share live stats with a partner or co-op.",
                            )
                        }
                    } else {
                        items(state.data, key = { it.id }) { link ->
                            ObserverLinkRow(
                                link = link,
                                onCopy = {
                                    val url = link.url ?: return@ObserverLinkRow
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Observer link", url))
                                    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                                },
                                onOpen = {
                                    link.url?.let { u ->
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
                                    }
                                },
                                onDelete = { viewModel.delete(link.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ObserverLinkRow(
    link: ObserverLink,
    onCopy: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    HashCoreCard {
        Text(
            link.label?.takeIf { it.isNotBlank() } ?: "Observer link",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            link.url ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = HashCoreTextSecondary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCopy, enabled = !link.url.isNullOrBlank()) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = HashCoreAccent)
            }
            IconButton(onClick = onOpen, enabled = !link.url.isNullOrBlank()) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "Open", tint = HashCoreAccent)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = HashCoreError)
            }
        }
    }
}
