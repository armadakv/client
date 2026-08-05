# Armada Node.js SDK

This directory is reserved for the future Armada Node.js SDK.

Planned structure:

- `node/` as the package/workspace root
- generated protobuf or gRPC bindings shared from `api/proto/`
- a core SDK surface aligned with the Go and Java conformance fixtures

The intended public surface should stay focused on the core client API:

- table lifecycle
- key/value operations
- transactions
- cluster membership

Framework-specific or native integrations should stay out of the core SDK surface.
