package com.example.firenotes.data.local

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.firenotes.data.local.dao.OcorrenciaDao
import com.example.firenotes.data.local.dao.HomeOperationalDao
import com.example.firenotes.data.local.dao.CalendarDao
import com.example.firenotes.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        RoomProntidaoDia::class,
        // Entidades do Calendário e Notificações (V8 - V11)
        RoomEscalaConfig::class,
        RoomEquipe::class,
        RoomTurno::class,
        RoomCalendarEvento::class,
        RoomCalendarTarefa::class,
        RoomNotificacao::class,
        RoomCalendarSettings::class,
        // V13
        RoomSubtarefa::class,
        RoomLembrete::class
    ],
    version = 15,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ocorrenciaDao(): OcorrenciaDao
    abstract fun homeOperationalDao(): HomeOperationalDao
    abstract fun calendarDao(): CalendarDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        @Volatile
        private var isCreatedJustNow = false

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `tarefas` (`id` TEXT NOT NULL, `titulo` TEXT NOT NULL, `concluida` INTEGER NOT NULL, `data` TEXT NOT NULL, `categoria` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `eventos_agenda` (`id` TEXT NOT NULL, `titulo` TEXT NOT NULL, `descricao` TEXT, `data` TEXT NOT NULL, `horaInicio` TEXT, `horaFim` TEXT, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `prontidao_dias` (`data` TEXT NOT NULL, `escala` TEXT NOT NULL, PRIMARY KEY(`data`))")
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tarefas` ADD COLUMN `descricao` TEXT")
                db.execSQL("ALTER TABLE `tarefas` ADD COLUMN `prioridade` TEXT NOT NULL DEFAULT 'MEDIA'")
                db.execSQL("ALTER TABLE `tarefas` ADD COLUMN `criadoEm` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `tarefas` ADD COLUMN `concluidoEm` INTEGER")
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tarefas` ADD COLUMN `hora` TEXT")
                db.execSQL("ALTER TABLE `eventos_agenda` ADD COLUMN `tipo` TEXT")
            }
        }

        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `configuracoes` (`id` TEXT NOT NULL DEFAULT 'global_config', `tema` TEXT NOT NULL DEFAULT 'Automático', `backupAutomatico` TEXT NOT NULL DEFAULT 'Desativado', `backupSomenteWifi` INTEGER NOT NULL DEFAULT 1, `backupUriSaf` TEXT, `ultimoBackupData` TEXT, `ultimoBackupTamanho` INTEGER NOT NULL DEFAULT 0, `ultimoBackupStatus` TEXT, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `backup_log` (`id` TEXT NOT NULL, `dataHora` TEXT NOT NULL, `tipo` TEXT NOT NULL, `status` TEXT NOT NULL, `tamanho` INTEGER NOT NULL, `mensagem` TEXT, PRIMARY KEY(`id`))")
            }
        }

        private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `viaturas_master` (`id` TEXT NOT NULL, `prefixo` TEXT NOT NULL, `placa` TEXT, `tipo` TEXT NOT NULL, `marca` TEXT, `modelo` TEXT, `postiFixo` TEXT, `status` TEXT NOT NULL, `capacidadeEquipe` INTEGER NOT NULL, `observacoes` TEXT, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `militares_master` (`id` TEXT NOT NULL, `re` TEXT NOT NULL, `nomeCompleto` TEXT NOT NULL, `nomeGuerra` TEXT NOT NULL, `postoGraduacao` TEXT NOT NULL, `funcaoHabitual` TEXT, `subunidade` TEXT, `status` TEXT NOT NULL, `telefone` TEXT, `fotoUrl` TEXT, PRIMARY KEY(`id`))")
            }
        }

        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `viaturas_ocorrencia` (`id` TEXT NOT NULL, `ocorrenciaId` TEXT NOT NULL, `prefixo` TEXT NOT NULL, `tipo` TEXT NOT NULL, `placa` TEXT, `kmSaida` REAL, `kmChegada` REAL, `horarioDespacho` TEXT, `horarioChegadaLocal` TEXT, `horarioTermino` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`ocorrenciaId`) REFERENCES `ocorrencias`(`id`) ON DELETE CASCADE)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `militares_viatura` (`id` TEXT NOT NULL, `viaturaOcorrenciaId` TEXT NOT NULL, `re` TEXT NOT NULL, `nomeGuerra` TEXT NOT NULL, `postoGraduacao` TEXT NOT NULL, `funcaoNaViatura` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`viaturaOcorrenciaId`) REFERENCES `viaturas_ocorrencia`(`id`) ON DELETE CASCADE)")
            }
        }

        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `escala_config` (`id` TEXT NOT NULL, `nome` TEXT NOT NULL, `trabalhoHoras` INTEGER NOT NULL, `descansoHoras` INTEGER NOT NULL, `quantidadeTurnos` INTEGER NOT NULL, `ativa` INTEGER NOT NULL, `descricao` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `equipes` (`id` TEXT NOT NULL, `nome` TEXT NOT NULL, `sigla` TEXT NOT NULL, `corFundo` TEXT NOT NULL, `corTexto` TEXT NOT NULL, `corBorda` TEXT, `escalaId` TEXT, `dataInicial` TEXT NOT NULL, `ordemTurno` INTEGER NOT NULL, `ativa` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`escalaId`) REFERENCES `escala_config`(`id`) ON DELETE SET NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `turnos` (`id` TEXT NOT NULL, `nome` TEXT NOT NULL, `horaInicio` TEXT NOT NULL, `horaTermino` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `calendar_eventos` (`id` TEXT NOT NULL, `titulo` TEXT NOT NULL, `descricao` TEXT NOT NULL, `data` TEXT NOT NULL, `hora` TEXT, `local` TEXT, `categoria` TEXT NOT NULL, `cor` TEXT NOT NULL, `recorrencia` TEXT NOT NULL, `lembreteMinutos` INTEGER, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `calendar_tarefas` (`id` TEXT NOT NULL, `titulo` TEXT NOT NULL, `descricao` TEXT NOT NULL, `data` TEXT NOT NULL, `hora` TEXT, `prioridade` TEXT NOT NULL, `status` TEXT NOT NULL, `categoria` TEXT NOT NULL, `responsavel` TEXT, `anexos` TEXT, `checklistJson` TEXT, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `notificacoes_historico` (`id` TEXT NOT NULL, `categoria` TEXT NOT NULL, `titulo` TEXT NOT NULL, `descricao` TEXT NOT NULL, `data` TEXT NOT NULL, `hora` TEXT NOT NULL, `prioridade` TEXT NOT NULL, `lida` INTEGER NOT NULL, `origem` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `calendar_settings` (`id` TEXT NOT NULL, `mostrarPopupInicial` INTEGER NOT NULL, `badgeHabilitado` INTEGER NOT NULL, `somHabilitado` INTEGER NOT NULL, `vibracaoHabilitada` INTEGER NOT NULL, `lembretesAntecipadosMinutos` INTEGER NOT NULL, `popupExibidoHoje` TEXT, PRIMARY KEY(`id`))")
            }
        }

        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `calendar_settings` ADD COLUMN `calendarioConfigurado` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migration 9-10 no-op ou schema update se necessário
            }
        }

        private val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `equipes` ADD COLUMN `horaInicio` TEXT NOT NULL DEFAULT '07:00'")
                db.execSQL("ALTER TABLE `equipes` ADD COLUMN `horaTermino` TEXT NOT NULL DEFAULT '07:00'")
            }
        }

        private val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `eventos_agenda` ADD COLUMN `local` TEXT")
            }
        }

        private val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `subtarefas` (`id` TEXT NOT NULL, `tarefaId` TEXT NOT NULL, `titulo` TEXT NOT NULL, `concluida` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`tarefaId`) REFERENCES `tarefas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_subtarefas_tarefaId` ON `subtarefas` (`tarefaId`)")
                
                db.execSQL("CREATE TABLE IF NOT EXISTS `lembretes` (`id` TEXT NOT NULL, `referenciaId` TEXT NOT NULL, `tipoReferencia` TEXT NOT NULL, `minutosAntes` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_lembretes_referenciaId` ON `lembretes` (`referenciaId`)")
            }
        }

        private val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tarefas` ADD COLUMN `escalaId` TEXT")
                db.execSQL("ALTER TABLE `eventos_agenda` ADD COLUMN `escalaId` TEXT")
                db.execSQL("ALTER TABLE `calendar_tarefas` ADD COLUMN `escalaId` TEXT")
                db.execSQL("ALTER TABLE `calendar_eventos` ADD COLUMN `escalaId` TEXT")
            }
        }

        private val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `subtarefas` ADD COLUMN `parentId` TEXT")
                db.execSQL("ALTER TABLE `tarefas` ADD COLUMN `cor` TEXT NOT NULL DEFAULT '#10B981'")
                db.execSQL("ALTER TABLE `eventos_agenda` ADD COLUMN `cor` TEXT NOT NULL DEFAULT '#3B82F6'")
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
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                    MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                    MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                    MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
                    MIGRATION_13_14, MIGRATION_14_15
                )
                .fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = instance

                if (isCreatedJustNow) {
                    isCreatedJustNow = false
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            seedCatalogs(instance.ocorrenciaDao())
                            seedCalendarDefaultSettings(instance.calendarDao())
                        } catch (e: Exception) {
                            android.util.Log.e("FireDatabase", "Erro ao semear catálogos: ${e.message}", e)
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
            val agencies = listOf(
                RoomOrgaoApoio("orgao_1", "Policia Militar", "PM"),
                RoomOrgaoApoio("orgao_2", "SAMU", "SAMU"),
                RoomOrgaoApoio("orgao_3", "Defesa Civil", "DC"),
                RoomOrgaoApoio("orgao_4", "Guarda Municipal", "GM")
            )
            agencies.forEach { dao.insertOrgaoApoio(it) }

            val viaturas = listOf(
                RoomViaturaMaster("v_1", "UR-15201", null, "Resgate", "Chevrolet", "S10", "15º GB", "Ativo", 5, "Maca, O2, KED"),
                RoomViaturaMaster("v_2", "ABS-15012", null, "Autobomba", "Scania", "P310", "15º GB", "Ativo", 6, "Mangueiras, Desencarcerador"),
                RoomViaturaMaster("v_3", "VO-15001", null, "Oficial de Área", "Toyota", "Hilux", "15º GB", "Ativo", 2, "Rádio, EPI")
            )
            viaturas.forEach { dao.insertViaturaMaster(it) }

            val militares = listOf(
                RoomMilitarMaster("m_1", "123456-7", "Carlos Souza", "Sgt Souza", "3º SGT PM", "Encarregado", "1º Pelotão", "Ativo", null, null),
                RoomMilitarMaster("m_2", "765432-1", "Marcos Silva", "Cb Silva", "CB PM", "Motorista", "1º Pelotão", "Ativo", null, null),
                RoomMilitarMaster("m_3", "987654-3", "João Santos", "Sd Santos", "SD PM", "Socorrista", "1º Pelotão", "Ativo", null, null)
            )
            militares.forEach { dao.insertMilitarMaster(it) }

            dao.insertConfiguracao(RoomConfiguracao())
        }

        private suspend fun seedCalendarDefaultSettings(dao: CalendarDao) {
            dao.insertSettings(
                RoomCalendarSettings(
                    id = "global_calendar_settings",
                    mostrarPopupInicial = true,
                    badgeHabilitado = true,
                    somHabilitado = true,
                    vibracaoHabilitada = true,
                    lembretesAntecipadosMinutos = 15,
                    popupExibidoHoje = null,
                    calendarioConfigurado = false
                )
            )
        }
    }
}
