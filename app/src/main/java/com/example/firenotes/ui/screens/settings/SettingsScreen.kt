package com.example.firenotes.ui.screens.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // Google Sign-In Intent launcher
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
                // error login
            }
        }
    }

    var showPinDialog by remember { mutableStateOf(false) }
    var pinValue by remember { mutableStateOf("") }
    var showEraseConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            FireTopBar(title = "Configurações do Sistema")
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(FireSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(FireSpacing.Large)
        ) {
            // General Info and Messages using FireCard
            if (uiState.infoMessage != null) {
                FireCard(
                    containerColor = Color(0xFFE8F5E9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(FireSpacing.Medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = FireIcons.CheckCircle, contentDescription = "Sucesso", tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(FireSpacing.MediumSmall))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(uiState.infoMessage!!, color = Color(0xFF1B5E20), style = FireTypography.Body, fontWeight = FontWeight.Medium)
                        }
                        FireIconButton(icon = FireIcons.Close, onClick = viewModel::clearMessages, tint = Color(0xFF2E7D32))
                    }
                }
            }

            if (uiState.errorMessage != null) {
                FireCard(
                    containerColor = Color(0xFFFFEBEE),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(FireSpacing.Medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = FireIcons.Error, contentDescription = "Erro", tint = Color(0xFFC62828))
                        Spacer(modifier = Modifier.width(FireSpacing.MediumSmall))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(uiState.errorMessage!!, color = Color(0xFFB71C1C), style = FireTypography.Body, fontWeight = FontWeight.Medium)
                        }
                        FireIconButton(icon = FireIcons.Close, onClick = viewModel::clearMessages, tint = Color(0xFFC62828))
                    }
                }
            }

            // --- SECTION 1: SEGURANÇA ---
            Text("Segurança e Acesso", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColors.Primary)
            FireCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(FireSpacing.Medium), verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Acesso por PIN", style = FireTypography.Title, fontWeight = FontWeight.Bold)
                            Text(text = if (uiState.pinEnabled) "PIN ativado" else "Desativado", style = FireTypography.Caption, color = Color.Gray)
                        }
                        Switch(
                            checked = uiState.pinEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    pinValue = ""
                                    showPinDialog = true
                                } else {
                                    viewModel.updatePin("", false)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = FireColors.Primary)
                        )
                    }

                    if (uiState.pinEnabled) {
                        FireButton(
                            text = "Alterar código PIN",
                            onClick = {
                                pinValue = uiState.pinCode
                                showPinDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    FireDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Autenticação Biométrica", style = FireTypography.Title, fontWeight = FontWeight.Bold)
                            Text("Usar impressão digital ou reconhecimento facial.", style = FireTypography.Caption, color = Color.Gray)
                        }
                        Switch(
                            checked = uiState.biometricEnabled,
                            onCheckedChange = viewModel::updateBiometric,
                            colors = SwitchDefaults.colors(checkedTrackColor = FireColors.Primary)
                        )
                    }
                }
            }

            // --- SECTION 2: BACKUP (GOOGLE DRIVE) ---
            Text("Backup & Restauração (Google Drive)", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColors.Primary)
            FireCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(FireSpacing.Medium), verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)) {
                    // Drive Account Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Google Drive", style = FireTypography.Title, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (uiState.isGoogleConnected) "Conectado como: ${uiState.googleAccountName}" else "Não conectado",
                                style = FireTypography.Caption,
                                color = Color.Gray
                            )
                        }
                        if (uiState.isGoogleConnected) {
                            FireButton(
                                text = "Desconectar",
                                onClick = viewModel::disconnectGoogleDrive,
                                containerColor = FireColors.Error
                            )
                        } else {
                            FireButton(
                                text = "Conectar Drive",
                                onClick = {
                                    val client = viewModel.googleDriveBackupService.getGoogleSignInClient()
                                    googleSignInLauncher.launch(client.signInIntent)
                                }
                            )
                        }
                    }

                    FireDivider()

                    // Auto Backup Frequency
                    Column(verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)) {
                        Text("Periodicidade do Backup", style = FireTypography.Label, fontWeight = FontWeight.Bold)
                        var expandedF by remember { mutableStateOf(false) }
                        Box {
                            FireOutlinedButton(
                                text = uiState.config.backupAutomatico,
                                onClick = { expandedF = true },
                                modifier = Modifier.fillMaxWidth(),
                                icon = FireIcons.ArrowDropDown
                            )
                            DropdownMenu(expanded = expandedF, onDismissRequest = { expandedF = false }) {
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
                        label = "Apenas em conexões Wi-Fi"
                    )

                    FireDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Último Backup Realizado:", style = FireTypography.Label, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = uiState.config.ultimoBackupData ?: "Pendente",
                            style = FireTypography.Label,
                            color = Color.DarkGray
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FireSpacing.Small)
                    ) {
                        FireButton(
                            text = "Backup Agora",
                            onClick = viewModel::performDriveBackup,
                            enabled = !uiState.isProcessing && uiState.isGoogleConnected,
                            icon = FireIcons.CloudUpload,
                            modifier = Modifier.weight(1f)
                        )

                        FireButton(
                            text = "Restaurar",
                            onClick = viewModel::fetchDriveBackups,
                            enabled = !uiState.isProcessing && uiState.isGoogleConnected,
                            containerColor = FireColors.Secondary,
                            icon = FireIcons.CloudDownload,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // --- SECTION 3: APARÊNCIA ---
            Text("Aparência e Temas", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColors.Primary)
            FireCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(FireSpacing.Small)) {
                    listOf("Claro", "Escuro", "Automático").forEach { themeName ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateTheme(themeName) }
                                .padding(vertical = FireSpacing.Small, horizontal = FireSpacing.Medium)
                        ) {
                            FireRadioButton(
                                selected = uiState.config.tema == themeName,
                                onClick = { viewModel.updateTheme(themeName) },
                                label = themeName
                            )
                        }
                    }
                }
            }

            // --- SECTION 4: EXCLUSÃO DE DADOS ---
            Text("Limpeza de Dados", style = FireTypography.Title, fontWeight = FontWeight.Bold, color = FireColors.Primary)
            FireButton(
                text = "EXCLUIR TODOS OS DADOS DO APARELHO",
                onClick = { showEraseConfirmDialog = true },
                containerColor = FireColors.Error,
                icon = FireIcons.DeleteForever,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // PIN Setup Dialog
    if (showPinDialog) {
        FireDialog(
            onDismissRequest = { showPinDialog = false },
            title = "Configurar código PIN",
            confirmButton = {
                FireButton(
                    text = "Salvar",
                    onClick = {
                        if (pinValue.length == 4) {
                            viewModel.updatePin(pinValue, true)
                            showPinDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                FireTextButton(text = "Cancelar", onClick = { showPinDialog = false })
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(FireSpacing.Small)) {
                Text("Digite um PIN de 4 dígitos para proteger o aplicativo.", style = FireTypography.Body)
                FireOutlinedTextField(
                    value = pinValue,
                    onValueChange = { if (it.length <= 4) pinValue = it },
                    label = "Código PIN",
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Erase Confirmation Dialog
    if (showEraseConfirmDialog) {
        FireDialog(
            onDismissRequest = { showEraseConfirmDialog = false },
            title = "Excluir Definitivamente?",
            confirmButton = {
                FireButton(
                    text = "SIM, APAGAR TUDO",
                    onClick = {
                        showEraseConfirmDialog = false
                        viewModel.eraseAllData {
                            // Restart application
                            activity?.let {
                                val intent = Intent(it, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                }
                                it.startActivity(intent)
                                Runtime.getRuntime().exit(0)
                            }
                        }
                    },
                    containerColor = FireColors.Error
                )
            },
            dismissButton = {
                FireTextButton(text = "Cancelar", onClick = { showEraseConfirmDialog = false })
            }
        ) {
            Text(
                text = "Esta ação irá apagar permanentemente todas as ocorrências, vítimas, veículos, arquivos de mídia e configurações armazenados localmente no aparelho. Esta ação não poderá ser desfeita.",
                style = FireTypography.Body
            )
        }
    }

    // Restore File selection from Google Drive Dialog
    if (uiState.showRestoreDialog) {
        FireDialog(
            onDismissRequest = viewModel::dismissRestoreDialog,
            title = "Restaurar Backup do Drive",
            confirmButton = {
                FireTextButton(text = "Fechar", onClick = viewModel::dismissRestoreDialog)
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FireSpacing.Medium)
            ) {
                Text("Selecione um arquivo de backup para restaurar:", style = FireTypography.Body)
                if (uiState.driveBackups.isEmpty()) {
                    Text("Nenhum backup encontrado na pasta do aplicativo no Google Drive.", style = FireTypography.Body, color = Color.Gray)
                } else {
                    uiState.driveBackups.forEach { file ->
                        FireCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.restoreDriveBackup(file.id) {
                                        // Restart Activity on successful restore to reload DB instance
                                        activity?.let {
                                            val intent = Intent(it, MainActivity::class.java).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                            }
                                            it.startActivity(intent)
                                            Runtime.getRuntime().exit(0)
                                        }
                                    }
                                }
                        ) {
                            Column(modifier = Modifier.padding(FireSpacing.Medium)) {
                                Text(file.name, style = FireTypography.Body, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Tamanho: ${file.size / 1024} KB", style = FireTypography.Caption, color = Color.Gray)
                                    Text(file.createdTime.take(16).replace("T", " "), style = FireTypography.Caption, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
