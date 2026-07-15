package com.example.firenotes.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firenotes.data.local.AppDatabase
import com.example.firenotes.data.local.dao.OcorrenciaDao
import com.example.firenotes.data.local.entities.RoomConfiguracao
import com.example.firenotes.data.service.GoogleDriveBackupService
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val config: RoomConfiguracao = RoomConfiguracao(),
    val isGoogleConnected: Boolean = false,
    val googleAccountName: String? = null,
    val driveBackups: List<com.example.firenotes.data.service.DriveFile> = emptyList(),
    val isProcessing: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val showRestoreDialog: Boolean = false,
    val authRecoveryIntent: android.content.Intent? = null,

    // Local settings
    val idioma: String = "Português (BR)",
    val formatoData: String = "DD/MM/YYYY",
    val sistemaUnidades: String = "Métrico",

    // Security states
    val pinCode: String = "",
    val pinConfirmValue: String? = null,
    val pinError: String? = null,
    val pinEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val lastAccessTime: String? = null,

    // Log Management states
    val logLevel: String = "INFO",
    val logSize: String = "0 KB",
    val logContent: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val ocorrenciaDao: OcorrenciaDao,
    val googleDriveBackupService: GoogleDriveBackupService,
    private val settingsRepository: com.example.firenotes.domain.repository.SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        checkGoogleConnection()
        loadPinSettings()
        calculateLogSize()
        loadLastAccessTime()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            ocorrenciaDao.getConfiguracaoFlow().collect { config ->
                config?.let { c ->
                    _uiState.update { it.copy(config = c) }
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.languageFlow.collect { lang ->
                _uiState.update { it.copy(idioma = lang) }
            }
        }
        viewModelScope.launch {
            settingsRepository.dateFormatFlow.collect { format ->
                _uiState.update { it.copy(formatoData = format) }
            }
        }
        viewModelScope.launch {
            settingsRepository.unitSystemFlow.collect { system ->
                _uiState.update { it.copy(sistemaUnidades = system) }
            }
        }
    }

    private fun checkGoogleConnection() {
        val account = googleDriveBackupService.getLastSignedInAccount()
        _uiState.update {
            it.copy(
                isGoogleConnected = account != null,
                googleAccountName = account?.email
            )
        }
    }

    private fun loadPinSettings() {
        viewModelScope.launch {
            settingsRepository.pinEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(pinEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.pinCodeFlow.collect { code ->
                _uiState.update { it.copy(pinCode = code) }
            }
        }
        viewModelScope.launch {
            settingsRepository.biometricEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(biometricEnabled = enabled) }
            }
        }
    }

    private fun loadLastAccessTime() {
        val prefs = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
        val lastAccess = prefs.getString("last_access_time", null)
        _uiState.update { it.copy(lastAccessTime = lastAccess) }
    }

    // --- PIN Management ---
    fun updatePinCode(pin: String) {
        _uiState.update { it.copy(pinCode = pin, pinError = null) }
    }

    fun updatePinConfirm(pin: String) {
        _uiState.update { it.copy(pinConfirmValue = pin, pinError = null) }
    }

    fun savePin() {
        val state = _uiState.value
        if (state.pinCode.length != 4) {
            _uiState.update { it.copy(pinError = "O PIN deve ter exatamente 4 dígitos.") }
            return
        }
        if (state.pinCode != state.pinConfirmValue) {
            _uiState.update { it.copy(pinError = "Os PINs não coincidem.") }
            return
        }
        updatePin(state.pinCode, true)
    }

    fun clearPinError() {
        _uiState.update { it.copy(pinError = null) }
    }

    fun updatePin(pin: String, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPinCode(pin)
            settingsRepository.setPinEnabled(enabled)
            _uiState.update {
                it.copy(
                    infoMessage = if (enabled) "PIN de segurança ativado com sucesso." else "PIN desativado.",
                    pinCode = pin,
                    pinEnabled = enabled,
                    pinConfirmValue = null,
                    pinError = null
                )
            }
        }
    }

    fun updateBiometric(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricEnabled(enabled)
            _uiState.update {
                it.copy(
                    infoMessage = if (enabled) "Autenticação biométrica ativada." else "Biometria desativada.",
                    biometricEnabled = enabled
                )
            }
        }
    }

    // --- Google Drive Actions ---
    fun connectGoogleDrive(account: GoogleSignInAccount) {
        _uiState.update {
            it.copy(
                isGoogleConnected = true,
                googleAccountName = account.email,
                infoMessage = "Google Drive conectado com sucesso!"
            )
        }
    }

    fun disconnectGoogleDrive() {
        viewModelScope.launch {
            googleDriveBackupService.getGoogleSignInClient().signOut()
            _uiState.update {
                it.copy(
                    isGoogleConnected = false,
                    googleAccountName = null,
                    driveBackups = emptyList(),
                    infoMessage = "Google Drive desconectado."
                )
            }
        }
    }

    fun clearRecoveryIntent() {
        _uiState.update { it.copy(authRecoveryIntent = null) }
    }

    fun fetchDriveBackups() {
        val account = googleDriveBackupService.getLastSignedInAccount()
        if (account == null) {
            _uiState.update { it.copy(errorMessage = "Conecte sua conta do Google Drive primeiro.") }
            return
        }

        _uiState.update { it.copy(isProcessing = true, infoMessage = null, errorMessage = null) }
        viewModelScope.launch {
            try {
                val token = getGoogleAccessToken(account)
                googleDriveBackupService.listBackupsFromDrive(token)
                    .onSuccess { list ->
                        _uiState.update { it.copy(driveBackups = list, showRestoreDialog = true, isProcessing = false) }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isProcessing = false, errorMessage = "Falha ao listar backups: ${error.localizedMessage}") }
                    }
            } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
                _uiState.update { it.copy(isProcessing = false, authRecoveryIntent = e.intent, errorMessage = "Permissão do Google Drive necessária.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, errorMessage = "Erro de autenticação do Google: ${e.localizedMessage}") }
            }
        }
    }

    fun performDriveBackup() {
        val account = googleDriveBackupService.getLastSignedInAccount()
        if (account == null) {
            _uiState.update { it.copy(errorMessage = "Conecte sua conta do Google Drive primeiro.") }
            return
        }

        _uiState.update { it.copy(isProcessing = true, infoMessage = null, errorMessage = null) }
        viewModelScope.launch {
            try {
                val token = getGoogleAccessToken(account)
                googleDriveBackupService.uploadBackupToDrive(token)
                    .onSuccess { log ->
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                infoMessage = "Backup enviado para o Drive com sucesso!",
                                config = it.config.copy(ultimoBackupData = java.time.LocalDateTime.now().toString())
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isProcessing = false, errorMessage = "Falha ao enviar backup: ${error.localizedMessage}") }
                    }
            } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
                _uiState.update { it.copy(isProcessing = false, authRecoveryIntent = e.intent, errorMessage = "Permissão do Google Drive necessária.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, errorMessage = "Erro ao acessar o Drive: ${e.localizedMessage}") }
            }
        }
    }

    fun restoreDriveBackup(fileId: String, onRestored: () -> Unit) {
        val account = googleDriveBackupService.getLastSignedInAccount()
        if (account == null) return

        _uiState.update { it.copy(isProcessing = true, showRestoreDialog = false) }
        viewModelScope.launch {
            try {
                val token = getGoogleAccessToken(account)
                googleDriveBackupService.restoreBackupFromDrive(token, fileId)
                    .onSuccess {
                        _uiState.update { it.copy(isProcessing = false, infoMessage = "Restauração concluída!") }
                        onRestored()
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isProcessing = false, errorMessage = "Falha ao restaurar dados: ${error.localizedMessage}") }
                    }
            } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
                _uiState.update { it.copy(isProcessing = false, authRecoveryIntent = e.intent, errorMessage = "Permissão do Google Drive necessária.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, errorMessage = "Erro de restauração: ${e.localizedMessage}") }
            }
        }
    }

    private suspend fun getGoogleAccessToken(account: GoogleSignInAccount): String = withContext(Dispatchers.IO) {
        GoogleAuthUtil.getToken(
            context,
            account.account ?: throw IllegalStateException("Conta sem e-mail do sistema"),
            "oauth2:https://www.googleapis.com/auth/drive.appdata"
        )
    }

    fun dismissRestoreDialog() {
        _uiState.update { it.copy(showRestoreDialog = false) }
    }

    // --- Configurações Gerais ---
    fun updateTheme(tema: String) {
        viewModelScope.launch {
            settingsRepository.setTheme(tema)
            val newConfig = _uiState.value.config.copy(tema = tema)
            ocorrenciaDao.insertConfiguracao(newConfig)
            _uiState.update { it.copy(infoMessage = "Tema atualizado para: $tema") }
        }
    }

    fun updateLanguage(idioma: String) {
        viewModelScope.launch {
            settingsRepository.setLanguage(idioma)
            _uiState.update { it.copy(infoMessage = "Idioma atualizado.") }
        }
    }

    fun updateDateTimeFormat(format: String) {
        viewModelScope.launch {
            settingsRepository.setDateFormat(format)
            _uiState.update { it.copy(infoMessage = "Formato de data atualizado.") }
        }
    }

    fun updateUnitSystem(system: String) {
        viewModelScope.launch {
            settingsRepository.setUnitSystem(system)
            _uiState.update { it.copy(infoMessage = "Sistema de unidades atualizado.") }
        }
    }

    fun updateBackupFrequency(frequency: String) {
        viewModelScope.launch {
            val newConfig = _uiState.value.config.copy(backupAutomatico = frequency)
            ocorrenciaDao.insertConfiguracao(newConfig)
            _uiState.update { it.copy(infoMessage = "Frequência de backup atualizada.") }
        }
    }

    fun updateBackupWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            val newConfig = _uiState.value.config.copy(backupSomenteWifi = wifiOnly)
            ocorrenciaDao.insertConfiguracao(newConfig)
            _uiState.update { it.copy(infoMessage = if (wifiOnly) "Backup apenas em Wi-Fi ativado." else "Backup em qualquer rede ativado.") }
        }
    }

    // --- Log Management ---
    private fun calculateLogSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val logFile = File(context.cacheDir, "firenotes_logs.txt")
            val sizeStr = if (logFile.exists()) {
                val sizeBytes = logFile.length()
                when {
                    sizeBytes > 1024 * 1024 -> "%.2f MB".format(sizeBytes.toFloat() / (1024 * 1024))
                    sizeBytes > 1024 -> "${sizeBytes / 1024} KB"
                    else -> "$sizeBytes B"
                }
            } else {
                "0 B"
            }
            _uiState.update { it.copy(logSize = sizeStr) }
        }
    }

    fun loadLogContent() {
        viewModelScope.launch(Dispatchers.IO) {
            val logFile = File(context.cacheDir, "firenotes_logs.txt")
            val content = if (logFile.exists()) {
                logFile.readText().take(50000) // Limite para performance
            } else {
                "Nenhum log disponível"
            }
            _uiState.update { it.copy(logContent = content) }
        }
    }

    fun clearLogContent() {
        _uiState.update { it.copy(logContent = null) }
    }

    fun updateLogLevel(level: String) {
        _uiState.update {
            it.copy(
                logLevel = level,
                infoMessage = "Nível de log alterado para: $level"
            )
        }
        // Log da mudança
        appendLog("INFO", "Nível de log alterado para $level")
    }

    private fun appendLog(level: String, message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val logFile = File(context.cacheDir, "firenotes_logs.txt")
                val timestamp = java.time.LocalDateTime.now().toString()
                val entry = "[$timestamp] [$level] $message\n"
                logFile.appendText(entry)
                calculateLogSize()
            } catch (_: Exception) { }
        }
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            val logFile = File(context.cacheDir, "firenotes_logs.txt")
            if (logFile.exists()) logFile.delete()
            _uiState.update {
                it.copy(
                    logSize = "0 B",
                    infoMessage = "Arquivo de logs limpo.",
                    logContent = null
                )
            }
        }
    }

    fun exportLogs(onShared: (Uri) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val logFile = File(context.cacheDir, "firenotes_logs.txt")
                if (!logFile.exists()) {
                    logFile.writeText("FIRE NOTES - LOG DE AUDITORIA\n")
                    logFile.appendText("Inicializado em: ${java.time.LocalDateTime.now()}\n")
                }

                // Adiciona metadados
                logFile.appendText("\n--- METADADOS DO SISTEMA ---\n")
                logFile.appendText("Versão: 8.3\n")
                logFile.appendText("Nível de Log: ${_uiState.value.logLevel}\n")
                logFile.appendText("PIN Ativo: ${_uiState.value.pinEnabled}\n")
                logFile.appendText("Biometria Ativa: ${_uiState.value.biometricEnabled}\n")
                logFile.appendText("Backup Automático: ${_uiState.value.config.backupAutomatico}\n")
                logFile.appendText("----------------------------\n\n")

                val logUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "com.example.firenotes.fileprovider",
                    logFile
                )
                withContext(Dispatchers.Main) {
                    onShared(logUri)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Erro ao exportar logs: ${e.localizedMessage}") }
            }
        }
    }

    // --- Erase All Data ---
    fun eraseAllData(onErased: () -> Unit) {
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                AppDatabase.closeDatabase()
                val dbFile = context.getDatabasePath("firenotes.db")
                if (dbFile.exists()) dbFile.delete()
                File(dbFile.path + "-shm").delete()
                File(dbFile.path + "-wal").delete()

                val baseDir = context.getExternalFilesDir(null)
                if (baseDir != null && baseDir.exists()) {
                    baseDir.deleteRecursively()
                }

                settingsRepository.setPinEnabled(false)
                settingsRepository.setPinCode("")
                settingsRepository.setBiometricEnabled(false)
                settingsRepository.setTheme("Automático")
                context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE).edit().clear().apply()

                // Log do evento
                appendLog("WARN", "Todos os dados foram apagados por comando do usuário")
            }.onSuccess {
                _uiState.update { it.copy(isProcessing = false, infoMessage = "Todos os dados foram excluídos.") }
                withContext(Dispatchers.Main) { onErased() }
            }.onFailure { error ->
                _uiState.update { it.copy(isProcessing = false, errorMessage = "Falha ao excluir dados: ${error.localizedMessage}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(infoMessage = null, errorMessage = null) }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }
}