package com.example.firenotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.firenotes.data.local.dao.OcorrenciaDao
import com.example.firenotes.ui.navigation.Screen
import com.example.firenotes.ui.screens.home.HomeScreen
import com.example.firenotes.ui.screens.home.HomeViewModel
import com.example.firenotes.ui.screens.occurrence.OccurrenceFormScreen
import com.example.firenotes.ui.screens.occurrence.OccurrenceFormViewModel
import com.example.firenotes.ui.screens.occurrence.PersonIdentificationScreen
import com.example.firenotes.ui.screens.occurrence.PersonIdentificationViewModel
import com.example.firenotes.ui.screens.settings.SettingsScreen
import com.example.firenotes.ui.screens.settings.SettingsViewModel
import com.example.firenotes.ui.screens.reports.ReportsScreen
import com.example.firenotes.ui.screens.reports.ReportsViewModel
import com.example.firenotes.ui.designsystem.theme.FireNotesTheme
import com.example.firenotes.ui.designsystem.components.navigation.FireBottomNavigation
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.screens.agenda.AgendaScreen
import com.example.firenotes.ui.screens.consult.OccurrenceDetailsScreen
import com.example.firenotes.ui.screens.consult.OccurrenceDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : androidx.fragment.app.FragmentActivity() {

    @Inject
    lateinit var ocorrenciaDao: OcorrenciaDao

    @Inject
    lateinit var settingsRepository: com.example.firenotes.domain.repository.SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.firenotes.util.LogHelper.init(applicationContext)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            val theme by settingsRepository.themeFlow.collectAsState(initial = "Automático")
            val isDarkTheme = when (theme) {
                "Claro" -> false
                "Escuro" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            FireNotesTheme(darkTheme = isDarkTheme) {
                val showSplash = remember { mutableStateOf(true) }
                val isUnlocked = rememberSaveable { mutableStateOf(false) }
                val pinEnabled by settingsRepository.pinEnabledFlow.collectAsState(initial = false)
                val correctPin by settingsRepository.pinCodeFlow.collectAsState(initial = "")
                val biometricEnabled by settingsRepository.biometricEnabledFlow.collectAsState(initial = false)

                LaunchedEffect(biometricEnabled, pinEnabled) {
                    if (pinEnabled && biometricEnabled && !isUnlocked.value) {
                        showBiometricPrompt {
                            isUnlocked.value = true
                        }
                    }
                }

                if (showSplash.value) {
                    com.example.firenotes.ui.screens.splash.FireSplashScreen(
                        onAnimationComplete = { showSplash.value = false }
                    )
                } else if (pinEnabled && !isUnlocked.value) {
                    com.example.firenotes.ui.screens.security.PinLockScreen(
                        correctPin = correctPin,
                        onUnlocked = { isUnlocked.value = true }
                    )
                } else {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    val showBottomBar = currentRoute != null

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (showBottomBar) {
                                FireBottomNavigation {
                                    NavigationBarItem(
                                        icon = { Icon(FireIcons.Home, contentDescription = "Home") },
                                        label = { Text("Home") },
                                        selected = currentRoute == Screen.Home.route,
                                        onClick = {
                                            navController.navigate(Screen.Home.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    inclusive = true
                                                }
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(FireIcons.AddAlert, contentDescription = "Nova") },
                                        label = { Text("Nova") },
                                        selected = false,
                                        onClick = {
                                            navController.navigate(Screen.OccurrenceWizard.route)
                                        }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(FireIcons.List, contentDescription = "Consultar") },
                                        label = { Text("Consultar") },
                                        selected = currentRoute == Screen.Consult.route,
                                        onClick = {
                                            navController.navigate(Screen.Consult.route) {
                                                popUpTo(Screen.Home.route)
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(FireIcons.Info, contentDescription = "Dashboard") },
                                        label = { Text("Dashboard") },
                                        selected = currentRoute == Screen.Dashboard.route,
                                        onClick = {
                                            navController.navigate(Screen.Dashboard.route) {
                                                popUpTo(Screen.Home.route)
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(FireIcons.Settings, contentDescription = "Config") },
                                        label = { Text("Config") },
                                        selected = currentRoute == Screen.Settings.route,
                                        onClick = {
                                            navController.navigate(Screen.Settings.route) {
                                                popUpTo(Screen.Home.route)
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Home.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Home.route) {
                                val homeViewModel: HomeViewModel = hiltViewModel()
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    onNavigateToWizard = {
                                        navController.navigate(Screen.OccurrenceWizard.route)
                                    },
                                    onNavigateToDetails = { id ->
                                        navController.navigate(Screen.OccurrenceDetails.createRoute(id))
                                    },
                                    onNavigateToAgenda = { date ->
                                        navController.navigate(Screen.Agenda.createRoute(date))
                                    },
                                    onNavigateToConsult = {
                                        navController.navigate(Screen.Consult.route)
                                    }
                                )
                            }
                            composable(Screen.OccurrenceWizard.route) {
                                val formViewModel: OccurrenceFormViewModel = hiltViewModel()
                                OccurrenceFormScreen(
                                    viewModel = formViewModel,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    },
                                    onNavigateToDocumentScanner = { occurrenceId ->
                                        navController.navigate(Screen.DocumentScanner.createRoute(occurrenceId))
                                    }
                                )
                            }
                            composable(
                                route = Screen.OccurrenceDetails.route,
                                arguments = listOf(androidx.navigation.navArgument("occurrenceId") { type = androidx.navigation.NavType.StringType })
                            ) {
                                val detailsViewModel: OccurrenceDetailsViewModel = hiltViewModel()
                                OccurrenceDetailsScreen(
                                    viewModel = detailsViewModel,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    },
                                    onNavigateToEdit = { occurrenceId ->
                                        navController.navigate(Screen.OccurrenceEdit.createRoute(occurrenceId))
                                    }
                                )
                            }
                            composable(
                                route = Screen.OccurrenceEdit.route,
                                arguments = listOf(androidx.navigation.navArgument("occurrenceId") { type = androidx.navigation.NavType.StringType })
                            ) { backStackEntry ->
                                val id = backStackEntry.arguments?.getString("occurrenceId")
                                val formViewModel: OccurrenceFormViewModel = hiltViewModel()
                                LaunchedEffect(id) {
                                    if (id != null) {
                                        formViewModel.loadOccurrence(id)
                                    }
                                }
                                OccurrenceFormScreen(
                                    viewModel = formViewModel,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    },
                                    onNavigateToDocumentScanner = { occurrenceId ->
                                        navController.navigate(Screen.DocumentScanner.createRoute(occurrenceId))
                                    }
                                )
                            }
                            composable(
                                route = Screen.DocumentScanner.route,
                                arguments = listOf(androidx.navigation.navArgument("occurrenceId") { type = androidx.navigation.NavType.StringType })
                            ) { backStackEntry ->
                                val id = backStackEntry.arguments?.getString("occurrenceId").orEmpty()
                                val docViewModel: PersonIdentificationViewModel = hiltViewModel()
                                PersonIdentificationScreen(
                                    occurrenceId = id,
                                    viewModel = docViewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(Screen.Consult.route) {
                                val consultViewModel: com.example.firenotes.ui.screens.consult.ConsultViewModel = hiltViewModel()
                                com.example.firenotes.ui.screens.consult.ConsultScreen(
                                    viewModel = consultViewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToDetails = { id ->
                                        navController.navigate(Screen.OccurrenceDetails.createRoute(id))
                                    }
                                )
                            }
                            composable(Screen.Dashboard.route) {
                                val dashboardViewModel: com.example.firenotes.ui.screens.dashboard.DashboardViewModel = hiltViewModel()
                                com.example.firenotes.ui.screens.dashboard.DashboardScreen(
                                    viewModel = dashboardViewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToReports = {
                                        navController.navigate(Screen.Reports.route) {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                            composable(Screen.Settings.route) {
                                val settingsViewModel: SettingsViewModel = hiltViewModel()
                                SettingsScreen(
                                    viewModel = settingsViewModel
                                )
                            }
                            composable(Screen.Reports.route) {
                                val reportsViewModel: ReportsViewModel = hiltViewModel()
                                ReportsScreen(
                                    viewModel = reportsViewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(
                                route = Screen.Agenda.route,
                                arguments = listOf(androidx.navigation.navArgument("date") { type = androidx.navigation.NavType.StringType })
                            ) { backStackEntry ->
                                val dateStr = backStackEntry.arguments?.getString("date") ?: java.time.LocalDate.now().toString()
                                val date = runCatching { java.time.LocalDate.parse(dateStr) }.getOrDefault(java.time.LocalDate.now())
                                val homeViewModel: HomeViewModel = hiltViewModel()
                                AgendaScreen(
                                    date = date,
                                    viewModel = homeViewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticação Biométrica")
            .setSubtitle("Acesse o Fire Notes com sua digital")
            .setNegativeButtonText("Usar PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}