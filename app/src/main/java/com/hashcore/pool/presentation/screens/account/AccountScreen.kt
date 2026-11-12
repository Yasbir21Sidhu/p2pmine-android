package com.hashcore.pool.presentation.screens.account

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hashcore.pool.BuildConfig
import com.hashcore.pool.presentation.components.HashCoreCard
import com.hashcore.pool.presentation.components.SectionTitle
import com.hashcore.pool.presentation.theme.HashCoreAccent
import com.hashcore.pool.presentation.theme.HashCoreBlack
import com.hashcore.pool.presentation.theme.HashCoreBorder
import com.hashcore.pool.presentation.theme.HashCoreCard
import com.hashcore.pool.presentation.theme.HashCoreError
import com.hashcore.pool.presentation.theme.HashCoreSuccess
import com.hashcore.pool.presentation.theme.HashCoreTextMuted
import com.hashcore.pool.presentation.theme.HashCoreTextSecondary
import com.hashcore.pool.util.FeeFormatter
import com.hashcore.pool.util.TimeFormatter

private val BIOMETRIC_TIMEOUTS = listOf(0, 1, 5, 15, 30, 60)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountScreen(
    onLoggedOut: () -> Unit,
    onChangePassword: () -> Unit,
    onTwoFa: () -> Unit,
    onObserverLinks: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val displayFiat by viewModel.displayFiat.collectAsStateWithLifecycle()
    val fiatCurrency by viewModel.fiatCurrency.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val biometric by viewModel.biometric.collectAsStateWithLifecycle()
    val biometricTimeout by viewModel.biometricTimeout.collectAsStateWithLifecycle()
    val amoled by viewModel.amoledBlack.collectAsStateWithLifecycle()
    val mutedUntil by viewModel.mutedUntil.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isMuted = mutedUntil > System.currentTimeMillis()

    LaunchedEffect(ui.loggedOut) {
        if (ui.loggedOut) onLoggedOut()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            "Account",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))

        HashCoreCard {
            SectionTitle("Profile")
            val user = ui.user
            Text(
                user?.displayName ?: user?.email ?: "—",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (user?.email != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(user.email, color = HashCoreTextSecondary)
            }
            if (!user?.walletAddress.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Payout wallet", style = MaterialTheme.typography.labelMedium, color = HashCoreTextMuted)
                Text(
                    user!!.walletAddress!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = HashCoreTextSecondary,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        HashCoreCard {
            SectionTitle("Pool fees")
            val fee = ui.settings?.visibleFeePercent
            Text(
                text = if (fee != null) {
                    "${FeeFormatter.percent(fee)}% pool fees"
                } else {
                    "—"
                },
                style = MaterialTheme.typography.titleMedium,
                color = HashCoreAccent,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "All earnings in the app are shown after this fee.",
                style = MaterialTheme.typography.bodySmall,
                color = HashCoreTextMuted,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        HashCoreCard {
            SectionTitle("Preferences")
            PrefRow(
                "Show fiat",
                "Display estimated value next to BTC amounts",
                displayFiat,
                viewModel::setDisplayFiat,
            )
            if (displayFiat) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Fiat currency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Used across dashboard, earnings, and payouts",
                    style = MaterialTheme.typography.bodySmall,
                    color = HashCoreTextMuted,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    viewModel.availableFiats().forEach { fiat ->
                        val selected = fiatCurrency.equals(fiat.code, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setFiatCurrency(fiat.code) },
                            label = {
                                Text("${fiat.code} (${fiat.symbol})")
                            },
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
                if (ui.fiatSaving) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Saving…", color = HashCoreTextMuted, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            PrefRow(
                "Notifications",
                "Local pool alerts (worker offline, payouts)",
                notifications,
                viewModel::setNotifications,
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isMuted) {
                Text(
                    "Muted until ${TimeFormatter.dateTime(mutedUntil)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = HashCoreTextMuted,
                )
                TextButton(onClick = viewModel::unmuteAlerts) {
                    Text("Unmute alerts", color = HashCoreAccent)
                }
            } else {
                TextButton(onClick = viewModel::muteAlerts24h) {
                    Text("Mute alerts 24h", color = HashCoreTextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            PrefRow(
                "Biometric lock",
                "Require fingerprint / face to open",
                biometric,
                viewModel::setBiometric,
            )
            if (biometric) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Lock after background",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "0 = lock every time you leave the app",
                    style = MaterialTheme.typography.bodySmall,
                    color = HashCoreTextMuted,
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BIOMETRIC_TIMEOUTS.forEach { min ->
                        val selected = biometricTimeout == min
                        val label = when (min) {
                            0 -> "Always"
                            1 -> "1 min"
                            else -> "$min min"
                        }
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setBiometricTimeout(min) },
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
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            PrefRow(
                "AMOLED black",
                "True black backgrounds for OLED screens",
                amoled,
                viewModel::setAmoledBlack,
            )
            if (ui.message != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(ui.message!!, color = HashCoreSuccess, style = MaterialTheme.typography.bodySmall)
            }
            if (ui.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(ui.error!!, color = HashCoreError, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        HashCoreCard {
            SectionTitle("Security")
            OutlinedButton(
                onClick = onChangePassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Change password" },
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Change password", color = HashCoreAccent)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onTwoFa,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Two-factor authentication" },
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Two-factor authentication", color = HashCoreAccent)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onObserverLinks,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Observer links" },
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Observer links (read-only share)", color = HashCoreAccent)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        HashCoreCard {
            SectionTitle("Legal & support")
            TextButton(onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_POLICY_URL)),
                )
            }) {
                Text("Privacy policy", color = HashCoreAccent)
            }
            val supportUrl = ui.settings?.supportUrl
            val supportEmail = ui.settings?.supportEmail
            if (!supportUrl.isNullOrBlank()) {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(supportUrl)))
                }) {
                    Text("Help center / p2pmine.com", color = HashCoreAccent)
                }
            }
            if (!supportEmail.isNullOrBlank()) {
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$supportEmail")),
                    )
                }) {
                    Text(supportEmail, color = HashCoreAccent)
                }
            }
            val wa = ui.settings?.supportWhatsapp
            if (!wa.isNullOrBlank()) {
                TextButton(onClick = {
                    val digits = wa.filter { it.isDigit() }
                    val uri = Uri.parse("https://wa.me/$digits")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }) {
                    Text("WhatsApp support", color = HashCoreAccent)
                }
            }
            if (supportUrl.isNullOrBlank() && supportEmail.isNullOrBlank() && wa.isNullOrBlank()) {
                Text("Contact admin@p2pmine.com for support.", color = HashCoreTextSecondary)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = viewModel::logout,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .semantics { contentDescription = "Log out" },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HashCoreError.copy(alpha = 0.15f),
                contentColor = HashCoreError,
            ),
        ) {
            Text("Log out", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PrefRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$title, ${if (checked) "on" else "off"}" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = HashCoreTextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = HashCoreBlack,
                checkedTrackColor = HashCoreAccent,
            ),
        )
    }
}
