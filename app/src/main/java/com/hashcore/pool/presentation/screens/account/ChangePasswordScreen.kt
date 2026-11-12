package com.hashcore.pool.presentation.screens.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hashcore.pool.presentation.theme.HashCoreAccent
import com.hashcore.pool.presentation.theme.HashCoreBlack
import com.hashcore.pool.presentation.theme.HashCoreBorder
import com.hashcore.pool.presentation.theme.HashCoreCard
import com.hashcore.pool.presentation.theme.HashCoreError
import com.hashcore.pool.presentation.theme.HashCoreSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var current by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    LaunchedEffect(ui.passwordChanged) {
        if (ui.passwordChanged) {
            viewModel.clearMessages()
            onBack()
        }
    }

    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = HashCoreAccent,
        unfocusedBorderColor = HashCoreBorder,
        focusedContainerColor = HashCoreCard,
        unfocusedContainerColor = HashCoreCard,
        cursorColor = HashCoreAccent,
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Change password") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = HashCoreBlack),
        )
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = current,
                onValueChange = { current = it },
                label = { Text("Current password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = colors,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = newPass,
                onValueChange = { newPass = it },
                label = { Text("New password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = colors,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it },
                label = { Text("Confirm password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = colors,
            )
            if (ui.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(ui.error!!, color = HashCoreError)
            }
            if (ui.message != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(ui.message!!, color = HashCoreSuccess)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.changePassword(current, newPass, confirm) },
                enabled = !ui.passwordChanging,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HashCoreAccent,
                    contentColor = HashCoreBlack,
                ),
            ) {
                if (ui.passwordChanging) {
                    CircularProgressIndicator(color = HashCoreBlack, strokeWidth = 2.dp)
                } else {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
