# 🔄 Roadmap do Projeto - Fire Notes

O desenvolvimento do Fire Notes está dividido em 10 fases estratégicas. Isso nos permite construir uma base estável, testar cada componente isoladamente e evoluir o aplicativo de maneira sustentável.

---

## 📈 Status do Roadmap

| Fase | Tópico | Status | Descrição |
| :--- | :--- | :--- | :--- |
| **Fase 1** | **Estrutura, Arquitetura, Tema, Navegação e Banco** | **Concluído** | Configuração do gradle raiz, módulos, Version Catalog, injeção de dependências, banco Room completo e navegação base com Compose. |
| **Fase 2** | **Cadastro de ocorrência** | *Pendente* | Implementação da UI/Domain/Data para salvar/listar ocorrências (sem as relações complexas). |
| **Fase 3** | **Viaturas** | *Pendente* | Atribuição de N viaturas, controle de KM e horários de saída/local/regresso. |
| **Fase 4** | **Militares** | *Pendente* | Vínculo de múltiplos bombeiros militares às viaturas em atendimento (RE, graduação, função). |
| **Fase 5** | **Envolvidos** | *Pendente* | Vínculo de vítimas, testemunhas e condutores com RG, CPF, CNH e endereço. |
| **Fase 6** | **Fotos e Áudio** | *Pendente* | Captura física com CameraX e gravação de áudio em campo vinculados à ocorrência. |
| **Fase 7** | **OCR de Documentos** | *Pendente* | Reconhecimento de texto em RG, CNH, CRLV e CPF e preenchimento automático via ML Kit. |
| **Fase 8** | **Autenticação e Google Drive** | *Pendente* | Login social com Google e integração inicial com a API REST do Drive. |
| **Fase 9** | **Pesquisa Operacional** | *Pendente* | Filtros locais complexos no Room integrados à UI de busca. |
| **Fase 10** | **Backup e WorkManager** | *Pendente* | Sincronizador periódico em background via WorkManager enviando JSON e mídias para a nuvem. |

---

## 🎯 Detalhes da Fase Atual (Fase 1 - Concluída)

*   **Configuração de Build:** Definição do Version Catalog (`libs.versions.toml`) centralizando dependências e plugins.
*   **Módulos Declarados:** Criação de todos os 19 módulos no Gradle (`app`, 7 submódulos `core` e 11 submódulos `features`).
*   **Banco de Dados Local (Room):** Criação das entidades `Occurrence`, `Vehicle`, `Military`, `Person`, `Photo`, `Document` e `Audio`, com as chaves estrangeiras e índices correspondentes no SQLite. Mapeamento relacional completo com `OccurrenceWithDetails` e transações atômicas na `OccurrenceDao`.
*   **Navegação e Fluxo:** Mapeamento de rotas seladas em `core:common` e configuração do `NavHost` central no módulo `:app`.
*   **Tema MD3:** Implementação de uma paleta de cores harmoniosa em tons de vermelho bombeiro com suporte a light/dark mode e dynamic colors.
