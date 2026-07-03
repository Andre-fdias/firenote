# Migração para Room - Fire Notes V6.0

Este documento descreve o processo de migração da persistência antiga baseada no Supabase para o banco de dados local Room SQLite.

## Remocao do Supabase
- Removidas todas as dependências do Supabase SDK (Auth, Database, Storage) do `build.gradle.kts`.
- Removidas todas as classes de configuração do cliente Supabase e migrações SQL remotas.
- O aplicativo agora funciona completamente offline, independente por dispositivo, sem conceito de login ou sincronização central.

## Estrutura das Entidades Room
- Criadas tabelas locais com chaves estrangeiras (`ON DELETE CASCADE`), transações e índices otimizados para:
  - Ocorrências
  - Pessoas
  - Documentos
  - Veículos
  - Viaturas
  - Militares
  - Vítimas
  - Apoios
  - Evidências
  - Timeline
  - Configurações
  - BackupLogs
