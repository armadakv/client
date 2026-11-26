#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROTO_DIR="${ROOT_DIR}/api/proto"
FILE_LIST="${ROOT_DIR}/api/proto.files"
OUT_DIR="${1:-${ROOT_DIR}/go/internal/proto}"
BIN_DIR="${ROOT_DIR}/tmp/tools/bin"

mkdir -p "${OUT_DIR}" "${BIN_DIR}"

if ! command -v protoc >/dev/null 2>&1; then
  echo "protoc is required to generate Go protobuf sources" >&2
  exit 1
fi

ensure_go_tool() {
  local binary="$1"
  local module="$2"

  if [[ ! -x "${BIN_DIR}/${binary}" ]]; then
    GOBIN="${BIN_DIR}" go install "${module}"
  fi
}

ensure_go_tool "protoc-gen-go" "google.golang.org/protobuf/cmd/protoc-gen-go@v1.36.12"
ensure_go_tool "protoc-gen-go-grpc" "google.golang.org/grpc/cmd/protoc-gen-go-grpc@v1.6.2"
ensure_go_tool "protoc-gen-go-vtproto" "github.com/planetscale/vtprotobuf/cmd/protoc-gen-go-vtproto@v0.6.1-0.20260702190614-8ae5a48058df"

proto_files=()
while IFS= read -r proto_file; do
  if [[ -z "${proto_file}" ]]; then
    continue
  fi
  proto_files+=("${PROTO_DIR}/${proto_file}")
done < "${FILE_LIST}"

find "${OUT_DIR}" -maxdepth 1 -type f \( -name '*.pb.go' -o -name '*_grpc.pb.go' -o -name '*_vtproto.pb.go' \) -delete

PATH="${BIN_DIR}:${PATH}" protoc -I "${PROTO_DIR}" \
  --go_out="${OUT_DIR}" --go_opt=paths=source_relative \
  --go-grpc_out="${OUT_DIR}" --go-grpc_opt=paths=source_relative \
  --go-vtproto_out="${OUT_DIR}" --go-vtproto_opt=paths=source_relative,features=marshal+unmarshal+size \
  "${proto_files[@]}"
