package com.example.firenotes.ui.screens.occurrence.models

import com.example.firenotes.domain.model.NaturezaOcorrencia

data class SubNatureza(
    val nome: String,
    val baseNatureza: NaturezaOcorrencia,
    val categoria: String,
    val keywords: List<String>
)

val subNaturezas = listOf(
    // INCÊNDIOS
    SubNatureza("Incêndio em residência", NaturezaOcorrencia.INCENDIO, "🔥🔥 INCÊNDIOS", listOf("casa", "fogo", "residencia", "lar", "domestico")),
    SubNatureza("Incêndio em comércio", NaturezaOcorrencia.INCENDIO, "🔥🔥 INCÊNDIOS", listOf("loja", "fogo", "estabelecimento", "predio")),
    SubNatureza("Incêndio em veículo", NaturezaOcorrencia.INCENDIO, "🔥🔥 INCÊNDIOS", listOf("carro", "fogo", "veiculo", "moto", "caminhao")),
    SubNatureza("Incêndio florestal", NaturezaOcorrencia.INCENDIO, "🔥🔥 INCÊNDIOS", listOf("mato", "fogo", "arvore", "floresta", "queimada", "vegetacao")),
    SubNatureza("Incêndio industrial", NaturezaOcorrencia.INCENDIO, "🔥🔥 INCÊNDIOS", listOf("galpao", "fogo", "industria", "fabrica", "quimico")),
    // APH
    SubNatureza("Mal súbito", NaturezaOcorrencia.PESSOAL, "🚑🚑 APH", listOf("desmaio", "pressao", "passando mal", "infarto")),
    SubNatureza("Queda", NaturezaOcorrencia.PESSOAL, "🚑🚑 APH", listOf("altura", "propria altura", "chao", "queda")),
    SubNatureza("Trauma", NaturezaOcorrencia.PESSOAL, "🚑🚑 APH", listOf("fratura", "corte", "sangramento", "ferimento")),
    SubNatureza("PCR", NaturezaOcorrencia.PESSOAL, "🚑🚑 APH", listOf("parada", "cardio", "respiratoria", "reanimacao")),
    SubNatureza("Parto", NaturezaOcorrencia.PESSOAL, "🚑🚑 APH", listOf("nascimento", "bebe", "gravida", "gestante")),
    SubNatureza("Afogamento", NaturezaOcorrencia.PESSOAL, "🚑🚑 APH", listOf("agua", "piscina", "rio", "mar")),
    // SALVAMENTOS
    SubNatureza("Altura", NaturezaOcorrencia.SALVAMENTO, "🚒🚒 SALVAMENTOS", listOf("rapel", "ponte", "predio", "elevado")),
    SubNatureza("Aquático", NaturezaOcorrencia.SALVAMENTO, "🚒🚒 SALVAMENTOS", listOf("rio", "mar", "represa", "afogamento")),
    SubNatureza("Estrutural", NaturezaOcorrencia.SALVAMENTO, "🚒🚒 SALVAMENTOS", listOf("desabamento", "escombros", "colapso")),
    SubNatureza("Animal", NaturezaOcorrencia.SALVAMENTO, "🚒🚒 SALVAMENTOS", listOf("cachorro", "gato", "cobra", "resgate", "bicho")),
    SubNatureza("Busca", NaturezaOcorrencia.SALVAMENTO, "🚒🚒 SALVAMENTOS", listOf("desaparecido", "floresta", "resgate", "perdido")),
    // ACIDENTES
    SubNatureza("Colisão", NaturezaOcorrencia.ACIDENTE_TRANSITO, "🚗🚗 ACIDENTES", listOf("batida", "carro", "veiculo", "transito")),
    SubNatureza("Capotamento", NaturezaOcorrencia.ACIDENTE_TRANSITO, "🚗🚗 ACIDENTES", listOf("tombamento", "carro", "veiculo", "transito")),
    SubNatureza("Atropelamento", NaturezaOcorrencia.ACIDENTE_TRANSITO, "🚗🚗 ACIDENTES", listOf("pedestre", "carro", "veiculo", "atropelar")),
    SubNatureza("Moto", NaturezaOcorrencia.ACIDENTE_TRANSITO, "🚗🚗 ACIDENTES", listOf("colisao moto", "queda moto", "motocicleta")),
    SubNatureza("Caminhão", NaturezaOcorrencia.ACIDENTE_TRANSITO, "🚗🚗 ACIDENTES", listOf("carreta", "caminhao", "veiculo pesado")),
    // OUTROS
    SubNatureza("Queda de árvore", NaturezaOcorrencia.QUEDA, "🌳🌳 OUTROS", listOf("arvore", "via", "bloqueio", "vento")),
    SubNatureza("Choque elétrico", NaturezaOcorrencia.QUEDA, "🌳🌳 OUTROS", listOf("energia", "fio", "poste", "eletrocussao")),
    SubNatureza("Vazamento", NaturezaOcorrencia.QUEDA, "🌳🌳 OUTROS", listOf("gas", "agua", "produto", "vazando")),
    SubNatureza("Produtos perigosos", NaturezaOcorrencia.QUEDA, "🌳🌳 OUTROS", listOf("quimico", "gas", "carga", "explosivo"))
)
