package com.hashcore.pool.presentation.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hashcore.pool.presentation.theme.HashCoreAccent
import com.hashcore.pool.presentation.theme.HashCoreBlack
import com.hashcore.pool.presentation.theme.HashCoreBorder
import com.hashcore.pool.presentation.theme.HashCoreTextMuted
import com.hashcore.pool.presentation.theme.HashCoreTextSecondary
import kotlinx.coroutines.launch

private data class Page(
    val step: String,
    val title: String,
    val body: String,
    val icon: ImageVector,
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    // Conversion checklist: Register → Worker → Point ASIC → First share / payout address
    val pages = listOf(
        Page(
            "Step 1 of 4",
            "Register with your wallet",
            "Create a free P2PMine account using your BTC wallet (recommended) or email. Wallet-native login is unique and can double as your payout address.",
            Icons.Outlined.AccountBalanceWallet,
        ),
        Page(
            "Step 2 of 4",
            "Create a worker",
            "Add a worker name in the app or on p2pmine.com. Your stratum username is built from that worker so each machine is tracked separately.",
            Icons.Outlined.Memory,
        ),
        Page(
            "Step 3 of 4",
            "Point your ASIC",
            "Point your miner to stratum+tcp://p2pmine.com on ports 3333, 2525, or 9999. Use the exact worker username from the Workers screen.",
            Icons.Outlined.Cable,
        ),
        Page(
            "Step 4 of 4",
            "See first share & set payout",
            "Within a minute of the first accepted share, live hashrate appears. Set your BTC payout address on the Payouts tab and request withdrawals when ready.",
            Icons.Outlined.Payments,
        ),
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HashCoreBlack)
            .padding(24.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinished) {
                Text("Skip", color = HashCoreTextSecondary)
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { page ->
            val p = pages[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(p.step, style = MaterialTheme.typography.labelLarge, color = HashCoreAccent)
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(HashCoreAccent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(p.icon, contentDescription = null, tint = HashCoreAccent, modifier = Modifier.size(44.dp))
                }
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    p.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    p.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = HashCoreTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(modifier = Modifier.height(20.dp))
                // Checklist preview of all 4 steps
                pages.forEachIndexed { i, step ->
                    val done = i < page
                    val current = i == page
                    Text(
                        "${if (done) "✓" else if (current) "●" else "○"}  ${step.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            done -> HashCoreAccent
                            current -> MaterialTheme.colorScheme.onSurface
                            else -> HashCoreTextMuted
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp, horizontal = 12.dp),
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            pages.indices.forEach { i ->
                val selected = pagerState.currentPage == i
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (selected) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (selected) HashCoreAccent else HashCoreBorder),
                )
            }
        }
        val last = pagerState.currentPage == pages.lastIndex
        Button(
            onClick = {
                if (last) onFinished()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HashCoreAccent,
                contentColor = HashCoreBlack,
            ),
        ) {
            Text(
                if (last) "Get started" else "Next step",
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
