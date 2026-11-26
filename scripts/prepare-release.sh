#!/usr/bin/env bash
# Prepares the release commit for version X.Y.Z: the Java version and the core
# version every Go plugin requires. All Go modules are tagged on that commit,
# so a plugin at vX.Y.Z resolves the core at vX.Y.Z.
#
#   scripts/prepare-release.sh 0.2.0          # release commit
#   scripts/prepare-release.sh --next 0.3.0   # after the release: Java back to a snapshot
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CORE=github.com/armadakv/client/go

next=false
if [[ "${1:-}" == "--next" ]]; then next=true; shift; fi
VERSION="${1:-}"
VERSION="${VERSION#v}"
if [[ ! "${VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "usage: $0 [--next] X.Y.Z" >&2
  exit 2
fi

set_java_version() {
  sed -i.bak "s/^version=.*/version=$1/" "${ROOT_DIR}/java/gradle.properties"
  rm -f "${ROOT_DIR}/java/gradle.properties.bak"
}

if ${next}; then
  set_java_version "${VERSION}-SNAPSHOT"
  echo "java: ${VERSION}-SNAPSHOT"
  exit 0
fi

set_java_version "${VERSION}"
echo "java: ${VERSION}"
while read -r dir module; do
  [[ "${module}" == "${CORE}" ]] && continue
  if grep -qE "^\s*${CORE} v" "${ROOT_DIR}/${dir}/go.mod"; then
    (cd "${ROOT_DIR}/${dir}" && go mod edit -require="${CORE}@v${VERSION}" && go mod tidy)
    echo "${module}: requires ${CORE} v${VERSION}"
  fi
done < <("${ROOT_DIR}/scripts/go-modules.sh")
