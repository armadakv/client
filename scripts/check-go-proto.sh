#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CURRENT_DIR="${ROOT_DIR}/go/internal/proto"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

"${ROOT_DIR}/scripts/generate-go-proto.sh" "${TMP_DIR}"

if ! diff -ru "${CURRENT_DIR}" "${TMP_DIR}" >/dev/null; then
  echo "Go protobuf sources are out of sync with api/proto" >&2
  diff -ru "${CURRENT_DIR}" "${TMP_DIR}" || true
  exit 1
fi
