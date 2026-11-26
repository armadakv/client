/*
 * Copyright JAMF Software, LLC
 */

plugins {
    id("armada.library-conventions")
}

sourceSets {
    test {
        resources.srcDir("../../conformance/fixtures")
    }
}

dependencies {
    // Exported to consumers, that is to say found on their compile classpath.
    api(project(":armada-java-grpc"))
    api(libs.protobuf.java)
    api(libs.grpc.core)
    api(libs.grpc.stub)
    api(libs.grpc.netty.shaded)
    api(libs.sslcontext.kickstart)

    // Used internally, not exposed to consumers on their compile classpath.
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
