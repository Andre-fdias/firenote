# Relatório de Implementação - Fire Notes V6

Este documento apresenta o escopo da grande refatoração de infraestrutura da versão V6 do aplicativo de registro de emergências.

## Mudanças e Avanços Arquiteturais

1.  **Eliminação do Supabase**:
    - Removidos todos os acessos diretos à rede do Supabase, incluindo queries PostgREST e upload de arquivos de imagem no Supabase Storage.
    - O aplicativo é agora **100% autônomo e offline-first**.

2.  **Infraestrutura com Room Database**:
    - Desenvolvida a base local relacional no SQLite com o uso da biblioteca Room.
    - Seeding inicial configurado na criação do banco para carregar as listas de viaturas master, militares da corporação e órgãos de apoio.

3.  **Sistema de Backups via SAF**:
    - Desenvolvida a integração com o Storage Access Framework (SAF) para gravação manual e agendada de arquivos `firenotes_backup.zip` diretamente na pasta do Google Drive do usuário.

4.  **Interface Moderna Material Design 3**:
    - Implementação de um menu de Bottom Navigation fixo nas telas centrais da aplicação.
    - Refatoração dos temas Light e Dark com troca de tema instantânea.
