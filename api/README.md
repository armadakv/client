# API inputs

This directory contains the vendored protocol inputs consumed by every SDK in this repository.

- `proto/` holds synced `.proto` files from `armadakv/armada`.
- `upstream.env` pins the upstream source ref.
- `proto.files` defines the synced file list.
- `buf.yaml` captures shared lint and breaking-change settings for the vendored contract.
- `../scripts/generate-go-proto.sh` generates checked-in Go protobuf sources from this directory.
- `../java/armada-java-grpc` generates Java stubs from this directory during the Gradle build.
