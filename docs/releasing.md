# Releasing

One version `vX.Y.Z` for the whole repo. A release tags every module on the same commit:

| Artifact | Tag | Published to |
|---|---|---|
| Java `io.armadakv:armada-java-*` | `vX.Y.Z` | Maven Central |
| `github.com/armadakv/client/go` | `go/vX.Y.Z` | Go proxy |
| Go plugins | `go/plugin/<name>/vX.Y.Z` | Go proxy |

`rotel` and `rprom` require the core at the release version; their `replace` is for local development only.
Between releases `java/gradle.properties` holds the next `-SNAPSHOT`, which every push to `main` publishes
to GitHub Packages.

## Release

1. `scripts/prepare-release.sh X.Y.Z` in a PR, then merge.
2. `git tag vX.Y.Z && git push origin vX.Y.Z`. `release.yml` checks the versions, tags and verifies the Go
   modules, and publishes Java to Maven Central.
3. `scripts/prepare-release.sh --next X.Y+1.0` on `main`.

## One-time setup

1. Verify the `io.armadakv` namespace on [central.sonatype.com](https://central.sonatype.com) and create a
   user token.
2. Create a GPG signing key and publish the public key to a key server.
3. Add these secrets to the `maven-central` environment: `MAVEN_CENTRAL_USERNAME`,
   `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY` (armored private key) and `SIGNING_KEY_PASSWORD`.

## Updating the protocol

Set `ARMADA_REF` in `api/upstream.env` and the black-box image tag to the new Armada release, run
`make sync-proto generate-proto`, then `make check conformance-blackbox`.

## Snapshots

GitHub Packages needs a token with `read:packages`, even for public packages:

```kotlin
maven {
    url = uri("https://maven.pkg.github.com/armadakv/client")
    credentials { username = "<user>"; password = "<token>" }
}
```
