package com.firenotes.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.firenotes.core.common.domain.model.Photo
import com.firenotes.core.common.domain.model.PhotoType
import java.time.LocalDateTime

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = OccurrenceEntity::class,
            parentColumns = ["id"],
            childColumns = ["occurrenceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["occurrenceId"]), Index(value = ["personId"])]
)
data class PhotoEntity(
    @PrimaryKey
    val id: String,
    val occurrenceId: String?,
    val personId: String?,
    val type: PhotoType,
    val filePath: String,
    val createdAt: LocalDateTime
) {
    fun toDomain(): Photo {
        return Photo(
            id = id,
            occurrenceId = occurrenceId,
            personId = personId,
            type = type,
            filePath = filePath,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(domain: Photo): PhotoEntity {
            return PhotoEntity(
                id = domain.id,
                occurrenceId = domain.occurrenceId,
                personId = domain.personId,
                type = domain.type,
                filePath = domain.filePath,
                createdAt = domain.createdAt
            )
        }
    }
}
