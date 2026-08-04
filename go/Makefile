LDFLAGS = -X github.com/armadakv/client/go.Version=$(VERSION)
VERSION ?= $(shell git describe --tags --always --dirty)
CGO_ENABLED ?= 0

.PHONY: all
all: test build

.PHONY: check
check: test

.PHONY: test
test:
	GOWORK=off go test ./...

.PHONY: build
build:
	test $(VERSION) || (echo "version not set"; exit 1)
	CGO_ENABLED=$(CGO_ENABLED) GOWORK=off go build -ldflags="$(LDFLAGS)" ./...

