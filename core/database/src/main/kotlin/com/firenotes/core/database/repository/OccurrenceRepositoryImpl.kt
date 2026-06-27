package com.firenotes.core.database.repository

import com.firenotes.core.common.domain.model.Occurrence
import com.firenotes.core.common.domain.model.OccurrenceStatus
import com.firenotes.core.common.domain.repository.OccurrenceRepository
import com.firenotes.core.database.dao.OccurrenceDao
import com.firenotes.core.database.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OccurrenceRepositoryImpl @Inject constructor(
    private val occurrenceDao: OccurrenceDao
) : OccurrenceRepository {

    override fun getAllOccurrences(): Flow<List<Occurrence>> {
        return occurrenceDao.getAllOccurrences().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getOccurrenceById(id: String): Flow<Occurrence?> {
        return occurrenceDao.getOccurrenceById(id).map { it?.toDomain() }
    }

    override fun searchOccurrences(query: String): Flow<List<Occurrence>> {
        return occurrenceDao.searchOccurrences(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getPendingSyncOccurrences(): List<Occurrence> {
        return occurrenceDao.getPendingSyncOccurrences().map { it.toDomain() }
    }

    override suspend fun saveOccurrence(occurrence: Occurrence) {
        val occurrenceEntity = OccurrenceEntity.fromDomain(occurrence)
        
        // Mapear viaturas e militares associados
        val vehicleEntities = mutableListOf<VehicleEntity>()
        val militaryEntities = mutableListOf<MilitaryEntity>()
        occurrence.vehicles.forEach { vehicle ->
            vehicleEntities.add(VehicleEntity.fromDomain(vehicle))
            vehicle.militaryList.forEach { military ->
                militaryEntities.add(MilitaryEntity.fromDomain(military))
            }
        }

        // Mapear envolvidos
        val personEntities = occurrence.people.map { PersonEntity.fromDomain(it) }

        // Mapear mídias locais (garantindo também fotos e documentos associados às pessoas)
        val photoEntities = occurrence.photos.map { PhotoEntity.fromDomain(it) }
        val personPhotos = occurrence.people.flatMap { it.photos }.map { PhotoEntity.fromDomain(it) }
        val allPhotos = (photoEntities + personPhotos).distinctBy { it.id }

        val documentEntities = occurrence.documents.map { DocumentEntity.fromDomain(it) }
        val personDocs = occurrence.people.flatMap { it.documents }.map { DocumentEntity.fromDomain(it) }
        val allDocuments = (documentEntities + personDocs).distinctBy { it.id }

        val audioEntities = occurrence.audios.map { AudioEntity.fromDomain(it) }

        // Executar gravação na DAO usando transação do Room
        occurrenceDao.saveOccurrenceWithDetails(
            occurrence = occurrenceEntity,
            vehicles = vehicleEntities,
            military = militaryEntities,
            people = personEntities,
            photos = allPhotos,
            documents = allDocuments,
            audios = audioEntities
        )
    }

    override suspend fun deleteOccurrence(id: String) {
        occurrenceDao.deleteOccurrenceById(id)
    }

    override suspend fun updateSyncStatus(id: String, status: OccurrenceStatus) {
        occurrenceDao.updateSyncStatus(id, status)
    }
}
