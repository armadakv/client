/*
 * Copyright JAMF Software, LLC
 */

import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    `java-library`
    id("com.vanniktech.maven.publish")
}

// group and version come from gradle.properties; a release passes -Pversion=X.Y.Z.
val libs = the<VersionCatalogsExtension>().named("libs")

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Releases go to Maven Central through the Central Portal, signed; snapshots go to
// GitHub Packages. Credentials and the signing key are Gradle properties, normally
// passed as ORG_GRADLE_PROJECT_* environment variables by the release workflows:
// mavenCentralUsername, mavenCentralPassword, signingInMemoryKey,
// signingInMemoryKeyPassword, GitHubPackagesUsername, GitHubPackagesPassword.
mavenPublishing {
    configure(JavaLibrary(javadocJar = JavadocJar.Javadoc(), sourcesJar = true))
    publishToMavenCentral(automaticRelease = true)
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
    coordinates(project.group.toString(), project.name, project.version.toString())
    pom {
        name.set(project.name)
        description.set(provider { project.description ?: project.name })
        url.set("https://github.com/armadakv/client")
        inceptionYear.set("2025")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://github.com/armadakv/client/blob/main/LICENSE")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("armadakv")
                name.set("ArmadaKV Contributors")
                url.set("https://github.com/armadakv")
            }
        }
        scm {
            url.set("https://github.com/armadakv/client")
            connection.set("scm:git:https://github.com/armadakv/client.git")
            developerConnection.set("scm:git:ssh://git@github.com/armadakv/client.git")
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/armadakv/client")
            credentials(PasswordCredentials::class)
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
