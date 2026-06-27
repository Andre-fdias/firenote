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
    
    // Rotas de Viaturas
    data object OccurrenceVehicles : Screen("occurrence_vehicles/{occurrenceId}") {
        fun createRoute(occurrenceId: String) = "occurrence_vehicles/$occurrenceId"
    }
    data object VehicleForm : Screen("vehicle_form/{occurrenceId}/{vehicleId}") {
        fun createRoute(occurrenceId: String, vehicleId: String) = "vehicle_form/$occurrenceId/$vehicleId"
    }
    
    // Rotas de Militares
    data object VehicleMilitary : Screen("vehicle_military/{vehicleId}") {
        fun createRoute(vehicleId: String) = "vehicle_military/$vehicleId"
    }
}
