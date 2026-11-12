package com.hashcore.pool.presentation.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hashcore.pool.R
import com.hashcore.pool.domain.repository.RegisterMode
import com.hashcore.pool.presentation.theme.HashCoreAccent
import com.hashcore.pool.presentation.theme.HashCoreBlack
import com.hashcore.pool.presentation.theme.HashCoreBorder
import com.hashcore.pool.presentation.theme.HashCoreCard
import com.hashcore.pool.presentation.theme.HashCoreError
import com.hashcore.pool.presentation.theme.HashCoreTextMuted
import com.hashcore.pool.presentation.theme.HashCoreTextSecondary

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.success) {
        if (state.success) onRegistered()
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = HashCoreAccent,
        unfocusedBorderColor = HashCoreBorder,
        focusedContainerColor = HashCoreCard,
        unfocusedContainerColor = HashCoreCard,
        cursorColor = HashCoreAccent,
        focusedLabelColor = HashCoreAccent,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HashCoreBlack)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.logo_p2pmine),
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Create account",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(
            "Wallet-native or email — your choice",
            style = MaterialTheme.typography.bodyMedium,
            color = HashCoreTextSecondary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.mode == RegisterMode.WALLET,
                onClick = { viewModel.setMode(RegisterMode.WALLET) },
                label = { Text("BTC wallet") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HashCoreAccent,
                    selectedLabelColor = HashCoreBlack,
                ),
            )
            FilterChip(
                selected = state.mode == RegisterMode.EMAIL,
                onClick = { viewModel.setMode(RegisterMode.EMAIL) },
                label = { Text("Email") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HashCoreAccent,
                    selectedLabelColor = HashCoreBlack,
                ),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (state.mode == RegisterMode.WALLET) {
                "Recommended: use your payout wallet as login. No password needed."
            } else {
                "Email + password. You can add a payout address now or later."
            },
            style = MaterialTheme.typography.bodySmall,
            color = HashCoreTextMuted,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (state.mode == RegisterMode.EMAIL) {
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmail,
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPassword,
                label = { Text("Password (min 8 chars)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.btcAddress,
                onValueChange = viewModel::onBtc,
                label = { Text("BTC payout address (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
            )
        } else {
            OutlinedTextField(
                value = state.btcAddress,
                onValueChange = viewModel::onBtc,
                label = { Text("BTC wallet address") },
                singleLine = true,
                placeholder = { Text("bc1… or 1… / 3…") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.displayName,
            onValueChange = viewModel::onDisplayName,
            label = { Text("Display name (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors,
        )

        if (state.error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(state.error!!, color = HashCoreError)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = viewModel::register,
            enabled = !state.loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HashCoreAccent,
                contentColor = HashCoreBlack,
            ),
        ) {
            if (state.loading) {
                CircularProgressIndicator(color = HashCoreBlack, strokeWidth = 2.dp)
            } else {
                Text("Create account", fontWeight = FontWeight.Bold)
            }
        }
        TextButton(
            onClick = onBackToLogin,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text("Already registered? Sign in", color = HashCoreTextSecondary)
        }
    }
}
