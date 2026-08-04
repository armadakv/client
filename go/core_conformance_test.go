package client

import (
	"context"
	"encoding/json"
	"net"
	"os"
	"path/filepath"
	"runtime"
	"testing"

	armadapb "github.com/armadakv/client/go/internal/proto"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/credentials/insecure"
	grpcstatus "google.golang.org/grpc/status"
	"google.golang.org/grpc/test/bufconn"
)

type coreConformanceFixture struct {
	Scenarios struct {
		GetDefault        rangeScenario      `json:"getDefault"`
		GetPrefixKeysOnly rangeScenario      `json:"getPrefixKeysOnly"`
		GetCountOnlyRange rangeScenario      `json:"getCountOnlyRange"`
		IteratePrefix     iterateScenario    `json:"iteratePrefix"`
		PutPrevKV         putScenario        `json:"putPrevKV"`
		DeletePrefixPrev  deleteScenario     `json:"deletePrefixPrevKV"`
		DeleteRange       deleteScenario     `json:"deleteRange"`
		TxnCompareAndOps  txnScenario        `json:"txnCompareAndOps"`
		TxnFailureOps     txnScenario        `json:"txnFailureOps"`
		GetUnavailable    errorScenario      `json:"getUnavailableError"`
		MemberList        memberListScenario `json:"memberList"`
	} `json:"scenarios"`
}

type rangeScenario struct {
	Table           string        `json:"table"`
	Key             string        `json:"key"`
	Options         rangeOptions  `json:"options"`
	ExpectedRequest rangeExpected `json:"expectedRequest"`
	Response        rangeResponse `json:"response"`
}

type putScenario struct {
	Table           string      `json:"table"`
	Key             string      `json:"key"`
	Value           string      `json:"value"`
	Options         putOptions  `json:"options"`
	ExpectedRequest putExpected `json:"expectedRequest"`
	Response        putResponse `json:"response"`
}

type iterateScenario struct {
	Table           string          `json:"table"`
	Key             string          `json:"key"`
	Options         rangeOptions    `json:"options"`
	ExpectedRequest rangeExpected   `json:"expectedRequest"`
	ResponseChunks  []rangeResponse `json:"responseChunks"`
}

type deleteScenario struct {
	Table           string         `json:"table"`
	Key             string         `json:"key"`
	Options         deleteOptions  `json:"options"`
	ExpectedRequest deleteExpected `json:"expectedRequest"`
	Response        deleteResponse `json:"response"`
}

type txnScenario struct {
	Table    string             `json:"table"`
	Compare  txnCompareScenario `json:"compare"`
	Success  []txnOpScenario    `json:"success"`
	Failure  []txnOpScenario    `json:"failure"`
	Response txnResponse        `json:"response"`
}

type memberListScenario struct {
	Cluster string             `json:"cluster"`
	Members []memberListMember `json:"members"`
}

type rangeOptions struct {
	RangeEnd     string `json:"rangeEnd"`
	Prefix       bool   `json:"prefix"`
	Serializable bool   `json:"serializable"`
	KeysOnly     bool   `json:"keysOnly"`
	CountOnly    bool   `json:"countOnly"`
	Limit        int64  `json:"limit"`
}

type putOptions struct {
	PrevKV bool `json:"prevKV"`
}

type deleteOptions struct {
	RangeEnd string `json:"rangeEnd"`
	Prefix   bool   `json:"prefix"`
	PrevKV   bool   `json:"prevKV"`
}

type rangeExpected struct {
	RangeEnd     string `json:"rangeEnd"`
	Limit        int64  `json:"limit"`
	Linearizable bool   `json:"linearizable"`
	KeysOnly     bool   `json:"keysOnly"`
	CountOnly    bool   `json:"countOnly"`
}

type putExpected struct {
	PrevKV bool `json:"prevKV"`
}

type deleteExpected struct {
	RangeEnd string `json:"rangeEnd"`
	PrevKV   bool   `json:"prevKV"`
}

type responseHeader struct {
	Revision uint64 `json:"revision"`
}

type kvPair struct {
	Key   string `json:"key"`
	Value string `json:"value"`
}

type rangeResponse struct {
	Header responseHeader `json:"header"`
	KVs    []kvPair       `json:"kvs"`
	Count  int64          `json:"count"`
}

type putResponse struct {
	Header responseHeader `json:"header"`
	PrevKV kvPair         `json:"prevKv"`
}

type deleteResponse struct {
	Header  responseHeader `json:"header"`
	Deleted int64          `json:"deleted"`
	PrevKVs []kvPair       `json:"prevKvs"`
}

type txnCompareScenario struct {
	Key    string `json:"key"`
	Value  string `json:"value"`
	Result string `json:"result"`
}

type txnOpScenario struct {
	Type   string `json:"type"`
	Key    string `json:"key"`
	Value  string `json:"value"`
	PrevKV bool   `json:"prevKV"`
}

type txnResponse struct {
	Header    responseHeader  `json:"header"`
	Succeeded bool            `json:"succeeded"`
	Responses []txnResponseOp `json:"responses"`
}

type txnResponseOp struct {
	Type    string   `json:"type"`
	PrevKV  *kvPair  `json:"prevKv"`
	Deleted int64    `json:"deleted"`
	PrevKVs []kvPair `json:"prevKvs"`
}

type memberListMember struct {
	ID         string   `json:"id"`
	Name       string   `json:"name"`
	PeerURLs   []string `json:"peerURLs"`
	ClientURLs []string `json:"clientURLs"`
}

type errorScenario struct {
	Table string `json:"table"`
	Key   string `json:"key"`
	Code  string `json:"code"`
}

func TestCoreConformance(t *testing.T) {
	fixture := loadCoreConformanceFixture(t)

	t.Run("get default", func(t *testing.T) {
		scenario := fixture.Scenarios.GetDefault
		fake, stop := NewFake(FakeResponse{Response: (&GetResponse{
			Header: &ResponseHeader{Revision: scenario.Response.Header.Revision},
			Kvs:    fixtureKVs(scenario.Response.KVs),
			Count:  scenario.Response.Count,
		}).OpResponse()})
		defer stop()

		client := fake.Client()
		defer client.Close()

		resp, err := client.Get(context.Background(), scenario.Table, scenario.Key)
		if err != nil {
			t.Fatalf("get default returned error: %v", err)
		}

		recorded := singleRecordedRequest(t, fake.RecordedRequests())
		assertEqual(t, recorded.Table, scenario.Table, "table")
		assertEqual(t, string(recorded.Request.KeyBytes()), scenario.Key, "key")
		assertEqual(t, string(recorded.Request.RangeBytes()), scenario.ExpectedRequest.RangeEnd, "range end")
		assertEqual(t, recorded.Request.limit, scenario.ExpectedRequest.Limit, "limit")
		assertEqual(t, !recorded.Request.IsSerializable(), scenario.ExpectedRequest.Linearizable, "linearizable")
		assertEqual(t, recorded.Request.IsKeysOnly(), scenario.ExpectedRequest.KeysOnly, "keysOnly")
		assertEqual(t, recorded.Request.IsCountOnly(), scenario.ExpectedRequest.CountOnly, "countOnly")

		assertEqual(t, resp.Header.Revision, scenario.Response.Header.Revision, "response revision")
		assertEqual(t, len(resp.Kvs), len(scenario.Response.KVs), "response kv count")
		assertEqual(t, string(resp.Kvs[0].Key), scenario.Response.KVs[0].Key, "response key")
		assertEqual(t, string(resp.Kvs[0].Value), scenario.Response.KVs[0].Value, "response value")
		assertEqual(t, resp.Count, scenario.Response.Count, "response count")
	})

	t.Run("get prefix keys only", func(t *testing.T) {
		scenario := fixture.Scenarios.GetPrefixKeysOnly
		fake, stop := NewFake(FakeResponse{Response: (&GetResponse{
			Header: &ResponseHeader{Revision: scenario.Response.Header.Revision},
			Kvs:    fixtureKVs(scenario.Response.KVs),
			Count:  scenario.Response.Count,
		}).OpResponse()})
		defer stop()

		client := fake.Client()
		defer client.Close()

		resp, err := client.Get(
			context.Background(),
			scenario.Table,
			scenario.Key,
			WithPrefix(),
			WithSerializable(),
			WithKeysOnly(),
			WithLimit(scenario.Options.Limit),
		)
		if err != nil {
			t.Fatalf("get prefix returned error: %v", err)
		}

		recorded := singleRecordedRequest(t, fake.RecordedRequests())
		assertEqual(t, string(recorded.Request.RangeBytes()), scenario.ExpectedRequest.RangeEnd, "range end")
		assertEqual(t, recorded.Request.limit, scenario.ExpectedRequest.Limit, "limit")
		assertEqual(t, !recorded.Request.IsSerializable(), scenario.ExpectedRequest.Linearizable, "linearizable")
		assertEqual(t, recorded.Request.IsKeysOnly(), scenario.ExpectedRequest.KeysOnly, "keysOnly")
		assertEqual(t, resp.Count, scenario.Response.Count, "response count")
	})

	t.Run("get count only range", func(t *testing.T) {
		scenario := fixture.Scenarios.GetCountOnlyRange
		fake, stop := NewFake(FakeResponse{Response: (&GetResponse{
			Header: &ResponseHeader{Revision: scenario.Response.Header.Revision},
			Kvs:    fixtureKVs(scenario.Response.KVs),
			Count:  scenario.Response.Count,
		}).OpResponse()})
		defer stop()

		client := fake.Client()
		defer client.Close()

		resp, err := client.Get(
			context.Background(),
			scenario.Table,
			scenario.Key,
			WithRange(scenario.Options.RangeEnd),
			WithCountOnly(),
		)
		if err != nil {
			t.Fatalf("get count only returned error: %v", err)
		}

		recorded := singleRecordedRequest(t, fake.RecordedRequests())
		assertEqual(t, string(recorded.Request.RangeBytes()), scenario.ExpectedRequest.RangeEnd, "range end")
		assertEqual(t, !recorded.Request.IsSerializable(), scenario.ExpectedRequest.Linearizable, "linearizable")
		assertEqual(t, recorded.Request.IsCountOnly(), scenario.ExpectedRequest.CountOnly, "countOnly")
		assertEqual(t, resp.Count, scenario.Response.Count, "response count")
		assertEqual(t, len(resp.Kvs), 0, "response kv count")
	})

	t.Run("iterate prefix", func(t *testing.T) {
		scenario := fixture.Scenarios.IteratePrefix
		chunks := iterateChunks(scenario.ResponseChunks)
		fake, stop := NewFake(FakeResponse{Response: IteratorResponse(func(yield func(response *GetResponse, err error) bool) {
			for _, chunk := range chunks {
				if !yield(chunk, nil) {
					return
				}
			}
		}).OpResponse()})
		defer stop()

		client := fake.Client()
		defer client.Close()

		iter, err := client.Iterate(
			context.Background(),
			scenario.Table,
			scenario.Key,
			WithPrefix(),
			WithSerializable(),
			WithLimit(scenario.Options.Limit),
		)
		if err != nil {
			t.Fatalf("iterate returned error: %v", err)
		}

		var responses []*GetResponse
		iter(func(response *GetResponse, err error) bool {
			if err != nil {
				t.Fatalf("iterate callback returned error: %v", err)
			}
			responses = append(responses, response)
			return true
		})

		recorded := singleRecordedRequest(t, fake.RecordedRequests())
		assertEqual(t, string(recorded.Request.RangeBytes()), scenario.ExpectedRequest.RangeEnd, "range end")
		assertEqual(t, recorded.Request.limit, scenario.ExpectedRequest.Limit, "limit")
		assertEqual(t, !recorded.Request.IsSerializable(), scenario.ExpectedRequest.Linearizable, "linearizable")
		assertEqual(t, len(responses), len(scenario.ResponseChunks), "iterate chunk count")
		assertEqual(t, string(responses[0].Kvs[0].Key), scenario.ResponseChunks[0].KVs[0].Key, "iterate first key")
		assertEqual(t, string(responses[1].Kvs[0].Key), scenario.ResponseChunks[1].KVs[0].Key, "iterate second key")
	})

	t.Run("put prev kv", func(t *testing.T) {
		scenario := fixture.Scenarios.PutPrevKV
		fake, stop := NewFake(FakeResponse{Response: (&PutResponse{
			Header: &ResponseHeader{Revision: scenario.Response.Header.Revision},
			PrevKv: &KeyValue{Key: []byte(scenario.Response.PrevKV.Key), Value: []byte(scenario.Response.PrevKV.Value)},
		}).OpResponse()})
		defer stop()

		client := fake.Client()
		defer client.Close()

		resp, err := client.Put(context.Background(), scenario.Table, scenario.Key, scenario.Value, WithPrevKV())
		if err != nil {
			t.Fatalf("put returned error: %v", err)
		}

		recorded := singleRecordedRequest(t, fake.RecordedRequests())
		assertEqual(t, recorded.Request.prevKV, scenario.ExpectedRequest.PrevKV, "prevKV")
		assertEqual(t, string(resp.PrevKv.Key), scenario.Response.PrevKV.Key, "response prev key")
		assertEqual(t, string(resp.PrevKv.Value), scenario.Response.PrevKV.Value, "response prev value")
	})

	t.Run("delete prefix prev kv", func(t *testing.T) {
		scenario := fixture.Scenarios.DeletePrefixPrev
		fake, stop := NewFake(FakeResponse{Response: (&DeleteResponse{
			Header:  &ResponseHeader{Revision: scenario.Response.Header.Revision},
			Deleted: scenario.Response.Deleted,
			PrevKvs: fixtureKVs(scenario.Response.PrevKVs),
		}).OpResponse()})
		defer stop()

		client := fake.Client()
		defer client.Close()

		resp, err := client.Delete(context.Background(), scenario.Table, scenario.Key, WithPrefix(), WithPrevKV())
		if err != nil {
			t.Fatalf("delete returned error: %v", err)
		}

		recorded := singleRecordedRequest(t, fake.RecordedRequests())
		assertEqual(t, string(recorded.Request.RangeBytes()), scenario.ExpectedRequest.RangeEnd, "range end")
		assertEqual(t, recorded.Request.prevKV, scenario.ExpectedRequest.PrevKV, "prevKV")
		assertEqual(t, resp.Deleted, scenario.Response.Deleted, "deleted count")
		assertEqual(t, string(resp.PrevKvs[0].Key), scenario.Response.PrevKVs[0].Key, "response prev key")
	})

	t.Run("delete range", func(t *testing.T) {
		scenario := fixture.Scenarios.DeleteRange
		fake, stop := NewFake(FakeResponse{Response: (&DeleteResponse{
			Header:  &ResponseHeader{Revision: scenario.Response.Header.Revision},
			Deleted: scenario.Response.Deleted,
			PrevKvs: fixtureKVs(scenario.Response.PrevKVs),
		}).OpResponse()})
		defer stop()

		client := fake.Client()
		defer client.Close()

		resp, err := client.Delete(context.Background(), scenario.Table, scenario.Key, WithRange(scenario.Options.RangeEnd))
		if err != nil {
			t.Fatalf("delete range returned error: %v", err)
		}

		recorded := singleRecordedRequest(t, fake.RecordedRequests())
		assertEqual(t, string(recorded.Request.RangeBytes()), scenario.ExpectedRequest.RangeEnd, "range end")
		assertEqual(t, recorded.Request.prevKV, scenario.ExpectedRequest.PrevKV, "prevKV")
		assertEqual(t, resp.Deleted, scenario.Response.Deleted, "deleted count")
	})

	t.Run("txn compare and ops", func(t *testing.T) {
		scenario := fixture.Scenarios.TxnCompareAndOps
		fake, stop := NewFake(FakeResponse{Response: (&TxnResponse{
			Header:    &ResponseHeader{Revision: scenario.Response.Header.Revision},
			Succeeded: scenario.Response.Succeeded,
			Responses: []*ResponseOp{
				NewResponseOp(&PutResponse{PrevKv: &KeyValue{Key: []byte(scenario.Response.Responses[0].PrevKV.Key), Value: []byte(scenario.Response.Responses[0].PrevKV.Value)}}),
				NewResponseOp(&DeleteResponse{Deleted: scenario.Response.Responses[1].Deleted, PrevKvs: fixtureKVs(scenario.Response.Responses[1].PrevKVs)}),
			},
		}).OpResponse()})
		defer stop()

		client := fake.Client()
		defer client.Close()

		resp, err := client.Txn(context.Background(), scenario.Table).
			If(Compare(Value(scenario.Compare.Key), "=", scenario.Compare.Value)).
			Then(OpPut(scenario.Success[0].Key, scenario.Success[0].Value, WithPrevKV()), OpDelete(scenario.Success[1].Key, WithPrevKV())).
			Else(OpGet(scenario.Failure[0].Key)).
			Commit()
		if err != nil {
			t.Fatalf("txn returned error: %v", err)
		}

		recorded := singleRecordedRequest(t, fake.RecordedRequests())
		assertEqual(t, len(recorded.Request.cmps), 1, "compare count")
		assertEqual(t, string(recorded.Request.cmps[0].KeyBytes()), scenario.Compare.Key, "compare key")
		assertEqual(t, string(recorded.Request.cmps[0].ValueBytes()), scenario.Compare.Value, "compare value")
		assertEqual(t, len(recorded.Request.thenOps), len(scenario.Success), "success op count")
		assertEqual(t, len(recorded.Request.elseOps), len(scenario.Failure), "failure op count")
		if !recorded.Request.thenOps[0].IsPut() || !recorded.Request.thenOps[1].IsDelete() || !recorded.Request.elseOps[0].IsGet() {
			t.Fatalf("unexpected txn op ordering")
		}
		assertEqual(t, recorded.Request.thenOps[0].prevKV, scenario.Success[0].PrevKV, "txn put prevKV")
		assertEqual(t, recorded.Request.thenOps[1].prevKV, scenario.Success[1].PrevKV, "txn delete prevKV")
		if len(resp.Responses) != len(scenario.Response.Responses) || !resp.Responses[0].IsPut() || !resp.Responses[1].IsDelete() {
			t.Fatalf("unexpected txn response ordering")
		}
		assertEqual(t, string(resp.Responses[0].Put().PrevKv.Key), scenario.Response.Responses[0].PrevKV.Key, "txn response put key")
		assertEqual(t, resp.Responses[1].Delete().Deleted, scenario.Response.Responses[1].Deleted, "txn response delete count")
	})

	t.Run("txn failure ops", func(t *testing.T) {
		scenario := fixture.Scenarios.TxnFailureOps
		fake, stop := NewFake(FakeResponse{Response: (&TxnResponse{
			Header:    &ResponseHeader{Revision: scenario.Response.Header.Revision},
			Succeeded: scenario.Response.Succeeded,
			Responses: []*ResponseOp{
				NewResponseOp(&GetResponse{}),
				NewResponseOp(&DeleteResponse{Deleted: scenario.Response.Responses[1].Deleted, PrevKvs: fixtureKVs(scenario.Response.Responses[1].PrevKVs)}),
			},
		}).OpResponse()})
		defer stop()

		client := fake.Client()
		defer client.Close()

		resp, err := client.Txn(context.Background(), scenario.Table).
			If(Compare(Value(scenario.Compare.Key), "=", scenario.Compare.Value)).
			Then(OpPut(scenario.Success[0].Key, scenario.Success[0].Value, WithPrevKV())).
			Else(OpGet(scenario.Failure[0].Key), OpDelete(scenario.Failure[1].Key, WithPrevKV())).
			Commit()
		if err != nil {
			t.Fatalf("txn failure branch returned error: %v", err)
		}

		recorded := singleRecordedRequest(t, fake.RecordedRequests())
		assertEqual(t, len(recorded.Request.thenOps), len(scenario.Success), "success op count")
		assertEqual(t, len(recorded.Request.elseOps), len(scenario.Failure), "failure op count")
		if !recorded.Request.elseOps[0].IsGet() || !recorded.Request.elseOps[1].IsDelete() {
			t.Fatalf("unexpected txn failure op ordering")
		}
		assertEqual(t, resp.Succeeded, scenario.Response.Succeeded, "txn succeeded")
		if len(resp.Responses) != len(scenario.Response.Responses) || !resp.Responses[0].IsGet() || !resp.Responses[1].IsDelete() {
			t.Fatalf("unexpected txn failure response ordering")
		}
		assertEqual(t, resp.Responses[1].Delete().Deleted, scenario.Response.Responses[1].Deleted, "txn failure delete count")
	})

	t.Run("get unavailable error", func(t *testing.T) {
		scenario := fixture.Scenarios.GetUnavailable
		fake, stop := NewFake(FakeResponse{Err: grpcstatus.Error(codes.Unavailable, "conformance unavailable")})
		defer stop()

		client := fake.Client()
		defer client.Close()

		_, err := client.Get(context.Background(), scenario.Table, scenario.Key)
		if err == nil {
			t.Fatalf("expected get unavailable error")
		}
		if grpcstatus.Code(err) != codes.Unavailable {
			t.Fatalf("unexpected gRPC code: got %v want %v", grpcstatus.Code(err), codes.Unavailable)
		}
	})

	t.Run("member list", func(t *testing.T) {
		scenario := fixture.Scenarios.MemberList
		client, stop := newFakeClusterClient(t, scenario)
		defer stop()
		defer client.Close()

		resp, err := client.MemberList(context.Background())
		if err != nil {
			t.Fatalf("member list returned error: %v", err)
		}

		assertEqual(t, resp.Cluster, scenario.Cluster, "cluster name")
		assertEqual(t, len(resp.Members), len(scenario.Members), "member count")
		assertEqual(t, resp.Members[0].Name, scenario.Members[0].Name, "member name")
		assertEqual(t, resp.Members[0].ClientURLs[0], scenario.Members[0].ClientURLs[0], "member client url")
	})
}

type fakeClusterServer struct {
	armadapb.UnimplementedClusterServer
	response *armadapb.MemberListResponse
}

func (f *fakeClusterServer) MemberList(context.Context, *armadapb.MemberListRequest) (*armadapb.MemberListResponse, error) {
	return f.response, nil
}

func newFakeClusterClient(t *testing.T, scenario memberListScenario) (*Client, func()) {
	t.Helper()

	server := grpc.NewServer()
	cluster := &fakeClusterServer{
		response: &armadapb.MemberListResponse{
			Cluster: scenario.Cluster,
			Members: func() []*armadapb.Member {
				out := make([]*armadapb.Member, 0, len(scenario.Members))
				for _, member := range scenario.Members {
					out = append(out, &armadapb.Member{
						Id:         member.ID,
						Name:       member.Name,
						PeerURLs:   member.PeerURLs,
						ClientURLs: member.ClientURLs,
					})
				}
				return out
			}(),
		},
	}
	armadapb.RegisterClusterServer(server, cluster)

	listener := bufconn.Listen(1024)
	go server.Serve(listener)

	client, err := NewFromURL(
		"fake",
		WithDialOptions(
			grpc.WithTransportCredentials(insecure.NewCredentials()),
			grpc.WithContextDialer(func(context.Context, string) (net.Conn, error) {
				return listener.Dial()
			}),
		),
	)
	if err != nil {
		t.Fatalf("failed to create cluster client: %v", err)
	}

	return client, server.Stop
}

func loadCoreConformanceFixture(t *testing.T) coreConformanceFixture {
	t.Helper()

	_, currentFile, _, ok := runtime.Caller(0)
	if !ok {
		t.Fatal("failed to resolve test file path")
	}

	fixturePath := filepath.Join(filepath.Dir(currentFile), "..", "conformance", "fixtures", "core-sdk.json")
	data, err := os.ReadFile(fixturePath)
	if err != nil {
		t.Fatalf("failed to read conformance fixture: %v", err)
	}

	var fixture coreConformanceFixture
	if err := json.Unmarshal(data, &fixture); err != nil {
		t.Fatalf("failed to decode conformance fixture: %v", err)
	}

	return fixture
}

func fixtureKVs(in []kvPair) []*KeyValue {
	out := make([]*KeyValue, 0, len(in))
	for _, kv := range in {
		out = append(out, &KeyValue{Key: []byte(kv.Key), Value: []byte(kv.Value)})
	}
	return out
}

func iterateChunks(in []rangeResponse) []*GetResponse {
	out := make([]*GetResponse, 0, len(in))
	for _, chunk := range in {
		out = append(out, &GetResponse{
			Header: &ResponseHeader{Revision: chunk.Header.Revision},
			Kvs:    fixtureKVs(chunk.KVs),
			Count:  chunk.Count,
		})
	}
	return out
}

func singleRecordedRequest(t *testing.T, requests []RecordedRequest) RecordedRequest {
	t.Helper()
	if len(requests) != 1 {
		t.Fatalf("expected 1 recorded request, got %d", len(requests))
	}
	return requests[0]
}

func assertEqual[T comparable](t *testing.T, got, want T, field string) {
	t.Helper()
	if got != want {
		t.Fatalf("%s mismatch: got %v want %v", field, got, want)
	}
}
