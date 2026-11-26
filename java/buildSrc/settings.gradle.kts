/*
 * Copyright JAMF Software, LLC
 */

rootProject.name = "armada-java.conventions"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
