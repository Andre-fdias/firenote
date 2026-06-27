package com.firenotes.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.firenotes.core.common.domain.model.Person
import com.firenotes.core.common.domain.model.PersonType
import java.time.LocalDate

@Entity(
    tableName = "people",
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
data class PersonEntity(
    @PrimaryKey
    val id: String,
    val occurrenceId: String,
    val type: PersonType,
    val name: String,
    val cpf: String?,
    val rg: String?,
    val cnh: String?,
    val birthDate: LocalDate?,
    val phone: String?,
    val address: String?,
    val observations: String?
) {
    fun toDomain(): Person {
        return Person(
            id = id,
            occurrenceId = occurrenceId,
            type = type,
            name = name,
            cpf = cpf,
            rg = rg,
            cnh = cnh,
            birthDate = birthDate,
            phone = phone,
            address = address,
            observations = observations
        )
    }

    companion object {
        fun fromDomain(domain: Person): PersonEntity {
            return PersonEntity(
                id = domain.id,
                occurrenceId = domain.occurrenceId,
                type = domain.type,
                name = domain.name,
                cpf = domain.cpf,
                rg = domain.rg,
                cnh = domain.cnh,
                birthDate = domain.birthDate,
                phone = domain.phone,
                address = domain.address,
                observations = domain.observations
            )
        }
    }
}
