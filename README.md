# Armada client monorepo

This repository is the shared home for Armada client SDKs, protocol inputs, and cross-language conformance checks.

## Status

The repository is currently scaffolded for:

- shared release/version management,
- synced protocol inputs from `armadakv/armada`,
- root-level language SDK directories (`go/`, `java/`, and future siblings),
- shared conformance specifications and harnesses.

The Go and JVM SDKs have both been migrated into this monorepo under `go/` and `java/`.

## Layout

```text
.
|-- api/             # synced protobuf inputs and generation config
|-- conformance/     # shared behavior specs, fixtures, harnesses, reports
|-- docs/            # architecture, release, and compatibility docs
|-- examples/        # cross-language examples and snippets
|-- go/              # migrated Go SDK module and plugin modules
|-- java/            # migrated JVM SDK multi-module Gradle build
`-- scripts/         # sync and validation helpers
```

## Protocol source of truth

The canonical wire contract stays in `armadakv/armada`. This repository vendors a pinned copy of the protocol files under `api/proto/` so every SDK and conformance job uses the same contract snapshot.

The pin is an Armada release tag (currently `v0.3.2`); the black-box conformance suites run against the image of the same release.

## Common commands

```bash
make sync-proto
make generate-proto
make conformance
make check
```

`make generate-proto` regenerates Go and Java protobuf outputs from the shared vendored contract. `make conformance` runs the shared core conformance scenarios against the Go and JVM core SDKs. `make check` validates the scaffold, the protocol sync, generated Go protobuf drift, the conformance fixture scaffold, and the migrated Go and JVM SDKs.

## Using the SDKs

- **Go:** `go get github.com/armadakv/client/go` (plugins under `github.com/armadakv/client/go/plugin/...`).
- **Java:** `io.armadakv:armada-java-core` (client), `armada-java-spring-data` (Spring Data KeyValue),
  `armada-java-test` (JUnit and Testcontainers helpers) and `armada-java-grpc` (generated stubs). Releases
  are on Maven Central; snapshots of `main` are on GitHub Packages. See [docs/releasing.md](docs/releasing.md).

