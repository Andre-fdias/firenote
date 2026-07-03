# Módulo de Consultas de Ocorrências - Fire Notes V5.3

Este documento apresenta as funcionalidades e recursos do novo módulo de consulta de ocorrências.

## Funcionalidades do Módulo de Consulta

1. **Menu de Acesso**:
   - Adicionado o botão de pesquisa/lupa no cabeçalho do `HomeScreen` que redireciona o operador para a tela de consultas.

2. **Listagem Otimizada**:
   - Apresentação de ocorrências em cards detalhados contendo: Número do Talão, Data, Hora, Natureza (badge colorido correspondente), Cidade, Contagem de Vítimas, Contagem de Veículos e Status.

3. **Busca Global e Filtros Avançados**:
   - Campo de pesquisa global reativo que filtra por: Talão, Nome, CPF, Placa, Viatura, Militar, Documento e Histórico.
   - Painel flutuante de filtros específicos permitindo filtrar por: Data Inicial, Data Final, Natureza, Cidade, Bairro, Viatura, Militar, Placa, Vítima e Hospital.

4. **Duplicação de Ocorrências**:
   - Implementada a ação de **Duplicar** na própria listagem. O app recupera a ocorrência por ID e gera um novo rascunho completo contendo todos os veículos, vítimas e guarnições, limpando os campos específicos de tempo (Talão, Data, Hora) para edição.
