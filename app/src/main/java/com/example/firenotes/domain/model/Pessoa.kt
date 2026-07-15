package com.example.firenotes.domain.model

data class Pessoa(
    val id: String? = null,
    val nome: String = "",
    val nomeSocial: String? = null,
    val cpf: String? = null,
    val rg: String? = null,
    val rgOrgaoEmissor: String? = null,
    val rgUf: String? = null,
    val nascimento: String? = null,            // DD/MM/AAAA
    val naturalidade: String? = null,
    val nacionalidade: String? = null,
    val filiacao: String? = null,
    val sexo: String? = null,
    val telefone: String? = null,              // (XX) XXXXX-XXXX
    val email: String? = null,
    val documentos: List<Documento> = emptyList(),

    // Database compatibility fields
    val logradouro: String? = null,
    val numero: String? = null,
    val bairro: String? = null,
    val cidade: String? = null,
    val uf: String? = null,
    val cep: String? = null
)
