package com.firenotes.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.firenotes.core.common.domain.model.Military

@Entity(
    tableName = "military",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["vehicleId"])]
)
data class MilitaryEntity(
    @PrimaryKey
    val id: String,
    val vehicleId: String,
    val name: String,
    val re: String,
    val rank: String,
    val role: String,
    val phone: String?
) {
    fun toDomain(): Military {
        return Military(
            id = id,
            vehicleId = vehicleId,
            name = name,
            re = re,
            rank = rank,
            role = role,
            phone = phone
        )
    }

    companion object {
        fun fromDomain(domain: Military): MilitaryEntity {
            return MilitaryEntity(
                id = domain.id,
                vehicleId = domain.vehicleId,
                name = domain.name,
                re = domain.re,
                rank = domain.rank,
                role = domain.role,
                phone = domain.phone
            )
        }
    }
}
