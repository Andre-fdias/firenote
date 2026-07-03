package com.example.firenotes.domain.usecase

import com.example.firenotes.domain.model.Ocorrencia
import com.example.firenotes.domain.repository.OcorrenciaRepository
import javax.inject.Inject

class CriarOcorrenciaUseCase @Inject constructor(
    private val repository: OcorrenciaRepository
) {
    suspend operator fun invoke(ocorrencia: Ocorrencia): Result<Ocorrencia> {
        if (ocorrencia.protocolo.isBlank()) {
            return Result.failure(IllegalArgumentException("O protocolo não pode ser vazio."))
        }
        return repository.createOcorrencia(ocorrencia)
    }
}
