#!/usr/bin/env bash
# Prints "<dir> <module>" for each Go module, core first.
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"
for mod in go/go.mod go/plugin/*/go.mod; do
  [[ -f "${mod}" ]] || continue
  echo "$(dirname "${mod}") $(sed -n 's/^module //p' "${mod}")"
done
