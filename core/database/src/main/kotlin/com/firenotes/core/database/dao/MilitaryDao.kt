package com.firenotes.core.database.dao

import androidx.room.*
import com.firenotes.core.database.entity.MilitaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MilitaryDao {

    @Query("SELECT * FROM military WHERE vehicleId = :vehicleId")
    fun getMilitaryForVehicle(vehicleId: String): Flow<List<MilitaryEntity>>

    @Query("SELECT * FROM military WHERE id = :id")
    suspend fun getMilitaryById(id: String): MilitaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilitary(military: MilitaryEntity)

    @Query("DELETE FROM military WHERE id = :id")
    suspend fun deleteMilitaryById(id: String)
}
