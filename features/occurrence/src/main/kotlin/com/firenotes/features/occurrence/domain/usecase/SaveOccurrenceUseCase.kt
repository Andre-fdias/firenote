package com.firenotes.features.occurrence.domain.usecase

import com.firenotes.core.common.domain.model.Occurrence
import com.firenotes.core.common.domain.repository.OccurrenceRepository
import javax.inject.Inject

class SaveOccurrenceUseCase @Inject constructor(
    private val repository: OccurrenceRepository
) {
    suspend operator fun invoke(occurrence: Occurrence): Result<Unit> {
        return try {
            // Regras de negócio essenciais
            if (occurrence.internalNumber.isBlank()) {
                return Result.failure(IllegalArgumentException("O número interno do atendimento não pode estar em branco."))
            }
            if (occurrence.nature.isBlank()) {
                return Result.failure(IllegalArgumentException("A natureza da ocorrência deve ser obrigatoriamente especificada."))
            }
            repository.saveOccurrence(occurrence)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
