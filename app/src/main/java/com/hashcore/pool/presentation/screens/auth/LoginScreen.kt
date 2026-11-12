package com.hashcore.pool.presentation.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hashcore.pool.R
import com.hashcore.pool.presentation.components.AccentGradientBar
import com.hashcore.pool.presentation.theme.HashCoreAccent
import com.hashcore.pool.presentation.theme.HashCoreBlack
import com.hashcore.pool.presentation.theme.HashCoreBorder
import com.hashcore.pool.presentation.theme.HashCoreCard
import com.hashcore.pool.presentation.theme.HashCoreError
import com.hashcore.pool.presentation.theme.HashCoreTextMuted
import com.hashcore.pool.presentation.theme.HashCoreTextSecondary

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onCreateAccount: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(state.success) {
        if (state.success) onLoggedIn()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HashCoreBlack)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_p2pmine),
            contentDescription = "P2PMine",
            modifier = Modifier
                .size(88.dp)
                .align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "P2PMine",
            style = MaterialTheme.typography.displayMedium,
            color = HashCoreAccent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(
            text = "Mine Peer to Peer",
            style = MaterialTheme.typography.bodyMedium,
            color = HashCoreTextSecondary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(8.dp))
        AccentGradientBar()
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = if (state.needs2fa) "Two-factor authentication" else "Welcome back",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = if (state.needs2fa) {
                "Enter the code from your authenticator app"
            } else {
                "Sign in with email or BTC wallet"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = HashCoreTextMuted,
        )
        Spacer(modifier = Modifier.height(20.dp))

        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = HashCoreAccent,
            unfocusedBorderColor = HashCoreBorder,
            focusedContainerColor = HashCoreCard,
            unfocusedContainerColor = HashCoreCard,
            cursorColor = HashCoreAccent,
            focusedLabelColor = HashCoreAccent,
        )

        if (!state.needs2fa) {
            OutlinedTextField(
                value = state.identifier,
                onValueChange = viewModel::onIdentifierChange,
                label = { Text("Email or wallet") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password (if set)") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = HashCoreTextMuted,
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.login() }),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
            )
        } else {
            OutlinedTextField(
                value = state.twoFaCode,
                onValueChange = viewModel::onTwoFaChange,
                label = { Text("Authenticator code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.verify2fa() }),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
            )
        }

        if (state.error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(state.error!!, color = HashCoreError, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { if (state.needs2fa) viewModel.verify2fa() else viewModel.login() },
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
                CircularProgressIndicator(
                    modifier = Modifier.height(22.dp),
                    color = HashCoreBlack,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    if (state.needs2fa) "Verify" else "Sign in",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(
            onClick = onCreateAccount,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text("Create account", color = HashCoreAccent, fontWeight = FontWeight.SemiBold)
        }
        Text(
            text = "Wallet-native pool · p2pmine.com",
            style = MaterialTheme.typography.labelMedium,
            color = HashCoreTextMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}
