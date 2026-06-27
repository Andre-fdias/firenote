# 🛡️ Arquitetura do Fire Notes

Este projeto segue os princípios de **Clean Architecture**, **MVVM (Model-View-ViewModel)**, **SOLID** e desenvolvimento Android moderno baseado em componentes reativos (Coroutines, Flow, StateFlow).

---

## 📋 Diretrizes Gerais de Engenharia

1.  **Single Source of Truth (SSOT):** O banco de dados Room local representa a única fonte de verdade para a interface do usuário. Toda alteração passa por ele e é observada como um fluxo reativo (`Flow<List<Occurrence>>`).
2.  **Unidirectional Data Flow (UDF):** O fluxo de dados da tela segue o padrão de estados unidirecionais:
    *   **UiState:** O ViewModel expõe um `StateFlow` único contendo o estado imutável da tela.
    *   **UiEvent:** Ações do usuário (clique em botões, digitação) são disparadas para o ViewModel como eventos claros.
3.  **Dependências Baseadas em Abstração:** Nenhuma classe de UI acessa diretamente o banco de dados. Todo acesso é mediado por interfaces de repositórios localizadas no domínio comum (`core:common`).

---

## 🔄 Camadas Arquiteturais de Cada Módulo

Embora o projeto seja fisicamente dividido em módulos Gradle (`core` e `features`), internamente cada módulo de feature segue o padrão lógico de 3 camadas:

```
┌─────────────────────────────────────────────────────────┐
│                       Camada UI                         │
│   (Compose Views, ViewModels, UI State & UI Events)    │
└────────────────────────────┬────────────────────────────┘
                             │ depende de
                             ▼
┌─────────────────────────────────────────────────────────┐
│                     Camada Domain                       │
│       (Use Cases / Interactors, Domain Models,          │
│            Interfaces de Repositórios)                  │
└────────────────────────────▲────────────────────────────┘
                             │ implementa (Dependency Inversion)
                             │
┌─────────────────────────────────────────────────────────┐
│                      Camada Data                        │
│   (Room Entities, DAOs, Google Drive API, Data Sources, │
│            Implementações dos Repositórios)             │
└─────────────────────────────────────────────────────────┘
```

*   **Domain:** Contém as regras de negócio puras em Kotlin. Não depende de bibliotecas Android (Room, Compose, etc.). Define o modelo de dados de domínio e as interfaces que o sistema deve cumprir.
*   **Data:** Responsável pela persistência local (Room) ou remota (Drive API). Implementa as interfaces do domínio e converte dados de infraestrutura em modelos puros de negócio.
*   **UI (Presentation):** Componentes declarativos do Jetpack Compose que renderizam os estados da tela e capturam eventos de usuário, delegando-os para os ViewModels correspondentes.

---

## 🔗 Diagrama de Dependências entre Módulos

O diagrama abaixo mostra como os diferentes módulos do Gradle se relacionam:

```mermaid
graph TD
    %% Módulo Principal
    app[":app (Navegação & Theme)"]

    %% Módulos de Feature
    subgraph Features [Features]
        feat_login[":features:login"]
        feat_dash[":features:dashboard"]
        feat_occ[":features:occurrence"]
        feat_veh[":features:vehicles"]
        feat_mil[":features:military"]
        feat_peop[":features:people"]
        feat_docs[":features:documents"]
        feat_photos[":features:photos"]
        feat_settings[":features:settings"]
        feat_search[":features:search"]
        feat_backup[":features:backup"]
    end

    %% Módulos de Core
    subgraph Core [Core Infrastructure]
        core_common[":core:common (Domain Models & UI Bases)"]
        core_db[":core:database (Room SQL)"]
        core_drive[":core:drive (Google Drive Cloud)"]
        core_ocr[":core:ocr (ML Kit Recognition)"]
        core_cam[":core:camera (CameraX Wrapper)"]
        core_loc[":core:location (GPS/Fused Location)"]
        core_net[":core:network (Internet Connectivity)"]
    end

    %% Relações do app
    app --> feat_login
    app --> feat_dash
    app --> feat_occ
    app --> feat_search
    app --> feat_settings
    app --> feat_backup

    %% Relações das Features com Core
    feat_login --> core_common
    feat_dash --> core_common
    feat_occ --> core_common
    feat_occ --> core_db
    feat_occ --> core_cam
    feat_occ --> core_loc
    feat_search --> core_common
    feat_search --> core_db
    feat_settings --> core_common
    feat_backup --> core_common
    feat_backup --> core_drive
    feat_backup --> core_db

    %% Dependências internas do Core
    core_db --> core_common
    core_drive --> core_common
    core_ocr --> core_common
    core_cam --> core_common
    core_loc --> core_common
    core_net --> core_common
```

---

## 🏆 Padrões de Qualidade Adotados

*   **S.O.L.I.D.:**
    *   *Single Responsibility:* Módulos separados com DAOs e repositórios granulares.
    *   *Dependency Inversion:* Os módulos de UI e Use Cases interagem apenas com interfaces (e.g., `OccurrenceRepository`), cuja injeção é resolvida em tempo de compilação pelo Hilt.
*   **K.I.S.S. (Keep It Simple, Stupid):** Evitar complexidades desnecessárias no gerenciamento de estado.
*   **Y.A.G.N.I. (You Aren't Gonna Need It):** Codificar estritamente o que foi solicitado pelas especificações do Corpo de Bombeiros.
