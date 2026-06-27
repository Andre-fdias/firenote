pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Fire Notes"

// Módulo principal do app
include(":app")

// Módulos Core (reutilizáveis e de infraestrutura)
include(":core:common")
include(":core:database")
include(":core:drive")
include(":core:ocr")
include(":core:camera")
include(":core:location")
include(":core:network")

// Módulos de Feature (funcionalidades específicas do app)
include(":features:dashboard")
include(":features:login")
include(":features:occurrence")
include(":features:vehicles")
include(":features:military")
include(":features:people")
include(":features:documents")
include(":features:photos")
include(":features:settings")
include(":features:search")
include(":features:backup")
