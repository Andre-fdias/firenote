package com.firenotes.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.firenotes.core.database.dao.OccurrenceDao
import com.firenotes.core.database.entity.*
import com.firenotes.core.database.util.Converters

@Database(
    entities = [
        OccurrenceEntity::class,
        VehicleEntity::class,
        MilitaryEntity::class,
        PersonEntity::class,
        PhotoEntity::class,
        DocumentEntity::class,
        AudioEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val occurrenceDao: OccurrenceDao
}
