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
                    paint.color = Color.rgb(180, 20, 20)
                    paint.textSize = 12f
                    paint.isFakeBoldText = true
                    canvas.drawText("CORPO DE BOMBEIROS - RELATORIO DE OCORRENCIA", 50f, 50f, paint)
                    
                    // Header Line
                    paint.color = Color.DKGRAY
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
                        val newPair = createNewPage()
                        currentPagePair = Pair(newPair.first, newPair.second)
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
                    paint.color = Color.LTGRAY
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

                // Draw sections
                drawTextLine("INFORMACOES GERAIS", 50f, 12f, true)
                drawTextLine("Talao / Protocolo: ${occurrence.protocolo}", 50f, 11f, false)
                drawTextLine("Data/Hora: ${occurrence.dataHora}", 50f, 11f, false)
                drawTextLine("Natureza: ${occurrence.natureza.descricao}", 50f, 11f, false)
                drawTextLine("Endereco: ${occurrence.rua ?: "N/D"}, ${occurrence.numero ?: "N/D"}", 50f, 11f, false)
                drawTextLine("Cidade/UF: ${occurrence.cidade ?: "N/D"}/${occurrence.uf ?: "SP"}", 50f, 11f, false)
                
                drawDivider()
                
                drawTextLine("VIATURAS E EQUIPES", 50f, 12f, true)
                if (occurrence.viaturas.isEmpty()) {
                    drawTextLine("Nenhuma viatura empenhada.", 70f, 11f, false)
                } else {
                    occurrence.viaturas.forEach { v ->
                        drawTextLine("Viatura: ${v.prefixo} (${v.tipo}) - Unidade: ${v.unidade ?: "N/D"}", 70f, 11f, false)
                        v.equipe.forEach { m ->
                            drawTextLine("  • RE: ${m.re} - ${m.nomeGuerra} (${m.graduacao.descricao})", 90f, 10f, false)
                        }
                    }
                }
                
                drawDivider()
                
                drawTextLine("VITIMAS E SINAIS VITAIS", 50f, 12f, true)
                if (occurrence.vitimas.isEmpty()) {
                    drawTextLine("Nenhuma vitima registrada.", 70f, 11f, false)
                } else {
                    occurrence.vitimas.forEach { vt ->
                        drawTextLine("Nome: ${vt.nome ?: "Desconhecido"} | Glasgow (GCS): ${vt.sinaisVitais.escalaGCS ?: "N/D"} | Idade: ${vt.idade ?: "N/D"}", 70f, 11f, false)
                        drawTextLine("  • PA: ${vt.sinaisVitais.pressaoArterial ?: "N/D"} | Pulso: ${vt.sinaisVitais.pulso ?: "N/D"} bpm | SatO2: ${vt.sinaisVitais.saturacaoO2 ?: "N/D"}% | Temp: ${vt.sinaisVitais.temperatura ?: "N/D"}°C", 90f, 10f, false)
                    }
                }
                
                drawDivider()
                
                drawTextLine("HISTORICO NARRATIVO", 50f, 12f, true)
                val hist = occurrence.historico ?: "Nenhum historico informado."
                drawWrappedText(hist, 70f, 475f, 11f)

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
