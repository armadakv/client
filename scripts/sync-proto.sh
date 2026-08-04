#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST_DIR="${1:-${ROOT_DIR}/api/proto}"
FILE_LIST="${ROOT_DIR}/api/proto.files"

# shellcheck disable=SC1091
source "${ROOT_DIR}/api/upstream.env"

mkdir -p "${DEST_DIR}"

find "${DEST_DIR}" -maxdepth 1 -type f -name '*.proto' | while read -r existing; do
  base="$(basename "${existing}")"
  if ! grep -Fxq "${base}" "${FILE_LIST}"; then
    rm -f "${existing}"
  fi
done

while IFS= read -r proto_file; do
  if [[ -z "${proto_file}" ]]; then
    continue
  fi

  curl -fsSL \
    "https://raw.githubusercontent.com/${ARMADA_GITHUB_REPO}/${ARMADA_REF}/proto/${proto_file}" \
    -o "${DEST_DIR}/${proto_file}"
done < "${FILE_LIST}"
