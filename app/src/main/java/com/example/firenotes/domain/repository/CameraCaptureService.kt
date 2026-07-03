package com.example.firenotes.domain.repository

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher

interface CameraCaptureService {
    fun createPhotoUri(): Uri
    fun launchCamera(launcher: ActivityResultLauncher<Uri>, uri: Uri)
}
