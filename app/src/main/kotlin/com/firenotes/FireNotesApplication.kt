package com.firenotes

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class FireNotesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializa o Timber Logger apenas no modo de debug
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
