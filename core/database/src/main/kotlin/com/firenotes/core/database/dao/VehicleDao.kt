package com.firenotes.core.database.dao

import androidx.room.*
import com.firenotes.core.database.entity.VehicleEntity
import com.firenotes.core.database.model.VehicleWithMilitary
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {

    @Transaction
    @Query("SELECT * FROM vehicles WHERE occurrenceId = :occurrenceId")
    fun getVehiclesForOccurrence(occurrenceId: String): Flow<List<VehicleWithMilitary>>

    @Transaction
    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun getVehicleById(id: String): Flow<VehicleWithMilitary?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity)

    @Query("DELETE FROM vehicles WHERE id = :id")
    suspend fun deleteVehicleById(id: String)
}
