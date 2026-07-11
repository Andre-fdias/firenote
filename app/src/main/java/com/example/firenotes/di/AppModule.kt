package com.example.firenotes.di

import android.content.Context
import android.location.Geocoder
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.firenotes.data.local.AppDatabase
import com.example.firenotes.data.local.dao.OcorrenciaDao
import com.example.firenotes.data.local.dao.HomeOperationalDao
import com.example.firenotes.data.repository.RoomOcorrenciaRepository
import com.example.firenotes.data.service.LocationServiceImpl
import com.example.firenotes.data.service.OcrServiceImpl
import com.example.firenotes.data.service.OpenMeteoWeatherService
import com.example.firenotes.domain.repository.LocationService
import com.example.firenotes.domain.repository.OcrService
import com.example.firenotes.domain.repository.OcorrenciaRepository
import com.example.firenotes.domain.repository.WeatherService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Locale
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
    abstract fun bindWeatherService(
        openMeteoWeatherService: OpenMeteoWeatherService
    ): WeatherService

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

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        dataStoreSettingsRepository: com.example.firenotes.data.repository.DataStoreSettingsRepository
    ): com.example.firenotes.domain.repository.SettingsRepository

    companion object {
        private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "fire_notes_settings")

        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
            return context.settingsDataStore
        }

        @Provides
        @Singleton
        fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
            return LocationServices.getFusedLocationProviderClient(context)
        }

        @Provides
        @Singleton
        fun provideGeocoder(@ApplicationContext context: Context): Geocoder {
            return Geocoder(context, Locale.getDefault())
        }

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

        @Provides
        @Singleton
        fun provideHomeOperationalDao(database: AppDatabase): HomeOperationalDao {
            return database.homeOperationalDao()
        }
    }
}
