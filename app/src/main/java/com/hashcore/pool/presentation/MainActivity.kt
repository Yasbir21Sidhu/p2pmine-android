package com.hashcore.pool.presentation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hashcore.pool.presentation.navigation.HashCoreNavHost
import com.hashcore.pool.presentation.navigation.SessionViewModel
import com.hashcore.pool.presentation.theme.HashCoreBlack
import com.hashcore.pool.presentation.theme.HashCoreTheme
import com.hashcore.pool.presentation.theme.HashCoreTrueBlack
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val sessionVm: SessionViewModel = hiltViewModel()
            val amoled by sessionVm.amoledBlack.collectAsStateWithLifecycle(initialValue = false)

            // Biometric timeout: re-lock after background threshold
            androidx.compose.runtime.DisposableEffect(sessionVm) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> sessionVm.onAppBackgrounded()
                        Lifecycle.Event.ON_START -> sessionVm.onAppForegrounded()
                        else -> Unit
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }

            HashCoreTheme(amoledBlack = amoled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (amoled) HashCoreTrueBlack else HashCoreBlack,
                ) {
                    HashCoreNavHost(sessionViewModel = sessionVm)
                }
            }
        }
    }
}
