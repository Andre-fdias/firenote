package com.example.firenotes.ui.navigation

// Placeholder for Jetpack Compose Navigation destinations
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object OccurrenceDetails : Screen("occurrence_details/{occurrenceId}") {
        fun createRoute(occurrenceId: String) = "occurrence_details/$occurrenceId"
    }
    object OccurrenceForm : Screen("occurrence_form?natureza={natureza}") {
        fun createRoute(natureza: String) = "occurrence_form?natureza=$natureza"
    }
    object OccurrenceWizard : Screen("occurrence_wizard")
    object Consult : Screen("consult")
    object Dashboard : Screen("dashboard")
    object Reports : Screen("reports")
    object Settings : Screen("settings")
}
