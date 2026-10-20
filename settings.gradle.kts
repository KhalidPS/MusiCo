pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven{
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "MusiCo"
include(":app")
// The baseline profile generator module is local-only (gitignored), so CI builds without it.
// Its folder is "baselineProfile" (capital P): match that case exactly, since Linux CI is
// case-sensitive, and point the lowercase project path at it.
val baselineProfileDir = file("baselineProfile")
if (baselineProfileDir.isDirectory) {
    include(":baselineprofile")
    project(":baselineprofile").projectDir = baselineProfileDir
}
