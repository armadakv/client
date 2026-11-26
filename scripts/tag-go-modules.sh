#!/usr/bin/env bash
# Creates the Go module tags of release vX.Y.Z on the current commit: go/vX.Y.Z
# for the core and go/plugin/<name>/vX.Y.Z for every plugin. With --push they
# are pushed to origin.
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
push=false
if [[ "${1:-}" == "--push" ]]; then push=true; shift; fi
VERSION="${1:?version vX.Y.Z is required}"
VERSION="v${VERSION#v}"

tags=()
while read -r dir _; do
  tags+=("${dir}/${VERSION}")
done < <("${ROOT_DIR}/scripts/go-modules.sh")

for tag in "${tags[@]}"; do
  git -C "${ROOT_DIR}" tag "${tag}"
  echo "tagged ${tag}"
done
if ${push}; then
  git -C "${ROOT_DIR}" push origin "${tags[@]/#/refs/tags/}"
fi
