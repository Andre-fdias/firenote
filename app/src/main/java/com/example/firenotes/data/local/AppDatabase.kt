package com.example.firenotes.data.local

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.firenotes.data.local.dao.OcorrenciaDao
import com.example.firenotes.data.local.dao.HomeOperationalDao
import com.example.firenotes.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        RoomOcorrencia::class,
        RoomEndereco::class,
        RoomPessoa::class,
        RoomDocumento::class,
        RoomVeiculoMaster::class,
        RoomVeiculoOcorrencia::class,
        RoomViaturaMaster::class,
        RoomMilitarMaster::class,
        RoomViaturaOcorrencia::class,
        RoomMilitarViatura::class,
        RoomVitima::class,
        RoomOrgaoApoio::class,
        RoomApoioOcorrencia::class,
        RoomEvidencia::class,
        RoomTimelineEvento::class,
        RoomConfiguracao::class,
        RoomBackupLog::class,
        RoomTarefa::class,
        RoomEventoAgenda::class,
        RoomProntidaoDia::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ocorrenciaDao(): OcorrenciaDao
    abstract fun homeOperationalDao(): HomeOperationalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        @Volatile
        private var isCreatedJustNow = false

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tarefas` (
                        `id` TEXT NOT NULL, 
                        `titulo` TEXT NOT NULL, 
                        `concluida` INTEGER NOT NULL, 
                        `data` TEXT NOT NULL, 
                        `categoria` TEXT NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `eventos_agenda` (
                        `id` TEXT NOT NULL, 
                        `titulo` TEXT NOT NULL, 
                        `descricao` TEXT, 
                        `data` TEXT NOT NULL, 
                        `horaInicio` TEXT, 
                        `horaFim` TEXT, 
                        PRIMARY KEY(`id`)
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `prontidao_dias` (
                        `data` TEXT NOT NULL, 
                        `escala` TEXT NOT NULL, 
                        PRIMARY KEY(`data`)
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "firenotes.db"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        isCreatedJustNow = true
                    }
                })
                .addMigrations(MIGRATION_1_2)
                .build()
                
                INSTANCE = instance

                if (isCreatedJustNow) {
                    isCreatedJustNow = false
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            android.util.Log.d("FireDatabase", "🌱 Semeando catálogos do banco de dados pela primeira vez...")
                            seedCatalogs(instance.ocorrenciaDao())
                            android.util.Log.d("FireDatabase", "✅ Catálogos semeados com sucesso!")
                        } catch (e: Exception) {
                            android.util.Log.e("FireDatabase", "❌ Erro ao semear catálogos: ${e.message}", e)
                        }
                    }
                }
                
                instance
            }
        }

        fun closeDatabase() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        private suspend fun seedCatalogs(dao: OcorrenciaDao) {
            // Seed Support Agencies
            val agencies = listOf(
                RoomOrgaoApoio("orgao_1", "Policia Militar", "PM"),
                RoomOrgaoApoio("orgao_2", "SAMU", "SAMU"),
                RoomOrgaoApoio("orgao_3", "Defesa Civil", "DC"),
                RoomOrgaoApoio("orgao_4", "Concessionaria Rodoviaria", "CONCES")
            )
            agencies.forEach { dao.insertOrgaoApoio(it) }

            // Seed Master Viaturas
            val viaturas = listOf(
                RoomViaturaMaster("v_1", "UR-15201", null, "Resgate", "Chevrolet", "S10", "15º GB", "Ativo", 5, "Maca, O2, KED"),
                RoomViaturaMaster("v_2", "ABS-15012", null, "Autobomba", "Scania", "P310", "15º GB", "Ativo", 6, "Mangueiras, Desencarcerador"),
                RoomViaturaMaster("v_3", "ASE-15103", null, "Salvamento", "Ford", "Cargo", "15º GB", "Ativo", 4, "Bote, Cabos, Polias")
            )
            viaturas.forEach { dao.insertViaturaMaster(it) }

            // Seed Master Militares
            val militares = listOf(
                RoomMilitarMaster("m_1", "123456-7", "Carlos Souza", "Sgt Souza", "3º SGT PM", "Encarregado", "1º Pelotão", "Ativo", null, null),
                RoomMilitarMaster("m_2", "765432-1", "Marcos Silva", "Cb Silva", "CB PM", "Motorista", "1º Pelotão", "Ativo", null, null),
                RoomMilitarMaster("m_3", "987654-3", "Juliana Santos", "Sd Juliana", "SD PM", "Socorrista", "1º Pelotão", "Ativo", null, null)
            )
            militares.forEach { dao.insertMilitarMaster(it) }

            // Initialize Default Configuration
            dao.insertConfiguracao(RoomConfiguracao())
        }
    }
}
