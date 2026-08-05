# Armada Node.js SDK

This directory contains the Node.js SDK scaffold for Armada KV.

The first cut is intentionally small:

- package root: `node/src`
- core client entry point: `ArmadaClient`
- core models: key/value, get/put/delete/txn results, table and cluster metadata
- shared conformance fixture loader for `conformance/fixtures/core-sdk.json`

The SDK is intentionally focused on the core client surface only.
Framework-specific or native integrations should stay separate.

## Layout

```text
node/
|-- package.json
`-- src/
    |-- client.js
    |-- index.js
    `-- models.js
```
