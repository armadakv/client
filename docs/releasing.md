# Releasing

## Versioning

One repo-wide semantic version (`vX.Y.Z`) describes a compatible set of protocol inputs, SDKs and
conformance expectations. Release notes are repo-wide, and every SDK artifact carries the same version.

Go makes this slightly more complex: a Go module that lives in a subdirectory is only resolvable through
**prefixed tags**. A release `vX.Y.Z` therefore creates all of these tags on the same commit:

| Artifact | Tag | Published to |
|---|---|---|
| repo, release notes, Java (`io.armadakv:armada-java-*:X.Y.Z`) | `vX.Y.Z` | Maven Central |
| `github.com/armadakv/client/go` | `go/vX.Y.Z` | Go module proxy (no upload) |
| `github.com/armadakv/client/go/plugin/rotel` | `go/plugin/rotel/vX.Y.Z` | Go module proxy |
| `.../plugin/rprom`, `rzap`, `rlogrus` | `go/plugin/<name>/vX.Y.Z` | Go module proxy |

Between releases `java/gradle.properties` holds the next version as a `-SNAPSHOT`.

### Go plugins

`rotel` and `rprom` import the core. Their `go.mod` requires the core **at the version they are released
with** and keeps a `replace github.com/armadakv/client/go => ../..` for development inside this repository.
Consumers ignore `replace`, and because every module is tagged on the same commit, a plugin at `vX.Y.Z`
resolves the core at `go/vX.Y.Z`. `rzap` and `rlogrus` do not depend on the core.

Scripts:

| Script | Does |
|---|---|
| `scripts/prepare-release.sh X.Y.Z` | Sets the Java version and the core version the plugins require to `X.Y.Z` |
| `scripts/prepare-release.sh --next X.Y.Z` | After a release: Java back to `X.Y.Z-SNAPSHOT` |
| `scripts/check-release-versions.sh vX.Y.Z` | Fails unless both are at `X.Y.Z` (run by the dry run) |
| `scripts/tag-go-modules.sh [--push] vX.Y.Z` | Tags `go/vX.Y.Z` and `go/plugin/<name>/vX.Y.Z` on the current commit |
| `scripts/verify-go-release.sh vX.Y.Z` | Resolves every module through proxy.golang.org as a first-time consumer |

## Java publishing

Publishing is configured in `java/buildSrc` (`armada.library-conventions`) with the
[`com.vanniktech.maven.publish`](https://vanniktech.github.io/gradle-maven-publish-plugin/) plugin. Every
module publishes its jar, sources and javadoc jars, a POM with complete Maven Central metadata, and Gradle
module metadata.

| What | When | Workflow | Destination |
|---|---|---|---|
| Release `X.Y.Z` | a pushed tag `vX.Y.Z` | `release.yml` (job `java`) | Maven Central, via the Central Portal, signed, released automatically |
| Snapshot | every push to `main` touching `java/**` or `api/**` | `publish-snapshot.yml` | GitHub Packages, `https://maven.pkg.github.com/armadakv/client` |

A manual run of `release.yml` (`workflow_dispatch`) is a dry run only and never publishes.

### One-time setup

1. Verify the `io.armadakv` namespace on [central.sonatype.com](https://central.sonatype.com) and generate
   a Central Portal user token.
2. Create a GPG signing key for the project and publish its public key to a key server
   (`gpg --keyserver keys.openpgp.org --send-keys <id>`).
3. In the repository settings, create the environment `maven-central` (optionally with required reviewers)
   and add these secrets to it:

   | Secret | Value |
   |---|---|
   | `MAVEN_CENTRAL_USERNAME` | Central Portal token user name |
   | `MAVEN_CENTRAL_PASSWORD` | Central Portal token password |
   | `SIGNING_KEY` | ASCII-armored private key (`gpg --armor --export-secret-keys <id>`) |
   | `SIGNING_KEY_PASSWORD` | its passphrase |

   The release job fails before building if any of them is missing.
4. Snapshots need nothing: `publish-snapshot.yml` uses the workflow's `GITHUB_TOKEN` with `packages: write`.

### Checking locally

```bash
cd java
# everything a release would publish, into a throwaway repository, unsigned
./gradlew publishToMavenLocal -Pversion=0.0.0-test -Dmaven.repo.local=/tmp/m2-test
```

Pass `ORG_GRADLE_PROJECT_signingInMemoryKey` and `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword` to sign;
without a key, publications are left unsigned (fine for snapshots and local checks, rejected by Central).

### Using snapshots

GitHub Packages requires authentication even for public packages:

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/armadakv/client")
        credentials {
            username = providers.gradleProperty("gpr.user").get()
            password = providers.gradleProperty("gpr.key").get() // a token with read:packages
        }
    }
}
```

## Release flow

1. `api/upstream.env` pins an Armada release tag. To move it: update `ARMADA_REF`,
   `make sync-proto generate-proto`, update the black-box image tag to the same release, ensure no drift.
2. `make check` and `make conformance-blackbox` green against that image.
3. Open a release PR: `scripts/prepare-release.sh X.Y.Z` (Java version and the plugins' core
   requirement), plus the release notes. Merge.
4. Push the one tag: `git tag vX.Y.Z && git push origin vX.Y.Z`. `release.yml` then:
   - **dry-run**: validates the version and checks it is consistent (`check-release-versions.sh`);
   - **go**: tags `go/vX.Y.Z` and every `go/plugin/<name>/vX.Y.Z` on the same commit, pushes them, and
     resolves every module through proxy.golang.org (which also warms the proxy);
   - **java**: runs the tests and `publishAndReleaseToMavenCentral -Pversion=X.Y.Z`.
5. Create the GitHub Release with the notes, then `scripts/prepare-release.sh --next X.Y+1.0` on `main`.
6. Smoke test from a clean project: `go get` each module, resolve each Maven coordinate.

If one of the publishing jobs fails, re-run just that job: the Go tags and the Maven Central release are
independent, and neither can be repeated once it has succeeded (tags exist, Central versions are immutable).

## Suggested first release

`v0.1.0` of this repo (Go modules and Java alike), documenting the revision filters, with Armada `v0.3.2`
as the minimum server.
