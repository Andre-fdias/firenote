package com.firenotes.core.database.di

import android.content.Context
import androidx.room.Room
import com.firenotes.core.common.domain.repository.OccurrenceRepository
import com.firenotes.core.database.AppDatabase
import com.firenotes.core.database.dao.OccurrenceDao
import com.firenotes.core.database.repository.OccurrenceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindOccurrenceRepository(
        impl: OccurrenceRepositoryImpl
    ): OccurrenceRepository

    companion object {
        
        @Provides
        @Singleton
        fun provideAppDatabase(
            @ApplicationContext context: Context
        ): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "fire_notes.db"
            )
            .fallbackToDestructiveMigration() // Facilita atualizações na fase de desenvolvimento inicial
            .build()
        }

        @Provides
        @Singleton
        fun provideOccurrenceDao(
            database: AppDatabase
        ): OccurrenceDao {
            return database.occurrenceDao
        }
    }
}
