#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

required_paths=(
  ".github/workflows"
  "api/proto"
  "conformance/spec"
  "conformance/fixtures"
  "conformance/harness"
  "conformance/reports"
  "docs"
  "examples"
  "go"
  "java"
  "node"
  "python"
  "scripts"
)

for path in "${required_paths[@]}"; do
  if [[ ! -e "${ROOT_DIR}/${path}" ]]; then
    echo "missing required path: ${path}" >&2
    exit 1
  fi
done

if [[ -e "${ROOT_DIR}/clients" ]]; then
  echo "unexpected nested clients/ directory; SDKs should live at the repo root" >&2
  exit 1
fi

if [[ -e "${ROOT_DIR}/java/armada-java-grpc/src/main/proto" ]]; then
  echo "unexpected duplicated Java proto source directory; use api/proto as the shared source" >&2
  exit 1
fi
