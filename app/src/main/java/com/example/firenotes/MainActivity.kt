package com.example.firenotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.example.firenotes.ui.screens.settings.SettingsScreen
import com.example.firenotes.ui.screens.settings.SettingsViewModel
import com.example.firenotes.ui.designsystem.theme.FireNotesTheme
import com.example.firenotes.ui.designsystem.components.navigation.FireBottomNavigation
import com.example.firenotes.ui.designsystem.icons.FireIcons
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var ocorrenciaDao: OcorrenciaDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val configState = ocorrenciaDao.getConfiguracaoFlow().collectAsState(initial = null)
            val config = configState.value
            val isDarkTheme = when (config?.tema) {
                "Claro" -> false
                "Escuro" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            FireNotesTheme(darkTheme = isDarkTheme) {
                val isUnlocked = remember { mutableStateOf(false) }
                val prefs = remember { getSharedPreferences("security_prefs", android.content.Context.MODE_PRIVATE) }
                val pinEnabled = prefs.getBoolean("pin_enabled", false)
                val correctPin = prefs.getString("pin_code", "") ?: ""

                if (pinEnabled && !isUnlocked.value) {
                    com.example.firenotes.ui.screens.security.PinLockScreen(
                        correctPin = correctPin,
                        onUnlocked = { isUnlocked.value = true }
                    )
                } else {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    val showBottomBar = currentRoute in listOf(
                        Screen.Home.route,
                        Screen.Consult.route,
                        Screen.Dashboard.route,
                        Screen.Settings.route
                    )

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
                                                popUpTo(Screen.Home.route) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
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
                                    }
                                )
                            }
                            composable(Screen.OccurrenceForm.route) {
                                val formViewModel: OccurrenceFormViewModel = hiltViewModel()
                                OccurrenceFormScreen(
                                    viewModel = formViewModel,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                            composable(Screen.OccurrenceWizard.route) {
                                val formViewModel: OccurrenceFormViewModel = hiltViewModel()
                                OccurrenceFormScreen(
                                    viewModel = formViewModel,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                            composable(
                                route = Screen.OccurrenceDetails.route,
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
                                    }
                                )
                            }
                            composable(Screen.Consult.route) {
                                val consultViewModel: com.example.firenotes.ui.screens.consult.ConsultViewModel = hiltViewModel()
                                com.example.firenotes.ui.screens.consult.ConsultScreen(
                                    viewModel = consultViewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToDuplicate = { _ ->
                                        navController.navigate(Screen.OccurrenceWizard.route)
                                    }
                                )
                            }
                            composable(Screen.Dashboard.route) {
                                val dashboardViewModel: com.example.firenotes.ui.screens.dashboard.DashboardViewModel = hiltViewModel()
                                com.example.firenotes.ui.screens.dashboard.DashboardScreen(
                                    viewModel = dashboardViewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(Screen.Settings.route) {
                                val settingsViewModel: SettingsViewModel = hiltViewModel()
                                SettingsScreen(
                                    viewModel = settingsViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}