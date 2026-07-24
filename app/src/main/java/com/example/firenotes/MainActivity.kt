package com.example.firenotes

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.setValue
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

import com.example.firenotes.ui.screens.consult.OccurrenceDetailsScreen
import com.example.firenotes.ui.screens.consult.OccurrenceDetailsViewModel
import com.example.firenotes.ui.screens.calendar.SettingsCalendarScreen
import com.example.firenotes.ui.screens.calendar.SettingsCalendarViewModel
import com.example.firenotes.ui.screens.calendar.GoogleSyncScreen
import com.example.firenotes.ui.screens.calendar.CalendarWizardScreen
import com.example.firenotes.ui.screens.calendar.CalendarWizardViewModel
import com.example.firenotes.domain.calendar.GoogleCalendarSyncManager
import com.example.firenotes.domain.calendar.NotificationCenter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : androidx.fragment.app.FragmentActivity() {

    @Inject
    lateinit var ocorrenciaDao: OcorrenciaDao

    @Inject
    lateinit var settingsRepository: com.example.firenotes.domain.repository.SettingsRepository

    @Inject
    lateinit var occurrenceRepository: com.example.firenotes.domain.repository.OcorrenciaRepository

    @Inject
    lateinit var googleCalendarSyncManager: GoogleCalendarSyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.firenotes.util.LogHelper.init(applicationContext)

        // Register notifications channel
        NotificationCenter.initNotificationChannels(this)

        // Request runtime permission for notifications on Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

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
        handleIncomingIntent(intent)
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
                val pinEnabled by settingsRepository.pinEnabledFlow.collectAsState(initial = null)
                val correctPin by settingsRepository.pinCodeFlow.collectAsState(initial = "")
                val biometricEnabled by settingsRepository.biometricEnabledFlow.collectAsState(initial = null)

                // Dispara biometria assim que os valores reais chegam e o splash terminou
                LaunchedEffect(biometricEnabled, pinEnabled, showSplash.value) {
                    if (!showSplash.value && biometricEnabled == true && !isUnlocked.value) {
                        showBiometricPrompt {
                            isUnlocked.value = true
                        }
                    }
                }

                if (showSplash.value || pinEnabled == null || biometricEnabled == null) {
                    // Mantém splash enquanto carrega configurações
                    com.example.firenotes.ui.screens.splash.FireSplashScreen(
                        onAnimationComplete = { showSplash.value = false }
                    )
                } else if ((pinEnabled == true || biometricEnabled == true) && !isUnlocked.value) {
                    // Se PIN ou biometria ativados e ainda não desbloqueado, mostra PIN
                    com.example.firenotes.ui.screens.security.PinLockScreen(
                        correctPin = correctPin,
                        onUnlocked = { isUnlocked.value = true }
                    )
                } else {
                    val navController = rememberNavController()
                    
                    var lastNavTime by remember { mutableStateOf(0L) }
                    val debounceNavigate: (() -> Unit) -> Unit = { navAction ->
                        val now = System.currentTimeMillis()
                        if (now - lastNavTime > 500) {
                            lastNavTime = now
                            navAction()
                        }
                    }
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
                                        onClick = { debounceNavigate { navController.navigate(Screen.Home.route) { popUpTo(navController.graph.startDestinationId) {
                                                    inclusive = true
                                                }
                                                launchSingleTop = true } } }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(FireIcons.AddAlert, contentDescription = "Nova") },
                                        label = { Text("Nova") },
                                        selected = false,
                                        onClick = { debounceNavigate { navController.navigate(Screen.OccurrenceWizard.route) } }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(FireIcons.List, contentDescription = "Consultar") },
                                        label = { Text("Consultar") },
                                        selected = currentRoute == Screen.Consult.route,
                                        onClick = { debounceNavigate { navController.navigate(Screen.Consult.route) { popUpTo(Screen.Home.route)
                                                launchSingleTop = true
                                                restoreState = true } } }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(FireIcons.Info, contentDescription = "Dashboard") },
                                        label = { Text("Dashboard") },
                                        selected = currentRoute == Screen.Dashboard.route,
                                        onClick = { debounceNavigate { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Home.route)
                                                launchSingleTop = true
                                                restoreState = true } } }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(FireIcons.Settings, contentDescription = "Config") },
                                        label = { Text("Config") },
                                        selected = currentRoute == Screen.Settings.route,
                                        onClick = { debounceNavigate { navController.navigate(Screen.Settings.route) { popUpTo(Screen.Home.route)
                                                launchSingleTop = true
                                                restoreState = true } } }
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
                                    onNavigateToWizard = { debounceNavigate { navController.navigate(Screen.CalendarWizard.route) } },
                                    onNavigateToDetails = { id -> debounceNavigate { navController.navigate(Screen.OccurrenceDetails.createRoute(id)) } },
                                    onNavigateToConsult = { debounceNavigate { navController.navigate(Screen.Consult.route) } }
                                )
                            }
                            composable(Screen.OccurrenceWizard.route) {
                                val formViewModel: OccurrenceFormViewModel = hiltViewModel()
                                OccurrenceFormScreen(
                                    viewModel = formViewModel,
                                    onNavigateBack = { debounceNavigate { navController.popBackStack() } },
                                    onNavigateToDocumentScanner = { occurrenceId, documentId -> debounceNavigate { navController.navigate(Screen.DocumentScanner.createRoute(occurrenceId, documentId)) } }
                                )
                            }
                            composable(
                                route = Screen.OccurrenceDetails.route,
                                arguments = listOf(androidx.navigation.navArgument("occurrenceId") { type = androidx.navigation.NavType.StringType })
                            ) {
                                val detailsViewModel: OccurrenceDetailsViewModel = hiltViewModel()
                                OccurrenceDetailsScreen(
                                    viewModel = detailsViewModel,
                                    onNavigateBack = { debounceNavigate { navController.popBackStack() } },
                                    onNavigateToEdit = { occurrenceId -> debounceNavigate { navController.navigate(Screen.OccurrenceEdit.createRoute(occurrenceId)) } }
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
                                    onNavigateBack = { debounceNavigate { navController.popBackStack() } },
                                    onNavigateToDocumentScanner = { occurrenceId, documentId -> debounceNavigate { navController.navigate(Screen.DocumentScanner.createRoute(occurrenceId, documentId)) } }
                                )
                            }
                            composable(
                                route = Screen.DocumentScanner.route,
                                arguments = listOf(
                                    androidx.navigation.navArgument("occurrenceId") { type = androidx.navigation.NavType.StringType },
                                    androidx.navigation.navArgument("documentId") {
                                        type = androidx.navigation.NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    }
                                )
                            ) { backStackEntry ->
                                val id = backStackEntry.arguments?.getString("occurrenceId").orEmpty()
                                val docId = backStackEntry.arguments?.getString("documentId")
                                val docViewModel: PersonIdentificationViewModel = hiltViewModel()
                                PersonIdentificationScreen(
                                    occurrenceId = id,
                                    documentId = docId,
                                    viewModel = docViewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(Screen.Consult.route) {
                                val consultViewModel: com.example.firenotes.ui.screens.consult.ConsultViewModel = hiltViewModel()
                                com.example.firenotes.ui.screens.consult.ConsultScreen(
                                    viewModel = consultViewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToDetails = { id -> debounceNavigate { navController.navigate(Screen.OccurrenceDetails.createRoute(id)) } }
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
                                     viewModel = settingsViewModel,
                                     onNavigateToCalendarSettings = {
                                         navController.navigate(Screen.SettingsCalendar.route)
                                     },
                                     onNavigateToGoogleSync = {
                                         navController.navigate(Screen.GoogleSync.route)
                                     }
                                 )
                             }
                             composable(Screen.SettingsCalendar.route) {
                                 val settingsCalendarViewModel: SettingsCalendarViewModel = hiltViewModel()
                                 SettingsCalendarScreen(
                                     viewModel = settingsCalendarViewModel,
                                     onNavigateBack = { navController.popBackStack() },
                                     onNavigateToGoogleSync = {
                                         navController.navigate(Screen.GoogleSync.route)
                                     },
                                     onNavigateToWizard = { escalaId ->
                                         navController.navigate(Screen.CalendarWizard.createRoute(escalaId))
                                     }
                                 )
                             }
                             composable(
                                 route = Screen.CalendarWizard.route,
                                 arguments = listOf(androidx.navigation.navArgument("escalaId") {
                                     type = androidx.navigation.NavType.StringType
                                     nullable = true
                                     defaultValue = null
                                 })
                             ) {
                                 val wizardViewModel: CalendarWizardViewModel = hiltViewModel()
                                 CalendarWizardScreen(
                                     viewModel = wizardViewModel,
                                     onNavigateBack = { navController.popBackStack() }
                                 )
                             }
                             composable(Screen.GoogleSync.route) {

                                 GoogleSyncScreen(
                                     syncManager = googleCalendarSyncManager,
                                     onNavigateBack = { navController.popBackStack() }
                                 )
                             }
                            composable(Screen.Reports.route) {
                                val reportsViewModel: ReportsViewModel = hiltViewModel()
                                ReportsScreen(
                                    viewModel = reportsViewModel,
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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: android.content.Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type

        val uri: android.net.Uri? = when (action) {
            android.content.Intent.ACTION_SEND -> {
                if (type == "application/json" || type == "*/*") {
                    intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM) as? android.net.Uri
                } else null
            }
            android.content.Intent.ACTION_VIEW -> {
                intent.data
            }
            else -> null
        }

        uri?.let { inputUri ->
            lifecycleScope.launch {
                try {
                    contentResolver.openInputStream(inputUri)?.bufferedReader()?.use { reader ->
                        val jsonStr = reader.readText()
                        com.example.firenotes.util.JsonImportHelper.importOccurrenceFromJson(jsonStr, occurrenceRepository).fold(
                            onSuccess = {
                                runOnUiThread {
                                    android.widget.Toast.makeText(this@MainActivity, "Ocorrência recebida e importada com sucesso!", android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            onFailure = { e ->
                                runOnUiThread {
                                    android.widget.Toast.makeText(this@MainActivity, "Erro ao importar ocorrência recebida: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        android.widget.Toast.makeText(this@MainActivity, "Erro ao ler arquivo recebido: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}