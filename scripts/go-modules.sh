#!/usr/bin/env bash
# Lists the Go modules of this repository as "<dir> <module path>", the core first.
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"
for mod in go/go.mod go/plugin/*/go.mod; do
  [[ -f "${mod}" ]] || continue
  echo "$(dirname "${mod}") $(sed -n 's/^module //p' "${mod}")"
done
