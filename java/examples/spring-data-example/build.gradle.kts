/*
 * Copyright JAMF Software, LLC
 */

plugins {
    application
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.springframework.data:spring-data-keyvalue")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("io.armadakv:armada-java-spring-data")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.armadakv:armada-java-test")
}

testing {
    suites {
        // Configure the built-in test suite
        named<JvmTestSuite>("test") {
            // Use JUnit Jupiter test framework
            useJUnitJupiter(libs.versions.junit.get())
        }
    }
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
