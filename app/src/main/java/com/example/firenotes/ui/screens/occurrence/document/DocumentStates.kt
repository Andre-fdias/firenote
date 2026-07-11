package com.example.firenotes.ui.screens.occurrence.document

data class RgDocumentState(
    val nome: String = "",
    val rg: String = "",
    val cpf: String = "",
    val nascimento: String = "", // Keeping DD/MM/AAAA string representation for simpler textfield binding
    val mae: String = "",
    val naturalidade: String = "",
    val orgaoExpedidor: String = "",
    val dataExpedicao: String = "",
    val uf: String = ""
)

data class CinDocumentState(
    val cpf: String = "",
    val nome: String = "",
    val nascimento: String = "",
    val pai: String = "",
    val mae: String = "",
    val sexo: String = "",
    val nacionalidade: String = "",
    val naturalidade: String = "",
    val orgao: String = "",
    val expedicao: String = "",
    val validade: String = ""
)

data class CnhDocumentState(
    val nome: String = "",
    val cpf: String = "",
    val registro: String = "",
    val categoria: String = "",
    val nascimento: String = "",
    val filiacao: String = "",
    val primeiraHabilitacao: String = "",
    val validade: String = ""
)

data class CpfDocumentState(
    val nome: String = "",
    val cpf: String = "",
    val nascimento: String = "",
    val situacao: String = "",
    val dataInscricao: String = ""
)

data class CrlvDocumentState(
    val placa: String = "",
    val marca: String = "",
    val modelo: String = "",
    val versao: String = "",
    val anoFabricacao: String = "",
    val anoModelo: String = "",
    val cor: String = "",
    val motor: String = "",
    val renavam: String = "",
    val chassi: String = "",
    val proprietario: String = "",
    val cpfProprietario: String = ""
)

data class OabDocumentState(
    val nome: String = "",
    val numero: String = "",
    val uf: String = "",
    val expedicao: String = ""
)
