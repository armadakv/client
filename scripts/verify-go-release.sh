#!/usr/bin/env bash
# Resolves each Go module at vX.Y.Z through the public proxy as a new consumer.
# Per-consumer caches avoid a Go 1.27 bug with nested modules of a cached parent.
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION="${1:?version vX.Y.Z is required}"
VERSION="v${VERSION#v}"
export GOPROXY="${GOPROXY_VERIFY:-https://proxy.golang.org}"
export GOFLAGS=-mod=mod GOWORK=off

work="$(mktemp -d)"
export GOFLAGS="${GOFLAGS} -modcacherw"
trap 'rm -rf "${work}"' EXIT

consumer() {
  local name="$1"; shift
  local dir="${work}/${name//\//_}"
  mkdir -p "${dir}" && cd "${dir}"
  export GOMODCACHE="${dir}/modcache"
  go mod init example.invalid/verify >/dev/null 2>&1
  for attempt in 1 2 3 4 5 6; do
    if go get "$@" >/dev/null 2>"${dir}/err"; then
      local wrong
      wrong="$(go list -m all | awk -v v="${VERSION}" '$1 ~ /^github.com\/armadakv\// && $2 != v')"
      if [[ -n "${wrong}" ]]; then
        echo "${name}: modules resolved at another version:" >&2
        echo "${wrong}" >&2
        exit 1
      fi
      echo "ok ${name}: $(go list -m all | awk '$1 ~ /^github.com\/armadakv\// {print $1"@"$2}' | tr '\n' ' ')"
      return
    fi
    sleep $((attempt * 10))
  done
  cat "${dir}/err" >&2
  echo "could not resolve ${name}" >&2
  exit 1
}

all=()
while read -r _ module; do
  consumer "${module}" "${module}@${VERSION}"
  all+=("${module}@${VERSION}")
done < <("${ROOT_DIR}/scripts/go-modules.sh")
consumer "all modules together" "${all[@]}"
echo "all modules resolve at ${VERSION}"
