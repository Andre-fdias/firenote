package com.example.firenotes.ui.screens.occurrence.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatDate(instant: Instant): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(formatter)
    } catch (e: Exception) {
        ""
    }
}

fun formatTime(instant: Instant): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(formatter)
    } catch (e: Exception) {
        ""
    }
}

fun formatPlaca(placa: String): String {
    val clean = placa.uppercase().replace("[^A-Z0-9]".toRegex(), "")
    if (clean.length > 3) {
        return clean.substring(0, 3) + "-" + clean.substring(3).take(4)
    }
    return clean
}
