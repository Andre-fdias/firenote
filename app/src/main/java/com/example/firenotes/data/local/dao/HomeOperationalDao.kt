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

    @Query("SELECT * FROM tarefas WHERE data = :data ORDER BY id ASC")
    fun getTarefasForDayFlow(data: String): Flow<List<RoomTarefa>>

    @Query("SELECT * FROM tarefas WHERE data = :data ORDER BY id ASC")
    suspend fun getTarefasForDay(data: String): List<RoomTarefa>

    @Query("SELECT * FROM tarefas ORDER BY data ASC")
    fun getAllTarefasFlow(): Flow<List<RoomTarefa>>

    // --- Eventos da Agenda ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvento(evento: RoomEventoAgenda)

    @Query("DELETE FROM eventos_agenda WHERE id = :id")
    suspend fun deleteEvento(id: String)

    @Query("SELECT * FROM eventos_agenda WHERE data = :data ORDER BY horaInicio ASC")
    fun getEventosForDayFlow(data: String): Flow<List<RoomEventoAgenda>>

    @Query("SELECT * FROM eventos_agenda WHERE data = :data ORDER BY horaInicio ASC")
    suspend fun getEventosForDay(data: String): List<RoomEventoAgenda>

    @Query("SELECT * FROM eventos_agenda ORDER BY data ASC, horaInicio ASC")
    fun getAllEventosFlow(): Flow<List<RoomEventoAgenda>>

    // --- Prontidão / Escala ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProntidao(prontidao: RoomProntidaoDia)

    @Query("SELECT * FROM prontidao_dias WHERE data = :data")
    suspend fun getProntidaoForDay(data: String): RoomProntidaoDia?

    @Query("SELECT * FROM prontidao_dias")
    fun getAllProntidoesFlow(): Flow<List<RoomProntidaoDia>>
}
