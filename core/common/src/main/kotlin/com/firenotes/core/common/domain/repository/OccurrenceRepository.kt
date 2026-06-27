package com.firenotes.core.common.domain.repository

import com.firenotes.core.common.domain.model.Occurrence
import com.firenotes.core.common.domain.model.OccurrenceStatus
import kotlinx.coroutines.flow.Flow

interface OccurrenceRepository {
    
    fun getAllOccurrences(): Flow<List<Occurrence>>
    
    fun getOccurrenceById(id: String): Flow<Occurrence?>
    
    fun searchOccurrences(query: String): Flow<List<Occurrence>>
    
    suspend fun getPendingSyncOccurrences(): List<Occurrence>
    
    suspend fun saveOccurrence(occurrence: Occurrence)
    
    suspend fun deleteOccurrence(id: String)
    
    suspend fun updateSyncStatus(id: String, status: OccurrenceStatus)
}
