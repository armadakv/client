/*
 * Copyright JAMF Software, LLC
 */

plugins {
    `java-library`
    `maven-publish`
    signing
}

// group and version come from gradle.properties
val libs = the<VersionCatalogsExtension>().named("libs")

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}


publishing {
    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("ossrh.user") as String? ?: System.getenv("MAVEN_USERNAME")
                password = project.findProperty("ossrh.user") as String? ?: System.getenv("MAVEN_PASSWORD")
            }
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/armadakv/client")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("library") {
            from(components["java"])
        }
    }
}

testing {
    suites {
        // Configure the built-in test suite
        named<JvmTestSuite>("test") {
            // Use JUnit Jupiter test framework
            useJUnitJupiter(libs.findVersion("junit").get().requiredVersion)
        }
    }
}
