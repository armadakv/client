plugins {
    id("armada.library-conventions")
}

val grpcVersion = "1.66.0"
val testContainersVersion = "2.0.5"
val jupiterVersion = "5.10.3"
val autoServiceVersion = "1.1.1"

group = "io.armadakv"

version = "1.4.1-SNAPSHOT"

dependencies {
    api("io.grpc:grpc-core:${grpcVersion}")
    api("io.grpc:grpc-stub:${grpcVersion}")
    api("io.grpc:grpc-inprocess:${grpcVersion}")
    api("io.grpc:grpc-util:${grpcVersion}")
    api("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    api("org.testcontainers:testcontainers-junit-jupiter:$testContainersVersion")

    implementation("com.google.auto.service:auto-service-annotations:${autoServiceVersion}")

    annotationProcessor("com.google.auto.service:auto-service:${autoServiceVersion}")
}
