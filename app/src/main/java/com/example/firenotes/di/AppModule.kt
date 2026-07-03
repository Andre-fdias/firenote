package com.example.firenotes.di

import android.content.Context
import com.example.firenotes.data.local.AppDatabase
import com.example.firenotes.data.local.dao.OcorrenciaDao
import com.example.firenotes.data.repository.RoomOcorrenciaRepository
import com.example.firenotes.data.service.LocationServiceImpl
import com.example.firenotes.data.service.OcrServiceImpl
import com.example.firenotes.domain.repository.LocationService
import com.example.firenotes.domain.repository.OcrService
import com.example.firenotes.domain.repository.OcorrenciaRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindOcorrenciaRepository(
        roomOcorrenciaRepository: RoomOcorrenciaRepository
    ): OcorrenciaRepository

    @Binds
    @Singleton
    abstract fun bindLocationService(
        locationServiceImpl: LocationServiceImpl
    ): LocationService

    @Binds
    @Singleton
    abstract fun bindOcrService(
        ocrServiceImpl: OcrServiceImpl
    ): OcrService

    @Binds
    @Singleton
    abstract fun bindCameraCaptureService(
        cameraCaptureServiceImpl: com.example.firenotes.data.service.CameraCaptureServiceImpl
    ): com.example.firenotes.domain.repository.CameraCaptureService

    @Binds
    @Singleton
    abstract fun bindImageProcessingService(
        imageProcessingServiceImpl: com.example.firenotes.data.service.ImageProcessingServiceImpl
    ): com.example.firenotes.domain.repository.ImageProcessingService

    companion object {

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
            return AppDatabase.getDatabase(context)
        }

        @Provides
        @Singleton
        fun provideOcorrenciaDao(database: AppDatabase): OcorrenciaDao {
            return database.ocorrenciaDao()
        }
    }
}
