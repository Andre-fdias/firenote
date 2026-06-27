package com.firenotes.core.common.domain.repository

import com.firenotes.core.common.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow

interface VehicleRepository {
    
    fun getVehiclesForOccurrence(occurrenceId: String): Flow<List<Vehicle>>
    
    fun getVehicleById(id: String): Flow<Vehicle?>
    
    suspend fun saveVehicle(vehicle: Vehicle)
    
    suspend fun deleteVehicle(id: String)
}
