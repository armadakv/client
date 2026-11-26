/*
 * Copyright JAMF Software, LLC
 */

plugins {
    id("armada.library-conventions")
    alias(libs.plugins.protobuf)
}

sourceSets {
    main {
        proto {
            setSrcDirs(listOf("../../api/proto"))
        }
    }
}

dependencies {
    // The generated messages are this module's API; protoc 4.x code needs the matching runtime.
    api(libs.protobuf.java)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.services)
    implementation(libs.grpc.stub)
    implementation(libs.javax.annotation.api)
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    plugins {
        create("grpc") {
            artifact = libs.grpc.protoc.gen.get().toString()
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
            }
        }
    }
}
