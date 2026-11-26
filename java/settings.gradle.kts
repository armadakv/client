/*
 * Copyright JAMF Software, LLC
 */

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "armada-java"
include("armada-java-grpc")
include("armada-java-core")
include("armada-java-spring-data")
include("armada-java-test")
