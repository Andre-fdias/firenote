# Fire Notes V5 - Implementação do Modo Assistido (Wizard)

Esta versão introduz o **Modo Assistido (Wizard Operacional)** no aplicativo Fire Notes, com foco em uma interface adaptada para uso sob estresse ou com luvas (Modo Campo), fluxo guiado passo a passo, recuperação de estado em caso de fechamento inesperado e processamento de OCR em lote.

## Resumo das Entregas

1. **Wizard State & Steps**: Mapeamento do fluxo de 9 etapas utilizando a estrutura `WizardStep` e o modelo de estado persistível `WizardState`.
2. **Wizard Control & Navigation**: Implementado a classe controladora `WizardViewModel` gerenciando o ciclo de vida do fluxo, injeções de localização, OCR em background e salvamento/recuperação do estado local em `SharedPreferences`.
3. **Modo Campo (UI Otimizada)**: Interface de alto contraste, botões grandes e espaçamento robusto em `OccurrenceWizardScreen.kt`. Inclui suporte a modo noturno direto no cabeçalho.
4. **OCR em Lote & Associações**: Fluxo contínuo de captura de fotos com a câmera e processamento posterior em fila de background com checagem automática de CPFs duplicados e associações inteligentes (CNH/CRLV).
5. **Checklist Final**: Validador dinâmico que impede o encerramento com pendências obrigatórias e permite clique direto sobre o item para navegação direta de correção.
6. **Timeline & Log de Auditoria**: Registro automatizado nas tabelas estruturadas de auditoria da V4 ao finalizar o Wizard.
