module github.com/armadakv/client/go/plugin/rprom

go 1.26.5

require (
	github.com/armadakv/client/go v0.0.0
	github.com/prometheus/client_golang v1.19.1
	google.golang.org/grpc v1.83.0
)

replace github.com/armadakv/client/go => ../..

require (
	github.com/beorn7/perks v1.0.1 // indirect
	github.com/cespare/xxhash/v2 v2.3.0 // indirect
	github.com/klauspost/compress v1.19.1 // indirect
	github.com/planetscale/vtprotobuf v0.6.1-0.20240319094008-0393e58bdf10 // indirect
	github.com/prometheus/client_model v0.6.1 // indirect
	github.com/prometheus/common v0.54.0 // indirect
	github.com/prometheus/procfs v0.15.1 // indirect
	golang.org/x/net v0.57.0 // indirect
	golang.org/x/sys v0.47.0 // indirect
	golang.org/x/text v0.40.0 // indirect
	google.golang.org/genproto/googleapis/rpc v0.0.0-20260729162451-8efbd57d26e0 // indirect
	google.golang.org/protobuf v1.36.11 // indirect
)
