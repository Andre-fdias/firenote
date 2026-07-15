package com.example.firenotes.ui.screens.occurrence.models

import androidx.compose.ui.graphics.Color

data class ModuleInfo(
    val title: String,
    val icon: String,
    val summary: String,
    val status: Pair<String, Color>,
    val onSelected: (OccurrenceModule) -> Unit
) {
    val statusText = status.first
    val statusColor = status.second
}
