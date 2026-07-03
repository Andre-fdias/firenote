# Arquitetura Room Database - Fire Notes V6

Este guia documenta o esquema de banco de dados e os relacionamentos locais implementados na versão V6.

## Estrutura de Entidades e Relacionamentos

Todas as tabelas foram criadas com suporte a chaves estrangeiras, índices de busca rápida e comportamento de exclusão em cascata:

1.  **`ocorrencias`**: Registro principal da emergência.
2.  **`enderecos`**: Dados de localização vinculados por chave estrangeira à ocorrência com `ON DELETE CASCADE`.
3.  **`pessoas`**: Catálogo único de indivíduos identificados por CPF.
4.  **`documentos`**: Fotos e OCR de credenciais como CNH, RG, vinculados a pessoas e ocorrências.
5.  **`veiculos_master`**: Catálogo de veículos salvos localmente.
6.  **`veiculos_ocorrencia`**: Vínculo transacional dos carros envolvidos no sinistro.
7.  **`vitimas`**: Registro de lesões, dados clínicos de sinais vitais e transporte de feridos.
8.  **`viaturas_ocorrencia` & `militares_viatura`**: Escala de equipes e movimentação operacional na ocorrência.
9.  **`apoio_ocorrencia`**: Registros de órgãos externos que apoiaram na emergência (SAMU, Concessionária).
10. **`evidencias`**: Arquivos de imagem e dados de localização capturados da cena.
11. **`timeline_eventos`**: Histórico detalhado de despacho, chegada e encerramento.
12. **`configuracoes`**: Preferências locais de tema e agendamento de backup.
13. **`backup_log`**: Relatório histórico de transações de backup realizadas.
