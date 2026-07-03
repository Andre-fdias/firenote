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
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint()

                // Draw Report Headers
                paint.color = Color.BLACK
                paint.textSize = 18f
                paint.isFakeBoldText = true
                canvas.drawText("RELATORIO DE OCORRENCIA", 50f, 60f, paint)

                paint.textSize = 12f
                paint.isFakeBoldText = false
                canvas.drawText("Talao: ${occurrence.protocolo}", 50f, 90f, paint)
                canvas.drawText("Natureza: ${occurrence.natureza.descricao}", 50f, 110f, paint)
                canvas.drawText("Endereco: ${occurrence.rua}, ${occurrence.numero}", 50f, 130f, paint)
                canvas.drawText("Cidade/UF: ${occurrence.cidade}/${occurrence.uf}", 50f, 150f, paint)
                
                // Viaturas
                canvas.drawText("VIATURAS E MILITARES", 50f, 190f, paint)
                var yPos = 210f
                occurrence.viaturas.forEach { v ->
                    canvas.drawText("Viatura Prefixo: ${v.prefixo} (${v.tipo})", 70f, yPos, paint)
                    yPos += 20f
                    v.equipe.forEach { m ->
                        canvas.drawText("  • RE: ${m.re} - ${m.nomeGuerra} (${m.graduacao.descricao})", 90f, yPos, paint)
                        yPos += 20f
                    }
                }

                // Vitimas
                canvas.drawText("VITIMAS ATENDIDAS", 50f, yPos + 20f, paint)
                yPos += 40f
                if (occurrence.vitimas.isEmpty()) {
                    canvas.drawText("Nenhuma vitima cadastrada.", 70f, yPos, paint)
                    yPos += 20f
                } else {
                    occurrence.vitimas.forEach { vt ->
                        canvas.drawText("Nome: ${vt.nome ?: "N/D"} | Glasgow: ${vt.sinaisVitais.escalaGCS ?: "N/D"}", 70f, yPos, paint)
                        yPos += 20f
                    }
                }

                // Narrative history
                canvas.drawText("HISTORICO NARRATIVO", 50f, yPos + 20f, paint)
                yPos += 40f
                val hist = occurrence.historico ?: "Nenhum historico informado."
                canvas.drawText(if (hist.length > 50) hist.take(50) + "..." else hist, 70f, yPos, paint)

                pdfDocument.finishPage(page)

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
