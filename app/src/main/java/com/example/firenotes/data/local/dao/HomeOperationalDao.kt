package com.example.firenotes.data.local.dao

import androidx.room.*
import com.example.firenotes.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeOperationalDao {

    // --- Tarefas ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarefa(tarefa: RoomTarefa)

    @Update
    suspend fun updateTarefa(tarefa: RoomTarefa)

    @Query("DELETE FROM tarefas WHERE id = :id")
    suspend fun deleteTarefa(id: String)

    @Transaction
    @Query("SELECT * FROM tarefas WHERE data = :data ORDER BY id ASC")
    fun getTarefasForDayFlow(data: String): Flow<List<RoomTarefaComSubtarefas>>

    @Query("SELECT * FROM tarefas WHERE data = :data ORDER BY id ASC")
    suspend fun getTarefasForDay(data: String): List<RoomTarefa>

    @Transaction
    @Query("SELECT * FROM tarefas ORDER BY data ASC")
    fun getAllTarefasFlow(): Flow<List<RoomTarefaComSubtarefas>>

    // --- Eventos da Agenda ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvento(evento: RoomEventoAgenda)

    @Update
    suspend fun updateEvento(evento: RoomEventoAgenda)

    @Query("DELETE FROM eventos_agenda WHERE id = :id")
    suspend fun deleteEvento(id: String)

    @Transaction
    @Query("SELECT * FROM eventos_agenda WHERE data = :data ORDER BY horaInicio ASC")
    fun getEventosForDayFlow(data: String): Flow<List<RoomEventoComLembretes>>

    @Query("SELECT * FROM eventos_agenda WHERE data = :data ORDER BY horaInicio ASC")
    suspend fun getEventosForDay(data: String): List<RoomEventoAgenda>

    @Transaction
    @Query("SELECT * FROM eventos_agenda ORDER BY data ASC, horaInicio ASC")
    fun getAllEventosFlow(): Flow<List<RoomEventoComLembretes>>

    // --- Prontidão / Escala ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProntidao(prontidao: RoomProntidaoDia)

    @Query("SELECT * FROM prontidao_dias WHERE data = :data")
    suspend fun getProntidaoForDay(data: String): RoomProntidaoDia?

    @Query("SELECT * FROM prontidao_dias")
    fun getAllProntidoesFlow(): Flow<List<RoomProntidaoDia>>

    // --- Subtarefas ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtarefa(subtarefa: RoomSubtarefa)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtarefas(subtarefas: List<RoomSubtarefa>)

    @Query("DELETE FROM subtarefas WHERE tarefaId = :tarefaId")
    suspend fun deleteSubtarefasByTarefa(tarefaId: String)

    @Query("SELECT * FROM subtarefas WHERE tarefaId = :tarefaId")
    suspend fun getSubtarefasByTarefa(tarefaId: String): List<RoomSubtarefa>

    @Query("SELECT * FROM subtarefas WHERE tarefaId = :tarefaId")
    fun getSubtarefasByTarefaFlow(tarefaId: String): Flow<List<RoomSubtarefa>>

    @Update
    suspend fun updateSubtarefa(subtarefa: RoomSubtarefa)

    @Delete
    suspend fun deleteSubtarefa(subtarefa: RoomSubtarefa)


    // --- Lembretes (Comum a Eventos/Tarefas operacionais) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLembrete(lembrete: RoomLembrete)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLembretes(lembretes: List<RoomLembrete>)

    @Query("DELETE FROM lembretes WHERE referenciaId = :referenciaId")
    suspend fun deleteLembretesByReferencia(referenciaId: String)

    @Query("SELECT * FROM lembretes WHERE referenciaId = :referenciaId")
    suspend fun getLembretesByReferencia(referenciaId: String): List<RoomLembrete>

    @Query("SELECT * FROM lembretes WHERE referenciaId = :referenciaId")
    fun getLembretesByReferenciaFlow(referenciaId: String): Flow<List<RoomLembrete>>
}
