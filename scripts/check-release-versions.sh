#!/usr/bin/env bash
# Fails unless the tree is ready to be released as vX.Y.Z: java/gradle.properties
# holds X.Y.Z and every Go plugin requires the core at vX.Y.Z.
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CORE=github.com/armadakv/client/go
VERSION="${1:?version vX.Y.Z is required}"
VERSION="v${VERSION#v}"

fail=0
java_version="$(sed -n 's/^version=//p' "${ROOT_DIR}/java/gradle.properties")"
if [[ "${java_version}" != "${VERSION#v}" ]]; then
  echo "java/gradle.properties has version=${java_version}, want ${VERSION#v} (scripts/prepare-release.sh ${VERSION#v})" >&2
  fail=1
fi
while read -r dir module; do
  [[ "${module}" == "${CORE}" ]] && continue
  required="$(cd "${ROOT_DIR}/${dir}" && go mod edit -json | jq -r --arg core "${CORE}" '.Require[]? | select(.Path == $core) | .Version')"
  if [[ -n "${required}" && "${required}" != "${VERSION}" ]]; then
    echo "${module} requires ${CORE} ${required}, want ${VERSION} (scripts/prepare-release.sh ${VERSION#v})" >&2
    fail=1
  fi
done < <("${ROOT_DIR}/scripts/go-modules.sh")
exit "${fail}"
