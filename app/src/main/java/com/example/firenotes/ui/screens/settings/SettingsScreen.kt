package com.example.firenotes.ui.screens.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.firenotes.MainActivity
import com.example.firenotes.ui.designsystem.colors.FireColors
import com.example.firenotes.ui.designsystem.shapes.FireShapes
import com.example.firenotes.ui.designsystem.spacing.FireSpacing
import com.example.firenotes.ui.designsystem.typography.FireTypography
import com.example.firenotes.ui.designsystem.icons.FireIcons
import com.example.firenotes.ui.designsystem.components.topbar.FireTopBar
import com.example.firenotes.ui.designsystem.components.buttons.*
import com.example.firenotes.ui.designsystem.components.cards.FireCard
import com.example.firenotes.ui.designsystem.components.dialogs.FireDialog
import com.example.firenotes.ui.designsystem.components.widgets.FireDivider
import com.example.firenotes.ui.designsystem.components.inputs.FireOutlinedTextField
import com.example.firenotes.ui.designsystem.components.inputs.FireSwitch
import com.example.firenotes.ui.designsystem.components.inputs.FireRadioButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

private const val LOG_TAG = "FireSettings"
private fun logD(message: String) = android.util.Log.d(LOG_TAG, message)
private fun logE(message: String, throwable: Throwable? = null) = 
    android.util.Log.e(LOG_TAG, message, throwable)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // Google Sign-In Launcher
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
                logE("Erro no login Google", e)
                viewModel.setError("Falha ao conectar: ${e.localizedMessage}")
            }
        }
    }

    // Google Auth Recovery Launcher
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

    // State for dialogs
    var showPinDialog by remember { mutableStateOf(false) }
    var pinValue by remember { mutableStateOf("") }
    var pinConfirmValue by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }
    var showEraseConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            FireTopBar(
                title = "⚙️ Configurações",
                backgroundColor = FireColors.Surface,
                elevation = 2.dp
            )
        },
        containerColor = FireColors.Background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(FireColors.Background)
                .verticalScroll(rememberScrollState())
                .padding(FireSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Large)
        ) {
            // ============================================
            // MENSAGENS
            // ============================================
            AnimatedVisibility(
                visible = uiState.infoMessage != null || uiState.errorMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)) {
                    if (uiState.infoMessage != null) {
                        InfoCard(
                            message = uiState.infoMessage!!,
                            type = "success",
                            onDismiss = viewModel::clearMessages
                        )
                    }
                    if (uiState.errorMessage != null) {
                        InfoCard(
                            message = uiState.errorMessage!!,
                            type = "error",
                            onDismiss = viewModel::clearMessages
                        )
                    }
                }
            }

            // ============================================
            // SEÇÃO 1: SEGURANÇA
            // ============================================
            SectionHeader(title = "🔐 Segurança e Acesso", icon = FireIcons.Lock)
            FireCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FireSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                ) {
                    // PIN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🔑 Acesso por PIN", style = FireTypography.Title, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (uiState.pinEnabled) "✓ PIN ativado" else "Desativado",
                                style = FireTypography.Caption,
                                color = if (uiState.pinEnabled) FireColors.Success else FireColors.OnSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.pinEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    pinValue = ""
                                    pinConfirmValue = ""
                                    pinError = ""
                                    showPinDialog = true
                                } else {
                                    viewModel.updatePin("", false)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = FireColors.Primary,
                                uncheckedTrackColor = FireColors.OnSurfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    }

                    if (uiState.pinEnabled) {
                        FireButton(
                            text = "🔄 Alterar código PIN",
                            onClick = {
                                pinValue = uiState.pinCode
                                pinConfirmValue = ""
                                pinError = ""
                                showPinDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = FireColors.Secondary
                        )
                    }

                    FireDivider()

                    // Biometria
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🔓 Autenticação Biométrica", style = FireTypography.Title, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (uiState.biometricEnabled) "✓ Usando impressão digital ou facial" else "Desativado",
                                style = FireTypography.Caption,
                                color = if (uiState.biometricEnabled) FireColors.Success else FireColors.OnSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.biometricEnabled,
                            onCheckedChange = viewModel::updateBiometric,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = FireColors.Primary,
                                uncheckedTrackColor = FireColors.OnSurfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    }

                    if (uiState.biometricEnabled) {
                        Text(
                            text = "ℹ️ A biometria será solicitada ao abrir o app",
                            style = FireTypography.Caption,
                            color = FireColors.OnSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // ============================================
            // SEÇÃO 2: BACKUP (GOOGLE DRIVE)
            // ============================================
            SectionHeader(title = "☁️ Backup & Restauração", icon = FireIcons.Cloud)
            FireCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FireSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
                ) {
                    // Status da conta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("📧 Google Drive", style = FireTypography.Title, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (uiState.isGoogleConnected) {
                                    "✓ Conectado como: ${uiState.googleAccountName}"
                                } else {
                                    "Não conectado"
                                },
                                style = FireTypography.Caption,
                                color = if (uiState.isGoogleConnected) FireColors.Success else FireColors.OnSurfaceVariant
                            )
                        }
                        if (uiState.isGoogleConnected) {
                            FireButton(
                                text = "Desconectar",
                                onClick = viewModel::disconnectGoogleDrive,
                                containerColor = FireColors.Error,
                                modifier = Modifier
                            )
                        } else {
                            FireButton(
                                text = "🔗 Conectar Drive",
                                onClick = {
                                    val client = viewModel.googleDriveBackupService.getGoogleSignInClient()
                                    googleSignInLauncher.launch(client.signInIntent)
                                },
                                icon = FireIcons.Cloud,
                                modifier = Modifier
                            )
                        }
                    }

                    FireDivider()

                    // Frequência de Backup
                    Column(verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)) {
                        Text("⏰ Periodicidade", style = FireTypography.Label, fontWeight = FontWeight.Bold)
                        var expandedF by remember { mutableStateOf(false) }
                        Box {
                            FireOutlinedButton(
                                text = uiState.config.backupAutomatico,
                                onClick = { expandedF = true },
                                modifier = Modifier.fillMaxWidth(),
                                icon = FireIcons.ArrowDropDown
                            )
                            DropdownMenu(
                                expanded = expandedF,
                                onDismissRequest = { expandedF = false },
                                modifier = Modifier.background(FireColors.Surface)
                            ) {
                                listOf("Nunca", "Diário", "Semanal", "Mensal").forEach { f ->
                                    DropdownMenuItem(
                                        text = { Text(f, style = FireTypography.Body) },
                                        onClick = {
                                            viewModel.updateBackupFrequency(f)
                                            expandedF = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    FireSwitch(
                        checked = uiState.config.backupSomenteWifi,
                        onCheckedChange = viewModel::updateBackupWifiOnly,
                        label = "📶 Apenas em Wi-Fi"
                    )

                    FireDivider()

                    // Último backup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("🕐 Último Backup:", style = FireTypography.Label, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = uiState.config.ultimoBackupData ?: "Pendente",
                            style = FireTypography.Label,
                            color = FireColors.OnSurfaceVariant
                        )
                    }

                    // Botões de ação
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                    ) {
                        FireButton(
                            text = if (uiState.isProcessing) "⏳ Enviando..." else "📤 Backup Agora",
                            onClick = viewModel::performDriveBackup,
                            enabled = !uiState.isProcessing && uiState.isGoogleConnected,
                            icon = FireIcons.CloudUpload,
                            modifier = Modifier.weight(1f)
                        )

                        FireButton(
                            text = if (uiState.isProcessing) "⏳ Carregando..." else "📥 Restaurar",
                            onClick = viewModel::fetchDriveBackups,
                            enabled = !uiState.isProcessing && uiState.isGoogleConnected,
                            containerColor = FireColors.Secondary,
                            icon = FireIcons.CloudDownload,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Progresso
                    if (uiState.isProcessing) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = FireColors.Primary,
                            trackColor = FireColors.SurfaceVariant
                        )
                    }
                }
            }

            // ============================================
            // SEÇÃO 3: APARÊNCIA
            // ============================================
            SectionHeader(title = "🎨 Aparência e Temas", icon = FireIcons.Palette)
            FireCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(FireSpacing.Small)) {
                    listOf("Claro", "Escuro", "Automático").forEach { key ->
                        val themeName = when (key) {
                            "Claro" -> "Claro ☀️"
                            "Escuro" -> "Escuro 🌙"
                            else -> "Automático 🔄"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateTheme(key) }
                                .padding(vertical = FireSpacing.Small, horizontal = FireSpacing.Medium)
                        ) {
                            FireRadioButton(
                                selected = uiState.config.tema == key,
                                onClick = { viewModel.updateTheme(key) },
                                label = themeName
                            )
                        }
                    }
                }
            }

            // ============================================
            // SEÇÃO 4: DADOS
            // ============================================
            SectionHeader(title = "🗑️ Gerenciamento de Dados", icon = FireIcons.Delete)
            FireButton(
                text = "⚠️ EXCLUIR TODOS OS DADOS",
                onClick = { showEraseConfirmDialog = true },
                containerColor = FireColors.Error,
                icon = FireIcons.DeleteForever,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Versão do app
            Text(
                text = "Fire Notes v2.0",
                style = FireTypography.Caption,
                color = FireColors.OnSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    // ============================================
    // DIÁLOGOS
    // ============================================

    // PIN Setup Dialog
    if (showPinDialog) {
        PinSetupDialog(
            pinValue = pinValue,
            pinConfirmValue = pinConfirmValue,
            pinError = pinError,
            onPinChange = { pinValue = it },
            onPinConfirmChange = { pinConfirmValue = it },
            onSave = {
                if (pinValue.length != 4) {
                    pinError = "O PIN deve ter 4 dígitos."
                    return@PinSetupDialog
                }
                if (pinValue != pinConfirmValue) {
                    pinError = "Os PINs não coincidem."
                    return@PinSetupDialog
                }
                viewModel.updatePin(pinValue, true)
                showPinDialog = false
                pinError = ""
            },
            onDismiss = {
                showPinDialog = false
                pinError = ""
            }
        )
    }

    // Erase Confirmation Dialog
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

    // Restore Dialog
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
}

// ============================================
// COMPONENTES AUXILIARES
// ============================================

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FireColors.Primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = FireTypography.Title,
            fontWeight = FontWeight.Bold,
            color = FireColors.Primary
        )
    }
}

@Composable
private fun InfoCard(
    message: String,
    type: String,
    onDismiss: () -> Unit
) {
    val (bgColor, textColor, icon) = when (type) {
        "success" -> Triple(
            Color(0xFFE8F5E9),
            Color(0xFF1B5E20),
            FireIcons.CheckCircle
        )
        else -> Triple(
            Color(0xFFFFEBEE),
            Color(0xFFB71C1C),
            FireIcons.Error
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = bgColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FireSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = textColor,
                style = FireTypography.Body,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    FireIcons.Close,
                    contentDescription = "Fechar",
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

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
    FireDialog(
        onDismissRequest = onDismiss,
        title = "🔑 Configurar PIN",
        confirmButton = {
            FireButton(
                text = "Salvar",
                onClick = onSave,
                containerColor = FireColors.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            FireTextButton(text = "Cancelar", onClick = onDismiss)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            Text(
                text = "Digite um PIN de 4 dígitos para proteger o aplicativo.",
                style = FireTypography.Body,
                color = FireColors.OnSurfaceVariant
            )
            
            FireOutlinedTextField(
                value = pinValue,
                onValueChange = { 
                    if (it.length <= 4) {
                        onPinChange(it)
                    }
                },
                label = "Novo PIN",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            
            FireOutlinedTextField(
                value = pinConfirmValue,
                onValueChange = { 
                    if (it.length <= 4) {
                        onPinConfirmChange(it)
                    }
                },
                label = "Confirmar PIN",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            
            if (pinError.isNotEmpty()) {
                Text(
                    text = "⚠️ $pinError",
                    style = FireTypography.Caption,
                    color = FireColors.Error
                )
            }
        }
    }
}

@Composable
private fun EraseConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    FireDialog(
        onDismissRequest = onDismiss,
        title = "⚠️ Excluir Definitivamente?",
        confirmButton = {
            FireButton(
                text = "SIM, APAGAR TUDO",
                onClick = onConfirm,
                containerColor = FireColors.Error,
                icon = FireIcons.DeleteForever,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            FireTextButton(text = "Cancelar", onClick = onDismiss)
        }
    ) {
        Text(
            text = "Esta ação irá apagar permanentemente todas as ocorrências, vítimas, veículos, arquivos de mídia e configurações armazenados localmente. Esta ação não poderá ser desfeita.",
            style = FireTypography.Body,
            color = FireColors.OnBackground
        )
    }
}

@Composable
private fun RestoreDialog(
    backups: List<com.example.firenotes.data.service.DriveFile>,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onRestore: (String) -> Unit
) {
    FireDialog(
        onDismissRequest = onDismiss,
        title = "📥 Restaurar Backup",
        confirmButton = {
            FireTextButton(text = "Fechar", onClick = onDismiss)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
        ) {
            Text(
                text = "Selecione um backup para restaurar:",
                style = FireTypography.Body,
                color = FireColors.OnSurfaceVariant
            )
            
            if (isProcessing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = FireColors.Primary
                )
            }
            
            if (backups.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = FireColors.SurfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "📭 Nenhum backup encontrado",
                        style = FireTypography.Body,
                        color = FireColors.OnSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                backups.forEach { file ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (!isProcessing) onRestore(file.id) }
                            .shadow(1.dp, RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = FireColors.Surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(FireSpacing.Medium)
                        ) {
                            Text(
                                text = "📄 ${file.name}",
                                style = FireTypography.Body,
                                fontWeight = FontWeight.Bold,
                                color = FireColors.OnBackground
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Tamanho: ${file.size / 1024} KB",
                                    style = FireTypography.Caption,
                                    color = FireColors.OnSurfaceVariant
                                )
                                Text(
                                    text = file.createdTime.take(16).replace("T", " "),
                                    style = FireTypography.Caption,
                                    color = FireColors.OnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
