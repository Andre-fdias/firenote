# 📌 Lista de Tarefas Pendentes - Fire Notes

Este documento rastreia as tarefas operacionais que devem ser resolvidas nas próximas fases de desenvolvimento.

---

## 🛠️ Próximas Fases

### ⬜ Fase 2: Cadastro de Ocorrência
- [ ] Criar `SaveOccurrenceUseCase` na camada Domain de `:features:occurrence`.
- [ ] Criar `GetOccurrencesUseCase` e `GetOccurrenceByIdUseCase` em `:features:occurrence`.
- [ ] Implementar a UI do formulário de Nova Ocorrência com validação de campos (número interno, natureza, data e horário).
- [ ] Implementar o ViewModel da listagem observando as ocorrências do repositório em tempo real.
- [ ] Implementar a UI da Lista de Ocorrências (Cards com status, data e número).
- [ ] Criar testes unitários para o ViewModel e os Use Cases de ocorrência.

### ⬜ Fase 3: Viaturas
- [ ] Atualizar o formulário de ocorrência para permitir a adição de viaturas em lote.
- [ ] Desenvolver a UI de formulário de viaturas (prefixo, KM saída/local/regresso, horários correspondentes).
- [ ] Implementar regras de negócio para consistência de quilometragem (KM chegada deve ser >= KM saída, etc.).
- [ ] Adicionar testes de comportamento de validação de dados de viaturas.

### ⬜ Fase 4: Militares
- [ ] Desenvolver a UI de atribuição de militares a viaturas no formulário operacional.
- [ ] Criar campos de preenchimento de RE (Registro Estatístico), nome, graduação e função na viatura (Motorista, Comandante, Auxiliar).
- [ ] Implementar validação para garantir que pelo menos um militar esteja associado a cada viatura adicionada.
- [ ] Criar testes unitários para validar a lógica de salvamento e recuperação de militares.

### ⬜ Fase 5: Envolvidos
- [ ] Desenvolver o formulário de pessoas envolvidas (vítimas, testemunhas, condutores).
- [ ] Adicionar máscaras de digitação para CPF, RG, CNH e telefone na interface do usuário.
- [ ] Adicionar validação de CPF nas regras de negócio da camada Domain.
- [ ] Criar testes de validação de CPF e mapeamento de dados de envolvidos.

### ⬜ Fase 6: Fotos e Áudio
- [ ] Configurar e instanciar o CameraX em `:core:camera`.
- [ ] Criar composable de Câmera Customizada na feature `:features:photos` para capturas rápidas.
- [ ] Configurar gravação de áudio nativa (`MediaRecorder`) encapsulada em um gerenciador.
- [ ] Salvar mídias em arquivos privados dentro de `Context.getExternalFilesDir()` e gravar caminhos na base Room.
- [ ] Testar a gravação física e permissões em tempo de execução.

### ⬜ Fase 7: OCR de Documentos
- [ ] Integrar ML Kit Text Recognition em `:core:ocr`.
- [ ] Criar o `DocumentOcrAnalyzer` processando frames ou imagens estáticas de RG, CNH, CPF e CRLV.
- [ ] Implementar expressões regulares (Regex) de extração automática de dados dos documentos.
- [ ] Desenvolver a rotina de preenchimento automático no formulário de envolvidos.
- [ ] Escrever testes de extração OCR com dados mockados.

### ⬜ Fase 8: Autenticação e Nuvem
- [ ] Configurar o Google Sign-In SDK na feature `:features:login`.
- [ ] Configurar credenciais OAuth do Google Drive e escopo `drive.appdata` ou `drive.file`.
- [ ] Desenvolver o client HTTP de comunicação de arquivos do Drive em `:core:drive`.
- [ ] Criar testes de upload e autenticação.

### ⬜ Fase 9: Pesquisa Operacional
- [ ] Conectar a barra de pesquisa da UI ao Use Case `SearchOccurrencesUseCase`.
- [ ] Implementar debounce de digitação (e.g. 300ms) usando Coroutines Flow para reduzir acessos ao banco de dados.
- [ ] Criar testes unitários para a funcionalidade de busca da DAO e do repositório.

### ⬜ Fase 10: Backup Automático
- [ ] Desenvolver o `SyncWorker` herdando de `CoroutineWorker` no módulo `:features:backup`.
- [ ] Configurar constraints no WorkManager (conexão Wi-Fi ou qualquer internet disponível).
- [ ] Implementar serialização do JSON da ocorrência e envio de mídias estruturado em pastas por Ano/Mês/Ocorrência no Google Drive.
- [ ] Atualizar status da ocorrência local para `SYNCED` após conclusão bem-sucedida.
- [ ] Adicionar testes de integração para o agendador e o sincronizador.
