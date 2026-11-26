/*
 * Copyright JAMF Software, LLC
 */

plugins {
    id("armada.library-conventions")
}

description = "Spring Data KeyValue support for Armada KV."

dependencies {
    api(libs.spring.data.keyvalue)
    api(libs.spring.context.support)
    api(project(":armada-java-core"))

    implementation(libs.jackson.core)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.xml)
}
