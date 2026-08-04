# Compatibility

## Contract compatibility

- The authoritative wire contract lives in `armadakv/armada`.
- This repository vendors a pinned copy of that contract.
- The current scaffold pins the `armada` master head commit as a bootstrap source, not a production release source.
- TODO: replace the bootstrap pin with the production-approved upstream ref before production use.
- SDK behavior must stay aligned with the vendored protocol snapshot and the conformance suite.
- The protobuf wire package still includes legacy `regatta.v1` naming where the upstream Armada API keeps it for compatibility.

## Language compatibility

- Shared behavior belongs in `conformance/spec/`.
- Language-specific ergonomics belong in the SDK directories.
- Legacy Regatta naming should only be preserved where explicitly required for compatibility.
- The Go SDK keeps its historic `client` package name while using Armada naming everywhere else.
- The JVM SDK now uses `io.armadakv` package coordinates and `armada-java-*` artifact names.

## Release compatibility

- A repo tag describes the compatible set of protocol inputs, SDK implementations, and conformance expectations.
- Diverging per-language versions should be avoided unless a later migration proves they are necessary.
