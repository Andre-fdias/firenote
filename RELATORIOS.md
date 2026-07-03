# Módulo de Relatórios e Exportações - Fire Notes V5.3

Este documento reporta as ferramentas e opções de geração de relatórios oficiais e exportações de dados operacionais.

## Funcionalidades de Relatórios

1. **Acesso**:
   - Ícone de compartilhamento no cabeçalho do `HomeScreen` que redireciona o operador para a tela de relatórios e exportações.

2. **Relatório de Ocorrência Completo (PDF)**:
   - Permite selecionar qualquer ocorrência da base para gerar um documento PDF completo formatado.
   - O PDF inclui dados de identificação, localização, viaturas e militares escalados, veículos apreendidos/envolvidos, vítimas, timeline e histórico narrativo.
   - Utiliza a classe nativa do Android `android.graphics.pdf.PdfDocument` garantindo total leveza e portabilidade.

3. **Relatório do Período Consolidado**:
   - O operador informa a data inicial e final e pode gerar o sumário contendo todas as ocorrências mapeadas e suas estatísticas de vítimas, viaturas e meios empregados.

4. **Formatos de Exportação**:
   - **PDF**: Relatório detalhado.
   - **Excel (XLS) / CSV**: Gera arquivo tabular compatível com Excel contendo os registros consolidados da guarnição, facilitando auditoria e integração com sistemas de inteligência corporativa.
   - **Mecanismo de Compartilhamento**: Integração direta com a API de compartilhamento nativa (`Intent.ACTION_SEND` e `FileProvider`), permitindo envio rápido via e-mail, WhatsApp ou exportação em nuvem.
