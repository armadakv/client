#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FILE_LIST="${ROOT_DIR}/api/proto.files"
CURRENT_DIR="${ROOT_DIR}/api/proto"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

"${ROOT_DIR}/scripts/sync-proto.sh" "${TMP_DIR}"

while IFS= read -r proto_file; do
  if [[ -z "${proto_file}" ]]; then
    continue
  fi

  current_file="${CURRENT_DIR}/${proto_file}"
  synced_file="${TMP_DIR}/${proto_file}"

  if [[ ! -f "${current_file}" ]]; then
    echo "missing synced proto: ${proto_file}" >&2
    exit 1
  fi

  if ! cmp -s "${current_file}" "${synced_file}"; then
    echo "out-of-date synced proto: ${proto_file}" >&2
    exit 1
  fi
done < "${FILE_LIST}"

expected_count="$(grep -vc '^[[:space:]]*$' "${FILE_LIST}")"
actual_count="$(find "${CURRENT_DIR}" -maxdepth 1 -type f -name '*.proto' | wc -l | tr -d ' ')"

if [[ "${expected_count}" != "${actual_count}" ]]; then
  echo "proto directory contains unexpected files" >&2
  exit 1
fi
