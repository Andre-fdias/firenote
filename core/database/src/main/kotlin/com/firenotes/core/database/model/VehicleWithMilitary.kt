package com.firenotes.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.firenotes.core.database.entity.MilitaryEntity
import com.firenotes.core.database.entity.VehicleEntity

data class VehicleWithMilitary(
    @Embedded val vehicle: VehicleEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "vehicleId"
    )
    val militaryList: List<MilitaryEntity>
)
