package com.firenotes.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.firenotes.core.common.domain.model.Document
import com.firenotes.core.common.domain.model.DocumentType
import java.time.LocalDateTime

@Entity(
    tableName = "documents",
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
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val occurrenceId: String?,
    val personId: String?,
    val type: DocumentType,
    val filePath: String,
    val rawText: String?,
    val parsedData: String?,
    val createdAt: LocalDateTime
) {
    fun toDomain(): Document {
        return Document(
            id = id,
            occurrenceId = occurrenceId,
            personId = personId,
            type = type,
            filePath = filePath,
            rawText = rawText,
            parsedData = parsedData,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(domain: Document): DocumentEntity {
            return DocumentEntity(
                id = domain.id,
                occurrenceId = domain.occurrenceId,
                personId = domain.personId,
                type = domain.type,
                filePath = domain.filePath,
                rawText = domain.rawText,
                parsedData = domain.parsedData,
                createdAt = domain.createdAt
            )
        }
    }
}
