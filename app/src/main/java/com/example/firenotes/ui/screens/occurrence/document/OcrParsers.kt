package com.example.firenotes.ui.screens.occurrence.document

import com.example.firenotes.domain.repository.OcrDocumentResult

object RgParser {
    fun parse(result: OcrDocumentResult): RgDocumentState {
        val map = result.extractedFields
        return RgDocumentState(
            nome = map["nome"].orEmpty(),
            rg = map["rg"].orEmpty(),
            cpf = map["cpf"].orEmpty(),
            nascimento = map["nascimento"].orEmpty(),
            mae = map["mae"] ?: map["filiacao"].orEmpty(),
            naturalidade = map["naturalidade"].orEmpty(),
            orgaoExpedidor = map["orgao_emissor"] ?: map["orgaoExpedidor"].orEmpty(),
            dataExpedicao = map["data_expedicao"] ?: map["dataExpedicao"].orEmpty(),
            uf = map["uf"].orEmpty()
        )
    }
}

object CinParser {
    fun parse(result: OcrDocumentResult): CinDocumentState {
        val map = result.extractedFields
        return CinDocumentState(
            cpf = map["cpf"].orEmpty(),
            nome = map["nome"].orEmpty(),
            nascimento = map["nascimento"].orEmpty(),
            pai = map["pai"].orEmpty(),
            mae = map["mae"].orEmpty(),
            sexo = map["sexo"].orEmpty(),
            nacionalidade = map["nacionalidade"].orEmpty(),
            naturalidade = map["naturalidade"].orEmpty(),
            orgao = map["orgao"].orEmpty(),
            expedicao = map["expedicao"].orEmpty(),
            validade = map["validade"].orEmpty()
        )
    }
}

object CnhParser {
    fun parse(result: OcrDocumentResult): CnhDocumentState {
        val map = result.extractedFields
        return CnhDocumentState(
            nome = map["nome"].orEmpty(),
            cpf = map["cpf"].orEmpty(),
            registro = map["registro"].orEmpty(),
            categoria = map["categoria"].orEmpty(),
            nascimento = map["nascimento"].orEmpty(),
            filiacao = map["filiacao"].orEmpty(),
            primeiraHabilitacao = map["primeira_habilitacao"] ?: map["primeiraHabilitacao"].orEmpty(),
            validade = map["validade"].orEmpty()
        )
    }
}

object CpfParser {
    fun parse(result: OcrDocumentResult): CpfDocumentState {
        val map = result.extractedFields
        return CpfParser.parseFields(map)
    }

    fun parseFields(map: Map<String, String>): CpfDocumentState {
        return CpfDocumentState(
            nome = map["nome"].orEmpty(),
            cpf = map["cpf"].orEmpty(),
            nascimento = map["nascimento"].orEmpty(),
            situacao = map["situacao"].orEmpty(),
            dataInscricao = map["data_inscricao"] ?: map["dataInscricao"].orEmpty()
        )
    }
}

object CrlvParser {
    fun parse(result: OcrDocumentResult): CrlvDocumentState {
        val map = result.extractedFields
        return CrlvDocumentState(
            placa = map["placa"].orEmpty(),
            marca = map["marca"].orEmpty(),
            modelo = map["modelo"].orEmpty(),
            versao = map["versao"].orEmpty(),
            anoFabricacao = map["ano_fabricacao"] ?: map["anoFabricacao"].orEmpty(),
            anoModelo = map["ano_modelo"] ?: map["anoModelo"].orEmpty(),
            cor = map["cor"].orEmpty(),
            motor = map["motor"].orEmpty(),
            renavam = map["renavam"].orEmpty(),
            chassi = map["chassi"].orEmpty(),
            proprietario = map["proprietario"].orEmpty(),
            cpfProprietario = map["cpf_proprietario"] ?: map["cpfProprietario"].orEmpty()
        )
    }
}

object OabParser {
    fun parse(result: OcrDocumentResult): OabDocumentState {
        val map = result.extractedFields
        return OabDocumentState(
            nome = map["nome"].orEmpty(),
            numero = map["numero"].orEmpty(),
            uf = map["uf"].orEmpty(),
            expedicao = map["expedicao"].orEmpty()
        )
    }
}
