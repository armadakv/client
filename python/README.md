# Armada Python SDK

This directory is reserved for the future Armada Python SDK.

Planned structure:

- `python/` as the package root
- generated protobuf or gRPC bindings shared from `api/proto/`
- a core SDK surface aligned with the Go and Java conformance fixtures

The intended public surface should stay focused on the core client API:

- table lifecycle
- key/value operations
- transactions
- cluster membership

Keep higher-level framework integrations separate from the core SDK.
