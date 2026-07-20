package com.example.firenotes.ui.screens.reports

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.FileProvider
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firenotes.domain.model.*
import com.example.firenotes.domain.repository.OcorrenciaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

data class ReportsUiState(
    val dataInicial: String = "",
    val dataFinal: String = "",
    val occurrences: List<Ocorrencia> = emptyList(),
    val isExporting: Boolean = false,
    val exportMessage: String? = null,
    val exportedFileUri: Uri? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val repository: OcorrenciaRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        val today = LocalDate.now()
        val formattedToday = today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val formattedMonthAgo = today.minusMonths(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        _uiState.update { it.copy(dataInicial = formattedMonthAgo, dataFinal = formattedToday) }
        loadOccurrences()
    }

    fun loadOccurrences() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.getOcorrencias().collect { list ->
                val fullList = list.map { o ->
                    repository.getOcorrenciaById(o.id ?: "").getOrDefault(o)
                }
                _uiState.update { it.copy(occurrences = fullList, isLoading = false) }
            }
        }
    }

    fun updateDates(inicial: String, final: String) {
        _uiState.update { it.copy(dataInicial = inicial, dataFinal = final) }
    }

    // --- Export PDF ---
    fun exportOccurrencePdf(occurrence: Ocorrencia, onCompleted: (Uri) -> Unit) {
        _uiState.update { it.copy(isExporting = true, exportMessage = null) }
        viewModelScope.launch {
            try {
                // Carregar ocorrência atualizada do banco para garantir que todas as viaturas/militares estejam presentes
                val occurrence = repository.getOcorrenciaById(occurrence.id ?: "").getOrDefault(occurrence)
                // Carregar pessoas e evidências associadas antes de gerar o PDF
                val pessoas = repository.getPessoasDaOcorrencia(occurrence.id ?: "").getOrDefault(emptyList())
                val evidencias = repository.getEvidencias(occurrence.id ?: "").getOrDefault(emptyList())

                val pdfDocument = PdfDocument()
                val paint = Paint()
                var pageNumber = 1
                
                var currentPagePair = Pair<PdfDocument.Page?, Canvas?>(null, null)
                var yPos = 85f

                fun createNewPage(): Pair<PdfDocument.Page, Canvas> {
                    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    
                    // Draw Header
                    paint.color = Color.rgb(26, 115, 232) // Azul Moderno
                    paint.textSize = 12f
                    paint.isFakeBoldText = true
                    canvas.drawText("CORPO DE BOMBEIROS - RELATÓRIO PROFISSIONAL DE OCORRÊNCIA", 50f, 50f, paint)
                    
                    // Header Line
                    paint.color = Color.rgb(218, 220, 224)
                    paint.strokeWidth = 1.5f
                    canvas.drawLine(50f, 62f, 545f, 62f, paint)
                    
                    // Page Number Footer
                    paint.textSize = 9f
                    paint.isFakeBoldText = false
                    paint.color = Color.GRAY
                    canvas.drawText("Página $pageNumber", 270f, 810f, paint)
                    
                    pageNumber++
                    return Pair(page, canvas)
                }

                fun checkPageBound(requiredHeight: Float) {
                    val currentCanvas = currentPagePair.second
                    if (currentCanvas == null || yPos + requiredHeight > 780f) {
                        currentPagePair.first?.let { pdfDocument.finishPage(it) }
                        val newPage = createNewPage()
                        currentPagePair = Pair(newPage.first, newPage.second)
                        yPos = 85f
                    }
                }

                fun drawTextLine(text: String, x: Float, fontSize: Float, isBold: Boolean, color: Int = Color.BLACK) {
                    paint.textSize = fontSize
                    paint.isFakeBoldText = isBold
                    paint.color = color
                    checkPageBound(fontSize + 6f)
                    currentPagePair.second?.drawText(text, x, yPos, paint)
                    yPos += fontSize + 6f
                }

                fun drawDivider() {
                    paint.color = Color.rgb(218, 220, 224)
                    paint.strokeWidth = 1f
                    checkPageBound(10f)
                    currentPagePair.second?.drawLine(50f, yPos, 545f, yPos, paint)
                    yPos += 15f
                }

                fun drawWrappedText(text: String, x: Float, maxWidth: Float, fontSize: Float, color: Int = Color.BLACK) {
                    paint.textSize = fontSize
                    paint.isFakeBoldText = false
                    paint.color = color
                    val words = text.split(" ")
                    var line = ""
                    for (word in words) {
                        val testLine = if (line.isEmpty()) word else "$line $word"
                        val width = paint.measureText(testLine)
                        if (width > maxWidth) {
                            checkPageBound(fontSize + 4f)
                            currentPagePair.second?.drawText(line, x, yPos, paint)
                            yPos += fontSize + 4f
                            line = word
                        } else {
                            line = testLine
                        }
                    }
                    if (line.isNotEmpty()) {
                        checkPageBound(fontSize + 4f)
                        currentPagePair.second?.drawText(line, x, yPos, paint)
                        yPos += fontSize + 4f
                    }
                }

                // Start the first page
                val initialPage = createNewPage()
                currentPagePair = Pair(initialPage.first, initialPage.second)
                yPos = 85f

                // 1. INFORMAÇÕES GERAIS
                drawTextLine("INFORMAÇÕES GERAIS", 50f, 12f, true, Color.rgb(26, 115, 232))
                val prontidao = com.example.firenotes.data.service.ProntidaoService.getProntidaoForInstant(occurrence.dataHora)
                val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(java.time.ZoneId.systemDefault())
                
                drawTextLine("Talão / Protocolo: ${occurrence.protocolo} | Status: ${if (occurrence.status == "ABERTA") "EM ABERTO" else "ENCERRADO"}", 50f, 11f, false)
                drawTextLine("Data/Hora: ${formatter.format(occurrence.dataHora)} | Prontidão: ${prontidao.nome}", 50f, 11f, false)
                drawTextLine("Natureza: ${occurrence.natureza.descricao}", 50f, 11f, false)
                drawTextLine("Endereço: ${occurrence.rua ?: "N/D"}, ${occurrence.numero ?: "N/D"} - ${occurrence.bairro ?: "N/D"}", 50f, 11f, false)
                drawTextLine("Cidade/UF: ${occurrence.cidade ?: "N/D"}/${occurrence.uf ?: "SP"}", 50f, 11f, false)
                occurrence.latitude?.let { lat ->
                    drawTextLine("Coordenadas: Lat ${"%.5f".format(lat)} | Lng ${"%.5f".format(occurrence.longitude)}", 50f, 11f, false)
                }

                drawDivider()

                // 2. VEÍCULOS ENVOLVIDOS
                if (occurrence.veiculos.isNotEmpty()) {
                    drawTextLine("VEÍCULOS ENVOLVIDOS", 50f, 12f, true, Color.rgb(26, 115, 232))
                    occurrence.veiculos.forEach { veiculo ->
                        drawTextLine("• Placa: ${veiculo.placa.ifBlank { "SEM PLACA" }} | Modelo: ${veiculo.modelo.ifBlank { "N/I" }} | Cor: ${veiculo.cor.ifBlank { "N/I" }}", 70f, 11f, false)
                    }
                    drawDivider()
                }

                // 3. VÍTIMAS E SINAIS VITAIS
                drawTextLine("VÍTIMAS E SINAIS VITAIS", 50f, 12f, true, Color.rgb(26, 115, 232))
                if (occurrence.vitimas.isEmpty()) {
                    drawTextLine("Nenhuma vítima registrada.", 70f, 11f, false)
                } else {
                    occurrence.vitimas.forEach { vt ->
                        val pCadastro = pessoas.find { p -> p.nome.trim().equals(vt.nome.trim(), ignoreCase = true) || (p.cpf != null && p.cpf == vt.cpf) }
                        val dadosAdicionais = if (pCadastro != null) {
                            " | CPF: ${pCadastro.cpf ?: "N/I"} | Sexo: ${pCadastro.sexo ?: "N/I"} | Nasc: ${pCadastro.nascimento ?: "N/I"}"
                        } else ""
                        
                        drawTextLine("Nome: ${vt.nome ?: "Desconhecido"} | Idade: ${vt.idade ?: "N/D"}${dadosAdicionais}", 70f, 11f, false)
                        drawTextLine("  • GCS: ${vt.sinaisVitais.escalaGCS ?: "N/D"} | PA: ${vt.sinaisVitais.pressaoArterial.ifBlank { "N/D" }} | FC: ${vt.sinaisVitais.pulso ?: "N/D"} bpm | SatO2: ${vt.sinaisVitais.saturacaoO2 ?: "N/D"}% | Temp: ${vt.sinaisVitais.temperatura ?: "N/D"}°C", 90f, 10f, false)
                        drawTextLine("  • Hospital Destino: ${vt.hospitalDestino.ifBlank { "Não encaminhado" }}", 90f, 10f, false)
                    }
                }

                drawDivider()

                // 4. PESSOAS ENVOLVIDAS (NÃO VÍTIMAS)
                val vitimasNomes = occurrence.vitimas.map { it.nome.trim().lowercase() }.toSet()
                val naoVitimas = pessoas.filter { !vitimasNomes.contains(it.nome.trim().lowercase()) }
                if (naoVitimas.isNotEmpty()) {
                    drawTextLine("PESSOAS ENVOLVIDAS (NÃO VÍTIMAS)", 50f, 12f, true, Color.rgb(26, 115, 232))
                    naoVitimas.forEach { p ->
                        drawTextLine("Nome: ${p.nome} | CPF: ${p.cpf ?: "N/I"} | Tel: ${p.telefone ?: "N/I"}", 70f, 11f, false)
                        drawTextLine("  • Sexo: ${p.sexo ?: "N/I"} | Data Nasc.: ${p.nascimento ?: "N/I"} | Cidade: ${p.cidade ?: "N/I"}/${p.uf ?: "SP"}", 90f, 10f, false)
                    }
                    drawDivider()
                }

                // 5. VIATURAS E MILITARES
                drawTextLine("VIATURAS E EQUIPES EM EMPENHO", 50f, 12f, true, Color.rgb(26, 115, 232))
                if (occurrence.viaturas.isEmpty()) {
                    drawTextLine("Nenhuma viatura empenhada.", 70f, 11f, false)
                } else {
                    occurrence.viaturas.forEach { v ->
                        drawTextLine("Viatura: ${v.prefixo} (${v.tipo}) - Unidade: ${v.unidade ?: "N/D"}", 70f, 11f, false)
                        v.equipe.forEach { m ->
                            drawTextLine("  • RE: ${m.re} - ${m.nomeGuerra} (${m.graduacao}) [${m.funcao.ifBlank { "Equipe" }}]", 90f, 10f, false)
                        }
                    }
                }

                drawDivider()

                // 6. ÓRGÃOS DE APOIO
                if (occurrence.apoiosDetalhados.isNotEmpty()) {
                    drawTextLine("APOIOS ACIONADOS", 50f, 12f, true, Color.rgb(26, 115, 232))
                    occurrence.apoiosDetalhados.forEach { ap ->
                        drawTextLine("• Órgão: ${ap.orgaoSigla} - ${ap.orgaoNome} | Viatura: ${ap.viatura.ifBlank { "N/I" }}", 70f, 11f, false)
                        if (ap.encarregado.isNotBlank() || ap.descricaoOutros.isNotBlank()) {
                            drawTextLine("  Informações: ${if (ap.encarregado.isNotBlank()) ap.encarregado else ap.descricaoOutros}", 90f, 10f, false)
                        }
                    }
                    drawDivider()
                }

                // 7. HISTÓRICO NARRATIVO
                drawTextLine("HISTÓRICO NARRATIVO", 50f, 12f, true, Color.rgb(26, 115, 232))
                val hist = occurrence.historico ?: "Nenhum histórico informado."
                drawWrappedText(hist, 70f, 475f, 11f)

                // 8. EVIDÊNCIAS E ANEXOS
                if (evidencias.isNotEmpty()) {
                    drawDivider()
                    drawTextLine("EVIDÊNCIAS E ANEXOS REGISTRADOS", 50f, 12f, true, Color.rgb(26, 115, 232))
                    evidencias.forEach { ev ->
                        drawTextLine("• Tipo: ${ev.tipo} | Arquivo: ${ev.urlStorage.substringAfterLast("/")}", 70f, 11f, false)
                        if (!ev.ocrBruto.isNullOrBlank()) {
                            drawTextLine("  OCR: ${ev.ocrBruto}", 90f, 10f, false)
                        }
                    }
                }

                currentPagePair.first?.let { pdfDocument.finishPage(it) }

                val file = File(context.cacheDir, "relatorio_ocorrencia_${occurrence.protocolo}.pdf")
                val out = FileOutputStream(file)
                pdfDocument.writeTo(out)
                out.close()
                pdfDocument.close()

                val uri = FileProvider.getUriForFile(context, "com.example.firenotes.fileprovider", file)
                _uiState.update { it.copy(isExporting = false, exportedFileUri = uri, exportMessage = "PDF gerado com sucesso!") }
                onCompleted(uri)
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, exportMessage = "Falha ao gerar PDF: ${e.localizedMessage}") }
            }
        }
    }

    // --- Export Excel (XML spreadsheet template) ---
    fun exportPeriodExcel(onCompleted: (Uri) -> Unit) {
        _uiState.update { it.copy(isExporting = true, exportMessage = null) }
        viewModelScope.launch {
            try {
                val csvContent = StringBuilder()
                csvContent.append("Talao,Natureza,Data/Hora,Cidade,UF,Vitimas,Veiculos\n")
                _uiState.value.occurrences.forEach { o ->
                    csvContent.append("${o.protocolo},${o.natureza.descricao},${o.dataHora},${o.cidade},${o.uf},${o.vitimas.size},${o.veiculos.size}\n")
                }

                val file = File(context.cacheDir, "relatorio_periodo_${System.currentTimeMillis()}.csv")
                FileOutputStream(file).use { out ->
                    out.write(csvContent.toString().toByteArray())
                }

                val uri = FileProvider.getUriForFile(context, "com.example.firenotes.fileprovider", file)
                _uiState.update { it.copy(isExporting = false, exportedFileUri = uri, exportMessage = "Excel (CSV) gerado com sucesso!") }
                onCompleted(uri)
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, exportMessage = "Falha ao gerar Excel: ${e.localizedMessage}") }
            }
        }
    }

    // --- Export CSV ---
    fun exportPeriodCsv(onCompleted: (Uri) -> Unit) {
        exportPeriodExcel(onCompleted) // CSV and Excel exports are built using identical compliant standard CSV format
    }
}
