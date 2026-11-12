package com.hashcore.pool.presentation.screens.account

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hashcore.pool.presentation.components.HashCoreCard
import com.hashcore.pool.presentation.components.LoadingState
import com.hashcore.pool.presentation.components.SectionTitle
import com.hashcore.pool.presentation.theme.HashCoreAccent
import com.hashcore.pool.presentation.theme.HashCoreBlack
import com.hashcore.pool.presentation.theme.HashCoreError
import com.hashcore.pool.presentation.theme.HashCoreSuccess
import com.hashcore.pool.presentation.theme.HashCoreTextMuted
import com.hashcore.pool.presentation.theme.HashCoreTextSecondary

private const val PLAY_GOOGLE_AUTH =
    "https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2"
private const val PLAY_AUTHY =
    "https://play.google.com/store/apps/details?id=com.authy.authy"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoFaScreen(
    onBack: () -> Unit,
    viewModel: TwoFaViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Two-factor auth") },
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

        if (ui.loading && ui.enabled == null) {
            LoadingState()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                HashCoreCard {
                    SectionTitle("Status")
                    Text(
                        if (ui.enabled == true) "2FA is enabled" else "2FA is off",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (ui.enabled == true) HashCoreSuccess else HashCoreTextSecondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Adds an authenticator code when signing in.",
                        style = MaterialTheme.typography.bodySmall,
                        color = HashCoreTextMuted,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                HashCoreCard {
                    SectionTitle("Authenticator apps")
                    Text(
                        "Install one of these free apps, then scan the QR or enter the secret.",
                        style = MaterialTheme.typography.bodySmall,
                        color = HashCoreTextMuted,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { openUrl(context, PLAY_GOOGLE_AUTH) },
                        modifier = Modifier.semantics {
                            contentDescription = "Open Google Authenticator on Play Store"
                        },
                    ) {
                        Text("Get Google Authenticator (Play Store)", color = HashCoreAccent)
                    }
                    TextButton(
                        onClick = { openUrl(context, PLAY_AUTHY) },
                        modifier = Modifier.semantics {
                            contentDescription = "Open Authy on Play Store"
                        },
                    ) {
                        Text("Get Authy (Play Store)", color = HashCoreAccent)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (ui.enabled != true) {
                    HashCoreCard {
                        SectionTitle("Enable 2FA")
                        if (ui.setupSecret == null) {
                            Button(
                                onClick = viewModel::startSetup,
                                enabled = !ui.loading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HashCoreAccent,
                                    contentColor = HashCoreBlack,
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (ui.loading) "Starting…" else "Start setup")
                            }
                        } else {
                            Text(
                                "1. Open your authenticator app\n2. Scan this QR code\n3. Enter the 6-digit code below",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HashCoreTextSecondary,
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Prefer client-generated otpauth QR; fall back to server data URL image
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                when {
                                    ui.setupQrBitmap != null -> {
                                        Image(
                                            bitmap = ui.setupQrBitmap!!.asImageBitmap(),
                                            contentDescription = "2FA QR code — scan with authenticator app",
                                            modifier = Modifier
                                                .size(220.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White)
                                                .padding(12.dp),
                                            contentScale = ContentScale.Fit,
                                        )
                                    }
                                    !ui.setupQrDataUrl.isNullOrBlank() &&
                                        ui.setupQrDataUrl!!.startsWith("data:image") -> {
                                        AsyncImage(
                                            model = ui.setupQrDataUrl,
                                            contentDescription = "2FA QR code — scan with authenticator app",
                                            modifier = Modifier
                                                .size(220.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White)
                                                .padding(8.dp),
                                            contentScale = ContentScale.Fit,
                                        )
                                    }
                                    else -> {
                                        Text(
                                            "QR could not be generated. Use the secret below.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = HashCoreTextMuted,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Or enter this secret manually",
                                style = MaterialTheme.typography.labelMedium,
                                color = HashCoreTextMuted,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                ui.setupSecret!!,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Monospace,
                                color = HashCoreAccent,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("2FA secret", ui.setupSecret))
                                    Toast.makeText(context, "Secret copied", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Copy secret", color = HashCoreAccent)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it.filter { c -> c.isDigit() }.take(8) },
                                label = { Text("6-digit code") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HashCoreAccent,
                                    cursorColor = HashCoreAccent,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.enable(code) },
                                enabled = code.length >= 6 && !ui.loading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HashCoreAccent,
                                    contentColor = HashCoreBlack,
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (ui.loading) "Enabling…" else "Confirm & enable")
                            }
                        }
                    }
                } else {
                    HashCoreCard {
                        SectionTitle("Disable 2FA")
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.filter { c -> c.isDigit() }.take(8) },
                            label = { Text("Authenticator code") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HashCoreAccent,
                                cursorColor = HashCoreAccent,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Account password") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HashCoreAccent,
                                cursorColor = HashCoreAccent,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.disable(code, password) },
                            enabled = code.length >= 6 && password.isNotBlank() && !ui.loading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HashCoreError.copy(alpha = 0.2f),
                                contentColor = HashCoreError,
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (ui.loading) "Disabling…" else "Disable 2FA")
                        }
                    }
                }

                ui.message?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(it, color = HashCoreSuccess)
                }
                ui.error?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(it, color = HashCoreError)
                }
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
        Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
    }
}
