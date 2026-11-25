# Core conformance specification

This specification captures the shared behavior every Armada core SDK must implement.

The suite is intentionally limited to the language-agnostic core client surface. It excludes symbiotic or framework-specific integrations such as Spring Data.

## Covered shared core surface

- `KV.Range`
- `KV.Put`
- `KV.DeleteRange`
- `KV.Txn`
- `Cluster.MemberList`

## Deferred surface

- `Cluster.Status` once every core SDK exposes it consistently.

## Required behavior areas

1. request and response mapping stays faithful to the vendored protobuf contract,
2. unary request defaults behave consistently across languages,
3. transaction success and failure branches preserve request and response ordering,
4. conformance scenarios are defined in shared fixtures and consumed by each language harness,
5. `KV.Range` (get and iterate) exposes the `min_mod_revision`, `max_mod_revision`,
   `min_create_revision` and `max_create_revision` filters as options; each bound is
   inclusive and 0 means unbounded. Readers use `min_mod_revision = watermark + 1` to
   fetch only keys changed since a previous read (live keys only; deletes are not
   reported).

## Current fixture source

`conformance/fixtures/core-sdk.json` is the shared fixture source for the Go and JVM core harnesses.
