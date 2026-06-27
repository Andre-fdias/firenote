package com.firenotes.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.firenotes.core.common.domain.model.Occurrence
import com.firenotes.core.database.entity.AudioEntity
import com.firenotes.core.database.entity.DocumentEntity
import com.firenotes.core.database.entity.OccurrenceEntity
import com.firenotes.core.database.entity.PersonEntity
import com.firenotes.core.database.entity.PhotoEntity
import com.firenotes.core.database.entity.VehicleEntity

data class OccurrenceWithDetails(
    @Embedded val occurrence: OccurrenceEntity,

    @Relation(
        entity = VehicleEntity::class,
        parentColumn = "id",
        entityColumn = "occurrenceId"
    )
    val vehicles: List<VehicleWithMilitary>,

    @Relation(
        parentColumn = "id",
        entityColumn = "occurrenceId"
    )
    val people: List<PersonEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "occurrenceId"
    )
    val photos: List<PhotoEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "occurrenceId"
    )
    val documents: List<DocumentEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "occurrenceId"
    )
    val audios: List<AudioEntity>
) {
    fun toDomain(): Occurrence {
        val mappedVehicles = vehicles.map { vm ->
            val vDomain = vm.vehicle.toDomain()
            vDomain.copy(
                militaryList = vm.militaryList.map { it.toDomain() }
            )
        }

        return occurrence.toDomain().copy(
            photos = photos.map { it.toDomain() },
            audios = audios.map { it.toDomain() },
            documents = documents.map { it.toDomain() },
            vehicles = mappedVehicles,
            people = people.map { pEntity ->
                // Filtrar fotos e documentos associados a esta pessoa específica
                val pPhotos = photos.filter { it.personId == pEntity.id }.map { it.toDomain() }
                val pDocs = documents.filter { it.personId == pEntity.id }.map { it.toDomain() }
                pEntity.toDomain().copy(
                    photos = pPhotos,
                    documents = pDocs
                )
            }
        )
    }
}
