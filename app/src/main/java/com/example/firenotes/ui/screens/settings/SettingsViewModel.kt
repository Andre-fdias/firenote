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
    
    // Security states
    val pinCode: String = "", // PIN code if configured
    val pinEnabled: Boolean = false,
    val biometricEnabled: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val ocorrenciaDao: OcorrenciaDao,
    val googleDriveBackupService: GoogleDriveBackupService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        checkGoogleConnection()
        loadPinSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            ocorrenciaDao.getConfiguracaoFlow().collect { config ->
                config?.let { c ->
                    _uiState.update { it.copy(config = c) }
                }
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
        val prefs = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
        _uiState.update {
            it.copy(
                pinEnabled = prefs.getBoolean("pin_enabled", false),
                pinCode = prefs.getString("pin_code", "") ?: "",
                biometricEnabled = prefs.getBoolean("biometric_enabled", false)
            )
        }
    }

    // --- Security Configuration ---
    fun updatePin(pin: String, enabled: Boolean) {
        val prefs = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("pin_enabled", enabled)
            putString("pin_code", pin)
            apply()
        }
        loadPinSettings()
        _uiState.update { it.copy(infoMessage = "PIN de segurança atualizado.") }
    }

    fun updateBiometric(enabled: Boolean) {
        val prefs = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        loadPinSettings()
        _uiState.update { it.copy(infoMessage = "Configuração de biometria atualizada.") }
    }

    // --- Google Drive Actions ---
    fun connectGoogleDrive(account: GoogleSignInAccount) {
        _uiState.update { 
            it.copy(
                isGoogleConnected = true,
                googleAccountName = account.email
            )
        }
        _uiState.update { it.copy(infoMessage = "Google Drive conectado com sucesso!") }
    }

    fun disconnectGoogleDrive() {
        viewModelScope.launch {
            googleDriveBackupService.getGoogleSignInClient().signOut()
            _uiState.update { 
                it.copy(
                    isGoogleConnected = false,
                    googleAccountName = null,
                    driveBackups = emptyList()
                )
            }
            _uiState.update { it.copy(infoMessage = "Google Drive desconectado.") }
        }
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
            } catch(e: Exception) {
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
                        _uiState.update { it.copy(isProcessing = false, infoMessage = "Backup enviado para o Drive com sucesso! Tamanho: ${log.tamanho / 1024} KB") }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isProcessing = false, errorMessage = "Falha ao enviar backup: ${error.localizedMessage}") }
                    }
            } catch(e: Exception) {
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
                        _uiState.update { it.copy(isProcessing = false, infoMessage = "Restauração concluída! Reiniciando o app...") }
                        onRestored()
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isProcessing = false, errorMessage = "Falha ao restaurar dados: ${error.localizedMessage}") }
                    }
            } catch(e: Exception) {
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

    // --- Clean and Erase All Local Data ---
    fun eraseAllData(onErased: () -> Unit) {
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                // Close DB and delete DB files
                AppDatabase.getDatabase(context).close()
                val dbFile = context.getDatabasePath("firenotes.db")
                if (dbFile.exists()) dbFile.delete()
                File(dbFile.path + "-shm").delete()
                File(dbFile.path + "-wal").delete()

                // Delete local folders documents, vehicles, evidence, reports, temp
                val baseDir = context.getExternalFilesDir(null)
                if (baseDir != null && baseDir.exists()) {
                    baseDir.deleteRecursively()
                }

                // Clear credentials settings
                context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            }.onSuccess {
                _uiState.update { it.copy(isProcessing = false, infoMessage = "Todos os dados foram excluídos do aparelho.") }
                withContext(Dispatchers.Main) { onErased() }
            }.onFailure { error ->
                _uiState.update { it.copy(isProcessing = false, errorMessage = "Falha ao excluir dados: ${error.localizedMessage}") }
            }
        }
    }

    fun dismissRestoreDialog() {
        _uiState.update { it.copy(showRestoreDialog = false) }
    }

    fun updateTheme(tema: String) {
        viewModelScope.launch {
            val newConfig = _uiState.value.config.copy(tema = tema)
            ocorrenciaDao.insertConfiguracao(newConfig)
        }
    }

    fun updateBackupFrequency(frequency: String) {
        viewModelScope.launch {
            val newConfig = _uiState.value.config.copy(backupAutomatico = frequency)
            ocorrenciaDao.insertConfiguracao(newConfig)
        }
    }

    fun updateBackupWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            val newConfig = _uiState.value.config.copy(backupSomenteWifi = wifiOnly)
            ocorrenciaDao.insertConfiguracao(newConfig)
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(infoMessage = null, errorMessage = null) }
    }
}
