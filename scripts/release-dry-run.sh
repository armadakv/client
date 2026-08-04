#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION="${1:-${GITHUB_REF_NAME:-}}"

if [[ -z "${VERSION}" ]]; then
  echo "release version is required" >&2
  exit 1
fi

if [[ ! "${VERSION}" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "release version must match vX.Y.Z" >&2
  exit 1
fi

# shellcheck disable=SC1091
source "${ROOT_DIR}/api/upstream.env"

echo "Release dry run"
echo "  version: ${VERSION}"
echo "  pinned armada ref: ${ARMADA_REF}"
echo "  sdk roots: go/, java/"
echo "  publication: disabled until SDK migrations land"
