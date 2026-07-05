package com.example.firenotes.data.service

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import com.example.firenotes.domain.repository.CameraCaptureService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraCaptureServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CameraCaptureService {

    override fun createPhotoUri(): Uri {
        try {
            val file = File(context.cacheDir, "camera_capture_${UUID.randomUUID()}.jpg").canonicalFile
            val parent = file.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            val uri = FileProvider.getUriForFile(
                context,
                "com.example.firenotes.fileprovider",
                file
            )
            android.util.Log.d("FireCamera", "Criação do arquivo: Path=${file.absolutePath}, URI=$uri, Authority=com.example.firenotes.fileprovider")
            return uri
        } catch (e: Exception) {
            android.util.Log.e("FireCamera", "Erro ao criar arquivo de captura: ${e.message}", e)
            throw e
        }
    }

    override fun launchCamera(launcher: ActivityResultLauncher<Uri>, uri: Uri) {
        android.util.Log.d("FireCamera", "Launch Camera: URI=$uri")
        launcher.launch(uri)
    }
}
