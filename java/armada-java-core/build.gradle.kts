/*
 * Copyright JAMF Software, LLC
 */

plugins {
    id("armada.library-conventions")
}

description = "Armada KV client for the JVM."

sourceSets {
    test {
        resources.srcDir("../../conformance/fixtures")
    }
}

dependencies {
    api(project(":armada-java-grpc"))
    api(libs.protobuf.java)
    api(libs.grpc.core)
    api(libs.grpc.stub)
    api(libs.grpc.netty.shaded)
    api(libs.sslcontext.kickstart)

    implementation(libs.slf4j.api)
    implementation(libs.snappy.java)
    implementation(libs.sslcontext.kickstart.netty)
    implementation(libs.sslcontext.kickstart.pem)
    implementation(libs.failsafe)
    implementation(libs.auto.service.annotations)

    annotationProcessor(libs.auto.service)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.assertj.core)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.grpc.inprocess)
    testImplementation(libs.grpc.testing)
    testImplementation(libs.junit.jupiter.migrationsupport)
    testImplementation(project(":armada-java-test"))
}
