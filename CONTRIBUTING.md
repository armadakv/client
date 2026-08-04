# Contributing

## Scope

This repository owns shared Armada client concerns:

- language SDKs,
- vendored protocol inputs from `armadakv/armada`,
- conformance coverage shared across SDKs,
- coordinated release metadata.

## Development flow

1. Update the pinned Armada protocol ref in `api/upstream.env` when the contract changes.
2. Run `make sync-proto`.
3. Run `make generate-proto`.
4. Run `make conformance` for shared core behavior changes.
5. Run `make check`.
6. Keep behavior changes reflected in `conformance/spec/`.

## Repository conventions

- Keep language SDKs at the repository root (`go/`, `java/`, ...).
- Do not hand-edit synced protobuf files under `api/proto/`.
- Keep release notes repo-wide even when only one language SDK changes.
