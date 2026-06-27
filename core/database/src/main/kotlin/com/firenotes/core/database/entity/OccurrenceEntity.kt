package com.firenotes.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.firenotes.core.common.domain.model.Occurrence
import com.firenotes.core.common.domain.model.OccurrenceStatus
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "occurrences")
data class OccurrenceEntity(
    @PrimaryKey
    val id: String,
    val internalNumber: String,
    val date: LocalDate,
    val dispatchTime: LocalTime,
    val arrivalTime: LocalTime?,
    val completionTime: LocalTime?,
    val nature: String,
    val observations: String,
    val status: OccurrenceStatus,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val number: String?,
    val complement: String?,
    val neighborhood: String?,
    val city: String?,
    val state: String?,
    val zipCode: String?,
    val referencePoint: String?
) {
    fun toDomain(): Occurrence {
        return Occurrence(
            id = id,
            internalNumber = internalNumber,
            date = date,
            dispatchTime = dispatchTime,
            arrivalTime = arrivalTime,
            completionTime = completionTime,
            nature = nature,
            observations = observations,
            status = status,
            latitude = latitude,
            longitude = longitude,
            address = address,
            number = number,
            complement = complement,
            neighborhood = neighborhood,
            city = city,
            state = state,
            zipCode = zipCode,
            referencePoint = referencePoint
        )
    }

    companion object {
        fun fromDomain(domain: Occurrence): OccurrenceEntity {
            return OccurrenceEntity(
                id = domain.id,
                internalNumber = domain.internalNumber,
                date = domain.date,
                dispatchTime = domain.dispatchTime,
                arrivalTime = domain.arrivalTime,
                completionTime = domain.completionTime,
                nature = domain.nature,
                observations = domain.observations,
                status = domain.status,
                latitude = domain.latitude,
                longitude = domain.longitude,
                address = domain.address,
                number = domain.number,
                complement = domain.complement,
                neighborhood = domain.neighborhood,
                city = domain.city,
                state = domain.state,
                zipCode = domain.zipCode,
                referencePoint = domain.referencePoint
            )
        }
    }
}
