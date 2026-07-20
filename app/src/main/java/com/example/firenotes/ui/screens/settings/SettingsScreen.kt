package com.example.firenotes.ui.screens.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firenotes.MainActivity
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.designsystem.components.topbar.FireTopBar
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.delay
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // Estado para controle de abas
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Geral", "Segurança", "Backup", "Logs")

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    viewModel.connectGoogleDrive(account)
                }
            } catch (e: Exception) {
                viewModel.setError("Falha ao conectar: ${e.localizedMessage}")
            }
        }
    }

    val recoveryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.clearRecoveryIntent()
            viewModel.fetchDriveBackups()
        } else {
            viewModel.clearRecoveryIntent()
        }
    }

    LaunchedEffect(uiState.authRecoveryIntent) {
        uiState.authRecoveryIntent?.let { intent ->
            recoveryLauncher.launch(intent)
        }
    }

    // Estados para diálogos
    var showPinDialog by remember { mutableStateOf(false) }
    var showEraseConfirmDialog by remember { mutableStateOf(false) }
    var showLogViewerDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            FireTopBar(
                title = "⚙️ Configurações",
                onBackClick = null,
                backgroundColor = FireColors.Surface,
                elevation = 2.dp
            )
        },
        containerColor = FireColors.Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Mensagens de feedback
            AnimatedVisibility(
                visible = uiState.infoMessage != null || uiState.errorMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.infoMessage != null) {
                        InfoBanner(
                            message = uiState.infoMessage!!,
                            type = "success",
                            onDismiss = viewModel::clearMessages
                        )
                    }
                    if (uiState.errorMessage != null) {
                        InfoBanner(
                            message = uiState.errorMessage!!,
                            type = "error",
                            onDismiss = viewModel::clearMessages
                        )
                    }
                }
            }

            // Abas de navegação
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = FireColors.Primary,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = FireColors.Primary
                    )
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = FireTypography.Title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp
                            )
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // Conteúdo das abas
            when (selectedTab) {
                0 -> GeneralSettingsTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    showEraseConfirmDialog = showEraseConfirmDialog,
                    onShowEraseDialog = { showEraseConfirmDialog = true },
                    onDismissEraseDialog = { showEraseConfirmDialog = false }
                )
                1 -> SecuritySettingsTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    showPinDialog = showPinDialog,
                    onShowPinDialog = { showPinDialog = true },
                    onDismissPinDialog = { showPinDialog = false }
                )
                2 -> BackupSettingsTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    googleSignInLauncher = googleSignInLauncher
                )
                3 -> LogSettingsTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    showLogViewerDialog = showLogViewerDialog,
                    onShowLogViewer = { showLogViewerDialog = true },
                    onDismissLogViewer = { showLogViewerDialog = false }
                )
            }
        }
    }

    // Diálogos
    if (showPinDialog) {
        PinSetupDialog(
            pinValue = uiState.pinCode,
            pinConfirmValue = uiState.pinConfirmValue ?: "",
            pinError = uiState.pinError ?: "",
            onPinChange = { viewModel.updatePinCode(it) },
            onPinConfirmChange = { viewModel.updatePinConfirm(it) },
            onSave = {
                viewModel.savePin()
                showPinDialog = false
            },
            onDismiss = {
                showPinDialog = false
                viewModel.clearPinError()
            }
        )
    }

    if (showEraseConfirmDialog) {
        EraseConfirmationDialog(
            onConfirm = {
                showEraseConfirmDialog = false
                viewModel.eraseAllData {
                    activity?.let {
                        val intent = Intent(it, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        it.startActivity(intent)
                        Runtime.getRuntime().exit(0)
                    }
                }
            },
            onDismiss = { showEraseConfirmDialog = false }
        )
    }

    if (uiState.showRestoreDialog) {
        RestoreDialog(
            backups = uiState.driveBackups,
            isProcessing = uiState.isProcessing,
            onDismiss = viewModel::dismissRestoreDialog,
            onRestore = { fileId ->
                viewModel.restoreDriveBackup(fileId) {
                    activity?.let {
                        val intent = Intent(it, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        it.startActivity(intent)
                        Runtime.getRuntime().exit(0)
                    }
                }
            }
        )
    }

    if (showLogViewerDialog) {
        LogViewerDialog(
            logContent = uiState.logContent ?: "Nenhum log disponível",
            onDismiss = {
                showLogViewerDialog = false
                viewModel.clearLogContent()
            },
            onExport = {
                viewModel.exportLogs { uri ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartilhar Logs"))
                }
            },
            onClear = {
                viewModel.clearLogs()
                showLogViewerDialog = false
            }
        )
    }
}

// ============================================
// ABA 1: CONFIGURAÇÕES GERAIS
// ============================================

@Composable
private fun GeneralSettingsTab(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    showEraseConfirmDialog: Boolean,
    onShowEraseDialog: () -> Unit,
    onDismissEraseDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tema
        item {
            PreferenceCard(
                title = "Tema",
                subtitle = "Escolha o tema do aplicativo",
                modifier = Modifier.fillMaxWidth()
            ) {
                TemaSelector(
                    selectedTheme = uiState.config.tema ?: "Automático",
                    onThemeSelected = viewModel::updateTheme
                )
            }
        }

        // Idioma
        item {
            PreferenceCard(
                title = "Idioma",
                subtitle = "Selecionar idioma do aplicativo",
                modifier = Modifier.fillMaxWidth()
            ) {
                IdiomaSelector(
                    selectedLanguage = uiState.idioma,
                    onLanguageSelected = viewModel::updateLanguage
                )
            }
        }

        // Data/Hora
        item {
            PreferenceCard(
                title = "Formato de Data/Hora",
                subtitle = "Escolha o formato preferido",
                modifier = Modifier.fillMaxWidth()
            ) {
                DateTimeFormatSelector(
                    selectedFormat = uiState.formatoData,
                    onFormatSelected = viewModel::updateDateTimeFormat
                )
            }
        }

        // Unidades
        item {
            PreferenceCard(
                title = "Sistema de Unidades",
                subtitle = "Escolha entre unidades métricas e imperiais",
                modifier = Modifier.fillMaxWidth()
            ) {
                UnitSystemSelector(
                    selectedSystem = uiState.sistemaUnidades,
                    onSystemSelected = viewModel::updateUnitSystem
                )
            }
        }

        // Limpeza de dados
        item {
            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Gerenciamento de Dados",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Exclua permanentemente todos os dados do aplicativo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onShowEraseDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Excluir Todos os Dados")
                    }
                }
            }
        }

        // Versão
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Fire Notes v8.3 • Seguro",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ============================================
// ABA 2: SEGURANÇA
// ============================================

@Composable
private fun SecuritySettingsTab(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    showPinDialog: Boolean,
    onShowPinDialog: () -> Unit,
    onDismissPinDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // PIN
        item {
            SwitchPreferenceCard(
                title = "Acesso por PIN",
                subtitle = if (uiState.pinEnabled) "PIN ativado - 4 dígitos" else "Desativado",
                checked = uiState.pinEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        onShowPinDialog()
                    } else {
                        viewModel.updatePin("", false)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Alterar PIN (se ativado)
        if (uiState.pinEnabled) {
            item {
                TextButton(
                    onClick = onShowPinDialog,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Alterar código PIN")
                }
            }
        }

        // Biometria
        item {
            SwitchPreferenceCard(
                title = "Autenticação Biométrica",
                subtitle = if (uiState.biometricEnabled)
                    "Usando impressão digital ou facial"
                else "Desativado",
                checked = uiState.biometricEnabled,
                onCheckedChange = viewModel::updateBiometric,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Status de segurança
        item {
            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        item {
            SecurityStatusCard(
                isEncrypted = uiState.pinEnabled || uiState.biometricEnabled,
                lastAccess = uiState.lastAccessTime ?: "Nunca"
            )
        }
    }
}

// ============================================
// ABA 3: BACKUP
// ============================================

@Composable
private fun BackupSettingsTab(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    googleSignInLauncher: androidx.activity.compose.ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Conexão Google Drive
        item {
            PreferenceCard(
                title = "Google Drive",
                subtitle = if (uiState.isGoogleConnected)
                    "Conectado como ${uiState.googleAccountName}"
                else "Não conectado",
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.isGoogleConnected) {
                        OutlinedButton(
                            onClick = viewModel::disconnectGoogleDrive,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Desconectar")
                        }
                    } else {
                        Button(
                            onClick = {
                                val client = viewModel.googleDriveBackupService.getGoogleSignInClient()
                                googleSignInLauncher.launch(client.signInIntent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Conectar Google Drive")
                        }
                    }
                }
            }
        }

        // Frequência de backup
        item {
            PreferenceCard(
                title = "Frequência de Backup",
                subtitle = "Backup automático: ${uiState.config.backupAutomatico ?: "Nunca"}",
                modifier = Modifier.fillMaxWidth()
            ) {
                FrequencySelector(
                    selectedFrequency = uiState.config.backupAutomatico ?: "Nunca",
                    onFrequencySelected = viewModel::updateBackupFrequency
                )
            }
        }

        // Wi-Fi only
        item {
            SwitchPreferenceCard(
                title = "Apenas em Wi-Fi",
                subtitle = if (uiState.config.backupSomenteWifi ?: false)
                    "Backup apenas em redes Wi-Fi"
                else "Permitir backup em dados móveis",
                checked = uiState.config.backupSomenteWifi ?: false,
                onCheckedChange = viewModel::updateBackupWifiOnly,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Último backup
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Último Backup",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = uiState.config.ultimoBackupData ?: "Pendente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Ações de backup
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = viewModel::performDriveBackup,
                    enabled = !uiState.isProcessing && uiState.isGoogleConnected,
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Backup")
                    }
                }

                OutlinedButton(
                    onClick = viewModel::fetchDriveBackups,
                    enabled = !uiState.isProcessing && uiState.isGoogleConnected,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Restaurar")
                }
            }
        }

        // Indicador de progresso
        if (uiState.isProcessing) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ============================================
// ABA 4: LOGS
// ============================================

@Composable
private fun LogSettingsTab(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    showLogViewerDialog: Boolean,
    onShowLogViewer: () -> Unit,
    onDismissLogViewer: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Nível de log
        item {
            PreferenceCard(
                title = "Nível de Registro",
                subtitle = "Nível atual: ${uiState.logLevel}",
                modifier = Modifier.fillMaxWidth()
            ) {
                LogLevelSelector(
                    selectedLevel = uiState.logLevel,
                    onLevelSelected = viewModel::updateLogLevel
                )
            }
        }

        // Tamanho do arquivo
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tamanho do arquivo de log",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = uiState.logSize,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Botão Visualizar Logs
        item {
            Button(
                onClick = {
                    viewModel.loadLogContent()
                    onShowLogViewer()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Visualizar Logs")
            }
        }

        // Botões de ação
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.exportLogs { uri ->
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Compartilhar Logs"))
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Exportar")
                }

                OutlinedButton(
                    onClick = viewModel::clearLogs,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Limpar")
                }
            }
        }

        // Informações adicionais
        item {
            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Diagnóstico",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Os logs contêm informações de auditoria e diagnósticos para auxiliar na resolução de problemas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ============================================
// COMPONENTES AUXILIARES
// ============================================

@Composable
private fun PreferenceCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = FireColors.Surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = FireTypography.Title,
                fontWeight = FontWeight.Bold,
                color = FireColors.OnSurface
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = FireTypography.Caption,
                    color = FireColors.OnSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
private fun SwitchPreferenceCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = FireColors.Surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = FireTypography.Title,
                    fontWeight = FontWeight.Bold,
                    color = FireColors.OnSurface
                )
                Text(
                    text = subtitle,
                    style = FireTypography.Caption,
                    color = FireColors.OnSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = FireColors.Primary,
                    uncheckedThumbColor = FireColors.OnSurfaceVariant,
                    uncheckedTrackColor = FireColors.SurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun InfoBanner(
    message: String,
    type: String,
    onDismiss: () -> Unit
) {
    val (bgColor, textColor) = when (type) {
        "success" -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        else -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fechar",
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SecurityStatusCard(
    isEncrypted: Boolean,
    lastAccess: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEncrypted)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isEncrypted)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isEncrypted) "🔒" else "🔓",
                        fontSize = 18.sp
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (isEncrypted) "Dispositivo Seguro" else "Modo de Acesso Livre",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEncrypted)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Último acesso: $lastAccess",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEncrypted)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================
// SELETORES
// ============================================

@Composable
private fun TemaSelector(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit
) {
    val themes = listOf("Automático", "Claro", "Escuro")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        themes.forEach { theme ->
            val isSelected = theme == selectedTheme
            FilterChip(
                selected = isSelected,
                onClick = { onThemeSelected(theme) },
                label = { Text(theme, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun IdiomaSelector(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    val languages = listOf("Português (BR)", "Inglês (US)", "Espanhol (ES)")
    var expanded by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { expanded = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(selectedLanguage)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        languages.forEach { lang ->
            DropdownMenuItem(
                text = { Text(lang) },
                onClick = {
                    onLanguageSelected(lang)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun DateTimeFormatSelector(
    selectedFormat: String,
    onFormatSelected: (String) -> Unit
) {
    val formats = listOf("DD/MM/YYYY", "MM/DD/YYYY", "YYYY-MM-DD")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        formats.forEach { format ->
            val isSelected = format == selectedFormat
            FilterChip(
                selected = isSelected,
                onClick = { onFormatSelected(format) },
                label = { Text(format, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun UnitSystemSelector(
    selectedSystem: String,
    onSystemSelected: (String) -> Unit
) {
    val systems = listOf("Métrico", "Imperial")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        systems.forEach { system ->
            val isSelected = system == selectedSystem
            FilterChip(
                selected = isSelected,
                onClick = { onSystemSelected(system) },
                label = { Text(system, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FrequencySelector(
    selectedFrequency: String,
    onFrequencySelected: (String) -> Unit
) {
    val frequencies = listOf("Nunca", "Diário", "Semanal", "Mensal")
    var expanded by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { expanded = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(selectedFrequency)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        frequencies.forEach { freq ->
            DropdownMenuItem(
                text = { Text(freq) },
                onClick = {
                    onFrequencySelected(freq)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun LogLevelSelector(
    selectedLevel: String,
    onLevelSelected: (String) -> Unit
) {
    val levels = listOf("DEBUG", "INFO", "WARN", "ERROR")
    var expanded by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { expanded = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Nível: $selectedLevel")
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        levels.forEach { level ->
            DropdownMenuItem(
                text = { Text(level) },
                onClick = {
                    onLevelSelected(level)
                    expanded = false
                }
            )
        }
    }
}

// ============================================
// DIÁLOGOS
// ============================================

@Composable
private fun PinSetupDialog(
    pinValue: String,
    pinConfirmValue: String,
    pinError: String,
    onPinChange: (String) -> Unit,
    onPinConfirmChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Configurar PIN",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Digite um PIN de 4 dígitos para proteger o aplicativo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = pinValue,
                    onValueChange = {
                        if (it.length <= 4) onPinChange(it)
                    },
                    label = { Text("Novo PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text(
                            text = "${pinValue.length}/4 dígitos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                OutlinedTextField(
                    value = pinConfirmValue,
                    onValueChange = {
                        if (it.length <= 4) onPinConfirmChange(it)
                    },
                    label = { Text("Confirmar PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = pinError.isNotEmpty(),
                    supportingText = {
                        if (pinError.isNotEmpty()) {
                            Text(
                                text = pinError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun EraseConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Excluir Todos os Dados?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Esta ação irá apagar permanentemente:",
                    style = MaterialTheme.typography.bodyMedium
                )
                BulletList(
                    items = listOf(
                        "Todas as ocorrências registradas",
                        "Dados de vítimas e veículos",
                        "Arquivos de mídia anexados",
                        "Configurações personalizadas"
                    )
                )
                Text(
                    text = "Esta ação não poderá ser desfeita.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Excluir Tudo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun BulletList(items: List<String>) {
    Column(
        modifier = Modifier.padding(start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { item ->
            Row {
                Text(
                    text = "• ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RestoreDialog(
    backups: List<com.example.firenotes.data.service.DriveFile>,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onRestore: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Restaurar Backup",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                Text(
                    text = "Selecione um backup para restaurar:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Carregando...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (backups.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Nenhum backup encontrado no Drive",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(backups) { file ->
                            BackupItemCard(
                                file = file,
                                onRestore = { onRestore(file.id) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
private fun BackupItemCard(
    file: com.example.firenotes.data.service.DriveFile,
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRestore() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = formatFileSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDateTime(file.createdTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Restore,
                contentDescription = "Restaurar",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LogViewerDialog(
    logContent: String,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Visualizar Logs",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                // Controles rápidos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onExport,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Exportar")
                    }
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Limpar")
                    }
                }

                // Conteúdo do log
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        if (logContent.isEmpty() || logContent == "Nenhum log disponível") {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nenhum log disponível",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            // Scrollable log content
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                reverseLayout = true
                            ) {
                                item {
                                    Text(
                                        text = logContent,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontSize = 10.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

// ============================================
// FUNÇÕES AUXILIARES
// ============================================

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> "${size / (1024 * 1024)} MB"
    }
}

private fun formatDateTime(dateTime: String): String {
    return try {
        val parts = dateTime.split("T")
        val date = parts[0].split("-")
        val time = parts.getOrNull(1)?.substring(0, 5) ?: ""
        "${date[2]}/${date[1]}/${date[0]} $time"
    } catch (_: Exception) {
        dateTime
    }
}