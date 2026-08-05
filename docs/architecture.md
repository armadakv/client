# Architecture

## Goals

- keep all Armada SDKs on one release train,
- share one set of release notes,
- validate cross-language behavior with one conformance suite,
- keep the Armada server repository as the protocol source of truth.

## Repository shape

- `api/` holds the vendored protocol inputs and shared protocol validation config.
- `go/` holds the migrated Go SDK plus its plugin modules; `java/` holds the migrated JVM multi-module build; `node/` and `python/` are reserved for future language SDKs.
- `conformance/` holds language-agnostic specs, fixtures, harnesses, and reports.
- `scripts/` holds repeatable sync and validation helpers used both locally and in CI.

## Protocol flow

1. `armadakv/armada` publishes or updates protobuf definitions.
2. This repository pins the desired upstream ref in `api/upstream.env`.
3. `scripts/sync-proto.sh` vendors the selected `.proto` files into `api/proto/`.
4. `scripts/generate-go-proto.sh` regenerates checked-in Go protobuf sources under `go/internal/proto/`.
5. `java/armada-java-grpc` generates JVM stubs from `api/proto/` during the Gradle build.
6. Every SDK and conformance job consumes the same vendored snapshot.

## Migration boundary

The Go SDK is imported under `go/` with its own nested plugin modules. The JVM SDK is imported under `java/` with its Gradle multi-module structure preserved.
