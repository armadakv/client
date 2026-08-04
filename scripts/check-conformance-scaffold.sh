#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SPEC_FILE="${ROOT_DIR}/conformance/spec/core.md"

required_files=(
  "conformance/spec/core.md"
  "conformance/fixtures/core-sdk.json"
  "conformance/fixtures/README.md"
  "conformance/harness/README.md"
  "conformance/reports/README.md"
)

for file in "${required_files[@]}"; do
  if [[ ! -f "${ROOT_DIR}/${file}" ]]; then
    echo "missing conformance scaffold file: ${file}" >&2
    exit 1
  fi
done

required_symbols=(
  "KV.Range"
  "KV.Put"
  "KV.DeleteRange"
  "KV.Txn"
  "Cluster.MemberList"
)

for symbol in "${required_symbols[@]}"; do
  if ! grep -Fq "${symbol}" "${SPEC_FILE}"; then
    echo "missing conformance requirement: ${symbol}" >&2
    exit 1
  fi
done
