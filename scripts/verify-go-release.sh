#!/usr/bin/env bash
# Resolves every Go module of release vX.Y.Z through the public module proxy,
# as a first-time consumer would (no replace directives, no workspace, an empty
# module cache), each module on its own and then all of them together. This also
# warms the proxy, which can take a moment to see new tags, so each is retried.
#
# Each consumer gets its own module cache: with Go 1.27, `go get` of a nested
# module fails ("found, but does not contain package") when its parent module
# is already cached at the same version; the same happens with
# go.opentelemetry.io/otel and otel/sdk, so it is not specific to this layout.
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION="${1:?version vX.Y.Z is required}"
VERSION="v${VERSION#v}"
export GOPROXY="${GOPROXY_VERIFY:-https://proxy.golang.org}"
export GOFLAGS=-mod=mod GOWORK=off

work="$(mktemp -d)"
# The module cache is read-only by default; let the cleanup remove it.
export GOFLAGS="${GOFLAGS} -modcacherw"
trap 'rm -rf "${work}"' EXIT

# consumer <name> <module@version>...: a fresh module that requires the given ones;
# every armadakv module in its build list must then be at the release.
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
