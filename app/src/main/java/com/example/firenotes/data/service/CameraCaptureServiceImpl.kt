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
        val file = File(context.cacheDir, "camera_capture_${UUID.randomUUID()}.jpg")
        val parent = file.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        return FileProvider.getUriForFile(
            context,
            "com.example.firenotes.fileprovider",
            file
        )
    }

    override fun launchCamera(launcher: ActivityResultLauncher<Uri>, uri: Uri) {
        launcher.launch(uri)
    }
}
