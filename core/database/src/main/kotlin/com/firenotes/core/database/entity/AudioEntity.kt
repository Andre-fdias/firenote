package com.firenotes.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.firenotes.core.common.domain.model.Audio
import java.time.LocalDateTime

@Entity(
    tableName = "audios",
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
data class AudioEntity(
    @PrimaryKey
    val id: String,
    val occurrenceId: String,
    val filePath: String,
    val durationMs: Long,
    val createdAt: LocalDateTime
) {
    fun toDomain(): Audio {
        return Audio(
            id = id,
            occurrenceId = occurrenceId,
            filePath = filePath,
            durationMs = durationMs,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(domain: Audio): AudioEntity {
            return AudioEntity(
                id = domain.id,
                occurrenceId = domain.occurrenceId,
                filePath = domain.filePath,
                durationMs = domain.durationMs,
                createdAt = domain.createdAt
            )
        }
    }
}
