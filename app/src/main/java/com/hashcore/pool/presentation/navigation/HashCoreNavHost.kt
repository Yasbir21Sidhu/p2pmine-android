package com.hashcore.pool.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hashcore.pool.BuildConfig
import com.hashcore.pool.presentation.components.DemoBanner
import com.hashcore.pool.presentation.screens.account.AccountScreen
import com.hashcore.pool.presentation.screens.account.ChangePasswordScreen
import com.hashcore.pool.presentation.screens.account.ObserverLinksScreen
import com.hashcore.pool.presentation.screens.account.TwoFaScreen
import com.hashcore.pool.presentation.screens.auth.BiometricLockScreen
import com.hashcore.pool.presentation.screens.auth.LoginScreen
import com.hashcore.pool.presentation.screens.auth.RegisterScreen
import com.hashcore.pool.presentation.screens.earnings.EarningsScreen
import com.hashcore.pool.presentation.screens.home.HomeScreen
import com.hashcore.pool.presentation.screens.onboarding.OnboardingScreen
import com.hashcore.pool.presentation.screens.payouts.PayoutsScreen
import com.hashcore.pool.presentation.screens.workerdetail.WorkerDetailScreen
import com.hashcore.pool.presentation.screens.workers.WorkersScreen
import com.hashcore.pool.presentation.theme.HashCoreAccent
import com.hashcore.pool.presentation.theme.HashCoreSurface
import com.hashcore.pool.presentation.theme.HashCoreTextMuted

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun HashCoreNavHost(
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val session by sessionViewModel.session.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    when (val s = session) {
        SessionState.Loading -> {
            // blank while reading prefs
        }
        is SessionState.Ready -> {
            val start = when {
                !s.onboardingDone -> Routes.ONBOARDING
                !s.loggedIn -> Routes.LOGIN
                s.needsBiometric -> Routes.BIOMETRIC_LOCK
                else -> Routes.MAIN
            }

            // Re-lock: navigate to biometric when session flips needsBiometric while on main
            LaunchedEffect(s.needsBiometric, s.loggedIn) {
                val route = navController.currentDestination?.route
                if (s.loggedIn && s.needsBiometric && route == Routes.MAIN) {
                    navController.navigate(Routes.BIOMETRIC_LOCK) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = start,
            ) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(
                        onFinished = {
                            sessionViewModel.completeOnboarding()
                            navController.navigate(if (s.loggedIn) Routes.MAIN else Routes.LOGIN) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Routes.LOGIN) {
                    LoginScreen(
                        onLoggedIn = {
                            sessionViewModel.refreshSession()
                            navController.navigate(Routes.MAIN) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        },
                        onCreateAccount = {
                            navController.navigate(Routes.REGISTER)
                        },
                    )
                }
                composable(Routes.REGISTER) {
                    RegisterScreen(
                        onRegistered = {
                            sessionViewModel.refreshSession()
                            navController.navigate(Routes.MAIN) {
                                popUpTo(Routes.ONBOARDING) { inclusive = false }
                                popUpTo(Routes.LOGIN) { inclusive = true }
                                popUpTo(Routes.REGISTER) { inclusive = true }
                            }
                        },
                        onBackToLogin = { navController.popBackStack() },
                    )
                }
                composable(Routes.BIOMETRIC_LOCK) {
                    BiometricLockScreen(
                        onUnlocked = {
                            sessionViewModel.markUnlocked()
                            navController.navigate(Routes.MAIN) {
                                popUpTo(Routes.BIOMETRIC_LOCK) { inclusive = true }
                            }
                        },
                        onUsePassword = {
                            sessionViewModel.logoutLocal()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Routes.MAIN) {
                    MainTabs(
                        onLogout = {
                            sessionViewModel.refreshSession()
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                    )
                }
            }

            // React to logout from deep inside main
            LaunchedEffect(s.loggedIn) {
                if (!s.loggedIn && navController.currentDestination?.route == Routes.MAIN) {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainTabs(onLogout: () -> Unit) {
    val tabs = listOf(
        Tab(Routes.HOME, "Home", Icons.Outlined.Home),
        Tab(Routes.WORKERS, "Workers", Icons.Outlined.Memory),
        Tab(Routes.EARNINGS, "Earnings", Icons.AutoMirrored.Outlined.TrendingUp),
        Tab(Routes.PAYOUTS, "Payouts", Icons.Outlined.Payments),
        Tab(Routes.ACCOUNT, "Account", Icons.Outlined.AccountCircle),
    )
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in tabs.map { it.route }

    Scaffold(
        containerColor = MaterialThemeBackground(),
        topBar = {
            if (BuildConfig.DEMO_MODE) {
                DemoBanner(true)
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = HashCoreSurface,
                    modifier = Modifier.semantics { contentDescription = "Main navigation" },
                ) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HashCoreAccent,
                                selectedTextColor = HashCoreAccent,
                                unselectedIconColor = HashCoreTextMuted,
                                unselectedTextColor = HashCoreTextMuted,
                                indicatorColor = HashCoreAccent.copy(alpha = 0.12f),
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) { HomeScreen() }
            composable(Routes.WORKERS) {
                WorkersScreen(onWorkerClick = { id ->
                    navController.navigate(Routes.workerDetail(id))
                })
            }
            composable(
                route = Routes.WORKER_DETAIL,
                arguments = listOf(navArgument("workerId") { type = NavType.StringType }),
            ) {
                WorkerDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.EARNINGS) { EarningsScreen() }
            composable(Routes.PAYOUTS) { PayoutsScreen() }
            composable(Routes.ACCOUNT) {
                AccountScreen(
                    onLoggedOut = onLogout,
                    onChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
                    onTwoFa = { navController.navigate(Routes.TWO_FA) },
                    onObserverLinks = { navController.navigate(Routes.OBSERVER_LINKS) },
                )
            }
            composable(Routes.CHANGE_PASSWORD) {
                ChangePasswordScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.TWO_FA) {
                TwoFaScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.OBSERVER_LINKS) {
                ObserverLinksScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun MaterialThemeBackground() = androidx.compose.material3.MaterialTheme.colorScheme.background
