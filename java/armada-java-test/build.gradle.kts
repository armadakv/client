/*
 * Copyright JAMF Software, LLC
 */

plugins {
    id("armada.library-conventions")
}

dependencies {
    api(libs.grpc.core)
    api(libs.grpc.stub)
    api(libs.grpc.inprocess)
    api(libs.grpc.util)
    api(platform(libs.junit.bom))
    api(libs.junit.jupiter.api)
    api(libs.testcontainers.junit.jupiter)

    implementation(libs.auto.service.annotations)

    annotationProcessor(libs.auto.service)
}
