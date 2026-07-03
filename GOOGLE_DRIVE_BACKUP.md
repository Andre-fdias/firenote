# Manual de Backups no Google Drive - Fire Notes V6

Este documento orienta o operador de campo sobre a mecânica de gravação e restauração dos dados.

## Integração via Storage Access Framework (SAF)

Em obediência às diretrizes da LGPD, o aplicativo opera sem servidores centrais. A conexão com o Google Drive do bombeiro utiliza a API nativa de acesso a documentos (SAF):

1.  **Escolha de Pasta (Permissão Persistente)**:
    - No primeiro backup ou ao acessar as Configurações, o operador clica em "Selecionar Pasta".
    - O sistema operacional abre a tela do seletor SAF do Google Drive.
    - O operador escolhe ou cria a pasta de destino (ex: `Backups FireNotes`).
    - O app adquire permissão persistente via `takePersistableUriPermission`, dispensando solicitações futuras.

2.  **Mecânica de Backup**:
    - O banco de dados SQLite local (`firenotes.db`) e a pasta interna de mídias (`FireNotes/`) são empacotados em um único arquivo compactado `firenotes_backup.zip`.
    - O arquivo é copiado para a pasta escolhida em background.

3.  **Frequência Automática**:
    - Configurável nas opções de backup: **Diário**, **Semanal**, **Mensal** ou **Desativado**.
    - Opcional de sincronizar **Somente em redes Wi-Fi** para poupar franquias de internet móvel.
