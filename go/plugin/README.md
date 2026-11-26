Plugins
---

This directory contains plug-in support for external libraries to use with a `Client`.

<pre>
<a href="./">plugin</a> — you are here
├── <a href="./rprom">rprom</a> — plug-in Prometheus metrics
├── <a href="./rotel">rotel</a> — plug-in OpenTelemetry tracing
├── <a href="./rlogrus">rlogrus</a> — plug-in sirupsen/logrus logging lib
└── <a href="./rzap">rzap</a> — plug-in uber-go/zap logging lib
</pre>

Each plugin is its own Go module, released together with the core at the same version:

```
go get github.com/armadakv/client/go@vX.Y.Z github.com/armadakv/client/go/plugin/rotel@vX.Y.Z
```

These plugins can be enabled by using [Client.WithHooks](https://pkg.go.dev/github.com/armadakv/client/go?utm_source=godoc#WithHooks) in your
Go application. For example:

```
// Create the client
c, err := client.New(
    client.WithEndpoints("127.0.0.1:8443"),
    client.WithLogger(client.PrintLogger{}),
    client.WithSecureConfig(&client.SecureConfig{
        InsecureSkipVerify: true, // Skip verification of self-signed certificate
    }),
    client.WithHooks(rprom.NewMetrics(), rotel.NewTracing()), // Enable rprom and rotel plugins
)
if err != nil {
    panic(err)
}
```
