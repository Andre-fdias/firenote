package com.firenotes.core.common.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Dashboard : Screen("dashboard")
    data object OccurrenceList : Screen("occurrence_list")
    data object OccurrenceNew : Screen("occurrence_new")
    data object OccurrenceEdit : Screen("occurrence_edit/{occurrenceId}") {
        fun createRoute(occurrenceId: String) = "occurrence_edit/$occurrenceId"
    }
    data object OccurrenceDetails : Screen("occurrence_details/{occurrenceId}") {
        fun createRoute(occurrenceId: String) = "occurrence_details/$occurrenceId"
    }
    data object Search : Screen("search")
    data object Settings : Screen("settings")
    data object Backup : Screen("backup")
}
