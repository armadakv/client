# Armada Python SDK

This directory contains the Python SDK scaffold for Armada KV.

The first cut is intentionally small:

- package root: `python/src/armadakv`
- core client entry point: `ArmadaClient`
- core models: key/value, get/put/delete/txn results, table and cluster metadata
- shared conformance fixture loader for `conformance/fixtures/core-sdk.json`

The SDK is intentionally focused on the core client surface only.
Framework-specific or native integrations should stay separate.

## Layout

```text
python/
|-- pyproject.toml
`-- src/
    `-- armadakv/
        |-- __init__.py
        |-- client.py
        `-- models.py
```
