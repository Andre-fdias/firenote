package com.firenotes.core.database.dao

import androidx.room.*
import com.firenotes.core.common.domain.model.OccurrenceStatus
import com.firenotes.core.database.entity.*
import com.firenotes.core.database.model.OccurrenceWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface OccurrenceDao {

    @Transaction
    @Query("SELECT * FROM occurrences ORDER BY date DESC, dispatchTime DESC")
    fun getAllOccurrences(): Flow<List<OccurrenceWithDetails>>

    @Transaction
    @Query("SELECT * FROM occurrences WHERE id = :id")
    fun getOccurrenceById(id: String): Flow<OccurrenceWithDetails?>

    @Transaction
    @Query("SELECT * FROM occurrences WHERE status = 'PENDING_SYNC'")
    suspend fun getPendingSyncOccurrences(): List<OccurrenceWithDetails>

    /**
     * Pesquisa abrangente em múltiplos campos e tabelas correlacionadas.
     * Permite buscar por nome de envolvido, CPF, rua, número, natureza, data, prefixo de viatura,
     * nome de militar ou texto OCR de documentos.
     */
    @Transaction
    @Query("""
        SELECT DISTINCT o.* FROM occurrences o
        LEFT JOIN people p ON o.id = p.occurrenceId
        LEFT JOIN vehicles v ON o.id = v.occurrenceId
        LEFT JOIN military m ON v.id = m.vehicleId
        LEFT JOIN documents d ON (o.id = d.occurrenceId OR p.id = d.personId)
        WHERE (:query IS NULL OR :query = '') OR (
            o.internalNumber LIKE '%' || :query || '%' OR
            o.nature LIKE '%' || :query || '%' OR
            o.address LIKE '%' || :query || '%' OR
            o.number LIKE '%' || :query || '%' OR
            o.date LIKE '%' || :query || '%' OR
            p.name LIKE '%' || :query || '%' OR
            p.cpf LIKE '%' || :query || '%' OR
            v.prefix LIKE '%' || :query || '%' OR
            m.name LIKE '%' || :query || '%' OR
            d.rawText LIKE '%' || :query || '%'
        )
        ORDER BY o.date DESC, o.dispatchTime DESC
    """)
    fun searchOccurrences(query: String): Flow<List<OccurrenceWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOccurrence(occurrence: OccurrenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(vehicles: List<VehicleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilitary(military: List<MilitaryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeople(people: List<PersonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<DocumentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudios(audios: List<AudioEntity>)

    @Transaction
    suspend fun saveOccurrenceWithDetails(
        occurrence: OccurrenceEntity,
        vehicles: List<VehicleEntity>,
        military: List<MilitaryEntity>,
        people: List<PersonEntity>,
        photos: List<PhotoEntity>,
        documents: List<DocumentEntity>,
        audios: List<AudioEntity>
    ) {
        // Limpar subitens existentes para evitar duplicados ou órfãos durante edições
        deleteVehiclesForOccurrence(occurrence.id)
        deletePeopleForOccurrence(occurrence.id)
        deletePhotosForOccurrence(occurrence.id)
        deleteDocumentsForOccurrence(occurrence.id)
        deleteAudiosForOccurrence(occurrence.id)

        // Inserir os novos dados
        insertOccurrence(occurrence)
        insertVehicles(vehicles)
        insertMilitary(military)
        insertPeople(people)
        insertPhotos(photos)
        insertDocuments(documents)
        insertAudios(audios)
    }

    @Query("DELETE FROM vehicles WHERE occurrenceId = :occurrenceId")
    suspend fun deleteVehiclesForOccurrence(occurrenceId: String)

    @Query("DELETE FROM people WHERE occurrenceId = :occurrenceId")
    suspend fun deletePeopleForOccurrence(occurrenceId: String)

    @Query("DELETE FROM photos WHERE occurrenceId = :occurrenceId")
    suspend fun deletePhotosForOccurrence(occurrenceId: String)

    @Query("DELETE FROM documents WHERE occurrenceId = :occurrenceId")
    suspend fun deleteDocumentsForOccurrence(occurrenceId: String)

    @Query("DELETE FROM audios WHERE occurrenceId = :occurrenceId")
    suspend fun deleteAudiosForOccurrence(occurrenceId: String)

    @Query("DELETE FROM occurrences WHERE id = :id")
    suspend fun deleteOccurrenceById(id: String)
    
    @Query("UPDATE occurrences SET status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: OccurrenceStatus)
}
