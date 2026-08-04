# Releasing

## Versioning

The repository uses one top-level semantic version tag such as `v0.11.0`.

- One tag covers all SDKs in the monorepo.
- Release notes are repo-wide.
- Language-specific publishing should derive from the same repo tag.

## Current scaffold state

The release workflow currently performs a dry run only. The Go and JVM SDKs are now present in the monorepo, but real package publication should wait until release metadata and artifact publishing are wired into the shared release flow.

## Intended release flow

1. Pin or update the upstream Armada protocol ref if needed.
2. Ensure protocol sync is clean.
3. Ensure conformance passes across all migrated SDKs.
4. Create one repo tag.
5. Publish language-specific artifacts from that same tag.
