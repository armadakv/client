/*
 * Copyright JAMF Software, LLC
 */

rootProject.name = "armada-java.conventions"

// buildSrc shares the SDK's version catalog.
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
