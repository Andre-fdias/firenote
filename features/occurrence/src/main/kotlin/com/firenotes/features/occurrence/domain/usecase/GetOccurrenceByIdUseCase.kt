package com.firenotes.features.occurrence.domain.usecase

import com.firenotes.core.common.domain.model.Occurrence
import com.firenotes.core.common.domain.repository.OccurrenceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetOccurrenceByIdUseCase @Inject constructor(
    private val repository: OccurrenceRepository
) {
    operator fun invoke(id: String): Flow<Occurrence?> {
        return repository.getOccurrenceById(id)
    }
}
