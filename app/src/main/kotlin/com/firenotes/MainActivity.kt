package com.firenotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.firenotes.core.common.navigation.Screen
import com.firenotes.core.common.ui.theme.FireNotesTheme
import com.firenotes.features.backup.BackupScreen
import com.firenotes.features.dashboard.DashboardScreen
import com.firenotes.features.login.LoginScreen
import com.firenotes.features.occurrence.OccurrenceDetailsScreen
import com.firenotes.features.occurrence.OccurrenceEditScreen
import com.firenotes.features.occurrence.OccurrenceListScreen
import com.firenotes.features.occurrence.OccurrenceNewScreen
import com.firenotes.features.search.SearchScreen
import com.firenotes.features.settings.SettingsScreen
import com.firenotes.ui.SplashScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FireNotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Login Screen
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Dashboard Screen
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToNewOccurrence = { navController.navigate(Screen.OccurrenceNew.route) },
                onNavigateToOccurrenceList = { navController.navigate(Screen.OccurrenceList.route) },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToBackup = { navController.navigate(Screen.Backup.route) }
            )
        }

        // Occurrence New Screen
        composable(Screen.OccurrenceNew.route) {
            OccurrenceNewScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Occurrence List Screen
        composable(Screen.OccurrenceList.route) {
            OccurrenceListScreen(
                onNavigateToDetails = { id -> navController.navigate(Screen.OccurrenceDetails.createRoute(id)) },
                onNavigateToEdit = { id -> navController.navigate(Screen.OccurrenceEdit.createRoute(id)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Occurrence Details Screen
        composable(
            route = Screen.OccurrenceDetails.route,
            arguments = listOf(navArgument("occurrenceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val occurrenceId = backStackEntry.arguments?.getString("occurrenceId") ?: ""
            OccurrenceDetailsScreen(
                occurrenceId = occurrenceId,
                onNavigateToEdit = { id -> navController.navigate(Screen.OccurrenceEdit.createRoute(id)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Occurrence Edit Screen
        composable(
            route = Screen.OccurrenceEdit.route,
            arguments = listOf(navArgument("occurrenceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val occurrenceId = backStackEntry.arguments?.getString("occurrenceId") ?: ""
            OccurrenceEditScreen(
                occurrenceId = occurrenceId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Search Screen
        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateToDetails = { id -> navController.navigate(Screen.OccurrenceDetails.createRoute(id)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Backup Screen
        composable(Screen.Backup.route) {
            BackupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
