SHELL := /usr/bin/env bash

.PHONY: sync-proto
sync-proto:
	./scripts/sync-proto.sh

.PHONY: check-proto-sync
check-proto-sync:
	./scripts/check-proto-sync.sh

.PHONY: generate-go-proto
generate-go-proto:
	./scripts/generate-go-proto.sh

.PHONY: check-go-proto
check-go-proto:
	./scripts/check-go-proto.sh

.PHONY: generate-java-proto
generate-java-proto:
	./scripts/generate-java-proto.sh

.PHONY: generate-proto
generate-proto: generate-go-proto generate-java-proto

.PHONY: check-layout
check-layout:
	./scripts/validate-layout.sh

.PHONY: check-conformance
check-conformance:
	./scripts/check-conformance-scaffold.sh

.PHONY: conformance
conformance:
	./scripts/run-conformance.sh

.PHONY: conformance-blackbox
conformance-blackbox:
	./scripts/run-conformance-blackbox.sh

.PHONY: check-go
check-go:
	./scripts/check-go.sh

.PHONY: check-java
check-java:
	./scripts/check-java.sh

.PHONY: check
check: check-layout check-conformance check-go check-java

.PHONY: release-dry-run
release-dry-run:
	./scripts/release-dry-run.sh
