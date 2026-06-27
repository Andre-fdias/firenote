package com.firenotes.core.common.domain.model

data class Military(
    val id: String,
    val vehicleId: String,
    val name: String,
    val re: String,
    val rank: String,
    val role: String,
    val phone: String? = null
)
