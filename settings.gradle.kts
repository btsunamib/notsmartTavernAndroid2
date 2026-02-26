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

rootProject.name = "AndroidNativeSillyTavern"
include(
    ":app",
    ":core-model",
    ":core-network",
    ":core-storage",
    ":feature-chat",
    ":feature-console",
    ":feature-import",
)
