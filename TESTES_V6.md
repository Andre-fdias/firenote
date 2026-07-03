# Relatório de Testes de Homologação - Fire Notes V6

Este relatório consolida os testes funcionais realizados para validar a estabilidade das novas rotinas offline e de backup.

## Casos de Teste Executados

1.  **Testes de Room Database**:
    - Gravação e leitura de ocorrências completas (com endereços, viaturas, militares, veículos e vítimas).
    - Verificação de chaves estrangeiras e integridade referencial com exclusão em cascata.
    - Seeding inicial de catálogos mestres executado e validado.

2.  **Testes de Backup (Google Drive / SAF)**:
    - Simulação de backup manual enviando o arquivo `firenotes_backup.zip` compactado para a URI selecionada via SAF.
    - Teste de restauro de dados: o banco foi devidamente substituído pelo backup e todas as mídias foram descompactadas em seus diretórios locais correspondentes.

3.  **Testes de Usabilidade MD3**:
    - Alternância rápida e fluida entre as abas da Bottom Navigation bar.
    - Troca em tempo real do tema selecionado (Claro/Escuro/Automático).
