package com.example.firenotes.data.service

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.firenotes.data.local.AppDatabase
import com.example.firenotes.data.local.dao.OcorrenciaDao
import com.example.firenotes.data.local.entities.RoomBackupLog
import com.example.firenotes.data.local.entities.RoomConfiguracao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ocorrenciaDao: OcorrenciaDao
) {

    // --- Compress Database & Files to ZIP ---
    suspend fun createBackupZip(outputFile: File): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            // Close database before zipping to ensure integrity
            AppDatabase.getDatabase(context).close()

            ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zos ->
                // 1. Add Room Database file
                val dbFile = context.getDatabasePath("firenotes.db")
                if (dbFile.exists()) {
                    addToZip(zos, dbFile, "database.db")
                }
                
                // Add shm and wal files if they exist (Room write-ahead logs)
                val shmFile = File(dbFile.path + "-shm")
                if (shmFile.exists()) addToZip(zos, shmFile, "database.db-shm")
                val walFile = File(dbFile.path + "-wal")
                if (walFile.exists()) addToZip(zos, walFile, "database.db-wal")

                // 2. Add FireNotes images and documents folder
                val baseDir = File(context.getExternalFilesDir(null), "FireNotes")
                if (baseDir.exists()) {
                    zipDirectory(zos, baseDir, "FireNotes")
                }
            }
            outputFile.length()
        }
    }

    private fun addToZip(zos: ZipOutputStream, file: File, entryPath: String) {
        FileInputStream(file).use { fis ->
            zos.putNextEntry(ZipEntry(entryPath))
            fis.copyTo(zos)
            zos.closeEntry()
        }
    }

    private fun zipDirectory(zos: ZipOutputStream, dir: File, parentPath: String) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            val entryPath = "$parentPath/${file.name}"
            if (file.isDirectory) {
                zipDirectory(zos, file, entryPath)
            } else {
                addToZip(zos, file, entryPath)
            }
        }
    }

    // --- Write Backup to chosen SAF Directory ---
    suspend fun performBackup(uriString: String, tipo: String): Result<RoomBackupLog> = withContext(Dispatchers.IO) {
        runCatching {
            val treeUri = Uri.parse(uriString)
            val pickedDir = DocumentFile.fromTreeUri(context, treeUri)
                ?: throw IllegalArgumentException("Diretório SAF indisponível")

            // Create local temporary zip file
            val tempZipFile = File(context.cacheDir, "firenotes_backup.zip")
            if (tempZipFile.exists()) tempZipFile.delete()

            val size = createBackupZip(tempZipFile).getOrThrow()

            // Find or create backup file in picked SAF directory
            var backupFile = pickedDir.findFile("firenotes_backup.zip")
            if (backupFile != null) {
                backupFile.delete()
            }
            backupFile = pickedDir.createFile("application/zip", "firenotes_backup.zip")
                ?: throw IOException("Falha ao criar arquivo de backup no SAF")

            // Copy local zip file to SAF document inputstream
            context.contentResolver.openOutputStream(backupFile.uri).use { outputStream ->
                if (outputStream == null) throw IOException("Falha ao abrir stream de escrita do SAF")
                FileInputStream(tempZipFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // Cleanup local temp zip
            tempZipFile.delete()

            // Log Success
            val log = RoomBackupLog(
                id = UUID.randomUUID().toString(),
                dataHora = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                tipo = tipo,
                status = "Sucesso",
                tamanho = size,
                mensagem = "Backup concluído com sucesso."
            )
            ocorrenciaDao.insertBackupLog(log)

            // Update Configuration
            val currentConfig = ocorrenciaDao.getConfiguracao() ?: RoomConfiguracao()
            ocorrenciaDao.insertConfiguracao(
                currentConfig.copy(
                    ultimoBackupData = log.dataHora,
                    ultimoBackupTamanho = log.tamanho,
                    ultimoBackupStatus = "Sucesso"
                )
            )

            log
        }.onFailure { error ->
            val log = RoomBackupLog(
                id = UUID.randomUUID().toString(),
                dataHora = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                tipo = tipo,
                status = "Falha",
                tamanho = 0,
                mensagem = error.localizedMessage
            )
            ocorrenciaDao.insertBackupLog(log)

            val currentConfig = ocorrenciaDao.getConfiguracao() ?: RoomConfiguracao()
            ocorrenciaDao.insertConfiguracao(
                currentConfig.copy(
                    ultimoBackupStatus = "Falha"
                )
            )
        }
    }

    // --- Restore Backup Zip file ---
    suspend fun restoreBackup(backupUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Close DB before overwriting
            AppDatabase.getDatabase(context).close()

            context.contentResolver.openInputStream(backupUri).use { inputStream ->
                if (inputStream == null) throw IOException("Falha ao ler arquivo de backup")
                ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name.startsWith("database.db")) {
                            val dbFile = when (entry.name) {
                                "database.db-shm" -> File(context.getDatabasePath("firenotes.db").path + "-shm")
                                "database.db-wal" -> File(context.getDatabasePath("firenotes.db").path + "-wal")
                                else -> context.getDatabasePath("firenotes.db")
                            }
                            dbFile.parentFile?.mkdirs()
                            FileOutputStream(dbFile).use { fos ->
                                zis.copyTo(fos)
                            }
                        } else if (entry.name.startsWith("FireNotes/")) {
                            val relativePath = entry.name.substringAfter("FireNotes/")
                            val targetFile = File(context.getExternalFilesDir(null), "FireNotes/$relativePath")
                            targetFile.parentFile?.mkdirs()
                            FileOutputStream(targetFile).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
        }
    }

    // --- Auto backup triggers check ---
    suspend fun checkAndTriggerAutoBackup() {
        val config = ocorrenciaDao.getConfiguracao() ?: return
        val uri = config.backupUriSaf ?: return
        if (config.backupAutomatico == "Desativado") return

        val lastBackupStr = config.ultimoBackupData
        val lastDate = if (!lastBackupStr.isNullOrBlank()) {
            try { LocalDate.parse(lastBackupStr, DateTimeFormatter.ofPattern("dd/MM/yyyy")) } catch(e: Exception) { null }
        } else null

        val today = LocalDate.now()
        val shouldBackup = when (config.backupAutomatico) {
            "Diário" -> lastDate == null || lastDate.isBefore(today)
            "Semanal" -> lastDate == null || lastDate.isBefore(today.minusWeeks(1))
            "Mensal" -> lastDate == null || lastDate.isBefore(today.minusMonths(1))
            else -> false
        }

        if (shouldBackup) {
            performBackup(uri, "Automático")
        }
    }
}
