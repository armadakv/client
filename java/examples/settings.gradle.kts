/*
 * Copyright JAMF Software, LLC
 */

rootProject.name = "armada-java.examples"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
include("spring-data-example")


includeBuild("../") {
    dependencySubstitution {
        substitute(module("io.armadakv:armada-java-grpc")).using(project(":armada-java-grpc"))
        substitute(module("io.armadakv:armada-java-core")).using(project(":armada-java-core"))
        substitute(module("io.armadakv:armada-java-spring-data")).using(project(":armada-java-spring-data"))
        substitute(module("io.armadakv:armada-java-test")).using(project(":armada-java-test"))
    }
}
