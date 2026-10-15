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
    }
}

rootProject.name = "GrammarAI"
include(":app")

// Spanish On-Demand Asset Packs (10-lesson tiers)
include(":audio_pack_es_11_20")
include(":audio_pack_es_21_30")
include(":audio_pack_es_31_40")
include(":audio_pack_es_41_50")
include(":audio_pack_es_51_60")
include(":audio_pack_es_61_70")
include(":audio_pack_es_71_80")
include(":audio_pack_es_81_90")

// English On-Demand Asset Packs (10-lesson tiers)
include(":audio_pack_en_11_20")
include(":audio_pack_en_21_30")
include(":audio_pack_en_31_40")
include(":audio_pack_en_41_50")
include(":audio_pack_en_51_60")
include(":audio_pack_en_61_70")
include(":audio_pack_en_71_80")
include(":audio_pack_en_81_90")