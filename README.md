# 🧾 Fire Notes

Aplicativo móvel offline-first de registro e gestão operacional de ocorrências em campo, projetado especificamente para o atendimento do **Corpo de Bombeiros**.

---

## 📋 Visão Geral

Durante o atendimento de emergências (combate a incêndios, salvamentos, resgates e atendimento pré-hospitalar), a agilidade e a confiabilidade das informações são críticas. O **Fire Notes** permite que os bombeiros registrem em tempo real dados de viaturas, militares envolvidos, vítimas/testemunhas, coordenadas de localização (GPS), registros em fotos, áudios explicativos e dados estruturados extraídos por OCR.

O aplicativo funciona de forma **100% offline** (utilizando persistência local com SQLite e Room) e sincroniza automaticamente com o **Google Drive** (como backup e armazenamento estruturado em JSON e arquivos de mídia) assim que uma conexão ativa com a internet é detectada via WorkManager.

---

## 🔧 Pilha Tecnológica (Stack)

*   **Linguagem:** Kotlin moderna (livre de Java/XML)
*   **Interface:** Jetpack Compose & Material Design 3 (UI declarativa e reativa)
*   **Arquitetura:** Clean Architecture & MVVM (Multi-módulo, orientado a Use Cases)
*   **Banco de Dados:** Room Database (SQLite offline-first com chaves estrangeiras e índices)
*   **Injeção de Dependências:** Hilt (Dagger Hilt)
*   **Navegação:** Navigation Compose (navegação de fluxo declarativa baseada em rotas)
*   **Assincronismo:** Kotlin Coroutines & Flow (StateFlow, UiState e UiEvent)
*   **Background Jobs:** WorkManager (sincronização automática resiliente em segundo plano)
*   **Multimídia:** CameraX (captura de imagens) e Gravação de Áudio nativa
*   **Inteligência Artificial:** Google ML Kit OCR (extração automática de texto de CNH, RG, CPF e CRLV)
*   **Integração de Nuvem:** Google Drive API & Google Sign-In (backup transparente)
*   **Outras bibliotecas:** DataStore (configurações do app), Coil (carregamento de imagens), Timber (logs)
*   **Qualidade:** JUnit e MockK (testes unitários e mocks)

---

## 🗂️ Estrutura do Projeto

O projeto é modularizado de forma a garantir isolamento de responsabilidades e tempos de compilação otimizados:

*   [`app/`](file:///home/andre/Repositorio/fire_notes/app/): Módulo orquestrador que aplica o plugin de aplicação, reúne as telas e implementa o grafo de navegação central.
*   [`core/`](file:///home/andre/Repositorio/fire_notes/core/): Infraestrutura e utilitários globais reutilizáveis.
    *   [`common/`](file:///home/andre/Repositorio/fire_notes/core/common/): Classes de domínio, modelos de dados comuns, base UI, temas, navegação abstrata e DataStore.
    *   [`database/`](file:///home/andre/Repositorio/fire_notes/core/database/): Banco Room, DAOs locais, relacionamentos e a implementação do repositório.
    *   [`drive/`](file:///home/andre/Repositorio/fire_notes/core/drive/): Lógica de autenticação e comunicação com a API do Google Drive.
    *   [`ocr/`](file:///home/andre/Repositorio/fire_notes/core/ocr/): Extração de dados de documentos via ML Kit.
    *   [`camera/`](file:///home/andre/Repositorio/fire_notes/core/camera/): Gerenciador da câmera com CameraX.
    *   [`location/`](file:///home/andre/Repositorio/fire_notes/core/location/): Captura automática de coordenadas geográficas.
    *   [`network/`](file:///home/andre/Repositorio/fire_notes/core/network/): Monitoramento de estado de conexão.
*   [`features/`](file:///home/andre/Repositorio/fire_notes/features/): Módulos isolados que representam telas e fluxos de negócio em MVVM.
    *   `dashboard/`, `login/`, `occurrence/`, `vehicles/`, `military/`, `people/`, `documents/`, `photos/`, `settings/`, `search/`, `backup/`

---

## 📖 Documentação Adicional

Para mais detalhes sobre as especificidades do projeto, consulte a documentação dedicada na raiz do repositório:

1.  [**Arquitetura do Sistema** (`ARCHITECTURE.md`)](file:///home/andre/Repositorio/fire_notes/ARCHITECTURE.md) - Fluxo de dados, diretrizes do SOLID, Clean Architecture e diagrama Mermaid de módulos.
2.  [**Modelo do Banco de Dados** (`DATABASE.md`)](file:///home/andre/Repositorio/fire_notes/DATABASE.md) - Entidades locais do Room, chaves estrangeiras, índices e diagrama de classes do banco SQLite.
3.  [**Roadmap do Projeto** (`ROADMAP.md`)](file:///home/andre/Repositorio/fire_notes/ROADMAP.md) - As 10 fases planejadas para o desenvolvimento completo.
4.  [**Registro de Alterações** (`CHANGELOG.md`)](file:///home/andre/Repositorio/fire_notes/CHANGELOG.md) - Histórico de commits, melhorias e correções efetuadas.
5.  [**Lista de Tarefas Pendentes** (`TODO.md`)](file:///home/andre/Repositorio/fire_notes/TODO.md) - O que precisa ser feito nos próximos passos.