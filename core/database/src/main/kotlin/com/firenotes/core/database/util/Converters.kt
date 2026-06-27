package com.firenotes.core.database.util

import androidx.room.TypeConverter
import com.firenotes.core.common.domain.model.DocumentType
import com.firenotes.core.common.domain.model.OccurrenceStatus
import com.firenotes.core.common.domain.model.PersonType
import com.firenotes.core.common.domain.model.PhotoType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Converters {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.format(dateFormatter)

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it, dateFormatter) }

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.format(timeFormatter)

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it, timeFormatter) }

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? = value?.format(dateTimeFormatter)

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? = value?.let { LocalDateTime.parse(it, dateTimeFormatter) }

    @TypeConverter
    fun fromOccurrenceStatus(value: OccurrenceStatus?): String? = value?.name

    @TypeConverter
    fun toOccurrenceStatus(value: String?): OccurrenceStatus? = value?.let { OccurrenceStatus.valueOf(it) }

    @TypeConverter
    fun fromPersonType(value: PersonType?): String? = value?.name

    @TypeConverter
    fun toPersonType(value: String?): PersonType? = value?.let { PersonType.valueOf(it) }

    @TypeConverter
    fun fromPhotoType(value: PhotoType?): String? = value?.name

    @TypeConverter
    fun toPhotoType(value: String?): PhotoType? = value?.let { PhotoType.valueOf(it) }

    @TypeConverter
    fun fromDocumentType(value: DocumentType?): String? = value?.name

    @TypeConverter
    fun toDocumentType(value: String?): DocumentType? = value?.let { DocumentType.valueOf(it) }
}
