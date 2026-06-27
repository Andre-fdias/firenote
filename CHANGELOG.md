# 📈 Registro de Alterações - Fire Notes

Todas as alterações notáveis neste projeto serão documentadas neste arquivo de acordo com as especificações do [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/).

---

## [1.0.0-alpha01] - 2026-06-27

### Adicionado
- **Configurações Globais do Projeto:**
  - Criação do Version Catalog (`gradle/libs.versions.toml`) com centralização de dependências estáveis (Room, Hilt, ML Kit, CameraX, Compose MD3, WorkManager, Coroutines).
  - Criação dos arquivos de compilação globais (`build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `local.properties`).
- **Módulos do Sistema:**
  - Criação e inicialização física de 19 módulos Android Gradle divididos em `:app`, `:core:*` e `:features:*`.
- **Modelos de Domínio Comum:**
  - Criação das classes de dados puras (`Occurrence`, `Vehicle`, `Military`, `Person`, `Photo`, `Document`, `Audio`) livres de dependências externas.
- **Banco de Dados Local (Room):**
  - Implementação das entidades SQLite locais correspondentes com chaves estrangeiras com ação de deleção em cascata (`onDelete = CASCADE`) e indexação.
  - Implementação dos conversores personalizados (`Converters`) para objetos de data/hora (Java 8 time APIs) e enums.
  - Implementação da relação aninhada profunda `OccurrenceWithDetails` para permitir a carga total dos grafos de atendimento operacional em lote.
  - Definição da `OccurrenceDao` contendo transações para salvar com segurança registros consolidados e remover órfãos em edições, além de uma consulta complexa para a funcionalidade de pesquisa avançada por texto.
- **Injeção de Dependências:**
  - Configuração do módulo de banco de dados (`DatabaseModule`) do Hilt provendo a base do SQLite, a DAO e fazendo o binding do `OccurrenceRepository`.
- **Navegação & Interface:**
  - Criação de classes seladas de rotas de navegação (`Screen.kt`) acessíveis a todas as features.
  - Implementação do design system baseado no Material Design 3 (`Color.kt`, `Theme.kt`, `Type.kt`) personalizado para o Corpo de Bombeiros em tons de vermelho com suporte a temas claros, escuros e cores dinâmicas.
  - Estruturação de placeholders modernos de interface (Compose Screens) para Login, Dashboard, Registro, Edição, Detalhes, Busca, Configurações, Backup e Splash.
  - Configuração do `NavHost` central e MainActivity acoplada ao Dagger Hilt no módulo `:app`.
- **Manifesto e Recursos:**
  - Definição do `strings.xml` com o nome do projeto.
  - Criação do `AndroidManifest.xml` mapeando permissões operacionais do app (Internet, GPS, Câmera, Gravação de Áudio).
- **Documentação de Engenharia:**
  - Criação de arquivos detalhados (`README.md`, `ROADMAP.md`, `ARCHITECTURE.md`, `DATABASE.md`, `CHANGELOG.md`, `TODO.md`) com diagramas de dependências e banco em Mermaid.
