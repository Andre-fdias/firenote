package com.example.firenotes.domain.model

data class VeiculoMaster(
    val id: String? = null,
    val placa: String,
    val renavam: String? = null,
    val chassi: String? = null,
    val marca: String? = null,
    val modelo: String? = null,
    val versao: String? = null,
    val tipo: String? = null,
    val categoria: String? = null,
    val cor: String? = null,
    val anoFabricacao: Int? = null,
    val anoModelo: Int? = null,
    val proprietarioId: String? = null,
    val status: String = "Ativo"
)

data class VeiculoEnvolvido(
    val id: String? = null,
    val ocorrenciaId: String,
    val veiculoMasterId: String? = null, // V4 Master vehicle reference
    val condutorId: String? = null,      // V4 Condutor reference
    
    val placa: String?,
    val cor: String?,
    val chassi: String?,
    val modelo: String?,
    val ano: Int?,
    val dadosMotorista: Motorista? = null,
    
    // V2 CRLV and Owner fields
    val proprietarioId: String? = null,
    val renavam: String? = null,
    val monobloco: String? = null,
    val especie: String? = null,
    val tipoVeiculo: String? = null,
    val carroceria: String? = null,
    val marca: String? = null,
    val versao: String? = null,
    val anoFabricacao: Int? = null,
    val anoModelo: Int? = null,
    val categoriaVeiculo: String? = null,
    val exercicio: String? = null,
    val urlCrlv: String? = null,
    val ocrTextoCrlv: String? = null,
    val ocrDadosEstruturados: Map<String, String> = emptyMap()
)

data class Motorista(
    val nome: String? = null,
    val cnh: String? = null,
    val categoriaCnh: String? = null,
    val dataNascimento: String? = null,
    val telefone: String? = null
)
