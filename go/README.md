# Armada Go client

This directory contains the migrated Go SDK for the Armada client monorepo.

- import path: `github.com/armadakv/client/go`
- package name: `client`
- plugin modules: `github.com/armadakv/client/go/plugin/...`

The Go surface now uses Armada naming. The main remaining legacy identifier is the wire-level `regatta.v1` protobuf package, which stays in place for protocol compatibility.

Checked-in Go protobuf sources in `internal/proto/` are generated from the shared vendored contract under `../api/proto`.

## Example use

```go
package main

import (
	"context"
	"fmt"
	"time"

	client "github.com/armadakv/client/go"
)

func main() {
	// Create Armada client
	c, err := client.New(
		client.WithEndpoints("127.0.0.1:8443"),
		client.WithLogger(client.PrintLogger{}),
		client.WithSecureConfig(&client.SecureConfig{
			InsecureSkipVerify: true, // Skip verification of self-signed certificate
		}),
	)
	if err != nil {
		panic(err)
	}

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second) // Provide operation timeout
	defer cancel()
	
	put, err := c.Table("armada-test").Put(ctx, "foo", "bar")
	if err != nil {
		panic(err)
	}
	fmt.Printf("%+v\n", put)
}
```

## Local checks

```bash
make -C go check
```

Additional functionality like Prometheus metrics and OpenTelemetry tracing is provided under `plugin/`.
