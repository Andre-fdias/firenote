# Changelog Oficial - Fire Notes V6

Todas as alterações desta versão estão listadas abaixo:

## [6.0.0] - 2026-07-02

### Adicionado
- Nova infraestrutura baseada no **Room Database** (SQLite local), contendo 17 tabelas relacionais com chaves estrangeiras, índices e exclusão em cascata.
- **BackupService**: empacotamento completo de banco de dados e pastas de mídias em arquivo ZIP enviado via Storage Access Framework (SAF) para o Google Drive.
- Nova aba de **Configurações** permitindo definir frequência de backup automático, backup somente em Wi-Fi e acionamento manual de backup e restauração.
- Novo layout baseado em **Material Design 3** para a Home, Top Bar e abas inferiores de Bottom Navigation.
- Suporte a temas Claro, Escuro e Automático com transição fluida e em tempo real.

### Removido
- Removida dependência da nuvem remota do Supabase. O aplicativo agora opera de forma 100% autônoma e independente por aparelho, sem autenticação central.
- Removido upload para Supabase Storage (substituído por armazenamento local estruturado em subpastas).
