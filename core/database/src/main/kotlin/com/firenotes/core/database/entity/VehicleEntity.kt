package com.firenotes.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.firenotes.core.common.domain.model.Vehicle
import java.time.LocalTime

@Entity(
    tableName = "vehicles",
    foreignKeys = [
        ForeignKey(
            entity = OccurrenceEntity::class,
            parentColumns = ["id"],
            childColumns = ["occurrenceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["occurrenceId"])]
)
data class VehicleEntity(
    @PrimaryKey
    val id: String,
    val occurrenceId: String,
    val prefix: String,
    val kmDeparture: Double,
    val kmArrival: Double?,
    val kmReturn: Double?,
    val timeDeparture: LocalTime,
    val timeArrival: LocalTime?,
    val timeReturn: LocalTime?,
    val observations: String
) {
    fun toDomain(): Vehicle {
        return Vehicle(
            id = id,
            occurrenceId = occurrenceId,
            prefix = prefix,
            kmDeparture = kmDeparture,
            kmArrival = kmArrival,
            kmReturn = kmReturn,
            timeDeparture = timeDeparture,
            timeArrival = timeArrival,
            timeReturn = timeReturn,
            observations = observations
        )
    }

    companion object {
        fun fromDomain(domain: Vehicle): VehicleEntity {
            return VehicleEntity(
                id = domain.id,
                occurrenceId = domain.occurrenceId,
                prefix = domain.prefix,
                kmDeparture = domain.kmDeparture,
                kmArrival = domain.kmArrival,
                kmReturn = domain.kmReturn,
                timeDeparture = domain.timeDeparture,
                timeArrival = domain.timeArrival,
                timeReturn = domain.timeReturn,
                observations = domain.observations
            )
        }
    }
}
