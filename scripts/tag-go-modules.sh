#!/usr/bin/env bash
# Tags every Go module at vX.Y.Z on HEAD; --push pushes the tags.
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
