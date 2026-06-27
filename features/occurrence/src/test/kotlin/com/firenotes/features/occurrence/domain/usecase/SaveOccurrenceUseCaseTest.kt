package com.firenotes.features.occurrence.domain.usecase

import com.firenotes.core.common.domain.model.Occurrence
import com.firenotes.core.common.domain.model.OccurrenceStatus
import com.firenotes.core.common.domain.repository.OccurrenceRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class SaveOccurrenceUseCaseTest {

    private val repository: OccurrenceRepository = mockk(relaxed = true)
    private lateinit var saveOccurrenceUseCase: SaveOccurrenceUseCase

    @Before
    fun setUp() {
        saveOccurrenceUseCase = SaveOccurrenceUseCase(repository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `should return success when occurrence is valid`() = runTest {
        // Arrange
        val occurrence = Occurrence(
            id = "test-uuid",
            internalNumber = "2026-001",
            date = LocalDate.now(),
            dispatchTime = LocalTime.now(),
            arrivalTime = null,
            completionTime = null,
            nature = "Incêndio Residencial",
            observations = "Sem vítimas",
            status = OccurrenceStatus.PENDING_SYNC,
            latitude = null,
            longitude = null,
            address = null,
            number = null,
            complement = null,
            neighborhood = null,
            city = null,
            state = null,
            zipCode = null,
            referencePoint = null
        )
        coEvery { repository.saveOccurrence(any()) } just Runs

        // Act
        val result = saveOccurrenceUseCase(occurrence)

        // Assert
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.saveOccurrence(occurrence) }
    }

    @Test
    fun `should return failure when internalNumber is blank`() = runTest {
        // Arrange
        val occurrence = Occurrence(
            id = "test-uuid",
            internalNumber = "  ",
            date = LocalDate.now(),
            dispatchTime = LocalTime.now(),
            arrivalTime = null,
            completionTime = null,
            nature = "Incêndio Residencial",
            observations = "Sem vítimas",
            status = OccurrenceStatus.PENDING_SYNC,
            latitude = null,
            longitude = null,
            address = null,
            number = null,
            complement = null,
            neighborhood = null,
            city = null,
            state = null,
            zipCode = null,
            referencePoint = null
        )

        // Act
        val result = saveOccurrenceUseCase(occurrence)

        // Assert
        assertTrue(result.isFailure)
        assertEquals("O número interno do atendimento não pode estar em branco.", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.saveOccurrence(any()) }
    }

    @Test
    fun `should return failure when nature is blank`() = runTest {
        // Arrange
        val occurrence = Occurrence(
            id = "test-uuid",
            internalNumber = "2026-001",
            date = LocalDate.now(),
            dispatchTime = LocalTime.now(),
            arrivalTime = null,
            completionTime = null,
            nature = "",
            observations = "Sem vítimas",
            status = OccurrenceStatus.PENDING_SYNC,
            latitude = null,
            longitude = null,
            address = null,
            number = null,
            complement = null,
            neighborhood = null,
            city = null,
            state = null,
            zipCode = null,
            referencePoint = null
        )

        // Act
        val result = saveOccurrenceUseCase(occurrence)

        // Assert
        assertTrue(result.isFailure)
        assertEquals("A natureza da ocorrência deve ser obrigatoriamente especificada.", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.saveOccurrence(any()) }
    }
}
