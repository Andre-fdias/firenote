package com.firenotes.core.database.repository

import com.firenotes.core.common.domain.model.Vehicle
import com.firenotes.core.common.domain.repository.VehicleRepository
import com.firenotes.core.database.dao.VehicleDao
import com.firenotes.core.database.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehicleRepositoryImpl @Inject constructor(
    private val vehicleDao: VehicleDao
) : VehicleRepository {

    override fun getVehiclesForOccurrence(occurrenceId: String): Flow<List<Vehicle>> {
        return vehicleDao.getVehiclesForOccurrence(occurrenceId).map { list ->
            list.map { vm ->
                val v = vm.vehicle.toDomain()
                v.copy(militaryList = vm.militaryList.map { it.toDomain() })
            }
        }
    }

    override fun getVehicleById(id: String): Flow<Vehicle?> {
        return vehicleDao.getVehicleById(id).map { vm ->
            vm?.let {
                val v = it.vehicle.toDomain()
                v.copy(militaryList = it.militaryList.map { it.toDomain() })
            }
        }
    }

    override suspend fun saveVehicle(vehicle: Vehicle) {
        vehicleDao.insertVehicle(VehicleEntity.fromDomain(vehicle))
    }

    override suspend fun deleteVehicle(id: String) {
        vehicleDao.deleteVehicleById(id)
    }
}
