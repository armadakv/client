#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is not available; skipping black-box conformance"
  exit 0
fi

export ARMADA_BLACKBOX=1
export ARMADA_IMAGE="${ARMADA_IMAGE:-ghcr.io/armadakv/armada:v0.3.2}"
export ARMADA_IMAGE_PLATFORM="${ARMADA_IMAGE_PLATFORM:-linux/amd64}"

(
  cd "${ROOT_DIR}/go"
  GOWORK=off go test ./... -run '^TestCoreBlackboxConformance$'
)

(
  cd "${ROOT_DIR}/java"
  ./gradlew :armada-java-core:test --tests 'io.armadakv.core.impl.CoreBlackboxConformanceTest'
)
