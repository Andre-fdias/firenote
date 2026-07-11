package com.example.firenotes.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

// ============================================
// DESTINATIONS PRINCIPAIS
// ============================================

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null,
    val selectedIcon: ImageVector? = null,
    val showInBottomBar: Boolean = false
) {
    // ============================================
    // TELAS PRINCIPAIS (BOTTOM NAVIGATION)
    // ============================================
    
    object Home : Screen(
        route = "home",
        title = "Início",
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home,
        showInBottomBar = true
    )
    
    object Consult : Screen(
        route = "consult",
        title = "Consultar",
        icon = Icons.Outlined.Search,
        selectedIcon = Icons.Filled.Search,
        showInBottomBar = true
    )
    
    object Dashboard : Screen(
        route = "dashboard",
        title = "Dashboard",
        icon = Icons.Outlined.Dashboard,
        selectedIcon = Icons.Filled.Dashboard,
        showInBottomBar = true
    )
    
    object Settings : Screen(
        route = "settings",
        title = "Configurações",
        icon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings,
        showInBottomBar = true
    )
    
    // ============================================
    // TELAS SECUNDÁRIAS (SEM BOTTOM BAR)
    // ============================================
    
    object OccurrenceWizard : Screen(
        route = "occurrence_wizard",
        title = "Nova Ocorrência",
        icon = Icons.Outlined.Add,
        selectedIcon = Icons.Filled.Add,
        showInBottomBar = false
    )
    
    object OccurrenceDetails : Screen(
        route = "occurrence_details/{occurrenceId}",
        title = "Detalhes da Ocorrência",
        icon = Icons.Outlined.Info,
        selectedIcon = Icons.Filled.Info,
        showInBottomBar = false
    ) {
        fun createRoute(occurrenceId: String): String {
            return "occurrence_details/$occurrenceId"
        }
        
        fun getOccurrenceId(route: String): String? {
            return route.removePrefix("occurrence_details/").takeIf { it.isNotBlank() }
        }
    }
    
    object Reports : Screen(
        route = "reports",
        title = "Relatórios",
        icon = Icons.Outlined.Description,
        selectedIcon = Icons.Filled.Description,
        showInBottomBar = false
    )
    
    object OccurrenceEdit : Screen(
        route = "occurrence_edit/{occurrenceId}",
        title = "Editar Ocorrência",
        icon = Icons.Outlined.Edit,
        selectedIcon = Icons.Filled.Edit,
        showInBottomBar = false
    ) {
        fun createRoute(occurrenceId: String): String {
            return "occurrence_edit/$occurrenceId"
        }
    }
    
    // ============================================
    // TELAS DE SEGURANÇA
    // ============================================
    
    object PinLock : Screen(
        route = "pin_lock",
        title = "PIN de Segurança",
        icon = Icons.Outlined.Lock,
        selectedIcon = Icons.Filled.Lock,
        showInBottomBar = false
    )
    
    object Splash : Screen(
        route = "splash",
        title = "Fire Notes",
        icon = null,
        selectedIcon = null,
        showInBottomBar = false
    )
    
    // ============================================
    // TELAS DE DOCUMENTOS (OCR)
    // ============================================
    
    object DocumentScanner : Screen(
        route = "document_scanner/{occurrenceId}",
        title = "Digitalizar Documento",
        icon = Icons.Outlined.Camera,
        selectedIcon = Icons.Filled.Camera,
        showInBottomBar = false
    ) {
        fun createRoute(occurrenceId: String): String {
            return "document_scanner/$occurrenceId"
        }
    }
    
    object DocumentDetails : Screen(
        route = "document_details/{documentId}",
        title = "Detalhes do Documento",
        icon = Icons.Outlined.Description,
        selectedIcon = Icons.Filled.Description,
        showInBottomBar = false
    ) {
        fun createRoute(documentId: String): String {
            return "document_details/$documentId"
        }
    }

    // ============================================
    // TELAS DE VIATURAS E MILITARES
    // ============================================
    
    object ViaturaList : Screen(
        route = "viaturas",
        title = "Viaturas",
        icon = Icons.Outlined.LocalFireDepartment,
        selectedIcon = Icons.Filled.LocalFireDepartment,
        showInBottomBar = false
    )
    
    object ViaturaDetails : Screen(
        route = "viatura_details/{viaturaId}",
        title = "Detalhes da Viatura",
        icon = Icons.Outlined.LocalFireDepartment,
        selectedIcon = Icons.Filled.LocalFireDepartment,
        showInBottomBar = false
    ) {
        fun createRoute(viaturaId: String): String {
            return "viatura_details/$viaturaId"
        }
    }
    
    object MilitarDetails : Screen(
        route = "militar_details/{militarId}",
        title = "Detalhes do Militar",
        icon = Icons.Outlined.Person,
        selectedIcon = Icons.Filled.Person,
        showInBottomBar = false
    ) {
        fun createRoute(militarId: String): String {
            return "militar_details/$militarId"
        }
    }

    // ============================================
    // COMPANION OBJECT COM UTILITÁRIOS
    // ============================================
    
    companion object {
        val bottomNavScreens = listOf(
            Home,
            Consult,
            Dashboard,
            Settings
        )
        
        val allScreens = listOf(
            Home,
            Consult,
            Dashboard,
            Settings,
            OccurrenceWizard,
            OccurrenceDetails,
            Reports,
            PinLock,
            Splash,
            DocumentScanner,
            DocumentDetails,
            ViaturaList,
            ViaturaDetails,
            MilitarDetails
        )
        
        fun fromRoute(route: String?): Screen? {
            return allScreens.find {
                it.route == route || route?.startsWith(it.route.replace("/{.*}".toRegex(), "")) == true
            }
        }
        
        fun getTitle(route: String?): String {
            return fromRoute(route)?.title ?: "Fire Notes"
        }
        
        fun getIcon(route: String?): ImageVector? {
            return fromRoute(route)?.icon
        }
        
        fun isBottomBarVisible(route: String?): Boolean {
            return bottomNavScreens.any { it.route == route }
        }
    }
}

// ============================================
// NAVIGATION ROUTES CONSTANTS
// ============================================

object NavRoutes {
    const val HOME = "home"
    const val CONSULT = "consult"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val OCCURRENCE_WIZARD = "occurrence_wizard"
    const val OCCURRENCE_DETAILS = "occurrence_details"
    const val REPORTS = "reports"
    const val PIN_LOCK = "pin_lock"
    const val SPLASH = "splash"
    const val DOCUMENT_SCANNER = "document_scanner"
    const val DOCUMENT_DETAILS = "document_details"
    const val VIATURA_LIST = "viaturas"
    const val VIATURA_DETAILS = "viatura_details"
    const val MILITAR_DETAILS = "militar_details"
    
    // Parâmetros
    const val OCCURRENCE_ID = "occurrenceId"
    const val DOCUMENT_ID = "documentId"
    const val VIATURA_ID = "viaturaId"
    const val MILITAR_ID = "militarId"
}

// ============================================
// NAVIGATION ARGUMENTS
// ============================================

sealed class NavArgument(val key: String) {
    object OccurrenceId : NavArgument(NavRoutes.OCCURRENCE_ID)
    object DocumentId : NavArgument(NavRoutes.DOCUMENT_ID)
    object ViaturaId : NavArgument(NavRoutes.VIATURA_ID)
    object MilitarId : NavArgument(NavRoutes.MILITAR_ID)
}

// ============================================
// NAVIGATION ACTIONS
// ============================================

interface NavigationActions {
    fun navigateTo(screen: Screen)
    fun navigateTo(route: String)
    fun navigateToOccurrenceDetails(occurrenceId: String)
    fun navigateToDocumentDetails(documentId: String)
    fun navigateToViaturaDetails(viaturaId: String)
    fun navigateToMilitarDetails(militarId: String)
    fun navigateBack()
    fun navigateHome()
    fun popUpTo(route: String, inclusive: Boolean = false)
    fun clearAndNavigate(route: String)
}

// ============================================
// NAVIGATION EXTENSIONS
// ============================================

fun Screen.withArgs(vararg pairs: Pair<String, String>): String {
    return if (pairs.isEmpty()) {
        route
    } else {
        val baseRoute = route.substringBefore("{")
        val args = pairs.joinToString("&") { (key, value) ->
            "$key=$value"
        }
        "$baseRoute?$args"
    }
}

fun String.extractArg(key: String): String? {
    val pattern = "$key=([^&]+)".toRegex()
    return pattern.find(this)?.groupValues?.get(1)
}

fun String.extractOccurrenceId(): String? {
    return extractArg(NavRoutes.OCCURRENCE_ID)
}

fun String.extractDocumentId(): String? {
    return extractArg(NavRoutes.DOCUMENT_ID)
}

fun String.extractViaturaId(): String? {
    return extractArg(NavRoutes.VIATURA_ID)
}
