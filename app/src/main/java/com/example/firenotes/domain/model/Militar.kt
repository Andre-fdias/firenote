package com.example.firenotes.domain.model

data class Militar(
    val id: String = "",
    val viaturaId: String = "",
    val militarMasterId: String? = null, // Link to master catalog for DB compatibility
    val re: String = "",                 // 6 dígitos
    val nomeGuerra: String = "",
    val graduacao: String = "",          // "1 - CEL PM", "2 - TEN CEL PM", etc.
    val funcao: String = ""              // Motorista, Comandante, Encarregado, Auxiliar
)

enum class FuncaoMilitar(val display: String) {
    MOTORISTA("Motorista"),
    COMANDANTE("Comandante"),
    ENCARREGADO("Encarregado"),
    AUXILIAR("Auxiliar")
}
