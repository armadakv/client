# Conformance harness

This repository currently executes the shared core conformance scenarios from language-local harnesses:

- `go/core_conformance_test.go`
- `java/armada-java-core/.../CoreConformanceTest.java`

Both harnesses consume the same fixture file from `conformance/fixtures/core-sdk.json`.

The harness should stay focused on the common core client surface and avoid language-native integrations or framework adapters.
