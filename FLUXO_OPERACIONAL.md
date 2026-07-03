# Fluxo Operacional Guiado - Fire Notes V5

O Modo Assistido organiza a coleta de informações em campo seguindo uma trilha linear intuitiva:

## Etapas do Processo

```
[Passo 1] Dados Iniciais e GPS da Cena
   ↓
[Passo 2] Seleção da Natureza
   ↓
[Passo 3] Viaturas Empregadas e Escala de Militares
   ↓
[Passo 4] Captura Sequencial de Documentos (Batch OCR)
   ↓
[Passo 5] Cadastro de Veículos (Auto-preenchido e Manual)
   ↓
[Passo 6] Registro de Vítimas e Sinais Vitais
   ↓
[Passo 7] Evidências e Fotos Classificadas
   ↓
[Passo 8] Geração de Histórico Narrativo Automático
   ↓
[Passo 9] Validação no Checklist e Encerramento
```

1. **Dados Iniciais (Passo 1)**: O operador informa o talão e clica em **"CAPTURAR GPS DA CENA"** para auto-preenchimento das coordenadas e do endereço aproximado via geocodificação reversa.
2. **Seleção de Natureza (Passo 2)**: Apresenta botões coloridos gigantes com os códigos operacionais (Incêndio, Salvamento, Acidente de Trânsito, etc.).
3. **Equipes e Viaturas (Passo 3)**: Aloca viaturas e militares. A seleção de militar impede a inserção de REs repetidos na mesma guarnição.
4. **Captura de Documentos em Lote (Passo 4)**: O operador clica sucessivas vezes no botão **"FOTOGRAFAR DOCUMENTO"** sem interrupção de formulários intermediários. Ao finalizar a rodada de fotos, clica em **"ESCANEAR"** para processar em segundo plano e gerar a lista de pessoas e veículos.
5. **Veículos e Associações (Passo 5)**: Lista os carros encontrados e permite associar condutores e proprietários. A associação de CPF é feita de forma automática se houver correspondência de documentos.
6. **Vítimas (Passo 6)**: Mostra as pessoas encontradas no OCR, permitindo registrar Escala Glasgow, P.A., Pulso, saturação de oxigênio e hospital de destino.
7. **Cena da Emergência (Passo 7)**: Registra evidências e fotos classificadas (Cena, Vítima, Veículo, etc.).
8. **Histórico (Passo 8)**: Um botão **"GERAR HISTÓRICO ESTRUTURADO"** usa o gerador narrativo em lote para redigir o sumário automaticamente.
9. **Conclusão (Passo 9)**: O checklist indica se há pendências. Clicar em qualquer item vermelho redireciona instantaneamente o operador para a respectiva etapa para correção imediata.
