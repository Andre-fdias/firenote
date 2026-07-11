package com.example.firenotes.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firenotes.domain.model.*
import com.example.firenotes.domain.repository.OcorrenciaRepository
import com.example.firenotes.data.service.DashboardService
import com.example.firenotes.data.service.DashboardStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class DashboardUiState(
    val totalHoje: Int = 0,
    val totalMes: Int = 0,
    val totalAno: Int = 0,
    val natureStats: Map<NaturezaOcorrencia, Int> = emptyMap(),
    val viaturaRanking: List<ViaturaRank> = emptyList(),
    val militarRanking: List<MilitarRank> = emptyList(),
    val hospitalRanking: List<HospitalRank> = emptyList(),
    val municipioRanking: List<MunicipioRank> = emptyList(),
    val regionGroups: List<RegionGroup> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class ViaturaRank(val prefixo: String, val count: Int, val kmPercorrida: Int, val tempoMinutos: Int)
data class MilitarRank(val re: String, val nomeGuerra: String, val count: Int, val horasTrabalhadas: Int)
data class HospitalRank(val nome: String, val count: Int)
data class MunicipioRank(val nome: String, val count: Int)
data class RegionGroup(val regiao: String, val count: Int, val coordenadas: List<Pair<Double, Double>>)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: OcorrenciaRepository,
    private val dashboardService: DashboardService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.getOcorrencias()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
                }
                .collect { list ->
                    // Use pre-fetched list directly to avoid redundant N+1 queries
                    val fullList = list

                    // 1. Calculate time periods
                    val today = LocalDate.now()
                    var hojeCount = 0
                    var mesCount = 0
                    var anoCount = 0

                    fullList.forEach { o ->
                        val date = o.dataHora.atZone(ZoneId.systemDefault()).toLocalDate()
                        if (date == today) hojeCount++
                        if (date.month == today.month && date.year == today.year) mesCount++
                        if (date.year == today.year) anoCount++
                    }

                    // 2. Nature stats
                    val natures = NaturezaOcorrencia.values().associateWith { nature ->
                        fullList.count { it.natureza == nature }
                    }

                    // 3. Viatura ranking
                    val viaturasMap = mutableMapOf<String, Triple<Int, Int, Int>>() // prefix -> (count, km, time)
                    fullList.forEach { o ->
                        o.viaturas.forEach { v ->
                            val kmDiff = if (v.kmLocal != null && v.kmSaida != null) v.kmLocal - v.kmSaida else 12
                            val time = 45 // mock average time
                            val current = viaturasMap[v.prefixo] ?: Triple(0, 0, 0)
                            viaturasMap[v.prefixo] = Triple(current.first + 1, current.second + kmDiff, current.third + time)
                        }
                    }
                    val viaturaRanks = viaturasMap.map { (prefix, stats) ->
                        ViaturaRank(prefix, stats.first, stats.second, stats.third)
                    }.sortedByDescending { it.count }

                    // 4. Militar ranking
                    val militarMap = mutableMapOf<String, Pair<Militar, Int>>() // re -> (militar, count)
                    fullList.forEach { o ->
                        o.viaturas.forEach { v ->
                            v.equipe.forEach { m ->
                                val current = militarMap[m.re] ?: Pair(m, 0)
                                militarMap[m.re] = Pair(current.first, current.second + 1)
                            }
                        }
                    }
                    val militarRanks = militarMap.map { (re, data) ->
                        MilitarRank(re, data.first.nomeGuerra, data.second, data.second * 8) // mock 8 hours per occurrence
                    }.sortedByDescending { it.count }

                    // 5. Hospital ranking
                    val hospitalMap = mutableMapOf<String, Int>()
                    fullList.forEach { o ->
                        o.vitimas.forEach { vt ->
                            vt.hospitalDestino?.let { h ->
                                if (h.isNotBlank()) {
                                    hospitalMap[h] = (hospitalMap[h] ?: 0) + 1
                                }
                            }
                        }
                    }
                    val hospitalRanks = hospitalMap.map { (name, count) ->
                        HospitalRank(name, count)
                    }.sortedByDescending { it.count }

                    // 6. Municipio ranking
                    val municipioMap = mutableMapOf<String, Int>()
                    fullList.forEach { o ->
                        val city = o.cidade
                        if (!city.isNullOrBlank()) {
                            municipioMap[city] = (municipioMap[city] ?: 0) + 1
                        }
                    }
                    val municipioRanks = municipioMap.map { (name, count) ->
                        MunicipioRank(name, count)
                    }.sortedByDescending { it.count }

                    // 7. Regions for georeferenced map groupings
                    val regions = fullList.filter { it.latitude != null && it.longitude != null }
                        .groupBy { it.cidade.orEmpty() }
                        .map { (city, list) ->
                            RegionGroup(city, list.size, list.map { Pair(it.latitude!!, it.longitude!!) })
                        }

                    // Fallback to mock service stats if database lists are empty
                    if (fullList.isEmpty()) {
                        dashboardService.getStatisticsSummary(30).onSuccess { stats ->
                            _uiState.update {
                                it.copy(
                                    totalHoje = 3,
                                    totalMes = stats.totalOcorrencias,
                                    totalAno = stats.totalOcorrencias * 12,
                                    natureStats = stats.totalPorNatureza,
                                    viaturaRanking = stats.viaturasMaisAtivas.map { (p, c) -> ViaturaRank(p, c, c * 15, c * 40) },
                                    militarRanking = stats.militaresMaisAtivos.map { (re, c) -> MilitarRank(re, "Militar RE $re", c, c * 8) },
                                    hospitalRanking = stats.principaisHospitaisDestino.map { (name, count) -> HospitalRank(name, count) },
                                    municipioRanking = stats.ocorrenciasPorMunicipio.map { (name, count) -> MunicipioRank(name, count) },
                                    regionGroups = stats.ocorrenciasPorMunicipio.map { (name, count) -> RegionGroup(name, count, listOf(Pair(-23.5505, -46.6333))) },
                                    isLoading = false
                                )
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                totalHoje = hojeCount,
                                totalMes = mesCount,
                                totalAno = anoCount,
                                natureStats = natures,
                                viaturaRanking = viaturaRanks,
                                militarRanking = militarRanks,
                                hospitalRanking = hospitalRanks,
                                municipioRanking = municipioRanks,
                                regionGroups = regions,
                                isLoading = false
                            )
                        }
                    }
                }
        }
    }
}
