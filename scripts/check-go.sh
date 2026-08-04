#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

modules=(
  "go"
  "go/plugin/rprom"
  "go/plugin/rzap"
  "go/plugin/rlogrus"
  "go/plugin/rotel"
)

for module in "${modules[@]}"; do
  echo "==> testing ${module}"
  (
    cd "${ROOT_DIR}/${module}"
    GOWORK=off go test ./...
  )
done
