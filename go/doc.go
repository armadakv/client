// Copyright JAMF Software, LLC

// Package client implements the Armada Go client.
//
// Create client using `client.New`:
//
//	_, err := client.New(
//		client.WithEndpoints("http://254.0.0.1:12345"),
//		client.WithInitialConnectionTimeout(2*time.Second),
//	})
//
//	if err == context.DeadlineExceeded {
//		// handle errors
//	}
//
//	cli, err := client.New(
//		client.WithEndpoints("localhost:2379", "localhost:22379", "localhost:32379"),
//		client.WithInitialConnectionTimeout(5*time.Second),
//	)
//	if err != nil {
//		// handle error!
//	}
//	defer cli.Close()
//
// Make sure to close the client after using it. If the client is not closed, the
// connection will have leaky goroutines.
//
// To specify a client request timeout, wrap the context with context.WithTimeout:
//
//	ctx, cancel := context.WithTimeout(context.Background(), timeout)
//	defer cancel()
//	resp, err := cli.Table("table").Put(ctx, "key", "sample_value")
//	if err != nil {
//		// handle error!
//	}
//	// use the response
//
// The package keeps its historic `client` package name, while the wire-level
// `regatta.v1` protobuf package remains for protocol compatibility.
//
// The Client has internal state, so Clients should be reused instead of created as needed.
// Clients are safe for concurrent use by multiple goroutines.
package client
