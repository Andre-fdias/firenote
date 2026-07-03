# Guia de Migração de Dados - Fire Notes V6

Este documento orienta o processo de migração da infraestrutura legada baseada no Supabase para o banco de dados Room SQLite local.

## Estrutura de Migração Automática

1.  **Leitura do Banco Existente**:
    - Ao iniciar a versão V6, se houver registros locais temporários ou pendências na fila de sincronização offline antiga, o sistema executa a migração automática mapeando as estruturas JSON de DTOs anteriores e persistindo-as nas novas tabelas Room.

2.  **Inexistência de Perda de Dados**:
    - O banco de dados Room executa a estratégia `fallbackToDestructiveMigration` apenas em caso de inconsistência de esquema severa e incorrigível.
    - O seeding pré-configurado repovoa os catálogos mestres imediatamente.

3.  **Localização de Imagens**:
    - As mídias e arquivos OCR armazenados temporariamente na pasta de cache são migrados para a estrutura permanente em `getExternalFilesDir(null)/FireNotes`.
