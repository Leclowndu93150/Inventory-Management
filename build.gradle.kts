plugins {
    id("dev.prism")
}

group = "com.leclowndu93150"
version = "1.0.4"

prism {
    metadata {
        modId = "inventorymanagement"
        name = "Inventory Management Deluxe"
        description = ""
        license = "MIT"
        author("Leclowndu93150")
        author("Roundaround")
    }

    curseMaven()

    publishing {
        changelogFile = "CHANGELOG.md"
        type = STABLE

        curseforge {
            accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
            projectId = "1293085"
        }
    }

    version("1.20.1") {
        parchmentMinecraftVersion = "1.20.1"
        parchmentMappingsVersion = "2023.09.03"

        forge {
            loaderVersion = "47.3.0"
            loaderVersionRange = "[4,)"
        }
    }

    version("1.21.1") {
        parchmentMinecraftVersion = "1.21.1"
        parchmentMappingsVersion = "2024.11.17"

        neoforge {
            loaderVersion = "21.1.182"
            loaderVersionRange = "[4,)"

            dependencies {
                modRuntimeOnly("curse.maven:mekanism-268560:6486993")
                modRuntimeOnly("curse.maven:ellies-storage-options-1225370:6483390")
                modRuntimeOnly("curse.maven:kotlin-for-forge-351264:6497906")
                modRuntimeOnly("curse.maven:iron-chests-228756:5491156")
            }
        }
    }
}
